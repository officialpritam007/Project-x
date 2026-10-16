package com.pritam.liquidchat.data

import android.content.Context
import androidx.room.withTransaction
import androidx.work.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ChatRepository(private val context:Context,val backend:Backend,val db:LocalDatabase) {
    val dao=db.dao()
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val syncMutex=Mutex()
    private val drainMutex=Mutex()
    val contactState=MutableStateFlow(JSONObject())
    val privateSettings=MutableStateFlow(JSONObject())
    val foreground=MutableStateFlow(false)
    val active=MutableStateFlow<String?>(null)
    private val _connection=MutableStateFlow("Connecting…")
    val connection=_connection.asStateFlow()
    private val signals=MutableSharedFlow<Unit>(extraBufferCapacity=1)
    init {
        scope.launch {
            combine(backend.session.map {it?.userId}.distinctUntilChanged(),foreground) {id,fg->if(fg) id else null}.collectLatest { account ->
                if(account==null) return@collectLatest
                enqueue(account)
                coroutineScope {
                    launch { while(isActive) {safeSync();delay(60000)} }
                    launch {
                        try {while(isActive) {runCatching {backend.rpc("update_presence",JSONObject().put("p_online",true))};delay(15000)}}
                        finally {withContext(NonCancellable) {runCatching {backend.rpc("update_presence",JSONObject().put("p_online",false))};active.value?.let {runCatching {typing(it,false)}}}}
                    }
                    launch {active.collectLatest {cid->
                        contactState.value=JSONObject()
                        if(cid!=null) while(isActive) {
                            try {contactState.value=JSONObject(backend.rpc("contact_state",JSONObject().put("p_conversation",cid)))}
                            catch(e:CancellationException){throw e}catch(_:Exception){contactState.value=JSONObject()}
                            delay(3000)
                        }
                    }}
                    launch { signals.collect {delay(180);safeSync()} }
                    launch {
                        backend.session.map {it?.access}.distinctUntilChanged().collectLatest { access ->
                            if(access!=null) RealtimeHints().listen(access).retryWhen {_,attempt-> delay(minOf(30000L,1000L*(attempt+1)));true}.collect {signals.tryEmit(Unit)}
                        }
                    }
                }
            }
        }
    }
    fun refresh() {signals.tryEmit(Unit)}
    private fun account()=backend.session.value?.userId ?: throw IOException("Please sign in.")
    private fun checkAccount(expected:String) {if(backend.session.value?.userId!=expected) throw CancellationException("Account changed")}
    fun contacts()=backend.session.map {it?.userId}.distinctUntilChanged().flatMapLatest {id->if(id==null)flowOf(emptyList()) else dao.contacts(id).distinctUntilChanged()}
    suspend fun loadContacts() {
        val id=account();val a=JSONArray(backend.rpc("list_contacts"))
        val contacts=(0 until a.length()).map {i->val j=a.getJSONObject(i);Contact(id,j.getString("id"),j.getString("display_name"),j.getString("username"),if(j.isNull("avatar_id"))"" else j.optString("avatar_id"),j.optString("bio"))}
        checkAccount(id);db.withTransaction {dao.clearContacts(id);dao.putContacts(contacts)}
    }
    suspend fun saveContact(peer:String,saved:Boolean) {backend.rpc("save_contact",JSONObject().put("p_peer",peer).put("p_saved",saved));loadContacts()}
    fun chats()=backend.session.map {it?.userId}.distinctUntilChanged().flatMapLatest {id-> if(id==null) flowOf(emptyList()) else dao.chats(id).distinctUntilChanged()}
    fun messages(cid:String)=backend.session.map {it?.userId}.distinctUntilChanged().flatMapLatest {id->if(id==null) flowOf(emptyList()) else dao.messages(id,cid).distinctUntilChanged()}
    fun unsent()=backend.session.map {it?.userId}.distinctUntilChanged().flatMapLatest {id->if(id==null) flowOf(0) else dao.unsent(id).distinctUntilChanged()}
    private suspend fun safeSync() {
        try {sync();_connection.value="Connected"} catch(e:CancellationException) {throw e}
        catch(_:Exception) {_connection.value="Offline or service unavailable · saved messages remain available"}
    }
    suspend fun sync()=syncMutex.withLock {
        val account=account()
        var before=Long.MAX_VALUE;var beforeId="ffffffff-ffff-ffff-ffff-ffffffffffff"
        val chats=mutableListOf<Chat>()
        do {
            val page=JSONArray(backend.rpc("list_chats",JSONObject().put("p_before_seq",before).put("p_before_id",beforeId)))
            for(i in 0 until page.length()) {
                val j=page.getJSONObject(i);val id=j.getString("id");val old=dao.chat(account,id);val pending=dao.latestUnsent(account,id)
                chats+=Chat(account,id,if(j.getString("user_a")==account)j.getString("user_b") else j.getString("user_a"),j.getString("display_name"),j.getString("username"),
                    pending?.let {"${it.status.lowercase().replaceFirstChar {it.uppercase()}}: ${it.body.take(160)}"} ?: if(j.isNull("preview")) "" else j.getString("preview"),j.getLong("last_seq"),j.getInt("unread"),j.getBoolean("favorite"),j.getBoolean("archived"),j.getBoolean("muted"),
                    old?.syncSeq ?: 0,j.getLong("deleted_seq"),j.getLong("peer_delivered_seq"),j.getLong("peer_read_seq"),
                    if(j.isNull("last_at"))"" else j.optString("last_at"),if(j.isNull("avatar_id"))"" else j.optString("avatar_id"),pending?.localTime ?: 0)
            }
            if(page.length()<100) break
            val last=page.getJSONObject(page.length()-1);before=last.getLong("last_seq");beforeId=last.getString("id")
        } while(true)
        checkAccount(account)
        db.withTransaction {
            dao.putChats(chats)
            val existing=dao.allChats(account)
            for(c in existing) if(chats.none {it.id==c.id}) {dao.removeHistory(account,c.id);dao.deleteChat(account,c.id)}
        }
        for(c in chats) {
            checkAccount(account)
            dao.prune(account,c.id,c.deletedSeq)
            if(c.lastSeq>c.syncSeq) {
                var cursor=c.syncSeq
                do {
                    val order=if(cursor==0L) "desc" else "asc"
                    val limit=if(cursor==0L)50 else 100
                    val page=JSONArray(backend.request("/rest/v1/messages?conversation_id=eq.${c.id}&seq=gt.${maxOf(cursor,c.deletedSeq)}&order=seq.$order&limit=$limit&select=*"))
                    val messages=(0 until page.length()).map {parseMessage(page.getJSONObject(it),account)}
                    checkAccount(account)
                    db.withTransaction {
                        for(m in messages) {
                            val old=dao.message(account,m.id)
                            dao.put(m.copy(localTime=old?.localTime ?: m.localTime))
                        }
                        if(messages.isNotEmpty()) {cursor=messages.maxOf {it.seq};dao.syncCursor(account,c.id,cursor)}
                    }
                    if(order=="desc" || messages.size<limit) break
                } while(true)
                acknowledge(c.id,read=false)
            }
        }
    }
    suspend fun loadOlder(cid:String):Boolean=syncMutex.withLock {
        val account=account();val before=dao.minSeq(account,cid) ?: Long.MAX_VALUE
        val page=JSONArray(backend.request("/rest/v1/messages?conversation_id=eq.$cid&seq=lt.$before&order=seq.desc&limit=50&select=*"))
        val messages=(0 until page.length()).map {parseMessage(page.getJSONObject(it),account)}
        checkAccount(account);dao.putAll(messages)
        messages.size==50
    }
    suspend fun acknowledge(cid:String,read:Boolean) {
        val account=account();val seq=dao.receivedSeq(account,cid)
        if(seq>0) backend.rpc("acknowledge",JSONObject().put("p_conversation",cid).put("p_seq",seq).put("p_read",read))
    }
    suspend fun find(username:String):JSONObject? = JSONArray(backend.rpc("find_profile",JSONObject().put("p_username",username))).optJSONObject(0)
    suspend fun start(peer:String):String {
        val id=JSONTokener(backend.rpc("start_conversation",JSONObject().put("p_peer",peer))).nextValue() as String
        sync();return id
    }
    suspend fun send(cid:String,body:String) {
        require(body.isNotBlank() && body.length<=4000)
        val account=account()
        val message=Message(account,UUID.randomUUID().toString(),cid,account,body.trim())
        db.withTransaction {dao.put(message);dao.pendingPreview(account,cid,"Queued: ${message.body.take(160)}",message.localTime)}
        enqueue(account)
    }
    suspend fun sendImage(cid:String,image:Images.Prepared,caption:String) {
        val account=account()
        val message=Message(account,UUID.randomUUID().toString(),cid,account,caption.ifBlank {"Photo"},localImage=image.file.absolutePath,imageWidth=image.width,imageHeight=image.height)
        db.withTransaction {dao.put(message);dao.pendingPreview(account,cid,"Queued: ${message.body.take(160)}",message.localTime)}
        enqueue(account)
    }
    suspend fun retry(id:String) {
        val account=account();val m=dao.message(account,id) ?: return
        if(m.status!="FAILED")return
        dao.put(m.copy(status="QUEUED",attempts=0,error=""));enqueue(account)
    }
    private fun enqueue(account:String) {
        val request=OneTimeWorkRequestBuilder<SendWorker>().setInputData(workDataOf("account" to account))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).addTag("account-$account").build()
        WorkManager.getInstance(context).enqueueUniqueWork("outbox-$account",ExistingWorkPolicy.APPEND_OR_REPLACE,request)
    }
    suspend fun drain(expected:String):Boolean=drainMutex.withLock {
        checkAccount(expected)
        for(candidate in dao.pending(expected)) {
            val m=dao.message(expected,candidate.id) ?: continue
            if(m.status!="QUEUED" && m.status!="SENDING") continue
            currentCoroutineContext().ensureActive();checkAccount(expected)
            var sending=m.copy(status="SENDING",attempts=m.attempts+1,error="")
            dao.put(sending)
            try {
                if(sending.localImage.isNotEmpty() && sending.mediaId.isEmpty()) {
                    val upload=backend.uploadImage(java.io.File(sending.localImage),m.conversation)
                    sending=sending.copy(mediaId=upload.getString("id"),imageWidth=upload.getInt("width"),imageHeight=upload.getInt("height"))
                    currentCoroutineContext().ensureActive();checkAccount(expected);dao.put(sending)
                }
                val raw=if(sending.mediaId.isNotEmpty()) backend.rpc("send_image",JSONObject().put("p_id",m.id).put("p_media",sending.mediaId).put("p_caption",m.body))
                    else backend.rpc("send_text",JSONObject().put("p_id",m.id).put("p_conversation",m.conversation).put("p_body",m.body))
                val parsed=JSONTokener(raw).nextValue()
                val j=if(parsed is JSONArray)parsed.getJSONObject(0) else parsed as JSONObject
                currentCoroutineContext().ensureActive();checkAccount(expected)
                dao.put(parseMessage(j,expected).copy(localTime=m.localTime))
                if(m.localImage.isNotEmpty())java.io.File(m.localImage).delete()
                refresh()
                // Durable server queue owns retries; FCM submission does not mark delivery.
                runCatching {backend.request("/functions/v1/notify","POST",JSONObject())}
            } catch(e:CancellationException) {throw e}
            catch(e:Exception) {
                val transient=e !is ApiException || e.status>=500 || e.status==429 || e.code=="P0001"
                val retry=Delivery.retryable(sending.status,sending.attempts,transient)
                currentCoroutineContext().ensureActive();checkAccount(expected)
                if(dao.message(expected,m.id)?.status!="SENT") dao.put(sending.copy(status=if(retry)"QUEUED" else "FAILED",error=e.message ?: "Send failed"))
                if(retry)return@withLock false
            }
        }
        true
    }
    suspend fun setChat(c:Chat,favorite:Boolean=c.favorite,archived:Boolean=c.archived,muted:Boolean=c.muted,delete:Boolean=false) {
        backend.rpc("set_conversation",JSONObject().put("p_conversation",c.id).put("p_favorite",favorite).put("p_archived",archived).put("p_muted",muted).put("p_delete",delete));sync()
    }
    suspend fun block(peer:String,blocked:Boolean) {backend.rpc("set_block",JSONObject().put("p_peer",peer).put("p_blocked",blocked))}
    suspend fun search(query:String)=dao.search(account(),query.replace("\\","\\\\").replace("%","\\%").replace("_","\\_"))
    suspend fun typing(cid:String,typing:Boolean) {backend.rpc("update_typing",JSONObject().put("p_conversation",cid).put("p_typing",typing))}
    suspend fun loadSettings() {
        val id=account()
        val a=JSONArray(backend.request("/rest/v1/settings?user_id=eq.$id&select=*&limit=1"))
        checkAccount(id);privateSettings.value=a.optJSONObject(0) ?: JSONObject()
    }
    suspend fun settings(presence:Boolean,receipts:Boolean,previews:Boolean) {
        backend.rpc("save_settings",JSONObject().put("p_presence",presence).put("p_receipts",receipts).put("p_previews",previews));loadSettings()
    }
    suspend fun clearAccount(account:String) {
        active.value=null;contactState.value=JSONObject();privateSettings.value=JSONObject()
        WorkManager.getInstance(context).cancelAllWorkByTag("account-$account")
        syncMutex.withLock {drainMutex.withLock {db.withTransaction {dao.clearMessages(account);dao.clearChats(account);dao.clearContacts(account);dao.clearProfile(account)}}}
        java.io.File(context.filesDir,"media/$account").deleteRecursively()
        coil.Coil.imageLoader(context).memoryCache?.clear()
    }
    private fun parseMessage(j:JSONObject,account:String):Message {
        val time=j.getString("created_at")
        return Message(account,j.getString("id"),j.getString("conversation_id"),j.getString("sender_id"),j.getString("body"),j.getLong("seq"),
            runCatching {java.time.Instant.parse(time).toEpochMilli()}.getOrDefault(System.currentTimeMillis()),time,"SENT",mediaId=if(j.isNull("media_id"))"" else j.optString("media_id"),imageWidth=j.optInt("image_width"),imageHeight=j.optInt("image_height"))
    }
}
