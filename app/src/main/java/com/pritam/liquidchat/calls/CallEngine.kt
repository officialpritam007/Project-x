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

/** Foreground-only WebRTC peer: DTLS/SRTP transports media directly; Supabase carries SDP/ICE only. */
class CallEngine(private val context: Context, private val video: Boolean,
                 private val sendCandidate: (JSONObject) -> Unit,
                 private val connectionChanged: (String) -> Unit) : Closeable {
    companion object {
        private val initialized = AtomicBoolean(false)
    }
    private val egl = EglBase.create()
    private var factory: PeerConnectionFactory? = null
    private var peer: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var audioTrack: AudioTrack? = null
    private var videoSource: VideoSource? = null
    private var videoTrack: VideoTrack? = null
    private var remoteVideo: VideoTrack? = null
    private var capturer: VideoCapturer? = null
    private var texture: SurfaceTextureHelper? = null
    private var localRenderer: SurfaceViewRenderer? = null
    private var remoteRenderer: SurfaceViewRenderer? = null
    private var remoteReady = false
    private val queuedIce = mutableListOf<IceCandidate>()
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val originalMode = audioManager.mode
    private val originalSpeaker = audioManager.isSpeakerphoneOn
    private var audioFocusRequest: AudioFocusRequest? = null
    private var closed = false

    fun start() {
        if (initialized.compareAndSet(false, true)) {
            PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(context.applicationContext).createInitializationOptions())
        }
        val focusRequest=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build())
            .setOnAudioFocusChangeListener {change->
                if(change==AudioManager.AUDIOFOCUS_LOSS)
                    connectionChanged("Audio focus lost")
            }
            .build()
        if(audioManager.requestAudioFocus(focusRequest)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            error("Could not acquire audio focus")
        audioFocusRequest=focusRequest
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = video
        factory = PeerConnectionFactory.builder().createPeerConnectionFactory()
        val config = PeerConnection.RTCConfiguration(listOf(
            PeerConnection.IceServer.builder("stun:stun.cloudflare.com:3478").createIceServer()
        )).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        peer = factory!!.createPeerConnection(config, object : PeerConnection.Observer {
            override fun onSignalingChange(newState: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                when (newState) {
                    PeerConnection.IceConnectionState.CONNECTED,
                    PeerConnection.IceConnectionState.COMPLETED -> connectionChanged("Connected")
                    PeerConnection.IceConnectionState.DISCONNECTED -> connectionChanged("Reconnecting")
                    PeerConnection.IceConnectionState.FAILED -> connectionChanged("Connection failed")
                    PeerConnection.IceConnectionState.CLOSED -> connectionChanged("Ended")
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
            }
            override fun onTrack(transceiver: RtpTransceiver?) {
                (transceiver?.receiver?.track() as? VideoTrack)?.let(::receiveVideo)
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
            capturer!!.startCapture(640,480,24)
            videoTrack = factory!!.createVideoTrack("liquid-video",videoSource).also {
                peer!!.addTrack(it,listOf("liquid-stream"))
            }
        }
    }

    private fun receiveVideo(track: VideoTrack) {
        if (remoteVideo !== track) {
            remoteVideo = track
            remoteRenderer?.let(track::addSink)
        }
    }

    fun attachRenderer(renderer: SurfaceViewRenderer, local: Boolean) {
        renderer.init(egl.eglBaseContext,null)
        renderer.setEnableHardwareScaler(true)
        renderer.setMirror(local)
        if (local) { localRenderer=renderer; videoTrack?.addSink(renderer) }
        else { remoteRenderer=renderer; remoteVideo?.addSink(renderer) }
    }

    fun detachRenderer(renderer: SurfaceViewRenderer, local: Boolean) {
        if (local) {videoTrack?.removeSink(renderer);if(localRenderer===renderer)localRenderer=null}
        else {remoteVideo?.removeSink(renderer);if(remoteRenderer===renderer)remoteRenderer=null}
        renderer.release()
    }

    fun mute(mute:Boolean) {audioTrack?.setEnabled(!mute)}
    fun setCameraEnabled(enabled:Boolean) {videoTrack?.setEnabled(enabled)}
    fun switchCamera() {(capturer as? CameraVideoCapturer)?.switchCamera(null)}
    fun speaker(enabled:Boolean) {audioManager.isSpeakerphoneOn=enabled}

    private suspend fun createDescription(offer:Boolean): SessionDescription =
        suspendCancellableCoroutine { cont ->
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
            if(offer)peer?.createOffer(observer,MediaConstraints())
            else peer?.createAnswer(observer,MediaConstraints())
        }

    private suspend fun setDescription(sdp:SessionDescription, local:Boolean) =
        suspendCancellableCoroutine<Unit> {cont ->
            val observer=object:SdpObserver {
                override fun onCreateSuccess(sdp:SessionDescription?) {}
                override fun onSetSuccess() {if(cont.isActive)cont.resume(Unit)}
                override fun onCreateFailure(error:String?) {}
                override fun onSetFailure(error:String?) {
                    if(cont.isActive)cont.resumeWithException(IllegalStateException(error ?: "SDP rejected"))
                }
            }
            if(local)peer?.setLocalDescription(observer,sdp)
            else peer?.setRemoteDescription(observer,sdp)
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
        synchronized(queuedIce) {
            remoteReady=true
            queuedIce.forEach {peer?.addIceCandidate(it)}
            queuedIce.clear()
        }
    }
    fun remoteCandidate(json:JSONObject) {
        val ice=IceCandidate(json.optString("sdpMid","0"),json.optInt("sdpMLineIndex"),json.getString("candidate"))
        synchronized(queuedIce) {
            if(remoteReady) peer?.addIceCandidate(ice)
            else if(queuedIce.size<256) queuedIce.add(ice)
            // A peer cannot force unbounded pre-answer ICE memory growth.
        }
    }

    override fun close() {
        if(closed)return
        closed=true
        synchronized(queuedIce) {queuedIce.clear();remoteReady=false}
        localRenderer?.let {videoTrack?.removeSink(it)}
        remoteRenderer?.let {remoteVideo?.removeSink(it)}
        localRenderer=null
        remoteRenderer=null
        try {capturer?.stopCapture()} catch(_:InterruptedException) {Thread.currentThread().interrupt()}
        capturer?.dispose()
        texture?.dispose()
        peer?.close();peer?.dispose()
        videoTrack?.dispose();audioTrack?.dispose()
        videoSource?.dispose();audioSource?.dispose()
        factory?.dispose()
        audioFocusRequest?.let {audioManager.abandonAudioFocusRequest(it)}
        audioFocusRequest=null
        audioManager.mode=originalMode
        audioManager.isSpeakerphoneOn=originalSpeaker
        egl.release()
    }
}
