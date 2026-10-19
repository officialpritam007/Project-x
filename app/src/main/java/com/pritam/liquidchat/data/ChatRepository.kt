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

/**
 * Reuse a render snapshot when an RPC returns identical presence/typing data.
 * JSONObject compares by identity, so replacing it every three seconds otherwise
 * causes avoidable Compose updates and visible header jitter.
 */
internal fun stableContactSnapshot(previous:JSONObject,incoming:JSONObject):JSONObject =
    if(previous.toString()==incoming.toString())previous else incoming

/**
 * Do not invalidate the entire inbox on every periodic or Realtime sync.
 * A changed chat is still upserted, including updated previews, receipts,
 * unread count, pinned/favourite/archive and synchronized cursors.
 */
internal fun changedChatRows(incoming:List<Chat>,existing:List<Chat>):List<Chat> {
    val previous=existing.associateBy {it.account to it.id}
    return incoming.filter {previous[it.account to it.id]!=it}
}

/** Never keep deleted text, attachments or reply metadata in newly cached tombstones. */
internal fun scrubDeletedMessage(message:Message):Message =
    if(message.deletedAt.isEmpty())message else message.copy(
        body="This message was deleted",mediaId="",localImage="",
        imageWidth=0,imageHeight=0,mediaKind="image",durationMs=0,
        replyTo="",reactions="{}",starred=false
    )

data class MessageRevocation(val account:String,val id:String)

internal fun changedContactRows(incoming:List<Contact>,existing:List<Contact>):List<Contact> {
    val previous=existing.associateBy {it.account to it.id}
    return incoming.filter {previous[it.account to it.id]!=it}
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ChatRepository(private val context:Context,val backend:Backend,val db:LocalDatabase) {
    val dao=db.dao()
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val syncMutex=Mutex()
    private val drainMutex=Mutex()
    private val draftMutex=Mutex()
    val contactState=MutableStateFlow(JSONObject())
    val privateSettings=MutableStateFlow(JSONObject())
    val foreground=MutableStateFlow(false)
    val active=MutableStateFlow<String?>(null)
    private val _connection=MutableStateFlow("Connecting…")
    val connection=_connection.asStateFlow()
    private val _uploadProgress=MutableStateFlow<Map<Pair<String,String>,Int>>(emptyMap())
    fun uploadProgress(account:String,id:String)=_uploadProgress.map {it[account to id]}.distinctUntilChanged()
    private val _revocations=MutableSharedFlow<MessageRevocation>(extraBufferCapacity=64)
    val revocations=_revocations.asSharedFlow()
    private val signals=MutableSharedFlow<Unit>(extraBufferCapacity=1)
    init {
        // Account-private flags must never appear in another signed-in account's menus.
        scope.launch {
            backend.session.map {it?.userId}.distinctUntilChanged().collect {
                privateSettings.value=JSONObject()
            }
        }
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
                        // A new conversation cannot display the previous peer's presence.
                        contactState.value=JSONObject()
                        if(cid!=null) while(isActive) {
                            try {
                                val refreshed=JSONObject(backend.rpc("contact_state",JSONObject().put("p_conversation",cid)))
                                contactState.value=stableContactSnapshot(contactState.value,refreshed)
                            } catch(e:CancellationException){throw e}
                              catch(_:Exception) {
                                // Fail closed on connection or authorization failure, but
                                // do not re-emit an identical empty state every poll.
                                contactState.value=stableContactSnapshot(contactState.value,JSONObject())
                              }
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
        checkAccount(id)
        db.withTransaction {
            val previous=dao.allContacts(id)
            val changed=changedContactRows(contacts,previous)
            if(changed.isNotEmpty())dao.putContacts(changed)
            val visibleIds=contacts.mapTo(HashSet(contacts.size)) {it.id}
            for(contact in previous) if(contact.id !in visibleIds)dao.removeContact(id,contact.id)
        }
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
                    if(j.isNull("last_at"))"" else j.optString("last_at"),if(j.isNull("avatar_id"))"" else j.optString("avatar_id"),pending?.localTime ?: 0,old?.changeCursor ?: 0,
                    if(j.isNull("pinned_at"))0 else runCatching {java.time.Instant.parse(j.getString("pinned_at")).toEpochMilli()}.getOrDefault(0),j.optBoolean("manual_unread"))
            }
            if(page.length()<100) break
            val last=page.getJSONObject(page.length()-1);before=last.getLong("last_seq");beforeId=last.getString("id")
        } while(true)
        checkAccount(account)
        db.withTransaction {
            val existing=dao.allChats(account)
            val changed=changedChatRows(chats,existing)
            // Room invalidations force inbox and avatar recomposition even if
            // the server snapshot has not changed. Avoid redundant upserts.
            if(changed.isNotEmpty())dao.putChats(changed)
            val incomingIds=chats.mapTo(HashSet(chats.size)) {it.id}
            for(c in existing) if(c.id !in incomingIds) {
                dao.removeHistory(account,c.id)
                dao.deleteChat(account,c.id)
            }
        }
        for(c in chats) {
            checkAccount(account)
            // A periodic sync with no stale messages must not issue a DELETE.
            // Even deleting zero rows can invalidate Room observers and churn the
            // message LazyColumn. Still prune whenever local stale history exists.
            if(c.deletedSeq>0 && dao.hasPrunableMessages(account,c.id,c.deletedSeq))
                dao.prune(account,c.id,c.deletedSeq)
            if(c.lastSeq>c.syncSeq) {
                var cursor=c.syncSeq
                do {
                    val order=if(cursor==0L) "desc" else "asc"
                    val limit=100
                    val page=JSONArray(backend.rpc("message_history",JSONObject().put("p_conversation",c.id).put("p_after",maxOf(cursor,c.deletedSeq)).put("p_desc",order=="desc")))
                    val messages=(0 until page.length()).map {parseMessage(page.getJSONObject(it),account)}
                    checkAccount(account)
                    db.withTransaction {
                        for(m in messages) {
                            val old=dao.message(account,m.id)
                            putVisible(m.copy(localTime=old?.localTime ?: m.localTime))
                        }
                        if(messages.isNotEmpty()) {cursor=messages.maxOf {it.seq};dao.syncCursor(account,c.id,cursor)}
                    }
                    if(order=="desc" || messages.size<limit) break
                } while(true)
                dao.syncCursor(account,c.id,maxOf(cursor,c.lastSeq))
                acknowledge(c.id,read=false)
            }
            var changedCursor=c.changeCursor
            do {
                val updates=JSONArray(backend.rpc("message_changes",JSONObject().put("p_conversation",c.id).put("p_after",changedCursor)))
                checkAccount(account)
                db.withTransaction {
                    for(i in 0 until updates.length()) {
                        val j=updates.getJSONObject(i);applySnapshot(j,account,existingOnly=true)
                        changedCursor=maxOf(changedCursor,j.getLong("change_seq"))
                    }
                    // An unchanged cursor is not a database mutation. Updating
                    // it every sync forces inbox re-collection even with no changes.
                    if(changedCursor>c.changeCursor)dao.changeCursor(account,c.id,changedCursor)
                }
                if(updates.length()<100)break
            }while(true)
        }
    }
    suspend fun loadOlder(cid:String):Boolean=syncMutex.withLock {
        val account=account();val before=dao.minSeq(account,cid) ?: Long.MAX_VALUE
        val watermark=dao.chat(account,cid)?.deletedSeq ?: 0
        val page=JSONArray(backend.rpc("message_history",JSONObject().put("p_conversation",cid).put("p_before",before).put("p_after",watermark)))
        val messages=(0 until page.length()).map {parseMessage(page.getJSONObject(it),account)}
        checkAccount(account)
        db.withTransaction {for(m in messages) {val old=dao.message(account,m.id);putVisible(m.copy(localTime=old?.localTime ?: m.localTime))}}
        messages.size==100
    }
    suspend fun reveal(cid:String,id:String) {
        val account=account()
        if(dao.message(account,id)!=null)return
        val target=JSONObject(backend.rpc("message_by_id",JSONObject().put("p_id",id)))
        checkAccount(account)
        require(!target.optBoolean("hidden")&&target.optString("conversation_id")==cid) {"Message unavailable"}
        val seq=target.getLong("seq")
        while((dao.minSeq(account,cid) ?: Long.MAX_VALUE)>seq) {
            currentCoroutineContext().ensureActive();checkAccount(account)
            val more=loadOlder(cid)
            if(!more)break
        }
    }
    suspend fun acknowledge(cid:String,read:Boolean) {
        val account=account();val seq=dao.receivedSeq(account,cid)
        if(read&&dao.chat(account,cid)?.manualUnread==true)backend.rpc("chat_action",JSONObject().put("p_conversation",cid).put("p_action","unread").put("p_enabled",false))
        if(seq>0) backend.rpc("acknowledge",JSONObject().put("p_conversation",cid).put("p_seq",seq).put("p_read",read))
    }
    suspend fun find(username:String):JSONObject? = JSONArray(backend.rpc("find_profile",JSONObject().put("p_username",username))).optJSONObject(0)
    suspend fun start(peer:String):String {
        val id=JSONTokener(backend.rpc("start_conversation",JSONObject().put("p_peer",peer))).nextValue() as String
        sync();return id
    }
    suspend fun draft(cid:String):Draft? = draftMutex.withLock {val id=backend.session.value?.userId ?: return@withLock null;dao.draft(id,cid).also {checkAccount(id);DraftVersions.observe(it?.updatedAt ?: 0)}}
    fun saveDraft(cid:String,body:String,replyTo:String=""):Long {
        val id=backend.session.value?.userId ?: return 0
        val version=DraftVersions.next()
        // Start in call order, keep the lock across Room IO, and finish independently of composition.
        scope.launch(start=CoroutineStart.UNDISPATCHED) {draftMutex.withLock {
            checkAccount(id)
            if(body.isEmpty()&&replyTo.isEmpty())dao.removeDraft(id,cid) else dao.putDraft(Draft(id,cid,body,replyTo,updatedAt=version))
        }}
        return version
    }
    suspend fun palette(cid:String):String {val id=backend.session.value?.userId ?: return "Ice";return dao.appearance(id,cid)?.palette ?: "Ice"}
    suspend fun setPalette(cid:String,palette:String) {
        require(palette in listOf("Ice","Lilac","Mint","Sand"));val id=account()
        dao.putAppearance(ChatAppearance(id,cid,palette));checkAccount(id)
    }
    suspend fun send(cid:String,body:String,replyTo:String="",draftVersion:Long?=null) {
        require(body.isNotBlank() && body.length<=4000)
        val account=account()
        val message=Message(account,UUID.randomUUID().toString(),cid,account,body,replyTo=replyTo)
        draftMutex.withLock {db.withTransaction {
            checkAccount(account);putVisible(message);dao.pendingPreview(account,cid,"Queued: ${message.body.take(160)}",message.localTime)
            if(draftVersion!=null)dao.consumeDraft(account,cid,body,replyTo,draftVersion)
        }}
        enqueue(account)
    }
    /**
     * Forward an already-visible text message to another authenticated 1:1 chat.
     * Never forward a media placeholder or a hidden/deleted message as plain text.
     * The ordinary durable outbox assigns a new ID and enforces server block/RLS rules.
     */
    suspend fun forwardText(source:Message,destination:String) {
        val account=account()
        require(source.account==account && source.status=="SENT" && source.deletedAt.isEmpty())
        require(source.mediaId.isEmpty() && source.localImage.isEmpty() && source.mediaKind=="image")
        require(destination!=source.conversation) {"Choose a different conversation."}
        val current=dao.message(account,source.id) ?: throw IOException("Source message is no longer available.")
        require(!dao.hidden(account,source.id) && current.conversation==source.conversation &&
            current.status=="SENT" && current.deletedAt.isEmpty() &&
            current.mediaId.isEmpty() && current.localImage.isEmpty() && current.mediaKind=="image" &&
            current.body==source.body && current.body.isNotBlank())
        require(dao.chat(account,destination)!=null) {"Choose an existing contact."}
        checkAccount(account)
        send(destination,current.body)
    }

    suspend fun sendImage(cid:String,image:Images.Prepared,caption:String,draftVersion:Long?=null) {
        val account=account()
        val message=Message(account,UUID.randomUUID().toString(),cid,account,caption.ifBlank {"Photo"},localImage=image.file.absolutePath,imageWidth=image.width,imageHeight=image.height)
        draftMutex.withLock {db.withTransaction {
            checkAccount(account);putVisible(message);dao.pendingPreview(account,cid,"Queued: ${message.body.take(160)}",message.localTime)
            if(draftVersion!=null)dao.consumeDraft(account,cid,caption,"",draftVersion)
        }}
        enqueue(account)
    }
    suspend fun sendVoice(cid:String,file:java.io.File,duration:Int) {
        val account=account()
        require(duration in 500..120000 && file.isFile && file.length() in 1..2097152)
        require(file.canonicalPath.startsWith(java.io.File(context.filesDir,"media/$account").canonicalPath+java.io.File.separator))
        val message=Message(account,UUID.randomUUID().toString(),cid,account,"Voice message",localImage=file.absolutePath,mediaKind="voice",durationMs=duration)
        db.withTransaction {checkAccount(account);putVisible(message);dao.pendingPreview(account,cid,"Queued: Voice message",message.localTime)}
        enqueue(account)
    }
    suspend fun discardUnsent(id:String) {
        val account=account()
        drainMutex.withLock {
            val m=dao.message(account,id) ?: return@withLock
            require(m.status in listOf("QUEUED","FAILED")) {"This message has already started sending."}
            checkAccount(account);db.withTransaction {dao.hide(HiddenMessage(account,id));dao.removeMessage(account,id);refreshLocalPreview(account,m.conversation)}
            if(m.localImage.isNotEmpty())java.io.File(m.localImage).delete()
        }
        try {sync()}catch(e:CancellationException){throw e}catch(_:Exception){refresh()}
    }
    suspend fun retry(id:String) {
        val account=account()
        val requeued=db.withTransaction {
            checkAccount(account)
            val m=dao.message(account,id) ?: return@withTransaction false
            if(m.status!="FAILED" || dao.hidden(account,id))return@withTransaction false
            putVisible(m.copy(status="QUEUED",attempts=0,error=""))
            dao.pendingPreview(account,m.conversation,"Queued: ${m.body.take(160)}",m.localTime)
            true
        }
        if(requeued)enqueue(account)
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
            if((m.status!="QUEUED" && m.status!="SENDING")||dao.hidden(expected,m.id)) continue
            currentCoroutineContext().ensureActive();checkAccount(expected)
            var sending=m.copy(status="SENDING",attempts=m.attempts+1,error="")
            putVisible(sending)
            try {
                var accepted:JSONObject?=JSONArray(backend.request("/rest/v1/messages?id=eq.${m.id}&conversation_id=eq.${m.conversation}&sender_id=eq.$expected&select=*&limit=1")).optJSONObject(0)
                if(sending.localImage.isNotEmpty()) {
                    if(accepted==null && sending.mediaId.isNotEmpty()) {
                        try {backend.rpc("image_access",JSONObject().put("p_id",sending.mediaId))}
                        catch(e:ApiException) {if(e.status==403 || e.code=="42501")sending=sending.copy(mediaId="") else throw e}
                    }
                }
                if(accepted==null) {
                    if(sending.localImage.isNotEmpty() && sending.mediaId.isEmpty()) {
                        val key=expected to m.id
                        val upload=try {
                            backend.uploadImage(java.io.File(sending.localImage),m.conversation,sending.mediaKind=="voice") {percent->
                                if(backend.session.value?.userId==expected)
                                    _uploadProgress.update {it+(key to percent)}
                            }
                        } finally {_uploadProgress.update {it-key}}
                        sending=sending.copy(mediaId=upload.getString("id"),imageWidth=upload.getInt("width"),imageHeight=upload.getInt("height"),durationMs=upload.optInt("duration_ms",sending.durationMs))
                        currentCoroutineContext().ensureActive();checkAccount(expected);putVisible(sending)
                    }
                    val raw=if(sending.mediaKind=="voice" && sending.mediaId.isNotEmpty()) backend.rpc("send_voice",JSONObject().put("p_id",m.id).put("p_media",sending.mediaId))
                        else if(sending.mediaId.isNotEmpty()) backend.rpc("send_image",JSONObject().put("p_id",m.id).put("p_media",sending.mediaId).put("p_caption",m.body))
                        else if(sending.replyTo.isNotEmpty())backend.rpc("send_reply",JSONObject().put("p_id",m.id).put("p_conversation",m.conversation).put("p_body",m.body).put("p_reply",sending.replyTo))
                        else backend.rpc("send_text",JSONObject().put("p_id",m.id).put("p_conversation",m.conversation).put("p_body",m.body))
                    val parsed=JSONTokener(raw).nextValue()
                    accepted=if(parsed is JSONArray)parsed.getJSONObject(0) else parsed as JSONObject
                }
                val j=accepted ?: throw IOException("Message not accepted yet. Try again.")
                currentCoroutineContext().ensureActive();checkAccount(expected)
                val snapshot=JSONObject(backend.rpc("message_by_id",JSONObject().put("p_id",j.getString("id"))))
                currentCoroutineContext().ensureActive();checkAccount(expected)
                applySnapshot(snapshot,expected)
                if(m.localImage.isNotEmpty())java.io.File(m.localImage).delete()
                refresh()
                // Durable server queue owns retries; FCM submission does not mark delivery.
                runCatching {backend.request("/functions/v1/notify","POST",JSONObject())}
            } catch(e:CancellationException) {throw e}
            catch(e:Exception) {
                val transient=e !is ApiException || e.status>=500 || e.status==429 || e.code=="P0001"
                val retry=Delivery.retryable(sending.status,sending.attempts,transient)
                currentCoroutineContext().ensureActive();checkAccount(expected)
                if(!dao.hidden(expected,m.id)&&dao.message(expected,m.id)?.status!="SENT")db.withTransaction {
                    putVisible(sending.copy(status=if(retry)"QUEUED" else "FAILED",error=e.message ?: "Send failed"))
                    val latest=dao.latestUnsent(expected,m.conversation)
                    if(latest!=null)dao.pendingPreview(expected,m.conversation,"${latest.status.lowercase().replaceFirstChar {it.uppercase()}}: ${latest.body.take(160)}",latest.localTime)
                }
                if(retry)return@withLock false
            }
        }
        true
    }
    private suspend fun refreshLocalPreview(account:String,cid:String) {
        val latest=dao.latestMessage(account,cid)
        dao.localPreview(account,cid,latest?.let {if(it.status=="SENT")it.body.take(160) else "${it.status.lowercase().replaceFirstChar {it.uppercase()}}: ${it.body.take(160)}"}.orEmpty(),latest?.takeIf {it.status!="SENT"}?.localTime ?: 0,latest?.serverTime.orEmpty())
    }
    private suspend fun putVisible(message:Message)=db.withTransaction {
        if(message.deletedAt.isNotEmpty())_revocations.emit(MessageRevocation(message.account,message.id))
        if(!dao.hidden(message.account,message.id)&&
            (message.seq==0L||message.seq>(dao.chat(message.account,message.conversation)?.deletedSeq ?: 0)))
            dao.putPreservingAcceptance(scrubDeletedMessage(message))
    }
    internal suspend fun applySnapshot(j:JSONObject,account:String,existingOnly:Boolean=false) {
        val id=j.getString("id");val old=dao.message(account,id)
        if(j.optBoolean("hidden")) {
            _revocations.emit(MessageRevocation(account,id))
            dao.hide(HiddenMessage(account,id))
            old?.localImage?.takeIf {it.isNotEmpty()}?.let {java.io.File(it).delete()}
            dao.removeMessage(account,id)
            if(old!=null)refreshLocalPreview(account,old.conversation)
            context.getSystemService(android.app.NotificationManager::class.java).cancel(id.hashCode())
        } else if(old==null&&existingOnly) {
            // An older Shared Media item need not exist in Room. Publish only its
            // revocation identity; never cache the old body just to notify viewers.
            if(!j.isNull("deleted_at")&&j.optString("deleted_at").isNotBlank()) {
                _revocations.emit(MessageRevocation(account,id))
                context.getSystemService(android.app.NotificationManager::class.java).cancel(id.hashCode())
            }
        } else {
            val parsed=parseMessage(j,account)
            val message=parsed.copy(localTime=old?.localTime ?: parsed.localTime)
            if(message.deletedAt.isNotEmpty()) {
                old?.localImage?.takeIf {it.isNotEmpty()}?.let {java.io.File(it).delete()}
                context.getSystemService(android.app.NotificationManager::class.java).cancel(id.hashCode())
            }
            putVisible(message)
        }
    }
    suspend fun messageAction(id:String,action:String,value:String="") {
        val account=account()
        syncMutex.withLock {
            val result=JSONObject(backend.rpc("message_action",JSONObject().put("p_id",id).put("p_action",action).put("p_value",value)))
            checkAccount(account);db.withTransaction {applySnapshot(result,account)}
        }
        sync()
    }
    suspend fun chatAction(cid:String,action:String,enabled:Boolean) {
        backend.rpc("chat_action",JSONObject().put("p_conversation",cid).put("p_action",action).put("p_enabled",enabled));sync()
    }
    suspend fun starred(cid:String,before:Long=Long.MAX_VALUE):List<Message> {
        val id=account();val a=JSONArray(backend.rpc("starred_messages",JSONObject().put("p_conversation",cid).put("p_before",before)))
        checkAccount(id);return (0 until a.length()).map {parseMessage(a.getJSONObject(it),id)}
    }
    suspend fun sharedMedia(cid:String,before:Long=Long.MAX_VALUE,kind:String="image"):List<Message> {
        val id=account();val a=JSONArray(backend.rpc("shared_media",JSONObject().put("p_conversation",cid).put("p_before",before).put("p_kind",kind)))
        checkAccount(id);return (0 until a.length()).map {parseMessage(a.getJSONObject(it),id)}
    }
    suspend fun setChat(c:Chat,favorite:Boolean=c.favorite,archived:Boolean=c.archived,muted:Boolean=c.muted,delete:Boolean=false) {
        val id=account()
        backend.rpc("set_conversation",JSONObject().put("p_conversation",c.id).put("p_favorite",favorite).put("p_archived",archived).put("p_muted",muted).put("p_delete",delete))
        checkAccount(id)
        if(delete)drainMutex.withLock {draftMutex.withLock {db.withTransaction {dao.removeHistory(id,c.id);dao.removeDraft(id,c.id)}}}
        sync()
    }
    /**
     * Supabase blocks_owner SELECT policy only exposes blocks created by
     * this user. A peer blocking us must not appear as an owned block.
     */
    suspend fun ownBlockState(peer:String):Boolean {
        val id=account()
        require(runCatching {UUID.fromString(peer)}.isSuccess) {"Invalid contact."}
        val rows=JSONArray(backend.request("/rest/v1/blocks?actor=eq.$id&target=eq.$peer&select=target&limit=1"))
        checkAccount(id)
        return rows.length()>0
    }
    suspend fun block(peer:String,blocked:Boolean) {
        val id=account()
        require(runCatching {UUID.fromString(peer)}.isSuccess) {"Invalid contact."}
        backend.rpc("set_block",JSONObject().put("p_peer",peer).put("p_blocked",blocked))
        checkAccount(id)
    }
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
        syncMutex.withLock {drainMutex.withLock {draftMutex.withLock {db.withTransaction {dao.clearMessages(account);dao.clearChats(account);dao.clearContacts(account);dao.clearProfile(account);dao.clearDrafts(account);dao.clearAppearance(account);dao.clearHidden(account)}}}}
        withContext(Dispatchers.IO) {
            for(directory in listOf(java.io.File(context.filesDir,"media/$account"),java.io.File(context.cacheDir,"camera"))) {
                check(!directory.exists() || directory.deleteRecursively()) {"Local media cleanup could not finish. Clear this app's stored data in Android Settings to complete removal."}
            }
            coil.Coil.imageLoader(context).diskCache?.clear()
        }
        coil.Coil.imageLoader(context).memoryCache?.clear()
    }
    private fun parseMessage(j:JSONObject,account:String):Message {
        val time=j.getString("created_at")
        return Message(account,j.getString("id"),j.getString("conversation_id"),j.getString("sender_id"),j.getString("body"),j.getLong("seq"),
            runCatching {java.time.Instant.parse(time).toEpochMilli()}.getOrDefault(System.currentTimeMillis()),time,"SENT",mediaId=if(j.isNull("media_id"))"" else j.optString("media_id"),imageWidth=j.optInt("image_width"),imageHeight=j.optInt("image_height"),mediaKind=j.optString("media_kind","image"),durationMs=j.optInt("duration_ms"),replyTo=if(j.isNull("reply_to_id"))"" else j.optString("reply_to_id"),editedAt=if(j.isNull("edited_at"))"" else j.optString("edited_at"),deletedAt=if(j.isNull("deleted_at"))"" else j.optString("deleted_at"),reactions=j.optJSONObject("reactions")?.toString() ?: "{}",starred=j.optBoolean("starred"))
    }
}
