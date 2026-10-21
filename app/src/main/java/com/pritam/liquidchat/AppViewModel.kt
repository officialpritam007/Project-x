package com.pritam.liquidchat

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pritam.liquidchat.data.Backend
import com.pritam.liquidchat.data.Chat
import com.pritam.liquidchat.data.CachedProfile
import com.pritam.liquidchat.data.Message
import com.pritam.liquidchat.data.localLogout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.tasks.await

/** Drop results from older keystrokes, a different account or a changed auth scope. */
internal fun searchRequestCurrent(
    requestedVersion:Long,currentVersion:Long,
    requestedAccount:String,currentAccount:String?,
    requestedEpoch:Long?,currentEpoch:Long?
):Boolean = requestedVersion==currentVersion &&
    requestedAccount==currentAccount && requestedEpoch!=null && requestedEpoch==currentEpoch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val displayPrefs=app.getSharedPreferences("display",android.content.Context.MODE_PRIVATE)
    val display=MutableStateFlow(com.pritam.liquidchat.ui.DisplayOptions(displayPrefs.getString("appearance","System") ?: "System",displayPrefs.getString("animation","Balanced") ?: "Balanced",displayPrefs.getBoolean("reducedMotion",false),displayPrefs.getBoolean("reducedTransparency",false),displayPrefs.getInt("blurIntensity",55).coerceIn(0,100),displayPrefs.getString("bubbleStyle","Tail")?.takeIf {it in setOf("Tail","Rounded","Compact")} ?: "Tail",displayPrefs.getString("bubbleColor","Original")?.takeIf {it in setOf("Original","Green","Lavender","Rose","Night")} ?: "Original",displayPrefs.getInt("messageFontSize",16).coerceIn(14,22),displayPrefs.getBoolean("showNavLabels",true),displayPrefs.getInt("navIconSize",24).coerceIn(18,30),displayPrefs.getInt("navLabelSize",11).coerceIn(9,15),displayPrefs.getInt("headerTitleSize",25).coerceIn(20,30),displayPrefs.getInt("homeChatNameSize",16).coerceIn(13,20),displayPrefs.getInt("homeChatPreviewSize",14).coerceIn(12,18),displayPrefs.getString("accentColor","Cyan")?.takeIf {it in setOf("Cyan","Blue","Green","Purple","Rose","Amber")} ?: "Cyan",displayPrefs.getBoolean("showProfileAsTitle",false),displayPrefs.getBoolean("hideArchiveAccess",false),displayPrefs.getString("fontPreset","System")?.takeIf {it in setOf("System","Serif","Mono","Cursive")} ?: "System",displayPrefs.getBoolean("hideOutgoingTyping",false),displayPrefs.getBoolean("preventScreenshots",false),displayPrefs.getInt("homeWallpaperDim",50).coerceIn(0,100),displayPrefs.getInt("chatWallpaperDim",50).coerceIn(0,100),displayPrefs.getBoolean("hideHomePreviews",false),com.pritam.liquidchat.ui.MbQuickReactions.normalize(displayPrefs.getString("quickReactions",null)),com.pritam.liquidchat.ui.MbNavigationOrder.normalize(displayPrefs.getString("navigationOrder",null)),displayPrefs.getInt("homeAvatarSize",52).coerceIn(40,68),displayPrefs.getInt("homeRowPadding",11).coerceIn(6,20),displayPrefs.getBoolean("hideHomeSearch",false),displayPrefs.getBoolean("showHomeDividers",false),displayPrefs.getString("navBarStyle","Reference")?.takeIf {it in setOf("Reference","Liquid")} ?: "Reference",displayPrefs.getBoolean("hidePhotoShortcut",false),displayPrefs.getBoolean("showPinBadges",true),displayPrefs.getInt("emojiPickerSize",24).coerceIn(18,32)))
    fun setDisplay(options:com.pritam.liquidchat.ui.DisplayOptions) {
        val safe=options.copy(
            blurIntensity=options.blurIntensity.coerceIn(0,100),
            bubbleStyle=options.bubbleStyle.takeIf {it in setOf("Tail","Rounded","Compact")} ?: "Tail",
            bubbleColor=options.bubbleColor.takeIf {it in setOf("Original","Green","Lavender","Rose","Night")} ?: "Original",
            messageFontSize=options.messageFontSize.coerceIn(14,22),
            navIconSize=options.navIconSize.coerceIn(18,30),
            navLabelSize=options.navLabelSize.coerceIn(9,15),
            headerTitleSize=options.headerTitleSize.coerceIn(20,30),
            homeChatNameSize=options.homeChatNameSize.coerceIn(13,20),
            homeChatPreviewSize=options.homeChatPreviewSize.coerceIn(12,18),
            accentColor=options.accentColor.takeIf {it in setOf("Cyan","Blue","Green","Purple","Rose","Amber")} ?: "Cyan",
            fontPreset=options.fontPreset.takeIf {it in setOf("System","Serif","Mono","Cursive")} ?: "System",
            homeWallpaperDim=options.homeWallpaperDim.coerceIn(0,100),
            chatWallpaperDim=options.chatWallpaperDim.coerceIn(0,100),
            quickReactions=com.pritam.liquidchat.ui.MbQuickReactions.normalize(options.quickReactions),
            navigationOrder=com.pritam.liquidchat.ui.MbNavigationOrder.normalize(options.navigationOrder),
            homeAvatarSize=options.homeAvatarSize.coerceIn(40,68),
            homeRowPadding=options.homeRowPadding.coerceIn(6,20),
            navBarStyle=options.navBarStyle.takeIf {it in setOf("Reference","Liquid")} ?: "Reference",
            emojiPickerSize=options.emojiPickerSize.coerceIn(18,32)
        )
        displayPrefs.edit().putString("appearance",safe.appearance).putString("animation",safe.animation).putBoolean("reducedMotion",safe.reducedMotion).putBoolean("reducedTransparency",safe.reducedTransparency).putInt("blurIntensity",safe.blurIntensity).putString("bubbleStyle",safe.bubbleStyle).putString("bubbleColor",safe.bubbleColor) .putInt("messageFontSize",safe.messageFontSize).putBoolean("showNavLabels",safe.showNavLabels).putInt("navIconSize",safe.navIconSize).putInt("navLabelSize",safe.navLabelSize).putInt("headerTitleSize",safe.headerTitleSize).putInt("homeChatNameSize",safe.homeChatNameSize).putInt("homeChatPreviewSize",safe.homeChatPreviewSize).putString("accentColor",safe.accentColor).putBoolean("showProfileAsTitle",safe.showProfileAsTitle).putBoolean("hideArchiveAccess",safe.hideArchiveAccess).putString("fontPreset",safe.fontPreset).putBoolean("hideOutgoingTyping",safe.hideOutgoingTyping).putBoolean("preventScreenshots",safe.preventScreenshots).putInt("homeWallpaperDim",safe.homeWallpaperDim).putInt("chatWallpaperDim",safe.chatWallpaperDim).putBoolean("hideHomePreviews",safe.hideHomePreviews).putString("quickReactions",safe.quickReactions).putString("navigationOrder",safe.navigationOrder).putInt("homeAvatarSize",safe.homeAvatarSize).putInt("homeRowPadding",safe.homeRowPadding).putBoolean("hideHomeSearch",safe.hideHomeSearch).putBoolean("showHomeDividers",safe.showHomeDividers).putString("navBarStyle",safe.navBarStyle).putBoolean("hidePhotoShortcut",safe.hidePhotoShortcut).putBoolean("showPinBadges",safe.showPinBadges).putInt("emojiPickerSize",safe.emojiPickerSize).apply()
        display.value=safe
    }
    val backend: Backend=(app as LiquidApplication).backend
    val repository=(app as LiquidApplication).repository
    val calls=com.pritam.liquidchat.calls.CallController(app,backend,repository.foreground,viewModelScope)
    val contacts=repository.contacts().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    val chats=repository.chats().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    val messagesReady=MutableStateFlow<String?>(null)
    val messages=repository.active.flatMapLatest {cid->
        messagesReady.value=null
        if(cid==null)flowOf(emptyList()) else repository.messages(cid).onEach {messagesReady.value=cid}
    }.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    val active=repository.active.asStateFlow()
    val connection=repository.connection
    val unsent=repository.unsent().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),0)
    val found=MutableStateFlow<JSONObject?>(null)
    val searchResults=MutableStateFlow<List<Message>>(emptyList())
    val focusMessage=MutableStateFlow<String?>(null)
    private var searchJob:kotlinx.coroutines.Job?=null
    private var searchVersion=0L
    val session=backend.session
    private val policyPrefs=app.getSharedPreferences("user_policy",android.content.Context.MODE_PRIVATE)
    private val policyEpoch=MutableStateFlow(0)
    val acceptedPolicy=combine(session.map {it?.userId},policyEpoch) {id,_->
        id!=null && policyPrefs.getString("accepted_$id",null)==com.pritam.liquidchat.ui.ACTIVE_USER_POLICY_VERSION
    }.stateIn(viewModelScope,SharingStarted.Eagerly,false)
    fun acceptPolicy()=action {
        val id=session.value?.userId ?: return@action
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            check(policyPrefs.edit().putString("accepted_$id",com.pritam.liquidchat.ui.ACTIVE_USER_POLICY_VERSION).commit())
        }
        policyEpoch.value++
    }
    val recovery=backend.recovery
    private val _ready=MutableStateFlow(false)
    val ready=_ready.asStateFlow()
    private val _busy=MutableStateFlow(false)
    val busy=_busy.asStateFlow()
    private val _notice=MutableStateFlow<String?>(null)
    val notice=_notice.asStateFlow()
    private val _profile=MutableStateFlow<JSONObject?>(null)
    val profile=_profile.asStateFlow()
    private val _profileLoaded=MutableStateFlow(false)
    val profileLoaded=_profileLoaded.asStateFlow()
    private val _profileError=MutableStateFlow<String?>(null)
    val profileError=_profileError.asStateFlow()
    private val profileMutex=Mutex()
    init {
        viewModelScope.launch { backend.restore(); _ready.value=true }
        viewModelScope.launch {
            combine(session.map { it?.userId },backend.uiReads.sessionEpoch) {id,epoch->id to epoch}
                .distinctUntilChanged().collectLatest { (id,_) ->
                _profile.value=null; _profileLoaded.value=false;_profileError.value=null;found.value=null;searchResults.value=emptyList();focusMessage.value=null
                if(id!=null) loadProfile() else _profileLoaded.value=true
            }
        }
    }
    fun sendVoice(cid:String,file:java.io.File,duration:Int,expected:String) {
        viewModelScope.launch {
            try {require(backend.session.value?.userId==expected);repository.sendVoice(cid,file,duration)}
            catch(e:CancellationException){throw e}
            catch(_:Exception){file.delete();_notice.value="Voice could not be queued. Please record again."}
        }
    }
    fun clearNotice() { _notice.value=null }
    fun action(block: suspend () -> Unit) {
        if(_busy.value) return
        _busy.value=true; _notice.value=null
        viewModelScope.launch {
            try { block() } catch(e: CancellationException) { throw e }
            catch(e: Exception) { _notice.value=e.message ?: "Connection unavailable. Try again." }
            finally { _busy.value=false }
        }
    }
    suspend fun loadProfile()=profileMutex.withLock {
        val id=session.value?.userId ?: return@withLock
        val epoch=backend.uiReads.epoch(id)
        val cacheScope=backend.profileCacheScope(id)
        fun current()=session.value?.userId==id && backend.uiReads.epoch(id)==epoch && backend.profileCacheScope(id)==cacheScope
        _profileError.value=null
        try {
            // Offline hydration is allowed only for this login and authorization scope.
            // Keep the current in-memory profile during refresh to avoid old avatar flashes.
            if(!_profileLoaded.value && cacheScope!=null)repository.dao.profile(id)?.let {cached->
                val json=runCatching {JSONObject(cached.json)}.getOrNull()
                if(json!=null && json.optString("id")==id && json.optString("_liquid_cache_scope")==cacheScope && current()) {
                    json.remove("_liquid_cache_scope")
                    _profile.value=json;_profileLoaded.value=true
                }
            }
            val a=JSONArray(backend.request("/rest/v1/profiles?id=eq.$id&select=*&limit=1"))
            if(current()) {
                _profile.value=a.optJSONObject(0);_profileLoaded.value=true
                _profile.value?.let {
                    val cached=JSONObject(it.toString()).put("_liquid_cache_scope",cacheScope)
                    repository.dao.cacheProfile(CachedProfile(id,cached.toString()))
                }
            }
        } catch(e: CancellationException) { throw e }
        catch(e:Exception) { if(current()){
            // A completed (but failed) request is not an infinite loading state.
            // Keep any authorized cached profile visible; if none exists, the UI
            // offers a retry instead of permanently displaying a spinner.
            _profileLoaded.value=true
            _profileError.value="Your profile is unavailable offline or while the service is unreachable. Connect and retry."
            _notice.value=e.message ?: "Cannot load profile. Retry when connected."
        } }
    }
    fun signIn(email:String,password:String)=action { backend.signIn(email,password) }
    fun register(email:String,password:String)=action {
        if(!backend.register(email,password)) _notice.value="Check your email to verify your account, then sign in."
    }
    fun reset(email:String)=action { backend.reset(email);_notice.value="If that address can receive a reset, an email is on its way. Open it on this device." }
    fun callback(uri: Uri)=action { backend.callback(uri) }
    suspend fun setAvatar(media:String?=null) {
        val id=session.value?.userId ?: return
        val epoch=backend.uiReads.epoch(id)
        backend.rpc("set_avatar",JSONObject().apply {if(media!=null)put("p_media",media)})
        if(session.value?.userId==id && backend.uiReads.epoch(id)==epoch) {
            _profile.value?.let {previous->
                val updated=JSONObject(previous.toString()).put("avatar_id",media ?: JSONObject.NULL)
                _profile.value=updated
                val cached=JSONObject(updated.toString()).put("_liquid_cache_scope",backend.profileCacheScope(id))
                repository.dao.cacheProfile(CachedProfile(id,cached.toString()))
            }
            repository.refresh()
            loadProfile()
        }
    }
    fun saveProfile(username:String,name:String,bio:String)=action {
        backend.rpc("save_profile",JSONObject().put("p_username",username).put("p_name",name).put("p_bio",bio));loadProfile()
    }
    fun open(cid:String,messageId:String?=null) {
        if(runCatching {java.util.UUID.fromString(cid)}.isFailure)return
        focusMessage.value=messageId;repository.active.value=cid
        repository.refresh()
        if(messageId!=null)viewModelScope.launch {
            try {repository.reveal(cid,messageId)} catch(e:CancellationException){throw e} catch(_:Exception){_notice.value="This message is unavailable. Retry when connected."}
        }
    }
    fun openNotification(cid:String,expectedAccount:String?) {
        viewModelScope.launch {
            ready.filter {it}.first()
            if(expectedAccount==null||session.value?.userId!=expectedAccount)return@launch
            try {
                repository.sync()
                if(session.value?.userId==expectedAccount&&repository.dao.chat(expectedAccount,cid)!=null)open(cid)
            } catch(e:CancellationException){throw e} catch(_:Exception){_notice.value="Connect to open this notification safely."}
        }
    }
    fun closeChat() {
        val cid=repository.active.value
        if(cid!=null)viewModelScope.launch {runCatching {repository.typing(cid,false)}}
        repository.active.value=null;focusMessage.value=null;repository.refresh()}
    fun send(cid:String,body:String,replyTo:String="",draftVersion:Long?=null,onQueued:()->Unit={}) = viewModelScope.launch {
        try {repository.send(cid,body,replyTo,draftVersion);onQueued();runCatching {repository.typing(cid,false)}}
        catch(e:CancellationException){throw e}
        catch(e:Exception) {_notice.value=e.message}
    }
    fun retry(id:String)=action {repository.retry(id)}
    fun older(cid:String)=action {if(!repository.loadOlder(cid)) _notice.value="All available history is loaded."}
    fun find(username:String)=action {found.value=repository.find(username);if(found.value==null)_notice.value="No matching contact found."}
    fun start(peer:String)=action {open(repository.start(peer))}
    fun updateChat(chat:Chat,favorite:Boolean=chat.favorite,archived:Boolean=chat.archived,muted:Boolean=chat.muted,delete:Boolean=false)=action {
        repository.setChat(chat,favorite,archived,muted,delete)
        if(delete)closeChat()
    }
    fun block(peer:String,blocked:Boolean)=action {repository.block(peer,blocked);_notice.value=if(blocked)"Contact blocked" else "Contact unblocked"}
    fun search(query:String) {
        searchJob?.cancel()
        val requestedVersion=++searchVersion
        val expected=session.value?.userId
        // Never display results from a previous query while debouncing this one.
        searchResults.value=emptyList()
        if(query.isBlank()||expected==null)return
        val expectedEpoch=backend.uiReads.epoch(expected)
        fun current()=searchRequestCurrent(requestedVersion,searchVersion,expected,
            session.value?.userId,expectedEpoch,backend.uiReads.epoch(expected))
        searchJob=viewModelScope.launch {
            kotlinx.coroutines.delay(200)
            try {
                val results=repository.search(query)
                if(current())searchResults.value=results
            } catch(e:CancellationException){throw e}
              catch(_:Exception){
                if(current()) {
                    searchResults.value=emptyList()
                    _notice.value="Search is unavailable. Please retry."
                }
            }
        }
    }
    fun report(peer:String,reason:String,messageIds:List<String> = emptyList())=action {
        backend.rpc("report_contact",JSONObject().put("p_target",peer).put("p_reason",reason).put("p_messages",JSONArray(messageIds)));_notice.value="Report submitted."
    }
    fun reportMoment(id:String,reason:String)=action {
        backend.rpc("report_moment",JSONObject().put("p_id",id).put("p_reason",reason));_notice.value="Report submitted."
    }
    fun deleteAccount(password:String)=action {
        val current=session.value ?: return@action
        if(password.isNotEmpty())backend.signIn(current.email,password)
        check(session.value?.userId==current.userId)
        val result=JSONObject(backend.request("/functions/v1/account","POST",JSONObject().put("action","delete")))
        backend.forgetLocal();repository.clearAccount(current.userId)
        com.pritam.liquidchat.data.MessageNotificationDedupe.clearAccount(getApplication(),current.userId)
        policyPrefs.edit().remove("accepted_${current.userId}").apply();policyEpoch.value++
        getApplication<Application>().getSystemService(android.app.NotificationManager::class.java).cancelAll()
        androidx.work.WorkManager.getInstance(getApplication<Application>()).cancelAllWorkByTag("device-registration")
        getApplication<Application>().getSharedPreferences("device",android.content.Context.MODE_PRIVATE).edit().remove("fcm_token").apply()
        _notice.value=if(result.optBoolean("pending"))"Account deletion requested. Media cleanup continues in the background." else "Your account has been deleted."
    }
    val logoutPrompt=MutableStateFlow(false)
    fun dismissLogout() {logoutPrompt.value=false}
    fun logout() {if(unsent.value>0)logoutPrompt.value=true else finishLogout()}
    override fun onCleared() {
        calls.hangup()
        super.onCleared()
    }
    fun finishLogout()=action {
        logoutPrompt.value=false
        val id=session.value?.userId
        val revoked=localLogout(revokeRemote={backend.logout()},clearLocal={
            backend.forgetLocal()
            if(id!=null){
                repository.clearAccount(id)
                com.pritam.liquidchat.data.MessageNotificationDedupe.clearAccount(getApplication(),id)
                policyPrefs.edit().remove("accepted_$id").apply();policyEpoch.value++
            }
            getApplication<Application>().getSystemService(android.app.NotificationManager::class.java).cancelAll()
            androidx.work.WorkManager.getInstance(getApplication<Application>()).cancelAllWorkByTag("device-registration")
            getApplication<Application>().getSharedPreferences("device",android.content.Context.MODE_PRIVATE).edit().remove("fcm_token").apply()
            found.value=null;searchResults.value=emptyList();focusMessage.value=null
        })
        if(!revoked)_notice.value="Signed out on this device. Server session revocation could not be confirmed while offline or unavailable."
    }
}
