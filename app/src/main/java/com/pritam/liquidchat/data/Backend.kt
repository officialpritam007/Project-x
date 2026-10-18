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
    suspend fun uploadImage(file:java.io.File,cid:String?,voice:Boolean=false):JSONObject = withContext(Dispatchers.IO) {
        val original=snapshot()
        val current=validSession(original)
        val form=okhttp3.MultipartBody.Builder().setType(okhttp3.MultipartBody.FORM)
            .addFormDataPart("file",if(voice)"voice.m4a" else "photo.jpg",file.asRequestBody((if(voice)"audio/mp4" else "image/jpeg").toMediaType()))
        if(voice)form.addFormDataPart("kind","voice")
        if(cid!=null)form.addFormDataPart("conversation_id",cid)
        val req=Request.Builder().url(URL+"/functions/v1/media").header("apikey",PUBLIC_KEY).header("Authorization","Bearer ${current.access}").post(form.build()).build()
        http.newCall(req).execute().use {res->
            val body=res.body?.string().orEmpty()
            if(!res.isSuccessful) {
                if(res.code==401||res.code==403)discardRejectedReads(SessionSnapshot(current,original.generation))
                throw ApiException(res.code,"",if(res.code==503)"Media sharing is temporarily unavailable." else "Media upload failed. Try again.")
            }
            checkSession(original)
            JSONObject(body)
        }
    }
    /** Authenticated, private video-story upload. The Edge Function re-validates the MP4. */
    suspend fun uploadStoryVideo(file:java.io.File):JSONObject=withContext(Dispatchers.IO) {
        if(!file.isFile||file.length() !in 1..(8L*1024*1024))
            throw IOException("Compressed video must be 8 MB or less.")
        val original=snapshot()
        val current=validSession(original)
        val form=okhttp3.MultipartBody.Builder().setType(okhttp3.MultipartBody.FORM)
            .addFormDataPart("kind","story_video")
            .addFormDataPart("file","story.mp4",file.asRequestBody("video/mp4".toMediaType()))
            .build()
        val req=Request.Builder().url(URL+"/functions/v1/media")
            .header("apikey",PUBLIC_KEY)
            .header("Authorization","Bearer ${current.access}")
            .post(form).build()
        http.newCall(req).execute().use {response->
            val body=response.body?.string().orEmpty()
            checkSession(original)
            if(!response.isSuccessful) {
                if(response.code==401||response.code==403)discardRejectedReads(SessionSnapshot(current,original.generation))
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
            ori…3916 tokens truncated…+
        },initialStoryId=selectedMyStoryId,openEditorInitially=addStoryImmediately)
}
@Composable fun ContactsScreen(model:AppViewModel,chats:List<Chat>,busy:Boolean) {
    var username by rememberSaveable {mutableStateOf("")}
    var showFind by rememberSaveable {mutableStateOf(false)}
    var alphabetical by rememberSaveable {mutableStateOf(true)}
    val context=LocalContext.current
    val contacts by model.contacts.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {model.action {model.repository.loadContacts()}}
    val found by model.found.collectAsStateWithLifecycle()
    val filtered=remember(contacts,username,alphabetical) {contacts.filter {username.isBlank()||it.name.contains(username,true)||it.username.contains(username,true)}.let {if(alphabetical)it.sortedBy {c->c.name.lowercase()} else it.reversed()}}
    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().padding(horizontal=12.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        GlassHeader("Contacts") {IconButton(onClick={alphabetical=!alphabetical}) {Icon(Icons.Outlined.SortByAlpha,"Change contact sort order")}}
        GlassSearch(username,{username=it.lowercase().take(24)},"Search Contacts")
        GlassCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().heightIn(min=46.dp).clickable {
                val intent=android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT,"Find me on Liquid Chat: @${model.profile.value?.optString("username").orEmpty()}")
                context.startActivity(android.content.Intent.createChooser(intent,"Invite Friends"))
            },verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(34.dp).background(Color(0xFF00A6E8),CircleShape),contentAlignment=Alignment.Center){Text("+",color=Color.White,fontWeight=FontWeight.Bold,fontSize=20.sp)}
                Text("Invite Friends",fontSize=18.sp,fontWeight=FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth().heightIn(min=46.dp).clickable {showFind=true},verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(34.dp).background(Color(0xFF11C748),CircleShape),contentAlignment=Alignment.Center){Text("↗",color=Color.White,fontSize=22.sp)}
                Text("Find Friends",fontSize=18.sp,fontWeight=FontWeight.Bold)
            }
        }
        LazyColumn(contentPadding=PaddingValues(bottom=140.dp+WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            item {Text(if(alphabetical)"Sorted by name" else "Reverse name order",Modifier.padding(horizontal=16.dp),color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold,fontSize=15.sp)}
            if(filtered.isEmpty()) item {GlassCard(Modifier.fillMaxWidth()){Text(if(username.isBlank())"Find someone by username and save them here." else "No saved contacts match.")}}
            items(filtered,key={"contact-${it.id}"}) {contact->
                GlassSurface(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                        Row(Modifier.weight(1f).clickable {val chat=chats.firstOrNull {it.peer==contact.id};if(chat!=null)model.open(chat.id) else model.start(contact.id)},verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            Avatar(contact.name,contact.avatarId,model.backend,size=43.dp)
                            Column {Text(contact.name,fontSize=17.sp,fontWeight=FontWeight.Bold);Text("@${contact.username}",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                        }
                        IconButton(onClick={model.action {model.repository.saveContact(contact.id,false)}},enabled=!busy) {Icon(Icons.Outlined.PersonRemove,"Remove saved contact")}
                    }
                }
            }
        }
    }
    GlassSurface(Modifier.align(Alignment.BottomEnd).padding(end=17.dp,bottom=78.dp+WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()).size(52.dp),CircleShape) {IconButton(onClick={showFind=true},modifier=Modifier.fillMaxSize()) {Icon(Icons.Outlined.Add,"Add contact",tint=MaterialTheme.colorScheme.primary)}}
    }
    if(showFind) AlertDialog(onDismissRequest={showFind=false},title={Text("Find Friends")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Find someone by their exact username.")
        OutlinedTextField(username,{username=it.lowercase().take(24)},label={Text("Username")},singleLine=true)
        Button(onClick={model.find(username)},enabled=!busy&&username.matches(Regex("[a-z0-9_]{3,24}"))) {Text("Find contact")}
        found?.takeIf {it.optString("username").equals(username,true)}?.let {p->
            Text(p.optString("display_name"),fontWeight=FontWeight.Bold);Text("@${p.optString("username")}")
            Row {TextButton(onClick={showFind=false;model.start(p.getString("id"))},enabled=!busy){Text("Start chat")};TextButton(onClick={model.action {model.repository.saveContact(p.getString("id"),true)}},enabled=!busy){Text("Save contact")}}
        }
    }},confirmButton={TextButton(onClick={showFind=false}){Text("Done")}})
}
@Composable fun ConversationScreen(model:AppViewModel,cid:String,chats:List<Chat>,busy:Boolean) {
    val chat=chats.firstOrNull {it.id==cid}
    val allMessages by model.messages.collectAsStateWithLifecycle()
    val session by model.session.collectAsStateWithLifecycle()
    val messagesReady by model.messagesReady.collectAsStateWithLifecycle()
    val messages=remember(allMessages,cid,session?.userId) {allMessages.filter {it.conversation==cid&&it.account==session?.userId}}
    val foreground by model.repository.foreground.collectAsStateWithLifecycle()
    val focus by model.focusMessage.collectAsStateWithLifecycle()
    val contact by model.repository.contactState.collectAsStateWithLifecycle()
    var input by remember(cid) {mutableStateOf(TextFieldValue(""))}
    var draftReady by remember(cid) {mutableStateOf(false)}
    var queueing by remember(cid) {mutableStateOf(false)}
    var draftVersion by remember(cid) {mutableLongStateOf(0L)}
    var inputFocused by remember(cid) {mutableStateOf(false)}
    var emojiPicker by remember(cid) {mutableStateOf(false)}
    var palette by remember(cid) {mutableStateOf("Ice")}
    var appearance by remember(cid) {mutableStateOf(false)}
    var searchOpen by remember(cid) {mutableStateOf(false)}
    var chatQuery by remember(cid) {mutableStateOf("")}
    var searchIndex by remember(cid) {mutableIntStateOf(0)}
    val chatResults=remember(messages,chatQuery) {if(chatQuery.isBlank())emptyList() else messages.filter {it.body.contains(chatQuery,true)}}
    var actionMessage by remember(cid) {mutableStateOf<Message?>(null)}
    val clipboard=LocalClipboardManager.current
    var replyTo by remember(cid) {mutableStateOf("")}
    var editMessage by remember(cid) {mutableStateOf<Message?>(null)}
    var editBody by remember(cid) {mutableStateOf("")}
    var deleteMessage by remember(cid) {mutableStateOf<Message?>(null)}
    var starsOpen by remember(cid) {mutableStateOf(false)}
    val body=input.text
    fun updateInput(value:TextFieldValue) {val changed=input.text!=value.text;input=value;if(changed)draftVersion=model.repository.saveDraft(cid,value.text,replyTo)}
    LaunchedEffect(cid) {
        val draft=model.repository.draft(cid)
        input=TextFieldValue(draft?.body.orEmpty(),TextRange(draft?.body?.length ?: 0));replyTo=draft?.replyTo.orEmpty();draftVersion=draft?.updatedAt ?: 0;draftReady=true
        palette=model.repository.palette(cid)
    }
    val context=LocalContext.current
    var requestedCallKind by remember(cid) {mutableStateOf("audio")}
    val callPermissionLauncher=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {granted->
        val needed=if(requestedCallKind=="video")listOf(android.Manifest.permission.RECORD_AUDIO,android.Manifest.permission.CAMERA) else listOf(android.Manifest.permission.RECORD_AUDIO)
        if(needed.all {granted[it]==true})chat?.let {model.calls.start(cid,it.peer,requestedCallKind)}
    }
    fun requestCall(kind:String) {
        val needed=if(kind=="video")arrayOf(android.Manifest.permission.RECORD_AUDIO,android.Manifest.permission.CAMERA) else arrayOf(android.Manifest.permission.RECORD_AUDIO)
        requestedCallKind=kind
        if(needed.all {androidx.core.content.ContextCompat.checkSelfPermission(context,it)==android.content.pm.PackageManager.PERMISSION_GRANTED})
            chat?.let {model.calls.start(cid,it.peer,kind)}
        else callPermissionLauncher.launch(needed)
    }
    var prepared by remember(cid) {mutableStateOf<Images.Prepared?>(null)}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {uri->if(uri!=null)model.action {
        val account=model.backend.session.value?.userId ?: return@action
        val image=Images.prepare(context,uri,account)
        if(model.backend.session.value?.userId==account)prepared=image else image.file.delete()
    }}
    var attachments by remember(cid) {mutableStateOf(false)}
    var cameraPath by rememberSaveable(cid) {mutableStateOf("")}
    var cameraAccount by rememberSaveable(cid) {mutableStateOf("")}
    val camera=rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {ok->
        val file=java.io.File(cameraPath)
        if(ok)model.action {
            try {
                if(model.backend.session.value?.userId==cameraAccount) {
                    val uri=androidx.core.content.FileProvider.getUriForFile(context,context.packageName+".files",file)
                    val image=Images.prepare(context,uri,cameraAccount)
                    if(model.backend.session.value?.userId==cameraAccount)prepared=image else image.file.delete()
                }
            } finally {file.delete();cameraPath=""}
        } else {file.delete();cameraPath=""}
    }
    val display=LocalDisplayOptions.current
    val sendScale by androidx.compose.animation.core.animateFloatAsState(if(body.isNotBlank())1.04f else 1f,animationSpec=if(display.reducedMotion)androidx.compose.animation.core.tween(0) else androidx.compose.animation.core.spring(stiffness=when(display.animation){"Normal"->400f;"Extra"->250f;else->650f}),label="Send readiness")
    var menu by remember {mutableStateOf(false)}
    var confirmDelete by remember {mutableStateOf(false)}
    var report by remember {mutableStateOf(false)}
    var reportMessage by remember {mutableStateOf<Message?>(null)}
    var details by remember {mutableStateOf(false)}
    BackHandler {model.closeChat()}
    // Read acknowledgments are emitted only after a foreground conversation frame exists.
    LaunchedEffect(cid,messages.lastOrNull()?.id,foreground) {
        if(foreground) {withFrameNanos { };runCatching {model.repository.acknowledge(cid,true)}}
    }
    LaunchedEffect(cid,foreground) {
        var lastSent=0L
        if(foreground) try {
            snapshotFlow {if(inputFocused&&draftReady)input.text else ""}.drop(1).collectLatest { text ->
                val keyTime=android.os.SystemClock.elapsedRealtime()
                if(text.isBlank()) runCatching {model.repository.typing(cid,false)}
                else {
                    kotlinx.coroutines.delay(maxOf(0L,2500-(keyTime-lastSent)))
                    runCatching {model.repository.typing(cid,true)}
                    lastSent=android.os.SystemClock.elapsedRealtime()
                    kotlinx.coroutines.delay(maxOf(0L,4500-(lastSent-keyTime)))
                    runCatching {model.repository.typing(cid,false)}
                }
            }
        } finally {kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {runCatching {model.repository.typing(cid,false)}}}
    }
    Box(Modifier.fillMaxSize()) {
    Box(Modifier.matchParentSize().glassSource(.5f).background(wallpaper(palette,LocalGlassDark.current)))
    Column(Modifier.fillMaxSize().imePadding()) {
        GlassSurface(Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=3.dp),Glass.capsule) {
        Row(Modifier.fillMaxWidth().padding(horizontal=4.dp,vertical=2.dp),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick=model::closeChat) {Icon(Icons.AutoMirrored.Outlined.ArrowBack,"Back to chats")}
            if(chat!=null) Box(Modifier.clickable {details=true}) {Avatar(chat.name,chat.avatarId,model.backend)}
            Column(Modifier.weight(1f).padding(horizontal=8.dp).clickable {details=true}) {Text(chat?.name ?: "Conversation",maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.titleMedium);Text(when {contact.optBoolean("online")->"Online";!contact.isNull("last_seen")&&contact.has("last_seen")->"Last seen "+runCatching {java.time.Instant.parse(contact.getString("last_seen")).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("dd MMM, HH:mm"))}.getOrDefault("");else->chat?.username?.let {"@$it"}.orEmpty()},style=MaterialTheme.typography.bodySmall)}
            if(chat!=null && com.pritam.liquidchat.BuildConfig.ENABLE_EXPERIMENTAL_CALLS) {
                IconButton(onClick={requestCall("audio")}) {Icon(Icons.Outlined.Call,"Start voice call")}
                IconButton(onClick={requestCall("video")}) {Icon(Icons.Outlined.Videocam,"Start video call")}
            }
            Box {IconButton(onClick={menu=true}) {Icon(Icons.Outlined.MoreVert,"Conversation options")}
                DropdownMenu(expanded=menu,onDismissRequest={menu=false}) {
                    chat?.let { c->
                        DropdownMenuItem(text={Text("Contact details & shared media")},onClick={menu=false;details=true})
                        DropdownMenuItem(text={Text("Search conversation")},onClick={menu=false;searchOpen=true})
                        DropdownMenuItem(text={Text("Starred messages")},onClick={menu=false;starsOpen=true})
                        DropdownMenuItem(text={Text(if(c.pinnedAt>0)"Unpin" else "Pin")},onClick={menu=false;model.action {model.repository.chatAction(c.id,"pin",c.pinnedAt==0L)}})
                        DropdownMenuItem(text={Text("Wallpaper")},onClick={menu=false;appearance=true})
                        DropdownMenuItem(text={Text(if(c.favorite)"Remove favorite" else "Favorite")},onClick={menu=false;model.updateChat(c,favorite=!c.favorite)})
                        DropdownMenuItem(text={Text(if(c.archived)"Unarchive" else "Archive")},onClick={menu=false;model.updateChat(c,archived=!c.archived)})
                        DropdownMenuItem(text={Text(if(c.muted)"Unmute" else "Mute")},onClick={menu=false;model.updateChat(c,muted=!c.muted)})
                        DropdownMenuItem(text={Text("Block contact")},onClick={menu=false;model.block(c.peer,true)})
                        DropdownMenuItem(text={Text("Unblock contact")},onClick={menu=false;model.block(c.peer,false)})
                        DropdownMenuItem(text={Text("Report contact")},onClick={menu=false;reportMessage=null;report=true})
                        DropdownMenuItem(text={Text("Delete history for me")},onClick={menu=false;confirmDelete=true})
                    }
                }
            }
        }
        }
        if(searchOpen) GlassSurface(Modifier.fillMaxWidth().padding(horizontal=8.dp)) {
            Column(Modifier.padding(8.dp)) {
                OutlinedTextField(chatQuery,{chatQuery=it;searchIndex=0},placeholder={Text("Search this chat")},singleLine=true,modifier=Modifier.fillMaxWidth(),trailingIcon={IconButton(onClick={searchOpen=false;chatQuery=""}) {Icon(Icons.Outlined.Close,"Close search")}})
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("Downloaded messages only · ${chatResults.size} matches",Modifier.weight(1f),style=MaterialTheme.typography.labelSmall)
                    IconButton(onClick={if(chatResults.isNotEmpty()){searchIndex=(searchIndex-1+chatResults.size)%chatResults.size;model.focusMessage.value=chatResults[searchIndex].id}},enabled=chatResults.isNotEmpty()) {Icon(Icons.Outlined.KeyboardArrowUp,"Previous match")}
                    IconButton(onClick={if(chatResults.isNotEmpty()){model.focusMessage.value=chatResults[searchIndex%chatResults.size].id;searchIndex=(searchIndex+1)%chatResults.size}},enabled=chatResults.isNotEmpty()) {Icon(Icons.Outlined.KeyboardArrowDown,"Next match")}
                }
            }
        }
        if(messagesReady!=cid)Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center) {CircularProgressIndicator(Modifier.size(24.dp))}
        else MessageTimeline(cid,messages,contact.optBoolean("typing"),focus,{model.focusMessage.value=null},loadOlder={model.repository.loadOlder(cid)},modifier=Modifier.weight(1f).fillMaxWidth()) {m->
            MessageBubble(m,chat,model,onRetry={model.retry(m.id)},highlight=m.id==focus,onLongClick={actionMessage=m},onReply={replyTo=m.id;draftVersion=model.repository.saveDraft(cid,body,replyTo)},quoted=messages.firstOrNull {it.id==m.replyTo},onQuoteClick={if(m.replyTo.isNotEmpty())model.focusMessage.value=m.replyTo})
        }
        if(body.length>=3800)Text("${body.length} / 4000",Modifier.align(Alignment.End).padding(end=20.dp),style=MaterialTheme.typography.labelSmall)
        if(replyTo.isNotEmpty()) GlassSurface(Modifier.fillMaxWidth().padding(horizontal=12.dp),Glass.compact) {
            Row(Modifier.padding(start=14.dp),verticalAlignment=Alignment.CenterVertically) {
                val original=messages.firstOrNull {it.id==replyTo}
                Column(Modifier.weight(1f)) {Text("Replying to ${if(original?.sender==original?.account)"yourself" else chat?.name.orEmpty()}",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary);Text(original?.takeIf {it.deletedAt.isEmpty()}?.body ?: "Original message unavailable",maxLines=1,overflow=TextOverflow.Ellipsis)}
                IconButton(onClick={replyTo="";draftVersion=model.repository.saveDraft(cid,body)}) {Icon(Icons.Outlined.Close,"Cancel reply")}
            }
        }
        GlassSurface(Modifier.fillMaxWidth().padding(8.dp),Glass.capsule) {
            Row(Modifier.fillMaxWidth().padding(4.dp),verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                IconButton(onClick={emojiPicker=true},enabled=draftReady) {Icon(Icons.Outlined.EmojiEmotions,"Open emoji picker")}
                androidx.compose.foundation.text.BasicTextField(input,{if(it.text.length<=4000)updateInput(it)},enabled=draftReady,keyboardOptions=KeyboardOptions(capitalization=KeyboardCapitalization.Sentences,keyboardType=KeyboardType.Text),modifier=Modifier.weight(1f).heightIn(min=48.dp,max=140.dp).padding(vertical=14.dp).onFocusChanged {inputFocused=it.isFocused}.testTag("message-input").semantics {contentDescription="Message"},textStyle=MaterialTheme.typography.bodyLarge.copy(color=MaterialTheme.colorScheme.onSurface),maxLines=5,decorationBox={inner->Box {if(body.isEmpty())Text("Message",color=MaterialTheme.colorScheme.onSurfaceVariant);inner()}})
                IconButton(onClick={attachments=true},enabled=!busy&&replyTo.isEmpty()) {Icon(Icons.Outlined.AttachFile,"Attach image")}
                if(body.isBlank()&&replyTo.isEmpty())VoiceCapture(model,cid,busy) else FilledIconButton(onClick={if(body.isNotBlank()&&!queueing){
                    val submittedBody=body
                    val submittedReply=replyTo
                    val submittedVersion=draftVersion
                    queueing=true
                    model.send(cid,submittedBody,submittedReply,submittedVersion){
                        // Preserve any new draft typed while the previous message was being queued.
                        if(draftVersion==submittedVersion&&input.text==submittedBody&&replyTo==submittedReply){input=TextFieldValue("");replyTo=""}
                    }.invokeOnCompletion {queueing=false}
                }},enabled=body.isNotBlank()&&!queueing&&draftReady,modifier=Modifier.size(48.dp).graphicsLayer {scaleX=sendScale;scaleY=sendScale}) {Icon(Icons.AutoMirrored.Outlined.Send,"Send message")}
            }
        }
    }

    }
    if(attachments)AlertDialog(onDismissRequest={attachments=false},title={Text("Attach")},text={Column {
        TextButton(onClick={attachments=false;picker.launch("image/*")}) {Icon(Icons.Outlined.PhotoLibrary,null);Spacer(Modifier.width(12.dp));Text("Gallery")}
        TextButton(onClick={attachments=false
            val account=model.backend.session.value?.userId
            if(account!=null) {
                cameraAccount=account
                val file=java.io.File(context.cacheDir,"camera/${java.util.UUID.randomUUID()}.jpg");file.parentFile?.mkdirs();cameraPath=file.absolutePath
                runCatching {camera.launch(androidx.core.content.FileProvider.getUriForFile(context,context.packageName+".files",file))}.onFailure {file.delete();cameraPath="";model.action {throw java.io.IOException("No camera app is available. Choose Gallery.")}}
            }
        }) {Icon(Icons.Outlined.PhotoCamera,null);Spacer(Modifier.width(12.dp));Text("Camera")}
    }},confirmButton={TextButton(onClick={attachments=false}) {Text("Cancel")}})
    if(emojiPicker)EmojiPicker(onEmoji={emoji->
        val start=minOf(input.selection.start,input.selection.end);val end=maxOf(input.selection.start,input.selection.end)
        val text=body.replaceRange(start,end,emoji)
        if(text.length<=4000)updateInput(TextFieldValue(text,TextRange(start+emoji.length)))
    },onDismiss={emojiPicker=false})
    if(appearance)AlertDialog(onDismissRequest={appearance=false},title={Text("Chat wallpaper")},text={Column {listOf("Ice","Lilac","Mint","Sand").forEach {choice->TextButton(onClick={model.action {model.repository.setPalette(cid,choice);palette=choice;appearance=false}}) {Text(if(choice==palette)"✓ $choice" else choice)}}}},confirmButton={TextButton(onClick={appearance=false}) {Text("Done")}})
    actionMessage?.let {m->AlertDialog(onDismissRequest={actionMessage=null},title={Text("Message")},text={Column(Modifier.verticalScroll(rememberScrollState())) {
        if(m.deletedAt.isEmpty()) {
            if(m.status=="SENT") {
                Row(Modifier.horizontalScroll(rememberScrollState())) {listOf("👍","❤️","😂","😮","😢","🙏").forEach {emoji->TextButton(onClick={actionMessage=null;model.action {val old=org.json.JSONObject(m.reactions).optString(m.account);model.repository.messageAction(m.id,"react",if(old==emoji)"" else emoji)}}) {Text(emoji)}}}
                TextButton(onClick={replyTo=m.id;draftVersion=model.repository.saveDraft(cid,body,replyTo);actionMessage=null}) {Text("Reply")}
                TextButton(onClick={actionMessage=null;model.action {model.repository.messageAction(m.id,"star",(!m.starred).toString())}}) {Text(if(m.starred)"Unstar" else "Star")}
            }
            if(m.status=="SENT"&&m.sender!=m.account)TextButton(onClick={reportMessage=m;actionMessage=null;report=true}) {Text("Report message")}
            TextButton(onClick={clipboard.setText(AnnotatedString(m.body));actionMessage=null}) {Text("Copy text")}
            TextButton(onClick={actionMessage=null;context.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT,m.body),"Share message"))}) {Text("Share text")}
            val age=runCatching {java.time.Duration.between(java.time.Instant.parse(m.serverTime),java.time.Instant.now()).seconds}.getOrDefault(Long.MAX_VALUE)
            if(m.sender==m.account&&m.status=="SENT"&&m.mediaId.isEmpty()&&age<=900)TextButton(onClick={editMessage=m;editBody=m.body;actionMessage=null}) {Text("Edit")}
        }
        if(m.status in listOf("SENT","QUEUED","FAILED"))TextButton(onClick={deleteMessage=m;actionMessage=null}) {Text("Delete")}
        Text("${if(m.sender==m.account)"You" else chat?.name.orEmpty()} · ${java.time.Instant.ofEpochMilli(m.localTime).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))}",style=MaterialTheme.typography.bodySmall)
    }},confirmButton={TextButton(onClick={actionMessage=null}) {Text("Done")}})}
    editMessage?.let {m->AlertDialog(onDismissRequest={editMessage=null},title={Text("Edit message")},text={OutlinedTextField(editBody,{if(it.length<=4000)editBody=it},keyboardOptions=KeyboardOptions(capitalization=KeyboardCapitalization.Sentences),modifier=Modifier.fillMaxWidth())},confirmButton={TextButton(onClick={model.action {model.repository.messageAction(m.id,"edit",editBody);editMessage=null}},enabled=!busy&&editBody.isNotBlank()) {Text("Save")}},dismissButton={TextButton(onClick={editMessage=null}) {Text("Cancel")}})}
    deleteMessage?.let {m->AlertDialog(onDismissRequest={deleteMessage=null},title={Text("Delete message?")},text={Column {
        Text(if(m.status=="SENT")"Delete for me hides this message on your devices. The other person keeps their copy. Saved copies and screenshots cannot be erased." else "Remove this unsent message and its local attachment from this device.")
        val age=runCatching {java.time.Duration.between(java.time.Instant.parse(m.serverTime),java.time.Instant.now()).seconds}.getOrDefault(Long.MAX_VALUE)
        if(m.sender==m.account&&age<=172800&&m.deletedAt.isEmpty())TextButton(onClick={model.action {model.repository.messageAction(m.id,"delete");deleteMessage=null}},enabled=!busy) {Text("Delete for everyone")}
    }},confirmButton={TextButton(onClick={model.action {if(m.status=="SENT")model.repository.messageAction(m.id,"hide") else model.repository.discardUnsent(m.id);deleteMessage=null}},enabled=!busy) {Text(if(m.status=="SENT")"Delete for me" else "Remove unsent")}},dismissButton={TextButton(onClick={deleteMessage=null}) {Text("Cancel")}})}
    if(starsOpen&&chat!=null)ContactMediaDialog(model,chat,messages,initialKind="star") {starsOpen=false}
    if(details&&chat!=null) ContactMediaDialog(model,chat,messages) {details=false}
    if(report&&chat!=null)ReportContentDialog(if(reportMessage==null)"Report contact" else "Report message",if(reportMessage==null)"Your explanation will be sent privately for review." else "Your explanation and this message's reference will be sent privately for review.",onSubmit={reason->model.report(chat.peer,reason,reportMessage?.let {listOf(it.id)} ?: emptyList());report=false;reportMessage=null},onDismiss={report=false;reportMessage=null})
    prepared?.let {image->AlertDialog(onDismissRequest={image.file.delete();prepared=null},title={Text("Send image")},text={Column {
        coil.compose.AsyncImage(image.file,"Image preview",Modifier.fillMaxWidth().heightIn(max=260.dp).aspectRatio(image.width.toFloat()/image.height))
        OutlinedTextField(input,{if(it.text.length<=4000)updateInput(it)},keyboardOptions=KeyboardOptions(capitalization=KeyboardCapitalization.Sentences,keyboardType=KeyboardType.Text),label={Text("Caption (optional)")},modifier=Modifier.fillMaxWidth())
    }},confirmButton={TextButton(onClick={val caption=body;val version=draftVersion;model.action {model.repository.sendImage(cid,image,caption,version);prepared=null;if(draftVersion==version){input=TextFieldValue("");replyTo=""}}},enabled=!busy) {Text("Send")}},dismissButton={TextButton(onClick={image.file.delete();prepared=null}) {Text("Cancel")}})}
    if(confirmDelete&&chat!=null) AlertDialog(onDismissRequest={confirmDelete=false},title={Text("Delete your chat history?")},text={Text("This hides existing history for your account on every device. The other person keeps their copy. Unsent local copies will be removed; messages already sending may still finish.")},confirmButton={TextButton(onClick={confirmDelete=false;model.updateChat(chat,delete=true)}) {Text("Delete for me")}},dismissButton={TextButton(onClick={confirmDelete=false}) {Text("Cancel")}})
}
@OptIn(ExperimentalFoundationApi::class)
@Composable internal fun MessageBubble(message:Message,chat:Chat?,model:AppViewModel,onRetry:()->Unit,highlight:Boolean,onLongClick:()->Unit={},onReply:()->Unit={},quoted:Message?=null,onQuoteClick:()->Unit={}) {
    val mine=message.sender==message.account
    var swipe by remember(message.id) {mutableFloatStateOf(0f)}
    val threshold=with(LocalDensity.current) {72.dp.toPx()}
    val replyCallback by rememberUpdatedState(onReply)
    val rtl=LocalLayoutDirection.current==LayoutDirection.Rtl
    val haptic=LocalHapticFeedback.current
    var crossed by remember(message.id) {mutableStateOf(false)}
    val drag=rememberDraggableState {delta->
        swipe=(swipe+if(rtl)-delta else delta).coerceIn(0f,threshold*1.3f)
        if(swipe>=threshold&&!crossed) {crossed=true;haptic.performHapticFeedback(HapticFeedbackType.LongPress)}
    }
    var expanded by remember(message.id) {mutableStateOf(false)}
    var truncated by remember(message.id) {mutableStateOf(false)}
    BoxWithConstraints(Modifier.fillMaxWidth()) {
    val bubbleWidth=minOf(maxWidth*.86f,480.dp)
    Row(Modifier.fillMaxWidth().offset {androidx.compose.ui.unit.IntOffset((if(rtl)-swipe else swipe).toInt(),0)}.draggable(drag,Orientation.Horizontal,enabled=message.status=="SENT"&&message.deletedAt.isEmpty(),onDragStopped={if(swipe>=threshold)replyCallback();swipe=0f;crossed=false}),horizontalArrangement=if(mine)Arrangement.End else Arrangement.Start) {
        Surface(shape=androidx.compose.foundation.shape.RoundedCornerShape(21.dp),color=if(highlight)MaterialTheme.colorScheme.tertiaryContainer else if(LocalGlassDark.current) {if(mine)Color(0xFF204F65) else Color(0xFF263C4A)} else if(mine)Color(0xFFD6F1FD) else Color(0xFFF8FCFF),border=androidx.compose.foundation.BorderStroke(1.dp,Color.White.copy(alpha=if(LocalGlassDark.current).16f else .8f)),shadowElevation=2.dp,modifier=Modifier.widthIn(max=bubbleWidth).glassSource().combinedClickable(onClick={},onLongClick=onLongClick)) {
            Column(Modifier.padding(horizontal=11.dp,vertical=9.dp)) {
                if(message.replyTo.isNotEmpty()&&message.deletedAt.isEmpty()) Surface(shape=Glass.compact,color=MaterialTheme.colorScheme.primary.copy(alpha=.1f),modifier=Modifier.fillMaxWidth().clickable(onClick=onQuoteClick)) {
                    Column(Modifier.padding(8.dp)) {Text("Reply",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary);Text(quoted?.takeIf {it.deletedAt.isEmpty()}?.body ?: "Original message unavailable",maxLines=2,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodySmall)}
                }
                if(message.mediaId.isNotEmpty()||message.localImage.isNotEmpty()) {
                    if(message.mediaKind=="voice")VoicePlayer(model,message) else SharedImagePreview(model,message)
                    Spacer(Modifier.height(8.dp))
                }
                if(message.mediaKind!="voice")Text(message.body,style=MaterialTheme.typography.bodyLarge,maxLines=if(expanded)Int.MAX_VALUE else 12,overflow=TextOverflow.Ellipsis,onTextLayout={truncated=it.hasVisualOverflow})
                if(truncated||expanded) TextButton(onClick={expanded=!expanded}) {Text(if(expanded)"Show less" else "Read more")}
                val time=remember(message.localTime) {java.time.Instant.ofEpochMilli(message.localTime).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}
                Row(Modifier.align(Alignment.End),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    if(message.starred)Icon(Icons.Outlined.Star,"Starred",Modifier.size(12.dp))
                    if(message.editedAt.isNotEmpty()&&message.deletedAt.isEmpty())Text("edited",style=MaterialTheme.typography.labelSmall)
                    Text(time,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    if(mine)Receipt(message.status,message.seq,chat?.peerDelivered ?: 0,chat?.peerRead ?: 0)
                }
                if(message.reactions!="{}"&&message.deletedAt.isEmpty()) {
                    val reactions=remember(message.reactions) {runCatching {val j=org.json.JSONObject(message.reactions);j.keys().asSequence().map {j.getString(it)}.groupingBy {it}.eachCount()}.getOrDefault(emptyMap())}
                    val own=remember(message.reactions,message.account) {runCatching {org.json.JSONObject(message.reactions).optString(message.account)}.getOrDefault("")}
                    Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {reactions.forEach {(emoji,count)->Surface(shape=Glass.capsule,border=if(emoji==own)androidx.compose.foundation.BorderStroke(1.dp,MaterialTheme.colorScheme.primary) else null,color=MaterialTheme.colorScheme.primaryContainer) {Text("$emoji $count",Modifier.padding(horizontal=8.dp,vertical=3.dp),style=MaterialTheme.typography.labelMedium)}}}
                }
                if(message.status=="FAILED") {Text(message.error,style=MaterialTheme.typography.bodySmall);TextButton(onClick=onRetry) {Text("Retry")}}
            }
        }
    }
    }
}
@Composable fun SettingsScreen(model:AppViewModel,busy:Boolean) {
    var section by rememberSaveable {mutableStateOf<String?>(null)}
    var unavailable by remember {mutableStateOf<String?>(null)}
    if(section==null) {
        SettingsHome(model) {title->
            if(title in listOf("Devices","Language","Chat Folders")) unavailable=title else section=title
        }
        unavailable?.let {title->AlertDialog(onDismissRequest={unavailable=null},title={Text(title)},text={Text(when(title){"Language"->"English is the current app language.";"Chat Folders"->"Use All, Favorite, Archive and Unread in the Chats menu.";"Data and Storage"->"Shared images are loaded on demand. Automatic downloads are not enabled.";else->"Device management is not available in this version yet."})},confirmButton={TextButton(onClick={unavailable=null}){Text("OK")}})}
        return
    }
    BackHandler {section=null}

    val profile by model.profile.collectAsStateWithLifecycle()
    val settings by model.repository.privateSettings.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {model.action {model.repository.loadSettings()}}
    val unsent by model.unsent.collectAsStateWithLifecycle()
    val display by model.display.collectAsStateWithLifecycle()
    val context=LocalContext.current
    val photoPicker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {uri->if(uri!=null)model.action {
        val id=model.backend.session.value?.userId ?: return@action
        val image=Images.prepare(context,uri,id)
        try {val uploaded=model.backend.uploadImage(image.file,null);model.setAvatar(uploaded.getString("id"))} finally {image.file.delete()}
    }}
    var confirm by remember {mutableStateOf(false)}
    var edit by remember {mutableStateOf(false)}
    var deleteAccount by remember {mutableStateOf(false)}
    var showPolicy by remember {mutableStateOf(false)}
    var password by remember {mutableStateOf("")}
    if(edit) {BackHandler {edit=false};Column {TextButton(onClick={edit=false}) {Text("Back to Settings")};ProfileScreen(model,busy)};return}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {IconButton(onClick={section=null}){Icon(Icons.AutoMirrored.Outlined.ArrowBack,"Back to Settings")};Text(section.orEmpty(),style=MaterialTheme.typography.headlineSmall)}
        if(section=="Data and Storage") DataStorage(model)
        if(section=="Account") GlassCard(Modifier.fillMaxWidth()) {
            val avatar=profile?.optString("avatar_id")
            if(!avatar.isNullOrEmpty()&&avatar!="null") ProtectedImage(model.backend,avatar,description="Your profile photo",modifier=Modifier.size(72.dp).clip(CircleShape),scale=androidx.compose.ui.layout.ContentScale.Crop)
            else Avatar(profile?.optString("display_name").orEmpty(),self=true)
            Spacer(Modifier.height(12.dp))
            Text(profile?.optString("display_name").orEmpty(),style=MaterialTheme.typography.titleLarge)
            Text("@${profile?.optString("username").orEmpty()}");Text(profile?.optString("bio").orEmpty())
            Row {TextButton(onClick={photoPicker.launch("image/*")},enabled=!busy) {Text("Edit photo")};TextButton(onClick={model.action {model.setAvatar()}},enabled=!busy) {Text("Remove photo")}}
            TextButton(onClick={edit=true}) {Text("Edit profile")}
        }
        if(section=="Privacy & Security") GlassCard(Modifier.fillMaxWidth()) {Text("Your privacy matters",fontWeight=FontWeight.SemiBold);Text("Chat access is restricted to participants. This version does not use end-to-end encryption.")}
        if(section in listOf("Chat Settings","Power Saving")) GlassCard(Modifier.fillMaxWidth()) {
            Text("Appearance",style=MaterialTheme.typography.titleMedium)
            Row(Modifier.horizontalScroll(rememberScrollState())) {listOf("System","Light","Dark").forEach {value->FilterChip(selected=display.appearance==value,onClick={model.setDisplay(display.copy(appearance=value))},label={Text(value)})}}
            Text("Animation level",style=MaterialTheme.typography.labelLarge)
            Row(Modifier.horizontalScroll(rememberScrollState())) {listOf("Normal","Balanced","Extra").forEach {value->FilterChip(selected=display.animation==value,onClick={model.setDisplay(display.copy(animation=value))},enabled=!display.reducedMotion,label={Text(value)})}}
            Row(verticalAlignment=Alignment.CenterVertically) {Text("Reduced motion",Modifier.weight(1f));Switch(display.reducedMotion,{model.setDisplay(display.copy(reducedMotion=it))})}
            Row(verticalAlignment=Alignment.CenterVertically) {Text("Reduced transparency",Modifier.weight(1f));Switch(display.reducedTransparency,{model.setDisplay(display.copy(reducedTransparency=it))})}
        }
        if(section in listOf("Privacy & Security","Notifications")) GlassCard(Modifier.fillMaxWidth()) {
            Text("Privacy & notifications",style=MaterialTheme.typography.titleMedium)
            if(!settings.has("show_presence")) {Text("Connect to load your privacy settings.");TextButton(onClick={model.action {model.repository.loadSettings()}},enabled=!busy) {Text("Retry")}}
            listOf("show_presence" to "Online & last seen","read_receipts" to "Read receipts","notification_previews" to "Notification previews").filter {if(section=="Notifications")it.first=="notification_previews" else it.first!="notification_previews"}.forEach { (key,label)->
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text(label,Modifier.weight(1f))
                    Switch(checked=settings.optBoolean(key,true),enabled=!busy&&settings.has(key),onCheckedChange={checked->model.action {
                        model.repository.settings(if(key=="show_presence")checked else settings.optBoolean("show_presence",true),if(key=="read_receipts")checked else settings.optBoolean("read_receipts",true),if(key=="notification_previews")checked else settings.optBoolean("notification_previews",true))
                    }})
                }
            }
        }
        if(section=="Account") OutlinedButton(onClick={confirm=true},enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text("Sign out")}
        if(section=="Account") TextButton(onClick={deleteAccount=true},enabled=!busy) {Text("Delete account",color=MaterialTheme.colorScheme.error)}
        TextButton(onClick={showPolicy=true}) {Text("Privacy & User Policy")}
        Text("Developed by Pritam Pal\n© 2026 Liquid Chat",style=MaterialTheme.typography.bodySmall)
    }
    if(showPolicy)AlertDialog(onDismissRequest={showPolicy=false},title={Text("Privacy & User Policy")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){UserPolicyText()}},confirmButton={TextButton(onClick={showPolicy=false}){Text("Done")}})
    if(deleteAccount) AlertDialog(onDismissRequest={deleteAccount=false;password=""},title={Text("Permanently delete account?")},text={Column(Modifier.verticalScroll(rememberScrollState())) {
        Text("Your profile, sessions, device tokens and owned media will be removed. Sent text remains in the other person's history under Deleted account. $unsent unsent messages will be discarded.")
        Text("Enter your password to verify. For Google accounts, sign in with Google again, then return here and confirm within five minutes.")
        PasswordField(password,{password=it},"Password (email accounts)")
        TextButton(onClick={model.action {val url=model.backend.googleUrl();context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse(url)))}}) {Text("Verify with Google")}
    }},confirmButton={TextButton(onClick={model.deleteAccount(password);password="";deleteAccount=false},enabled=!busy) {Text("Delete permanently")}},dismissButton={TextButton(onClick={deleteAccount=false;password=""}) {Text("Cancel")}})
    if(confirm) AlertDialog(onDismissRequest={confirm=false},title={Text("Sign out?")},text={Text(if(unsent>0)"$unsent unsent messages will be discarded. Saved local history, drafts and jobs will be removed from this device." else "Saved local history and account data will be removed from this device.")},confirmButton={TextButton(onClick={confirm=false;model.finishLogout()}) {Text("Sign out")}},dismissButton={TextButton(onClick={confirm=false}) {Text("Cancel")}})
}

@Composable internal fun SharedImagePreview(model:AppViewModel,message:Message) {
    var open by remember(message.id) {mutableStateOf(false)}
    ProtectedImage(model.backend,message.mediaId,message.localImage,"Shared photo",Modifier.widthIn(max=280.dp).fillMaxWidth().aspectRatio((if(message.imageHeight>0&&message.imageWidth>0)message.imageWidth.toFloat()/message.imageHeight else 1f).coerceIn(.5f,2f)).clickable {open=true})
    if(open) androidx.compose.ui.window.Dialog(onDismissRequest={open=false},properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
            Column(Modifier.statusBarsPadding().navigationBarsPadding()) {
                TextButton(onClick={open=false}) {Text("Close image")}
                var zoom by remember(message.id) {mutableFloatStateOf(1f)}
                var pan by remember(message.id) {mutableStateOf(Offset.Zero)}
                var size by remember {mutableStateOf(IntSize.Zero)}
                val transform=rememberTransformableState {scale,move,_->
                    zoom=(zoom*scale).coerceIn(1f,4f)
                    val limitX=size.width*(zoom-1)/2;val limitY=size.height*(zoom-1)/2
                    pan=Offset((pan.x+move.x).coerceIn(-limitX,limitX),(pan.y+move.y).coerceIn(-limitY,limitY))
                }
                Box(Modifier.weight(1f).fillMaxWidth().clip(androidx.compose.ui.graphics.RectangleShape).onSizeChanged {size=it}.transformable(transform)) {
                    ProtectedImage(model.backend,message.mediaId,message.localImage,"Full-size shared photo",Modifier.fillMaxSize().graphicsLayer {scaleX=zoom;scaleY=zoom;translationX=pan.x;translationY=pan.y})
                }
                TextButton(onClick={zoom=1f;pan=Offset.Zero}) {Text("Reset zoom")}
            }
        }
    }
}
