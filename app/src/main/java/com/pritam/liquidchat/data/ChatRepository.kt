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
                // Re-register local schedules when this account signs in after a reboot.
                for(pendingSchedule in dao.allScheduledTexts(account)) {
                    checkAccount(account)
                    enqueueScheduledText(pendingSchedule)
                }
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
    fun scheduledTexts(cid:String)=backend.session.map {it?.userId}.distinctUntilChanged()
        .flatMapLatest {id->if(id==null)flowOf(emptyList()) else dao.scheduledTexts(id,cid).distinctUntilChanged()}
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
        require(palette in listOf("Ice","Lilac","Mint","Sand","Photo","Video"))
        val id=account()
        if(palette=="Photo")require(PersonalWallpaper.has(context,id,cid)) {"Choose a photo first."}
        if(palette=="Video")require(PrivateVideoWallpaper.has(context,id,cid)) {"Choose a video first."}
        dao.putAppearance(ChatAppearance(id,cid,palette));checkAccount(id)
    }

    suspend fun setCustomWallpaper(cid:String,uri:android.net.Uri,expectedAccount:String) {
        val id=account()
        require(id==expectedAccount) {"The active account changed while choosing the wallpaper."}
        require(dao.chat(id,cid)!=null) {"Open a valid conversation."}
        val saved=PersonalWallpaper.save(context,id,cid,uri)
        try {
            checkAccount(id)
            dao.putAppearance(ChatAppearance(id,cid,"Photo"))
            checkAccount(id)
        } catch(e:Throwable) {
            // An account switch can race Android's gallery/bitmap work.
            // Never leave a newly imported private photo after old-account erasure.
            if(backend.session.value?.userId!=id) {
                withContext(Dispatchers.IO) {runCatching {saved.delete()}}
            }
            throw e
        }
    }
    suspend fun setCustomVideoWallpaper(cid:String,uri:android.net.Uri,expectedAccount:String) {
        val id=account()
        require(id==expectedAccount) {"The active account changed while choosing the wallpaper."}
        require(dao.chat(id,cid)!=null) {"Open a valid conversation."}
        val saved=PrivateVideoWallpaper.save(context,id,cid,uri)
        try {
            checkAccount(id)
            dao.putAppearance(ChatAppearance(id,cid,"Video"))
            checkAccount(id)
        } catch(e:Throwable) {
            if(backend.session.value?.userId!=id) {
                withContext(Dispatchers.IO) {runCatching {saved.delete()}}
            }
            throw e
        }
    }
    /** Local, account-scoped scheduling; queued only when WorkManager reaches sendAt. */
    suspend fun scheduleText(cid:String,body:String,replyTo:String="",sendAt:Long,draftVersion:Long?=null):ScheduledText {
        val now=System.currentTimeMillis()
        require(body.isNotBlank() && body.length<=4000) {"Enter up to 4000 characters."}
        require(sendAt>=now+60_000L && sendAt<=now+180L*24L*3600_000L) {"Choose between 1 minute and 180 days from now."}
        val user=account()
        require(dao.chat(user,cid)!=null) {"Choose an existing conversation."}
        val scheduled=ScheduledText(user,UUID.randomUUID().toString(),cid,body,replyTo,sendAt,now)
        draftMutex.withLock {db.withTransaction {
            checkAccount(user)
            dao.putScheduledText(scheduled)
            if(draftVersion!=null)dao.consumeDraft(user,cid,body,replyTo,draftVersion)
        }}
        enqueueScheduledText(scheduled)
        return scheduled
    }

    /** Edit an undelivered local schedule without changing its idempotent send UUID. */
    suspend fun editScheduledText(cid:String,id:String,body:String,sendAt:Long) {
        val now=System.currentTimeMillis()
        require(body.isNotBlank() && body.length<=4000) {"Enter up to 4000 characters."}
        require(sendAt>=now+60_000L && sendAt<=now+180L*24L*3600_000L) {
            "Choose between 1 minute and 180 days from now."
        }
        val user=account()
        val edited=db.withTransaction {
            checkAccount(user)
            val old=dao.scheduledText(user,id)
                ?: throw IOException("The scheduled message has already been sent or removed.")
            require(old.conversation==cid) {"Wrong scheduled conversation."}
            check(dao.message(user,id)==null) {"This message is already being sent."}
            old.copy(body=body,sendAt=sendAt).also {dao.putScheduledText(it)}
        }
        checkAccount(user)
        // ExistingWorkPolicy.REPLACE cancels the old timer. Workers racing the
        // transaction cannot deliver an obsolete body after a successful edit.
        enqueueScheduledText(edited)
    }

    /** Moving schedule -> queued message is atomic and uses the same UUID for retry/idempotence. */
    suspend fun dispatchScheduledText(expected:String,id:String) {
        checkAccount(expected)
        var future:ScheduledText?=null
        var moved=false
        db.withTransaction {
            checkAccount(expected)
            val scheduled=dao.scheduledText(expected,id) ?: return@withTransaction
            if(scheduled.sendAt>System.currentTimeMillis()) {
                future=scheduled
                return@withTransaction
            }
            check(dao.chat(expected,scheduled.conversation)!=null) {"Scheduled conversation is no longer available."}
            if(dao.message(expected,id)==null) {
                val queued=Message(expected,scheduled.id,scheduled.conversation,expected,scheduled.body,replyTo=scheduled.replyTo)
                putVisible(queued)
                dao.pendingPreview(expected,scheduled.conversation,"Queued: ${scheduled.body.take(160)}",queued.localTime)
            }
            dao.deleteScheduledText(expected,id)
            moved=true
        }
        future?.let {enqueueScheduledText(it)}
        if(moved)enqueue(expected)
    }

    suspend fun cancelScheduledText(id:String) {
        val account=account()
        val deleted=db.withTransaction {
            checkAccount(account)
            dao.deleteScheduledText(account,id)
        }
        if(deleted==0)throw IOException("This scheduled message is already being sent or was removed.")
        WorkManager.getInstance(context).cancelUniqueWork("scheduled-$account-$id")
    }

    private fun enqueueScheduledText(item:ScheduledText) {
        val delay=(item.sendAt-System.currentTimeMillis()).coerceAtLeast(0L)
        val request=OneTimeWorkRequestBuilder<ScheduledTextWorker>()
            .setInputData(workDataOf("account" to item.account,"scheduledId" to item.id))
            .setInitialDelay(delay,TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS)
            .addTag("account-${item.account}")
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "scheduled-${item.account}-${item.id}",ExistingWorkPolicy.REPLACE,request)
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

    /**
     * Queue a selected multi-photo batch atomically. Each photo remains a normal
     * independently acknowledged message, but all local outbox rows are written
     * in one Room transaction before the worker can observe them.
     *
     * Caller retains ownership of the files until this call succeeds. On
     * success the durable outbox owns every file and removes it after delivery.
     * Caption belongs to the first photo, matching the reference preview.
     */
    suspend fun sendImages(cid:String,images:List<Images.Prepared>,caption:String="") {
        require(images.size in 1..10) {"Choose between one and ten photos."}
        require(caption.length<=4000) {"Photo caption is too long."}
        val owner=account()
        val root=java.io.File(context.filesDir,"media/$owner").canonicalFile
        val paths=images.map {it.file.canonicalPath}
        require(paths.distinct().size==paths.size) {"The same photo was selected twice."}
        images.forEach {image->
            require(image.file.isFile && image.file.canonicalFile.parentFile==root &&
                image.file.extension.lowercase()=="jpg" &&
                image.file.length() in 100..9_500_000L &&
                image.width in 1..3072 && image.height in 1..3072 &&
                image.width.toLong()*image.height<=25_000_000L) {
                "Only account-private prepared photos can be sent."
            }
        }
        val queued=images.mapIndexed {index,image->
            Message(owner,UUID.randomUUID().toString(),cid,owner,
                if(index==0) caption.ifBlank {"Photo"} else "Photo",
                localImage=image.file.absolutePath,
                imageWidth=image.width,imageHeight=image.height)
        }
        draftMutex.withLock {db.withTransaction {
            checkAccount(owner)
            queued.forEach {putVisible(it)}
            val latest=queued.last()
            dao.pendingPreview(owner,cid,"Queued: ${latest.body.take(160)}",latest.localTime)
        }}
        enqueue(owner)
    }

    suspend fun sendChatVideo(cid:String,video:PreparedChatVideo,caption:String="") {
        val account=account()
        require(caption.length<=1000)
        val root=java.io.File(context.filesDir,"media/$account").canonicalFile
        require(video.file.isFile && video.file.extension.lowercase()=="mp4"
            && video.file.canonicalFile.parentFile==root
            && video.file.length() in 100..ChatVideo.MAX_BYTES
            && video.durationMs in 500..30000
            && minOf(video.width,video.height) in 1..720
            && maxOf(video.width,video.height) in 1..1280) {
            "Invalid or foreign-account video."
        }
        val body=caption.trim().ifBlank {"Video"}
        val message=Message(account,UUID.randomUUID().toString(),cid,account,
            body,localImage=video.file.absolutePath,
            imageWidth=video.width,imageHeight=video.height,
            mediaKind="chat_video",durationMs=video.durationMs)
        db.withTransaction {
            checkAccount(account)
            putVisible(message)
            dao.pendingPreview(account,cid,"Queued: Video",message.localTime)
        }
        enqueue(account)
    }

    /**
     * Enqueue several private PDFs as independent messages in one durable
     * transaction. A failed batch leaves all local input files with the picker
     * and never uploads a partially created server album.
     */
    suspend fun sendDocuments(cid:String,documents:List<PreparedPdf>) {
        require(documents.size in 1..5) {"Choose up to five PDFs."}
        val account=account()
        val root=java.io.File(context.filesDir,"media/$account").canonicalFile
        val paths=documents.map {it.file.canonicalPath }
        require(paths.distinct().size==paths.size) {"Duplicate document."}
        val queued=documents.map {document->
            require(document.file.isFile &&
                document.file.canonicalFile.parentFile==root &&
                document.file.extension.lowercase()=="pdf" &&
                document.file.length() in 100..PdfDocuments.MAX_BYTES.toLong()) {
                "Invalid or foreign-account PDF."
            }
            val body="Document: ${PdfDocuments.safeName(document.name)}"
            Message(account,UUID.randomUUID().toString(),cid,account,
                body,localImage=document.file.absolutePath,mediaKind="document")
        }
        draftMutex.withLock {db.withTransaction {
            checkAccount(account)
            queued.forEach {putVisible(it)}
            val latest=queued.last()
            dao.pendingPreview(account,cid,"Queued: ${latest.body}",latest.localTime)
        }}
        enqueue(account)
    }

    suspend fun sendDocument(cid:String,pdf:PreparedPdf) {
        val account=account()
        val name=PdfDocuments.safeName(pdf.name)
        require(pdf.file.isFile && pdf.file.extension.lowercase()=="pdf"
            && pdf.file.length() in 100..PdfDocuments.MAX_BYTES.toLong())
        val root=java.io.File(context.filesDir,"media/$account").canonicalFile
        require(pdf.file.canonicalFile.parentFile==root) {
            "Document belongs to a different account."
        }
        val body="Document: $name"
        val message=Message(account,UUID.randomUUID().toString(),cid,account,
            body,localImage=pdf.file.absolutePath,mediaKind="document")
        db.withTransaction {
            checkAccount(account)
            putVisible(message)
            dao.pendingPreview(account,cid,"Queued: $body",message.localTime)
        }
        enqueue(account)
    }

    suspend fun sendSticker(cid:String,sticker:Images.Prepared) {
        val account=account()
        require(sticker.width==512 && sticker.height==512 &&
            sticker.file.isFile && sticker.file.length() in 100..524288 &&
            sticker.file.extension.lowercase()=="png")
        val root=java.io.File(context.filesDir,"media/$account").canonicalFile
        require(sticker.file.canonicalFile.parentFile==root) {
            "Sticker must belong to the signed-in account."
        }
        val message=Message(account,UUID.randomUUID().toString(),cid,account,
            "Sticker",localImage=sticker.file.absolutePath,imageWidth=512,
            imageHeight=512,mediaKind="sticker")
        db.withTransaction {
            checkAccount(account)
            putVisible(message)
            dao.pendingPreview(account,cid,"Queued: Sticker",message.localTime)
        }
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
                            backend.uploadImage(java.io.File(sending.localImage),m.conversation,
                                voice=sending.mediaKind=="voice",sticker=sending.mediaKind=="sticker",
                                document=sending.mediaKind=="document",
                                chatVideo=sending.mediaKind=="chat_video") {percent->
                                if(backend.session.value?.userId==expected)
                                    _uploadProgress.update {it+(key to percent)}
                            }
                        } finally {_uploadProgress.update {it-key}}
                        sending=sending.copy(mediaId=upload.getString("id"),imageWidth=upload.getInt("width"),imageHeight=upload.getInt("height"),durationMs=upload.optInt("duration_ms",sending.durationMs))
                        currentCoroutineContext().ensureActive();checkAccount(expected);putVisible(sending)
                    }
                    val raw=if(sending.mediaKind=="voice" && sending.mediaId.isNotEmpty()) backend.rpc("send_voice",JSONObject().put("p_id",m.id).put("p_media",sending.mediaId))
                        else if(sending.mediaKind=="sticker" && sending.mediaId.isNotEmpty())
                            backend.rpc("send_sticker",JSONObject().put("p_id",m.id).put("p_media",sending.mediaId))
                        else if(sending.mediaKind=="chat_video" && sending.mediaId.isNotEmpty())
                            backend.rpc("send_chat_video",JSONObject()
                                .put("p_id",m.id).put("p_media",sending.mediaId)
                                .put("p_caption",m.body))
                        else if(sending.mediaKind=="document" && sending.mediaId.isNotEmpty())
                            backend.rpc("send_document",JSONObject()
                                .put("p_id",m.id).put("p_media",sending.mediaId)
                                .put("p_name",m.body.removePrefix("Document: ")))
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

    /**
     * Bulk starring uses the same server-authorized message_action RPC as a
     * single star, but applies snapshots without restarting the entire
     * conversation sync after every selected message.
     */
    suspend fun starMessages(ids:List<String>,star:Boolean) {
        val unique=ids.distinct()
        require(unique.isNotEmpty() && unique.size<=30) {"Select 1–30 messages."}
        val account=account()
        syncMutex.withLock {
            unique.forEach {id->
                checkAccount(account)
                val result=JSONObject(backend.rpc("message_action",JSONObject()
                    .put("p_id",id).put("p_action","star")
                    .put("p_value",star.toString())))
                checkAccount(account)
                db.withTransaction {applySnapshot(result,account)}
            }
        }
        sync()
    }
    suspend fun chatAction(cid:String,action:String,enabled:Boolean) {
        backend.rpc("chat_action",JSONObject().put("p_conversation",cid).put("p_action",action).put("p_enabled",enabled));sync()
    }

    /**
     * Explicit "Mark as read" from the inbox applies the highest authenticated
     * server conversation sequence, even if older unread messages are not cached
     * on this device. Opening a chat still uses acknowledge() with receivedSeq.
     */
    suspend fun markChatRead(cid:String) {
        val id=account()
        val chat=dao.chat(id,cid) ?: return
        checkAccount(id)
        if(chat.manualUnread) {
            backend.rpc("chat_action",JSONObject()
                .put("p_conversation",cid).put("p_action","unread")
                .put("p_enabled",false))
            checkAccount(id)
        }
        if(chat.lastSeq>0L) {
            backend.rpc("acknowledge",JSONObject()
                .put("p_conversation",cid).put("p_seq",chat.lastSeq)
                .put("p_read",true))
            checkAccount(id)
        }
        sync()
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
        // Do not erase local drafts, future scheduled texts, or private appearance
        // before Supabase acknowledges the delete. A failed RPC must be reversible.
        backend.rpc("set_conversation",JSONObject().put("p_conversation",c.id).put("p_favorite",favorite).put("p_archived",archived).put("p_muted",muted).put("p_delete",delete))
        checkAccount(id)
        if(delete) {
            val scheduled=dao.scheduledTexts(id,c.id).first()
            drainMutex.withLock {draftMutex.withLock {db.withTransaction {
                checkAccount(id)
                dao.clearScheduledConversation(id,c.id)
                dao.removeHistory(id,c.id)
                dao.removeDraft(id,c.id)
                dao.removeAppearance(id,c.id)
            }}}
            scheduled.forEach {WorkManager.getInstance(context).cancelUniqueWork("scheduled-$id-${it.id}")}
            withContext(Dispatchers.IO) {PersonalWallpaper.clearConversation(context,id,c.id)}
            ConversationNotificationChannels.clearConversation(context,id,c.id)
        }
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
        // Clean up contact names stored as system notification channel labels.
        // A channel cleanup failure must not block the more important local data erasure.
        runCatching {ConversationNotificationChannels.clearAccount(context,account)}
        syncMutex.withLock {drainMutex.withLock {draftMutex.withLock {db.withTransaction {dao.clearMessages(account);dao.clearChats(account);dao.clearContacts(account);dao.clearProfile(account);dao.clearDrafts(account);dao.clearAppearance(account);dao.clearHidden(account);dao.clearScheduledTexts(account)}}}}
        withContext(Dispatchers.IO) {
            for(directory in listOf(java.io.File(context.filesDir,"media/$account"),java.io.File(context.filesDir,"wallpapers/$account"),java.io.File(context.cacheDir,"camera"))) {
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
