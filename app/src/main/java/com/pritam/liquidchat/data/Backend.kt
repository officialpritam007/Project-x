package com.pritam.liquidchat.data

import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import java.util.UUID

class ApiException(val status: Int, val code: String, message: String) : IOException(message)
data class Session(val userId: String, val access: String, val refresh: String, val expires: Long,val email:String="")

class Backend(private val store: SessionStore, private val http:OkHttpClient = OkHttpClient.Builder().connectTimeout(15,TimeUnit.SECONDS).readTimeout(25,TimeUnit.SECONDS).build()) {
    companion object {
        const val URL = "https://qsdmspylumxysczzqnqc.supabase.co"
        const val PUBLIC_KEY = "sb_publishable_EIfZYiBXr4XR5eTW6vcPxw_qPLsQs78"
        const val CALLBACK = "liquidchat://auth/callback"
        private const val PROFILE_CACHE_SCOPE = "liquid_profile_cache_scope"
    }
    val uiReads=UiReadCache()
    private val authMutex = Mutex()
    private val deviceMutex=Mutex()
    private val _session = MutableStateFlow<Session?>(null)
    val session = _session.asStateFlow()
    private val _recovery = MutableStateFlow(false)
    val recovery = _recovery.asStateFlow()
    private var restored=false
    private val sessionMonitor=Any()
    private var sessionGeneration=0L
    private var rejectedAccess:String?=null
    private var persistedSession:JSONObject?=null
    private var profileScope:String?=null
    /** Non-secret identity of this login and its current authorization context, retained across restarts. */
    fun profileCacheScope(account:String?):String?=synchronized(sessionMonitor) {
        profileScope.takeIf {!account.isNullOrBlank()&&_session.value?.userId==account}
    }
    private data class SessionSnapshot(val session:Session,val generation:Long)
    private fun snapshot():SessionSnapshot=synchronized(sessionMonitor) {
        SessionSnapshot(_session.value ?: throw ApiException(401,"","Please sign in."),sessionGeneration)
    }
    private fun checkSession(expected:SessionSnapshot)=synchronized(sessionMonitor) {
        if(sessionGeneration!=expected.generation||_session.value?.userId!=expected.session.userId)
            throw IOException("Account changed. Please try again.")
    }
    // Refresh timestamps change every hour; permissions and the server auth-session identity must not.
    private fun authorization(access:String):String=runCatching {
        val payload=JSONObject(String(Base64.decode(access.split('.')[1],Base64.URL_SAFE or Base64.NO_WRAP),Charsets.UTF_8))
        fun canonical(value:Any?):String=when(value) {
            is JSONObject->value.keys().asSequence().sorted().joinToString(prefix="{",postfix="}") {key->JSONObject.quote(key)+":"+canonical(value.get(key))}
            is org.json.JSONArray->(0 until value.length()).joinToString(prefix="[",postfix="]") {canonical(value.get(it))}
            is String->JSONObject.quote(value)
            else->value.toString()
        }
        listOf("exp","iat","nbf","jti").forEach {payload.remove(it)}
        canonical(payload)
    }.getOrElse {access}
    private fun publishSession(s:Session,newLogin:Boolean)=synchronized(sessionMonitor) {
        if(newLogin)sessionGeneration++
        if(_session.value?.access!=s.access)rejectedAccess=null
        uiReads.activate(s.userId,authorization(s.access),force=newLogin)
        _session.value=s
    }
    private fun discardRejectedReads(expected:SessionSnapshot)=synchronized(sessionMonitor) {
        if(sessionGeneration==expected.generation&&_session.value?.access==expected.session.access&&rejectedAccess!=expected.session.access) {
            rejectedAccess=expected.session.access
            rotateVisibilityScope(expected.session)
        }
    }
    private fun rotateProfileScope()=synchronized(sessionMonitor) {
        val scope=UUID.randomUUID().toString()
        val serialized=JSONObject(checkNotNull(persistedSession).toString()).put(PROFILE_CACHE_SCOPE,scope)
        try {store.write("session",serialized.toString())}
        finally {
            // Fail closed in memory even if the device cannot persist the new context.
            profileScope=scope
            persistedSession=serialized
        }
    }
    private fun rotateVisibilityScope(current:Session)=synchronized(sessionMonitor) {
        try {rotateProfileScope()}
        finally {uiReads.activate(current.userId,authorization(current.access),force=true)}
    }
    private fun clearSession()=synchronized(sessionMonitor) {
        sessionGeneration++
        rejectedAccess=null
        persistedSession=null
        profileScope=null
        uiReads.clear()
        _session.value=null
        _recovery.value=false
    }
    suspend fun restore() = withContext(Dispatchers.IO) { authMutex.withLock {
        if(!restored) {
            store.read("session")?.let { runCatching {
                val serialized=JSONObject(it)
                val restoredSession=decode(serialized)
                synchronized(sessionMonitor) {
                    val savedScope=serialized.optString(PROFILE_CACHE_SCOPE).takeIf {value->
                        runCatching {UUID.fromString(value).toString()==value}.getOrDefault(false)
                    }
                    val scope=savedScope ?: UUID.randomUUID().toString()
                    serialized.put(PROFILE_CACHE_SCOPE,scope)
                    if(savedScope==null)store.write("session",serialized.toString())
                    persistedSession=serialized
                    profileScope=scope
                    publishSession(restoredSession,newLogin=true)
                }
            } }
            restored=true
        }
    } }
    private fun decode(j: JSONObject) = Session(j.getJSONObject("user").getString("id"),j.getString("access_token"),j.getString("refresh_token"),j.optLong("expires_at",System.currentTimeMillis()/1000+j.optLong("expires_in",3600)),j.getJSONObject("user").optString("email"))
    private fun persist(j: JSONObject,newLogin:Boolean=true) {
        val s=decode(j)
        j.put("expires_at",s.expires)
        synchronized(sessionMonitor) {
            val previous=_session.value
            val sameAuthorization=previous!=null&&previous.userId==s.userId&&authorization(previous.access)==authorization(s.access)
            val scope=profileScope?.takeIf {!newLogin&&sameAuthorization} ?: UUID.randomUUID().toString()
            j.put(PROFILE_CACHE_SCOPE,scope)
            store.write("session",j.toString())
            persistedSession=JSONObject(j.toString())
            profileScope=scope
            publishSession(s,newLogin)
        }
    }
    private fun raw(path: String, method: String, body: JSONObject?, token: String?): String {
        val builder=Request.Builder().url(URL+path).header("apikey",PUBLIC_KEY).header("Accept","application/json")
        token?.let { builder.header("Authorization","Bearer $it") }
        builder.method(method,if(method=="GET") null else (body?.toString() ?: "{}").toRequestBody("application/json".toMediaType()))
        http.newCall(builder.build()).execute().use { res ->
            val text=res.body?.string().orEmpty()
            if(!res.isSuccessful) {
                val e=runCatching { JSONObject(text) }.getOrDefault(JSONObject())
                val code=e.optString("code",e.optString("error_code"))
                val message=when {
                    code=="23505" -> "That username is already taken, or the message ID conflicts."
                    res.code==429 || code=="P0001" -> "Please wait a minute and try again."
                    res.code==401 -> "Please sign in again."
                    res.code==403 || code=="42501" -> "This operation is unavailable for this account or contact."
                    code=="email_not_confirmed" -> "Verify your email before signing in."
                    code=="invalid_credentials" -> "Email or password is incorrect."
                    code=="23514" -> "Check the username, name and message limits."
                    res.code>=500 -> "Service unavailable. Your saved content is safe. Try again later."
                    else -> "Request could not be completed. Check your details and try again."
                }
                throw ApiException(res.code,code,message)
            }
            return text
        }
    }
    suspend fun request(path: String, method: String="GET", body: JSONObject?=null): String = withContext(Dispatchers.IO) {
        val original=snapshot()
        val current=validSession(original)
        var attempted=current
        try {
            try { raw(path,method,body,current.access) } catch(e: ApiException) {
                if(e.status!=401) throw e
                discardRejectedReads(SessionSnapshot(current,original.generation))
                val renewed=validSession(SessionSnapshot(current,original.generation),force=true)
                attempted=renewed
                raw(path,method,body,renewed.access)
            }.also { checkSession(original) }
        } catch(e:ApiException) {
            if(e.status==401||e.status==403) {
                discardRejectedReads(SessionSnapshot(attempted,original.generation))
            }
            throw e
        }
    }
    private suspend fun validSession(expected: SessionSnapshot,force: Boolean=false): Session = authMutex.withLock {
        checkSession(expected)
        val current=_session.value ?: throw ApiException(401,"","Please sign in.")
        if(current.expires>System.currentTimeMillis()/1000+60 && (!force || current.access!=expected.session.access)) return@withLock current
        val response=try {
            JSONObject(raw("/auth/v1/token?grant_type=refresh_token","POST",JSONObject().put("refresh_token",current.refresh),null))
        } catch(e:ApiException) {
            if(e.status==400||e.status==401||e.status==403)discardRejectedReads(SessionSnapshot(current,expected.generation))
            throw e
        }
        persist(response,newLogin=false)
        _session.value!!
    }
    suspend fun rpc(name: String, body: JSONObject=JSONObject()):String {
        val expected=snapshot()
        val response=request("/rest/v1/rpc/$name","POST",body)
        withContext(Dispatchers.IO) {synchronized(sessionMonitor) {
            checkSession(expected)
            when(name) {
                "publish_moment","change_moment"->uiReads.invalidatePrefix(expected.session.userId,"moments:")
                "call_start","call_respond","call_hangup"->uiReads.invalidatePrefix(expected.session.userId,"calls:")
                "set_block","save_contact"->rotateVisibilityScope(_session.value!!)
                "set_avatar"->{
                    try {rotateProfileScope()}
                    finally {
                        uiReads.invalidatePrefix(expected.session.userId,"signed-image:")
                        uiReads.invalidatePrefix(expected.session.userId,"moments:")
                        uiReads.invalidatePrefix(expected.session.userId,"calls:")
                    }
                }
                "save_profile"->{
                    try {rotateProfileScope()}
                    finally {
                        uiReads.invalidatePrefix(expected.session.userId,"moments:")
                        uiReads.invalidatePrefix(expected.session.userId,"calls:")
                    }
                }
            }
        }}
        return response
    }
    suspend fun signIn(email: String,password: String) = withContext(Dispatchers.IO) { authMutex.withLock {
        persist(JSONObject(raw("/auth/v1/token?grant_type=password","POST",JSONObject().put("email",email.trim()).put("password",password),null)))
    } }
    suspend fun register(email: String,password: String): Boolean = withContext(Dispatchers.IO) { authMutex.withLock {
        val j=JSONObject(raw("/auth/v1/signup?redirect_to="+Uri.encode(CALLBACK),"POST",JSONObject().put("email",email.trim()).put("password",password).put("code_challenge",beginPkce("verification")).put("code_challenge_method","s256"),null))
        if(j.has("access_token") && !j.isNull("access_token")) { persist(j); true } else false
    } }
    private fun beginPkce(purpose: String): String {
        val random=ByteArray(32).also { SecureRandom().nextBytes(it) }
        fun encode(b:ByteArray)=Base64.encodeToString(b,Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val verifier=encode(random)
        store.write("pkce",JSONObject().put("verifier",verifier).put("purpose",purpose).put("expected_uid",_session.value?.userId.orEmpty()).put("created",System.currentTimeMillis()).toString())
        return encode(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()))
    }
    suspend fun googleUrl(): String = withContext(Dispatchers.IO) {
        Uri.parse("$URL/auth/v1/authorize").buildUpon().appendQueryParameter("provider","google").appendQueryParameter("redirect_to",CALLBACK)
            .appendQueryParameter("code_challenge",beginPkce("google")).appendQueryParameter("code_challenge_method","s256").build().toString()
    }
    suspend fun reset(email: String) = withContext(Dispatchers.IO) {
        raw("/auth/v1/recover?redirect_to="+Uri.encode(CALLBACK),"POST",JSONObject().put("email",email.trim()).put("code_challenge",beginPkce("recovery")).put("code_challenge_method","s256"),null)
        Unit
    }
    suspend fun callback(uri: Uri) = withContext(Dispatchers.IO) { authMutex.withLock {
        require(uri.scheme=="liquidchat" && uri.host=="auth" && uri.path=="/callback")
        val code=uri.getQueryParameter("code") ?: throw IOException("Sign-in was not completed. Try again.")
        val pending=JSONObject(store.read("pkce") ?: throw IOException("Start sign-in again on this device."))
        if(System.currentTimeMillis()-pending.getLong("created")>600000) throw IOException("Sign-in expired. Try again.")
        val j=JSONObject(raw("/auth/v1/token?grant_type=pkce","POST",JSONObject().put("auth_code",code).put("code_verifier",pending.getString("verifier")),null))
        val expected=pending.optString("expected_uid")
        if(expected.isNotEmpty() && decode(j).userId!=expected) {
            runCatching {raw("/auth/v1/logout","POST",null,j.getString("access_token"))}
            throw IOException("Use the Google account already signed in here.")
        }
        persist(j); store.write("pkce",null); _recovery.value=pending.getString("purpose")=="recovery"
    } }
    suspend fun updatePassword(password: String) { request("/auth/v1/user","PUT",JSONObject().put("password",password)); _recovery.value=false }
    suspend fun uploadImage(file:java.io.File,cid:String?,voice:Boolean=false,sticker:Boolean=false,document:Boolean=false,onProgress:(Int)->Unit={}):JSONObject = withContext(Dispatchers.IO) {
        require(listOf(voice,sticker,document).count {it}<=1) {"Use one media kind per upload."}
        if(document)require(cid!=null && file.isFile && file.length() in 100..8388608) {
            "Document must be a PDF under 8 MB."
        }
        if(sticker)require(cid!=null && file.isFile && file.length() in 100..524288) {
            "Sticker must be a PNG under 512 KB."
        }
        val original=snapshot()
        val current=validSession(original)
        val form=okhttp3.MultipartBody.Builder().setType(okhttp3.MultipartBody.FORM)
            .addFormDataPart("file",if(voice)"voice.m4a" else if(sticker)"sticker.png" else if(document)"document.pdf" else "photo.jpg",
                UploadProgressBody(file.asRequestBody(
                    (if(voice)"audio/mp4" else if(sticker)"image/png" else if(document)"application/pdf" else "image/jpeg").toMediaType()),onProgress))
        if(voice)form.addFormDataPart("kind","voice")
        if(sticker)form.addFormDataPart("kind","sticker")
        if(document)form.addFormDataPart("kind","document")
        if(cid!=null)form.addFormDataPart("conversation_id",cid)
        val req=Request.Builder().url(URL+"/functions/v1/media").header("apikey",PUBLIC_KEY).header("Authorization","Bearer ${current.access}").post(form.build()).build()
        http.newCall(req).execute().use {res->
            val body=res.body?.string().orEmpty()
            if(!res.isSuccessful) {
                // A 403 may be an authenticated conversation/block/quota denial,
                // not an expired auth session. Only 401 invalidates the session.
                if(res.code==401)
                    discardRejectedReads(SessionSnapshot(current,original.generation))
                throw ApiException(res.code,"",
                    if(res.code==403)
                        "Media sharing is not permitted for this chat, or the selected media format is not available yet."
                    else if(res.code==503)"Media sharing is temporarily unavailable."
                    else "Media upload failed. Try again.")
            }
            checkSession(original)
            JSONObject(body)
        }
    }
    /** Authenticated, private video-story upload. The Edge Function re-validates the MP4. */
    suspend fun uploadStoryVideo(file:java.io.File,onProgress:(Int)->Unit={}):JSONObject=withContext(Dispatchers.IO) {
        if(!file.isFile||file.length() !in 1..(8L*1024*1024))
            throw IOException("Compressed video must be 8 MB or less.")
        val original=snapshot()
        val current=validSession(original)
        val form=okhttp3.MultipartBody.Builder().setType(okhttp3.MultipartBody.FORM)
            .addFormDataPart("kind","story_video")
            .addFormDataPart("file","story.mp4",UploadProgressBody(file.asRequestBody("video/mp4".toMediaType()),onProgress))
            .build()
        val req=Request.Builder().url(URL+"/functions/v1/media")
            .header("apikey",PUBLIC_KEY)
            .header("Authorization","Bearer ${current.access}")
            .post(form).build()
        http.newCall(req).execute().use {response->
            val body=response.body?.string().orEmpty()
            checkSession(original)
            if(!response.isSuccessful) {
                // A server policy may deny an authenticated upload with 403.
                if(response.code==401)discardRejectedReads(SessionSnapshot(current,original.generation))
                throw ApiException(response.code,"",when(response.code) {
                    413->"Video is too large. Choose a shorter clip."
                    400->"Video format or duration unsupported. Choose another clip."
                    else->"Video upload failed. Your original file is safe."
                })
            }
            JSONObject(body)
        }
    }
    suspend fun registerDevice(token:String,remove:Boolean=false)=deviceMutex.withLock {
        val expected=snapshot()
        rpc("register_device",JSONObject().put("p_token",token).put("p_remove",remove))
        withContext(Dispatchers.IO) {authMutex.withLock {
            checkSession(expected)
            store.write("device_token",if(remove)null else token)
        }}
    }
    suspend fun forgetLocal()=deviceMutex.withLock {withContext(Dispatchers.IO) {authMutex.withLock {
        clearSession();store.write("session",null);store.write("pkce",null);store.write("device_token",null)
    }}}
    suspend fun logout() = deviceMutex.withLock {withContext(Dispatchers.IO) {
        val original=_session.value?.let {snapshot()}
        val expected=original?.let {validSession(it)}
        authMutex.withLock {
            original?.let {checkSession(it)}
            if(expected!=null && _session.value?.userId==expected.userId) {
                store.read("device_token")?.let {token->raw("/rest/v1/rpc/register_device","POST",JSONObject().put("p_token",token).put("p_remove",true),expected.access)}
                raw("/auth/v1/logout","POST",null,expected.access)
            }
            clearSession();store.write("session",null);store.write("pkce",null);store.write("device_token",null)
        }
    }}
}
