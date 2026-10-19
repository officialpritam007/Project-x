package com.pritam.liquidchat.calls

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.pritam.liquidchat.data.Backend
import com.pritam.liquidchat.data.Session
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

data class CallView(
    val id:String?,val conversation:String,val peer:String,val kind:String,
    val incoming:Boolean,val status:String,val muted:Boolean=false,
    val cameraEnabled:Boolean=true,val speaker:Boolean=false,
    val connectedAt:Long=0,val minimized:Boolean=false,val audioRoute:String="",
    val remoteVideoReady:Boolean=false,val answerRequested:Boolean=false,
    val compact:Boolean=false
) {
    val video get()=kind=="video"
    // An accepted incoming call keeps its direction, but is no longer an
    // unanswered invitation that should disappear when the app backgrounds.
    val unansweredIncoming get()=incoming && status=="Incoming call"
    // Direction is immutable: accepted incoming calls remain incoming, but must show video.
    val renderVideo get()=video && id!=null && status!="Incoming call"
}

private data class CallRecord(
    val id:String,val conversation:String,val caller:String,val callee:String,
    val kind:String,val state:String,val offer:String,val answer:String,val expiresAt:Instant
) {
    companion object {
        fun from(j:JSONObject):CallRecord=CallRecord(
            j.getString("id"),j.getString("conversation_id"),j.getString("caller_id"),
            j.getString("callee_id"),j.getString("kind"),j.getString("state"),
            j.getString("offer"),j.optString("answer",""),
            Instant.parse(j.getString("expires_at"))
        )
    }
}
private data class CallPoll(val call:CallRecord?,val candidates:JSONArray)

/** Network completion must not replace a newer outgoing call or another login's UI. */
internal fun canPresentIncomingCall(
    current:CallView?,expectedAccount:String,actualAccount:String?,visible:Boolean,
    expectedEpoch:Long?,actualEpoch:Long?
):Boolean=current==null && visible && expectedAccount==actualAccount &&
    expectedEpoch!=null && expectedEpoch==actualEpoch

private class CallApi(private val backend:Backend) {
    private suspend fun poll(id:String?,after:Long=0):CallPoll {
        val request=JSONObject().put("p_call",id ?: JSONObject.NULL).put("p_after",after)
        val json=JSONObject(backend.rpc("call_poll",request))
        return CallPoll(json.optJSONObject("call")?.let(CallRecord::from),
            json.optJSONArray("signals") ?: JSONArray())
    }
    suspend fun incoming()=poll(null).call
    suspend fun active(id:String,after:Long)=poll(id,after)
    suspend fun start(cid:String,kind:String,offer:String)=backend.rpc("call_start",JSONObject()
        .put("p_conversation",cid).put("p_kind",kind).put("p_offer",offer)).trim('"')
    suspend fun respond(id:String,accept:Boolean,answer:String?=null) {
        backend.rpc("call_respond",JSONObject().put("p_call",id).put("p_accept",accept)
            .put("p_answer",answer ?: JSONObject.NULL))
    }
    suspend fun candidate(id:String,payload:JSONObject) {
        backend.rpc("call_candidate",JSONObject().put("p_call",id).put("p_payload",payload))
    }
    suspend fun heartbeat(id:String) {backend.rpc("call_heartbeat",JSONObject().put("p_call",id))}
    suspend fun hangup(id:String) {backend.rpc("call_hangup",JSONObject().put("p_call",id))}
}

/**
 * Explicit foreground-only 1:1 call beta; never runs a microphone/camera service in the background.
 * No pay-per-minute provider, media server, or recording. STUN-only means some NATs cannot connect.
 */
class CallController(
    private val context:Context, private val backend:Backend,
    private val foreground:StateFlow<Boolean>, private val scope:CoroutineScope
) {
    private val api=CallApi(backend)
    private val _view=MutableStateFlow<CallView?>(null)
    val view=_view.asStateFlow()
    @Volatile private var owner:String?=null
    @Volatile private var engine:CallEngine?=null
    private var operation:Job?=null
    private var polling:Job?=null
    private var incomingWatch:Job?=null
    private var reconnectWatch:Job?=null
    private var connectWatch:Job?=null
    @Volatile private var mediaConnected=false
    @Volatile private var connectionGeneration=0L
    private var iceSending:Job?=null
    private var candidateQueue:Channel<JSONObject>?=null
    private var lastCandidate=0L

    init {
        if(com.pritam.liquidchat.BuildConfig.ENABLE_EXPERIMENTAL_CALLS) scope.launch {
            backend.session.map {it?.userId}.distinctUntilChanged().collect {id->
                if(owner!=null && owner!=id)stopLocal()
            }
        }
        if(com.pritam.liquidchat.BuildConfig.ENABLE_EXPERIMENTAL_CALLS) scope.launch {
            combine(backend.session,foreground) {s,fg->s?.userId.takeIf {fg}}
                .collectLatest {id->
                    if(id==null) {
                        val existing=_view.value
                        if(existing!=null && !foreground.value) {
                            if(existing.unansweredIncoming) {
                                // Only unaccepted invitations are dismissed in background.
                                // Accepted incoming calls use the same foreground service
                                // continuity and safety checks as outgoing calls.
                                stopLocal()
                            } else if(!ActiveCallService.running) {
                                // If the foreground notification/service did not start, never keep capture alive.
                                hangup()
                            }
                        } else if(existing!=null) {
                            hangup()
                        }
                        return@collectLatest
                    }
                    while(currentCoroutineContext().isActive) {
                        if(_view.value==null) {
                            try {
                                val epoch=backend.uiReads.epoch(id)
                                val incoming=api.incoming()
                                if(incoming!=null && incoming.callee==id &&
                                    canPresentIncomingCall(_view.value,id,backend.session.value?.userId,
                                        foreground.value,epoch,backend.uiReads.epoch(id)) &&
                                    incoming.expiresAt.isAfter(Instant.now()) && incoming.state=="ringing") {
                                    owner=id
                                    _view.value=CallView(incoming.id,incoming.conversation,incoming.caller,
                                        incoming.kind,true,"Incoming call",speaker=incoming.kind=="video")
                                    watchIncoming(incoming.id,id)
                                }
                            } catch(e:CancellationException){throw e}catch(_:Exception){}
                        }
                        delay(3500)
                    }
                }
        }
    }

    fun start(conversation:String,peer:String,kind:String) {
        if(!com.pritam.liquidchat.BuildConfig.ENABLE_EXPERIMENTAL_CALLS||_view.value!=null||backend.session.value==null||!foreground.value)return
        val account=backend.session.value!!.userId
        _view.value=CallView(null,conversation,peer,kind,false,"Preparing",speaker=kind=="video")
        owner=account
        operation=scope.launch {
            try {
                val connection=newEngine(kind=="video")
                val offer=connection.createOffer()
                ensureOwner(account)
                val id=api.start(conversation,kind,offer)
                ensureOwner(account)
                _view.value=_view.value?.copy(id=id,status="Ringing")
                begin(id,account)
                // Best-effort authenticated wakeup; a failed push never aborts foreground calling.
                scope.launch(Dispatchers.IO) {
                    for(attempt in 0..2) {
                        if(owner!=account||_view.value?.id!=id)return@launch
                        try {
                            val result=JSONObject(backend.request("/functions/v1/call-notify","POST",JSONObject().put("callId",id)))
                            if(result.optBoolean("complete")||!result.optBoolean("accepted"))return@launch
                        } catch(e:CancellationException){throw e}catch(_:Exception){}
                        delay(5000L)
                    }
                }
            }catch(e:CancellationException){throw e}
            catch(e:Exception){failure(e)}
        }
    }

    fun openIncomingNotification(callId:String,expectedAccount:String,answerRequested:Boolean=false) {
        if(!com.pritam.liquidchat.BuildConfig.ENABLE_EXPERIMENTAL_CALLS
            ||runCatching {UUID.fromString(callId)}.isFailure)return
        scope.launch {
            try {
                backend.restore()
                foreground.filter {it}.first()
                if(backend.session.value?.userId!=expectedAccount)return@launch
                if(_view.value?.id==callId) {
                    _view.value=_view.value?.copy(minimized=false,answerRequested=answerRequested);return@launch
                }
                if(_view.value!=null)return@launch
                val epoch=backend.uiReads.epoch(expectedAccount)
                val call=api.active(callId,0).call ?: return@launch
                if(!canPresentIncomingCall(_view.value,expectedAccount,backend.session.value?.userId,
                    foreground.value,epoch,backend.uiReads.epoch(expectedAccount)))return@launch
                if(call.callee!=expectedAccount||call.state!="ringing"
                    ||call.expiresAt.isBefore(Instant.now()))return@launch
                owner=expectedAccount
                _view.value=CallView(call.id,call.conversation,call.caller,call.kind,true,"Incoming call",speaker=call.kind=="video",answerRequested=answerRequested)
                watchIncoming(call.id,expectedAccount)
            }catch(e:CancellationException){throw e}
            catch(_:Exception){}
        }
    }

    fun accept() {
        val current=_view.value ?: return
        if(!current.incoming||current.status!="Incoming call"||current.id==null||engine!=null||!foreground.value)return
        incomingWatch?.cancel();incomingWatch=null
        val account=backend.session.value?.userId ?: return
        _view.value=current.copy(status="Connecting",answerRequested=false)
        dismissNotification(current.id)
        operation=scope.launch {
            try {
                val snapshot=api.active(current.id,0).call ?: error("Call expired")
                if(snapshot.callee!=account||snapshot.state!="ringing"||snapshot.expiresAt.isBefore(Instant.now())) error("Call expired")
                val connection=newEngine(snapshot.kind=="video")
                val answer=connection.answerOffer(snapshot.offer)
                ensureOwner(account)
                api.respond(snapshot.id,true,answer)
                // Direction is immutable; status alone marks the invitation answered.
                _view.value=_view.value?.copy(status="Connecting",answerRequested=false)
                begin(snapshot.id,account)
            }catch(e:CancellationException){throw e}catch(e:Exception){failure(e)}
        }
    }

    fun decline() {
        val current=_view.value ?: return
        val id=current.id
        id?.let(::dismissNotification)
        stopLocal()
        if(id!=null)scope.launch {try {api.respond(id,false)}catch(_:Exception){}}
    }

    fun hangup() {
        val id=_view.value?.id
        stopLocal()
        if(id!=null)scope.launch {try {api.hangup(id)}catch(_:Exception){}}
    }

    private fun watchIncoming(callId:String,account:String) {
        incomingWatch?.cancel()
        incomingWatch=scope.launch {
            val deadline=android.os.SystemClock.elapsedRealtime()+45000L
            while(isActive && owner==account && _view.value?.id==callId && _view.value?.incoming==true) {
                delay(1500)
                if(android.os.SystemClock.elapsedRealtime()>=deadline){stopLocal();break}
                try {
                    val call=api.active(callId,0).call
                    if(call==null || call.state!="ringing" || call.expiresAt.isBefore(Instant.now())) {
                        stopLocal()
                        break
                    }
                } catch(e:CancellationException) {throw e}
                catch(_:Exception) { /* Transient signaling failure: do not falsely report a decline. */ }
            }
        }
    }

    private fun newEngine(video:Boolean):CallEngine {
        val generation=++connectionGeneration
        mediaConnected=false
        val candidateChannel=Channel<JSONObject>(256)
        candidateQueue=candidateChannel
        val connection=CallEngine(context,video,
            {candidate->if(generation==connectionGeneration)candidateChannel.trySend(candidate)},
            {status->scope.launch {
                if(generation!=connectionGeneration)return@launch // Ignore stale native callbacks after teardown.
                val state=_view.value ?: return@launch
                if(state.incoming&&state.status=="Incoming call")return@launch
                _view.value=state.copy(status=status,connectedAt=if(status=="Connected"&&state.connectedAt==0L)android.os.SystemClock.elapsedRealtime() else state.connectedAt)
                when(status) {
                    "Connected" -> {
                        mediaConnected=true
                        connectWatch?.cancel();connectWatch=null
                        reconnectWatch?.cancel();reconnectWatch=null
                    }
                    "Reconnecting" -> {
                        reconnectWatch?.cancel()
                        reconnectWatch=scope.launch {
                            delay(20_000L)
                            if(generation==connectionGeneration && _view.value?.status=="Reconnecting")
                                failure(java.io.IOException("Connection lost. Please try again."))
                        }
                    }
                    "Connection failed" -> failure(java.io.IOException("Call could not connect. A TURN relay may be required."))
                    "Audio focus lost" -> failure(java.io.IOException("Call interrupted by another audio session."))
                    "Ended" -> hangup()
                }
            }}, {local->if(!local)scope.launch {if(generation==connectionGeneration)_view.value=_view.value?.copy(remoteVideoReady=true)}})
        engine=connection
        // The service is launched only by a visible, permission-accepting user action.
        ActiveCallService.onHangup={hangup()}
        ActiveCallService.start(context,video)
        connection.start()
        return connection
    }

    private fun begin(id:String,account:String) {
        lastCandidate=0
        val queue=candidateQueue ?: error("No local ICE queue")
        iceSending=scope.launch(Dispatchers.IO) {
            for(candidate in queue) {
                if(owner!=account||_view.value?.id!=id)break
                for(attempt in 0..2) {
                    try {api.candidate(id,candidate);break}
                    catch(e:CancellationException){throw e}
                    catch(e:Exception) {
                        if(attempt==2) {withContext(Dispatchers.Main){failure(java.io.IOException("Call signaling could not recover."))};return@launch}
                        delay(500L*(attempt+1))
                    }
                }
            }
        }
        polling=scope.launch(Dispatchers.IO) {
            var remoteAnswer=false
            var lastHeartbeat=System.currentTimeMillis()
            var lastSuccess=android.os.SystemClock.elapsedRealtime()
            try {
                while(isActive&&owner==account&&_view.value?.id==id) {
                    val snapshot=try {api.active(id,lastCandidate)} catch(e:CancellationException){throw e} catch(e:Exception) {
                        if(e is com.pritam.liquidchat.data.ApiException && e.status in listOf(400,401,403,404))throw e
                        if(android.os.SystemClock.elapsedRealtime()-lastSuccess>20000L)throw java.io.IOException("Signaling connection lost. Please call again.")
                        delay(1500);continue
                    }
                    lastSuccess=android.os.SystemClock.elapsedRealtime()
                    val call=snapshot.call
                    if(call==null || call.expiresAt.isBefore(Instant.now()) ||
                        call.state=="ended"||call.state=="declined") {
                        withContext(Dispatchers.Main) {stopLocal()}
                        return@launch
                    }
                    if(call.state=="accepted" && !mediaConnected && connectWatch==null) {
                        val generation=connectionGeneration
                        connectWatch=scope.launch {
                            delay(30_000L)
                            if(generation==connectionGeneration && !mediaConnected && _view.value?.id==id)
                                failure(java.io.IOException("Call connection timed out; this network may need TURN."))
                        }
                    }
                    if(call.state=="accepted" && call.answer.isNotBlank() && call.caller==account && !remoteAnswer) {
                        engine?.acceptAnswer(call.answer)
                        remoteAnswer=true
                    }
                    if(call.state=="accepted" && System.currentTimeMillis()-lastHeartbeat>12000L) {
                        try {api.heartbeat(id);lastHeartbeat=System.currentTimeMillis()}catch(e:CancellationException){throw e}catch(_:java.io.IOException){}
                    }
                    for(i in 0 until snapshot.candidates.length()) {
                        val row=snapshot.candidates.getJSONObject(i)
                        lastCandidate=maxOf(lastCandidate,row.getLong("id"))
                        if(row.optString("sender_id")!=account)engine?.remoteCandidate(row.getJSONObject("payload"))
                    }
                    delay(1300)
                }
            }catch(e:CancellationException){throw e}
            catch(e:Exception){withContext(Dispatchers.Main){failure(e)}}
        }
    }

    fun toggleMute() {
        val v=_view.value ?: return
        engine?.mute(!v.muted);_view.value=v.copy(muted=!v.muted)
    }
    fun toggleCamera() {
        val v=_view.value ?: return
        engine?.setCameraEnabled(!v.cameraEnabled);_view.value=v.copy(cameraEnabled=!v.cameraEnabled)
    }
    fun switchCamera() {engine?.switchCamera()}
    fun toggleSpeaker() {
        val v=_view.value ?: return
        val connection=engine ?: return
        // Keep the displayed route synchronized with what Android actually accepted.
        if(connection.speaker(!v.speaker)) {
            val route=connection.routeName()
            _view.value=v.copy(speaker=route=="Speaker",audioRoute=route)
        }
    }
    fun minimize() {_view.value=_view.value?.copy(minimized=true,compact=false)}
    fun expand() {_view.value=_view.value?.copy(minimized=false,compact=false)}
    // Pure presentation state: switching between floating video and a compact pill never
    // touches WebRTC, signaling, microphone, camera capture or existing call identity.
    fun toggleCompact() {_view.value=_view.value?.let {if(it.minimized&&it.video)it.copy(compact=!it.compact) else it}}
    fun audioRoutes()=engine?.audioRoutes().orEmpty()
    fun selectAudioRoute(id:Int) {if(engine?.selectAudioRoute(id)==true)_view.value=_view.value?.copy(audioRoute=engine?.routeName().orEmpty(),speaker=engine?.routeName()=="Speaker")}
    private fun dismissNotification(id:String) {context.getSystemService(android.app.NotificationManager::class.java).cancel(id.hashCode())}
    fun renderer()=engine

    private fun ensureOwner(account:String) {
        if(owner!=account||backend.session.value?.userId!=account||
            (!foreground.value&&!ActiveCallService.running))
            throw CancellationException("Account changed or media service unavailable")
    }
    private fun failure(e:Exception) {
        val previous=_view.value
        val id=previous?.id
        stopLocal()
        if(id!=null) scope.launch(Dispatchers.IO) {
            try {api.hangup(id)}catch(_:Exception){}
        }
        if(previous!=null) _view.value=previous.copy(status=e.message ?: "Call unavailable",id=null,incoming=false,minimized=false,answerRequested=false)
    }
    fun dismissError() {
        if(_view.value?.id==null)stopLocal()
    }
    private fun stopLocal() {
        _view.value?.id?.let(::dismissNotification)
        operation?.cancel();operation=null
        connectionGeneration++
        mediaConnected=false
        connectWatch?.cancel();connectWatch=null
        reconnectWatch?.cancel();reconnectWatch=null
        incomingWatch?.cancel();incomingWatch=null
        polling?.cancel();polling=null
        iceSending?.cancel();iceSending=null
        candidateQueue?.close();candidateQueue=null
        engine?.close();engine=null
        ActiveCallService.onHangup=null
        ActiveCallService.stop(context)
        owner=null
        _view.value=null
        lastCandidate=0
    }
}
