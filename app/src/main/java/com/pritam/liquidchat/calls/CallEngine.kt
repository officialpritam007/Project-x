package com.pritam.liquidchat.calls

import android.content.Context
import android.media.AudioManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import org.webrtc.*
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

/** Do not silently choose a headset when the requested call route is unavailable. */
internal fun matchingCommunicationDeviceId(devices:List<Pair<Int,Int>>,wantedType:Int):Int? =
    devices.firstOrNull {it.second==wantedType}?.first

internal fun interruptsCallAudio(change:Int):Boolean = change==AudioManager.AUDIOFOCUS_LOSS ||
    change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT || change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK

/** User-started WebRTC peer: DTLS/SRTP transports media directly; Supabase carries SDP/ICE only. */
class CallEngine(private val context: Context, private val video: Boolean,
                 private val sendCandidate: (JSONObject) -> Unit,
                 private val connectionChanged: (String) -> Unit,
                 private val frameReceived:(Boolean)->Unit = {},
                 private val routesChanged:()->Unit = {},
                 private val iceServers:List<PeerConnection.IceServer> = emptyList()) : Closeable {
    companion object {
        private val initialized = AtomicBoolean(false)

        @Synchronized
        internal fun createFactory(context: Context, eglContext: EglBase.Context): PeerConnectionFactory {
            if (initialized.compareAndSet(false, true)) {
                PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions
                    .builder(context.applicationContext).createInitializationOptions())
            }
            // Camera preview bypasses codecs. Without these factories audio can connect
            // while SDP has no usable video codecs and both remote surfaces stay black.
            return PeerConnectionFactory.builder()
                .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglContext, true, false))
                .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglContext))
                .createPeerConnectionFactory()
        }
    }
    private val egl = EglBase.create()
    private var factory: PeerConnectionFactory? = null
    private var peer: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var audioTrack: AudioTrack? = null
    private var videoSource: VideoSource? = null
    private var videoTrack: VideoTrack? = null
    private var remoteVideo: VideoTrack? = null
    private var remoteAudio:AudioTrack?=null
    private var capturer: VideoCapturer? = null
    private var texture: SurfaceTextureHelper? = null
    private var localRenderer: CallTextureRenderer? = null
    private var remoteRenderer: CallTextureRenderer? = null
    private val rendererLock = Any()
    private var remoteReady = false
    private val queuedIce = mutableListOf<IceCandidate>()
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val originalMode = audioManager.mode
    private val originalSpeaker = audioManager.isSpeakerphoneOn
    private var audioFocusRequest: AudioFocusRequest? = null
    private var routeListener:AudioManager.OnCommunicationDeviceChangedListener?=null
    private val resourceGate = CallResourceGate()
    private val closed get()=resourceGate.closed

    private var cameraRunning=false
    private var cameraWanted=true
    private var micMuted=false
    @Volatile private var held=false
    private var frontCamera=true
    private var manualRoute:Int?=null
    private var phoneBusy=false
    private val telephony=context.getSystemService(android.telephony.TelephonyManager::class.java)
    private val phoneListener=object:android.telephony.PhoneStateListener() {
        override fun onCallStateChanged(state:Int,number:String?) {
            if(closed)return
            val wasBusy=phoneBusy
            phoneBusy=state!=android.telephony.TelephonyManager.CALL_STATE_IDLE
            if(phoneBusy)connectionChanged("On hold")
            else if(wasBusy)connectionChanged("Focus restored")
        }
    }
    private val scoReceiver=object:android.content.BroadcastReceiver() {
        override fun onReceive(context:Context,intent:android.content.Intent) {if(!closed)routesChanged()}
    }
    fun refreshAudioRoute() {
        refreshRoute()
        if(!closed&&androidx.core.content.ContextCompat.checkSelfPermission(context,android.Manifest.permission.READ_PHONE_STATE)==android.content.pm.PackageManager.PERMISSION_GRANTED)
            telephony.listen(phoneListener,android.telephony.PhoneStateListener.LISTEN_CALL_STATE)
    }
    fun resumeAudio():Boolean=!phoneBusy&&audioFocusRequest?.let {audioManager.requestAudioFocus(it)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED}==true

    private val handler=android.os.Handler(android.os.Looper.getMainLooper())
    private val deviceCallback=object:android.media.AudioDeviceCallback() {
        override fun onAudioDevicesAdded(devices:Array<out android.media.AudioDeviceInfo>) {refreshRoute()}
        override fun onAudioDevicesRemoved(devices:Array<out android.media.AudioDeviceInfo>) {refreshRoute()}
    }
    private fun refreshRoute() {
        if(closed)return
        runCatching {
            if(android.os.Build.VERSION.SDK_INT>=31) {
                val devices=audioManager.availableCommunicationDevices
                val selected=manualRoute?.let {id->devices.firstOrNull {it.id==id}}
                if(selected==null)manualRoute=null
                val preferredId=preferredCallDevice(devices.map {it.id to it.type},manualRoute,video)
                val preferred=devices.firstOrNull {it.id==preferredId}
                preferred?.let {audioManager.setCommunicationDevice(it)}
            } else {
                val hasBluetooth=audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any {it.type in listOf(7,8)}
                if(manualRoute== -3&&!hasBluetooth)manualRoute=null
                if(manualRoute==null) {
                val bluetooth=audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any {it.type in listOf(7,8)}
                audioManager.isSpeakerphoneOn=!bluetooth&&video
                if(bluetooth){audioManager.startBluetoothSco();audioManager.isBluetoothScoOn=true}
                else {audioManager.stopBluetoothSco();audioManager.isBluetoothScoOn=false}
                }
            }
            Unit
        }
        routesChanged()
    }
    fun start():Unit = resourceGate.withOpen {
        val focusRequest=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build())
            .setOnAudioFocusChangeListener {change->
                // A cellular/other audio interruption must not leave live call
                // capture running silently behind a lost or ducked audio session.
                if(!closed&&interruptsCallAudio(change))
                    connectionChanged(if(change==AudioManager.AUDIOFOCUS_LOSS)"Focus lost permanently" else "On hold")
                else if(!closed&&change==AudioManager.AUDIOFOCUS_GAIN)connectionChanged("Focus restored")
            }
            .build()
        if(audioManager.requestAudioFocus(focusRequest)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            error("Could not acquire audio focus")
        audioFocusRequest=focusRequest
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        // Android 12+ routes calls with setCommunicationDevice, not the deprecated
        // isSpeakerphoneOn flag; selecting speaker matters especially for video calls.
        audioManager.registerAudioDeviceCallback(deviceCallback,handler)
        if(android.os.Build.VERSION.SDK_INT>=31) {
            routeListener=AudioManager.OnCommunicationDeviceChangedListener {if(!closed)routesChanged()}
            audioManager.addOnCommunicationDeviceChangedListener(context.mainExecutor,routeListener!!)
        }
        androidx.core.content.ContextCompat.registerReceiver(context,scoReceiver,android.content.IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED),androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
        if(androidx.core.content.ContextCompat.checkSelfPermission(context,android.Manifest.permission.READ_PHONE_STATE)==android.content.pm.PackageManager.PERMISSION_GRANTED)
            telephony.listen(phoneListener,android.telephony.PhoneStateListener.LISTEN_CALL_STATE)
        refreshRoute()
        factory = createFactory(context, egl.eglBaseContext)
        val config = PeerConnection.RTCConfiguration(listOf(
            PeerConnection.IceServer.builder("stun:stun.cloudflare.com:3478").createIceServer()
        )+iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        peer = factory!!.createPeerConnection(config, object : PeerConnection.Observer {
            override fun onSignalingChange(newState: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {}
            override fun onConnectionChange(newState:PeerConnection.PeerConnectionState?) {
                if(closed)return
                // Combined peer state includes DTLS. ICE alone is not media readiness.
                when(newState) {
                    PeerConnection.PeerConnectionState.CONNECTING -> connectionChanged("Connecting")
                    PeerConnection.PeerConnectionState.CONNECTED -> connectionChanged("Connected")
                    PeerConnection.PeerConnectionState.DISCONNECTED -> connectionChanged("Reconnecting")
                    PeerConnection.PeerConnectionState.FAILED -> connectionChanged("Connection failed")
                    PeerConnection.PeerConnectionState.CLOSED -> connectionChanged("Ended")
                    else -> Unit
                }
            }
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidate(candidate: IceCandidate?) {
                if (candidate != null) sendCandidate(JSONObject().put("sdpMid",candidate.sdpMid ?: "0")
                    .put("sdpMLineIndex",candidate.sdpMLineIndex).put("candidate",candidate.sdp))
            }
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                (receiver?.track() as? VideoTrack)?.let(::receiveVideo)
                (receiver?.track() as? AudioTrack)?.let(::receiveAudio)
            }
            override fun onTrack(transceiver: RtpTransceiver?) {
                (transceiver?.receiver?.track() as? VideoTrack)?.let(::receiveVideo)
                (transceiver?.receiver?.track() as? AudioTrack)?.let(::receiveAudio)
            }
        }) ?: error("Could not create WebRTC connection")
        audioSource = factory!!.createAudioSource(MediaConstraints())
        audioTrack = factory!!.createAudioTrack("liquid-audio",audioSource).also {
            peer!!.addTrack(it,listOf("liquid-stream"))
        }
        if (video) {
            val enumerator = Camera2Enumerator(context)
            val name = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
                ?: enumerator.deviceNames.firstOrNull()
                ?: error("No camera is available")
            capturer = enumerator.createCapturer(name,null)
                ?: error("Unable to open camera")
            videoSource = factory!!.createVideoSource(false)
            texture = SurfaceTextureHelper.create("LiquidCallCamera",egl.eglBaseContext)
            capturer!!.initialize(texture,context,videoSource!!.capturerObserver)
            frontCamera=enumerator.isFrontFacing(name)
            capturer!!.startCapture(640,480,24)
            cameraRunning=true
            videoTrack = factory!!.createVideoTrack("liquid-video",videoSource).also {
                peer!!.addTrack(it,listOf("liquid-stream"))
            }
        }
    }

    // Native observer callbacks must not wait for the disposal gate: peer.close()
    // may itself be waiting for the observer thread. This lock is released before close.
    private fun receiveAudio(track:AudioTrack) {
        synchronized(rendererLock) {
            if(!closed){remoteAudio=track;track.setEnabled(!held)}
        }
    }

    private fun receiveVideo(track: VideoTrack) = synchronized(rendererLock) {
        if (!closed && remoteVideo !== track) {
            remoteRenderer?.let { remoteVideo?.removeSink(it) }
            remoteVideo = track
            remoteRenderer?.let(track::addSink)
        }
    }

    fun attachRenderer(renderer: CallTextureRenderer, local: Boolean) = synchronized(rendererLock) {
        if (closed) return@synchronized
        renderer.init(egl.eglBaseContext) {frameReceived(local)}
        renderer.setMirror(local&&frontCamera)
        // Composition may attach a replacement before disposing the previous view.
        if(local)localRenderer?.let {videoTrack?.removeSink(it)}
        else remoteRenderer?.let {remoteVideo?.removeSink(it)}
        if (local) { localRenderer=renderer; videoTrack?.addSink(renderer) }
        else { remoteRenderer=renderer; remoteVideo?.addSink(renderer) }
    }

    fun detachRenderer(renderer: CallTextureRenderer, local: Boolean) {
        synchronized(rendererLock) {
            if (local && localRenderer===renderer) {
                videoTrack?.removeSink(renderer);localRenderer=null
            } else if (!local && remoteRenderer===renderer) {
                remoteVideo?.removeSink(renderer);remoteRenderer=null
            }
        }
        renderer.release()
    }

    fun mute(mute:Boolean) = resourceGate.ifOpen {micMuted=mute;audioTrack?.setEnabled(!mute&&!held);Unit}
    fun hold(value:Boolean) = resourceGate.ifOpen {
        held=value
        peer?.setAudioRecording(!held)
        peer?.setAudioPlayout(!held)
        audioTrack?.setEnabled(!micMuted&&!held)
        synchronized(rendererLock){remoteAudio?.setEnabled(!held)}
        updateCapture()
    }
    private fun updateCapture() {
        val enabled=CaptureIntent(micMuted,cameraWanted,held).captureVideo
        videoTrack?.setEnabled(enabled)
        if(enabled&&!cameraRunning&&capturer!=null) {capturer?.startCapture(640,480,24);cameraRunning=true}
        if(!enabled&&cameraRunning) {capturer?.stopCapture();cameraRunning=false}
    }
    fun setCameraEnabled(enabled:Boolean) = resourceGate.ifOpen {cameraWanted=enabled;updateCapture()}
    fun switchCamera() = resourceGate.ifOpen {
        if(cameraRunning)(capturer as? CameraVideoCapturer)?.switchCamera(object:CameraVideoCapturer.CameraSwitchHandler {
            override fun onCameraSwitchDone(isFrontCamera:Boolean) {synchronized(rendererLock){frontCamera=isFrontCamera;localRenderer?.setMirror(frontCamera)}}
            override fun onCameraSwitchError(error:String?) {}
        })
    }
    fun updateIceServers(servers:List<PeerConnection.IceServer>)=resourceGate.ifOpen {
        if(servers.isNotEmpty())peer?.setConfiguration(PeerConnection.RTCConfiguration(listOf(PeerConnection.IceServer.builder("stun:stun.cloudflare.com:3478").createIceServer())+servers).apply {sdpSemantics=PeerConnection.SdpSemantics.UNIFIED_PLAN})
        Unit
    }
    fun restartIce(servers:List<PeerConnection.IceServer> = emptyList())=resourceGate.ifOpen {
        updateIceServers(servers);peer?.restartIce();Unit
    }
    fun speaker(enabled:Boolean):Boolean {
        if(android.os.Build.VERSION.SDK_INT>=31) {
            val type=if(enabled)android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER else android.media.AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
            val devices=audioManager.availableCommunicationDevices
            val id=matchingCommunicationDeviceId(devices.map {it.id to it.type},type) ?: return false
            val device=devices.first {it.id==id}
            return selectAudioRoute(device.id)
        }
        return selectAudioRoute(if(enabled)-2 else -1)
    }
    fun audioRoutes():List<Pair<Int,String>> = runCatching {
        if(android.os.Build.VERSION.SDK_INT>=31)audioManager.availableCommunicationDevices.map {it.id to deviceName(it)}
        else buildList {
            add(-1 to "Earpiece / wired headset");add(-2 to "Speaker")
            if(audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any {it.type in listOf(7,8)})add(-3 to "Bluetooth headset")
        }
    }.getOrDefault(emptyList())
    private fun deviceName(d:android.media.AudioDeviceInfo):String=when(d.type) {
        android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER->"Speaker"
        android.media.AudioDeviceInfo.TYPE_BUILTIN_EARPIECE->"Earpiece"
        else->d.productName.toString()
    }
    fun routeName():String=if(android.os.Build.VERSION.SDK_INT>=31)audioManager.communicationDevice?.let(::deviceName).orEmpty() else if(audioManager.isBluetoothScoOn)"Bluetooth headset" else if(audioManager.isSpeakerphoneOn)"Speaker" else if(audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any {it.type in listOf(3,4,22)})"Wired headset" else "Earpiece"
    fun selectAudioRoute(id:Int):Boolean=runCatching {
        val accepted=if(android.os.Build.VERSION.SDK_INT>=31)audioManager.availableCommunicationDevices.firstOrNull {it.id==id}?.let {audioManager.setCommunicationDevice(it)} ?: false
        else {audioManager.isSpeakerphoneOn=id== -2;if(id== -3){audioManager.startBluetoothSco();audioManager.isBluetoothScoOn=true}else {audioManager.stopBluetoothSco();audioManager.isBluetoothScoOn=false};true}
        if(accepted)manualRoute=id
        accepted
    }.getOrDefault(false)

    private fun activePeer():PeerConnection=peer?.takeIf {!closed}
        ?: throw java.io.IOException("Call already ended")

    private suspend fun createDescription(offer:Boolean): SessionDescription {
        return suspendCancellableCoroutine { cont ->
            val observer = object: SdpObserver {
                override fun onCreateSuccess(sdp: SessionDescription?) {
                    if(sdp!=null && cont.isActive)cont.resume(sdp)
                    else if(cont.isActive)cont.resumeWithException(IllegalStateException("Empty call description"))
                }
                override fun onSetSuccess() {}
                override fun onCreateFailure(error:String?) {
                    if(cont.isActive)cont.resumeWithException(IllegalStateException(error ?: "SDP failed"))
                }
                override fun onSetFailure(error:String?) {}
            }
            resourceGate.withOpen {
                val connection=activePeer()
                if(offer)connection.createOffer(observer,MediaConstraints())
                else connection.createAnswer(observer,MediaConstraints())
            }
        }
    }

    private suspend fun setDescription(sdp:SessionDescription, local:Boolean) {
        return suspendCancellableCoroutine<Unit> {cont ->
            val observer=object:SdpObserver {
                override fun onCreateSuccess(sdp:SessionDescription?) {}
                override fun onSetSuccess() {if(cont.isActive)cont.resume(Unit)}
                override fun onCreateFailure(error:String?) {}
                override fun onSetFailure(error:String?) {
                    if(cont.isActive)cont.resumeWithException(IllegalStateException(error ?: "SDP rejected"))
                }
            }
            resourceGate.withOpen {
                val connection=activePeer()
                if(local)connection.setLocalDescription(observer,sdp)
                else connection.setRemoteDescription(observer,sdp)
            }
        }
    }

    suspend fun createOffer():String {
        val offer=createDescription(true)
        setDescription(offer,true)
        return offer.description
    }
    suspend fun answerOffer(offer:String):String {
        setRemote("offer",offer)
        val answer=createDescription(false)
        setDescription(answer,true)
        return answer.description
    }
    suspend fun acceptAnswer(answer:String) {setRemote("answer",answer)}
    private suspend fun setRemote(type:String,description:String) {
        setDescription(SessionDescription(SessionDescription.Type.fromCanonicalForm(type),description),false)
        resourceGate.withOpen {
            synchronized(queuedIce) {
                remoteReady=true
                queuedIce.forEach {activePeer().addIceCandidate(it)}
                queuedIce.clear()
            }
        }
    }
    fun remoteCandidate(json:JSONObject) = resourceGate.ifOpen {
        val ice=IceCandidate(json.optString("sdpMid","0"),json.optInt("sdpMLineIndex"),json.getString("candidate"))
        synchronized(queuedIce) {
            if(remoteReady) peer?.addIceCandidate(ice)
            else if(queuedIce.size<256) queuedIce.add(ice)
            // A peer cannot force unbounded pre-answer ICE memory growth.
        }
    }

    override fun close() = resourceGate.close {
        synchronized(rendererLock) {
            localRenderer?.let {videoTrack?.removeSink(it)}
            remoteRenderer?.let {remoteVideo?.removeSink(it)}
            localRenderer=null
            remoteRenderer=null
            remoteVideo=null;remoteAudio=null
        }
        synchronized(queuedIce) {queuedIce.clear();remoteReady=false}
        try {capturer?.stopCapture()} catch(_:InterruptedException) {Thread.currentThread().interrupt()} catch(_:RuntimeException) {}
        runCatching {capturer?.dispose()}
        runCatching {texture?.dispose()}
        runCatching {peer?.close()};runCatching {peer?.dispose()}
        runCatching {videoTrack?.dispose()};runCatching {audioTrack?.dispose()}
        runCatching {videoSource?.dispose()};runCatching {audioSource?.dispose()}
        runCatching {factory?.dispose()}
        runCatching {context.unregisterReceiver(scoReceiver)}
        runCatching {telephony.listen(phoneListener,android.telephony.PhoneStateListener.LISTEN_NONE)}
        if(android.os.Build.VERSION.SDK_INT>=31)routeListener?.let {audioManager.removeOnCommunicationDeviceChangedListener(it)}
        routeListener=null
        audioManager.unregisterAudioDeviceCallback(deviceCallback)
        audioFocusRequest?.let {audioManager.abandonAudioFocusRequest(it)}
        audioFocusRequest=null
        if(android.os.Build.VERSION.SDK_INT>=31)audioManager.clearCommunicationDevice()
        else {audioManager.stopBluetoothSco();audioManager.isBluetoothScoOn=false}
        audioManager.mode=originalMode
        audioManager.isSpeakerphoneOn=originalSpeaker
        egl.release()
    }
}
