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
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

class ApiException(val status: Int, val code: String, message: String) : IOException(message)
data class Session(val userId: String, val access: String, val refresh: String, val expires: Long)

class Backend(private val store: SessionStore) {
    companion object {
        const val URL = "https://qsdmspylumxysczzqnqc.supabase.co"
        const val PUBLIC_KEY = "sb_publishable_EIfZYiBXr4XR5eTW6vcPxw_qPLsQs78"
        const val CALLBACK = "liquidchat://auth/callback"
    }
    private val http = OkHttpClient.Builder().connectTimeout(15,TimeUnit.SECONDS).readTimeout(25,TimeUnit.SECONDS).build()
    private val authMutex = Mutex()
    private val _session = MutableStateFlow<Session?>(null)
    val session = _session.asStateFlow()
    private val _recovery = MutableStateFlow(false)
    val recovery = _recovery.asStateFlow()
    private var restored=false
    suspend fun restore() = withContext(Dispatchers.IO) { authMutex.withLock {
        if(!restored) {
            store.read("session")?.let { runCatching { _session.value = decode(JSONObject(it)) } }
            restored=true
        }
    } }
    private fun decode(j: JSONObject) = Session(j.getJSONObject("user").getString("id"),j.getString("access_token"),j.getString("refresh_token"),j.optLong("expires_at",System.currentTimeMillis()/1000+j.optLong("expires_in",3600)))
    private fun persist(j: JSONObject) {
        val s=decode(j)
        j.put("expires_at",s.expires)
        store.write("session",j.toString())
        _session.value=s
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
        val original=_session.value ?: throw ApiException(401,"","Please sign in.")
        val current=validSession(original)
        try { raw(path,method,body,current.access) } catch(e: ApiException) {
            if(e.status!=401) throw e
            val renewed=validSession(current,force=true)
            raw(path,method,body,renewed.access)
        }.also { if(_session.value?.userId!=original.userId) throw IOException("Account changed. Please try again.") }
    }
    private suspend fun validSession(expected: Session,force: Boolean=false): Session = authMutex.withLock {
        val current=_session.value ?: throw ApiException(401,"","Please sign in.")
        if(current.userId!=expected.userId) throw IOException("Account changed.")
        if(current.expires>System.currentTimeMillis()/1000+60 && (!force || current.access!=expected.access)) return@withLock current
        val response=JSONObject(raw("/auth/v1/token?grant_type=refresh_token","POST",JSONObject().put("refresh_token",current.refresh),null))
        persist(response)
        _session.value!!
    }
    suspend fun rpc(name: String, body: JSONObject=JSONObject()) = request("/rest/v1/rpc/$name","POST",body)
    suspend fun signIn(email: String,password: String) = withContext(Dispatchers.IO) { authMutex.withLock {
        persist(JSONObject(raw("/auth/v1/token?grant_type=password","POST",JSONObject().put("email",email.trim()).put("password",password),null)))
    } }
    suspend fun register(email: String,password: String): Boolean = withContext(Dispatchers.IO) { authMutex.withLock {
        val j=JSONObject(raw("/auth/v1/signup","POST",JSONObject().put("email",email.trim()).put("password",password),null))
        if(j.has("access_token") && !j.isNull("access_token")) { persist(j); true } else false
    } }
    private fun beginPkce(purpose: String): String {
        val random=ByteArray(32).also { SecureRandom().nextBytes(it) }
        fun encode(b:ByteArray)=Base64.encodeToString(b,Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val verifier=encode(random)
        store.write("pkce",JSONObject().put("verifier",verifier).put("purpose",purpose).put("created",System.currentTimeMillis()).toString())
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
        persist(j); store.write("pkce",null); _recovery.value=pending.getString("purpose")=="recovery"
    } }
    suspend fun updatePassword(password: String) { request("/auth/v1/user","PUT",JSONObject().put("password",password)); _recovery.value=false }
    suspend fun logout() = withContext(Dispatchers.IO) { authMutex.withLock {
        val s=_session.value
        // Revoke first: if offline, retain the session and explain the retry to the user.
        if(s!=null) raw("/auth/v1/logout","POST",null,s.access)
        store.write("session",null);store.write("pkce",null);_session.value=null;_recovery.value=false
    } }
}
