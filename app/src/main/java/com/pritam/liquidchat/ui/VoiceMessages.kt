package com.pritam.liquidchat.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.*
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pritam.liquidchat.AppViewModel
import com.pritam.liquidchat.data.Message
import com.pritam.liquidchat.data.VoiceRecorder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import org.json.JSONObject

/**
 * A microphone gesture is allowed to dismiss the composer IME only after
 * Android really started audio capture. Denied permissions, session errors
 * and recorder failures must not disrupt a text draft.
 */
internal fun startVoiceWithFocusHandoff(
    begin:()->Unit,recording:()->Boolean,dismissKeyboard:()->Unit
):Boolean {
    begin()
    if(!recording())return false
    dismissKeyboard()
    return true
}

@Composable fun VoiceCapture(model:AppViewModel,cid:String,busy:Boolean) {
    val context=LocalContext.current
    val focusManager=LocalFocusManager.current
    val keyboard=LocalSoftwareKeyboardController.current
    val session by model.session.collectAsStateWithLifecycle()
    val account=session?.userId.orEmpty()
    val call by model.calls.view.collectAsStateWithLifecycle()
    val unavailable=busy||call!=null
    val capture=remember(cid,account) {VoiceRecorder(context,account)}
    var error by remember {mutableStateOf<String?>(null)}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {granted->error=if(granted)"Hold the microphone to record." else "Microphone permission is needed to record."}
    fun start(locked:Boolean=false) {
        if(busy||model.calls.view.value!=null||account.isBlank())return
        if(ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) {permission.launch(Manifest.permission.RECORD_AUDIO);return}
        runCatching {
            startVoiceWithFocusHandoff(
                begin={capture.start(locked)},
                recording={capture.recording},
                dismissKeyboard={
                    // The parent chat uses imePadding(); first clear BasicTextField
                    // focus, then hide the IME so the mic tray never overlaps it.
                    focusManager.clearFocus(force=true)
                    keyboard?.hide()
                }
            )
        }.onFailure {error="Recording could not start. Try again."}
    }
    fun send() {
        val result=capture.finish() ?: return
        model.sendVoice(cid,result.first,result.second,account)
    }
    val lifecycle=LocalLifecycleOwner.current
    BackHandler(enabled=capture.recording) {capture.cancel()}
    DisposableEffect(capture,lifecycle) {
        val observer=LifecycleEventObserver {_,event->if(event==Lifecycle.Event.ON_STOP)capture.cancel()}
        lifecycle.lifecycle.addObserver(observer)
        onDispose {lifecycle.lifecycle.removeObserver(observer);capture.cancel()}
    }
    LaunchedEffect(capture,call!=null) {if(call!=null)capture.cancel()}
    LaunchedEffect(capture.recording) {while(capture.recording){capture.tick();if(capture.elapsed>=119500){send();break};delay(100)}}
    Box {
        Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary,CircleShape).semantics {
            contentDescription="Hold to record voice message";role=Role.Button
            if(unavailable)disabled()
            onClick {start(true);true}
        }.pointerInput(capture,unavailable) {
            awaitEachGesture {
                val down=awaitFirstDown();if(unavailable)return@awaitEachGesture
                start()
                if(!capture.recording){waitForUpOrCancellation();return@awaitEachGesture}
                try {
                    while(capture.recording&&!capture.locked) {
                        val event=awaitPointerEvent();val change=event.changes.firstOrNull {it.id==down.id} ?: break
                        val dx=change.position.x-down.position.x;val dy=change.position.y-down.position.y
                        when {
                            dx< -80.dp.toPx()->{capture.cancel();break}
                            dy< -70.dp.toPx()->{capture.lock();break}
                            !change.pressed->{send();break}
                        }
                        change.consume()
                    }
                    if(capture.recording&&!capture.locked)capture.cancel()
                } catch(e:CancellationException) {capture.cancel();throw e}
            }
        },contentAlignment=Alignment.Center) {Icon(Icons.Outlined.Mic,null,tint=MaterialTheme.colorScheme.onPrimary)}
        if(capture.recording) androidx.compose.ui.window.Popup(alignment=Alignment.BottomEnd,offset=androidx.compose.ui.unit.IntOffset(0,-160),onDismissRequest={}) {
            GlassCard(Modifier.widthIn(max=350.dp)) {
                Text("Recording ${capture.elapsed/1000}s / 120s",style=MaterialTheme.typography.titleSmall)
                Text(if(capture.locked)"Hands-free recording · Back to discard"
                    else "Release to send · left to cancel · up to lock",
                    style=MaterialTheme.typography.bodySmall)
                if(capture.locked)Row {TextButton(onClick=capture::cancel){Text("Discard")};TextButton(onClick={send()}){Text("Send voice")}}
            }
        }
    }
    error?.let {message->LiquidAlertDialog(onDismissRequest={error=null},text={Text(message)},confirmButton={TextButton(onClick={error=null}){Text("OK")}})}
}

/** Fixed choices avoid accidental chip-speed drift and keep playback accessible. */
internal fun nextVoicePlaybackSpeed(current:Float):Float = when(current) {
    1f->1.5f
    1.5f->2f
    else->1f
}

internal object VoicePlayback {
    var id by mutableStateOf<String?>(null);private set
    var playing by mutableStateOf(false);private set
    var position by mutableIntStateOf(0);private set
    var duration by mutableIntStateOf(1);private set
    var ready by mutableStateOf(false);private set
    var speed by mutableFloatStateOf(1f);private set
    private var player:MediaPlayer?=null
    private var manager:AudioManager?=null
    private var focus:AudioFocusRequest?=null
    private var generation=0
    var account:String?=null;private set
    fun begin(message:Message):Int {stop();id=message.id;account=message.account;return generation}
    fun current(token:Int)=token==generation
    // A callback/request owns only the player generation that created it.
    fun stopIfCurrent(token:Int) {if(current(token))stop()}
    fun play(context:android.content.Context,message:String,source:String,token:Int,onError:()->Unit) {
        if(!current(token))return
        val audio=context.getSystemService(AudioManager::class.java)
        val attrs=AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
        val request=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(attrs).setOnAudioFocusChangeListener {change->if(change<=0)stopIfCurrent(token)}.build()
        manager=audio;focus=request
        val media=MediaPlayer();player=media
        try {
            media.setAudioAttributes(attrs);media.setDataSource(source)
            media.setOnPreparedListener {p->
                if(current(token)&&player===p) {
                    if(id==message&&audio.requestAudioFocus(request)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        ready=true;duration=p.duration.coerceAtLeast(1);p.start();playing=true
                    } else stopIfCurrent(token)
                }
            }
            media.setOnCompletionListener {stopIfCurrent(token)}
            media.setOnErrorListener {_,_,_->if(current(token)){stop();onError()};true}
            media.prepareAsync()
        } catch(_:Exception) {if(current(token)){stop();onError()}}
    }
    fun pauseResume() {if(!ready)return;val p=player ?: return;if(playing){p.pause();playing=false}else {p.start();playing=true}}
    fun cycleSpeed() {
        val next=nextVoicePlaybackSpeed(speed)
        if(!ready) {speed=next;return}
        val p=player ?: return
        val wasPlaying=playing
        try {
            // MediaPlayer#setPlaybackParams can restart paused streams on some OEMs.
            // Restore the preexisting paused/playing state after changing the speed.
            p.playbackParams=p.playbackParams.setSpeed(next)
            if(!wasPlaying && p.isPlaying)p.pause()
            speed=next
            playing=wasPlaying
        } catch(_:IllegalStateException) {
            // Retain the last known working speed and do not break playback.
        } catch(_:IllegalArgumentException) {
            // Unsupported speed on a device must not interrupt the current voice message.
        }
    }
    fun seek(ms:Int) {if(!ready)return;player?.seekTo(ms);position=ms}
    fun tick() {position=runCatching {player?.currentPosition ?: 0}.getOrDefault(0)}
    fun stop() {generation++;runCatching {player?.release()};player=null;focus?.let {manager?.abandonAudioFocusRequest(it)};manager=null;focus=null;id=null;account=null;playing=false;ready=false;position=0;speed=1f}
}
/** A scrolled-offscreen voice item still stops immediately on a Room tombstone/hide. */
internal fun invalidatesVoicePlayback(message:Message?,wasCached:Boolean):Boolean =
    if(message==null)wasCached else message.deletedAt.isNotEmpty() || message.mediaKind!="voice"

@Composable fun VoicePlaybackLifecycle(model:AppViewModel,key:Any?) {
    val lifecycle=LocalLifecycleOwner.current
    DisposableEffect(key,lifecycle) {val observer=LifecycleEventObserver {_,e->if(e==Lifecycle.Event.ON_STOP)VoicePlayback.stop()};lifecycle.lifecycle.addObserver(observer);onDispose {lifecycle.lifecycle.removeObserver(observer);VoicePlayback.stop()}}
    val id=VoicePlayback.id
    val account=VoicePlayback.account
    LaunchedEffect(model) {
        model.calls.view.collect {if(it!=null)VoicePlayback.stop()}
    }
    LaunchedEffect(model,key) {
        model.repository.revocations.collect {event->
            if(VoicePlayback.account==event.account&&VoicePlayback.id==event.id)VoicePlayback.stop()
        }
    }
    LaunchedEffect(id,account) {
        if(id!=null&&account!=null) {
            var wasCached=false
            model.repository.dao.observeMessage(account,id).collect {message->
                if(VoicePlayback.id==id&&VoicePlayback.account==account&&invalidatesVoicePlayback(message,wasCached))VoicePlayback.stop()
                if(message!=null)wasCached=true
            }
        }
    }
}
@Composable fun VoicePlayer(model:AppViewModel,message:Message) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    val call by model.calls.view.collectAsStateWithLifecycle()
    var loading by remember(message.id) {mutableStateOf(false)}
    var error by remember(message.id) {mutableStateOf<String?>(null)}
    val selected=VoicePlayback.id==message.id&&VoicePlayback.account==message.account
    val playing=selected&&VoicePlayback.playing
    LaunchedEffect(selected,playing) {while(selected&&playing){VoicePlayback.tick();delay(200)}}
    // This item may leave the LazyColumn solely because it was scrolled offscreen.
    // Keep playback running while the current chat is visible; the screen-scoped
    // VoicePlaybackLifecycle stops audio when the chat/account or activity changes.
    Column(Modifier.widthIn(max=285.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            IconButton(enabled=call==null&&!loading&&(!selected||VoicePlayback.ready),onClick={
                if(model.calls.view.value!=null)return@IconButton
                if(selected)VoicePlayback.pauseResume() else {
                    loading=true;error=null;val token=VoicePlayback.begin(message)
                    scope.launch {
                        try {
                            val source=if(message.localImage.isNotBlank()&&java.io.File(message.localImage).exists())message.localImage else JSONObject(model.backend.request("/functions/v1/media","POST",JSONObject().put("action","download").put("id",message.mediaId))).getString("url")
                            if(model.calls.view.value!=null)VoicePlayback.stopIfCurrent(token)
                            else if(VoicePlayback.current(token))VoicePlayback.play(context,message.id,source,token){error="Playback failed. Tap to retry."}
                        } catch(e:CancellationException){VoicePlayback.stopIfCurrent(token);throw e}catch(_:Exception){if(VoicePlayback.current(token)){VoicePlayback.stop();error="Voice unavailable. Tap to retry."}}finally {loading=false}
                    }
                }
            }) {if(loading||(selected&&!VoicePlayback.ready))CircularProgressIndicator(Modifier.size(20.dp)) else Icon(if(playing)Icons.Outlined.Pause else Icons.Outlined.PlayArrow,if(playing)"Pause voice message" else "Play voice message")}
            Text("Voice message · ${message.durationMs/1000}s",modifier=Modifier.weight(1f),style=MaterialTheme.typography.labelLarge)
            TextButton(
                onClick={VoicePlayback.cycleSpeed()},
                enabled=call==null&&selected&&VoicePlayback.ready&&!loading,
                contentPadding=PaddingValues(horizontal=6.dp)
            ) {Text("${if(selected)VoicePlayback.speed else 1f}×",
                style=MaterialTheme.typography.labelLarge)}
        }
        Slider(value=if(selected)VoicePlayback.position.toFloat() else 0f,onValueChange={VoicePlayback.seek(it.toInt())},valueRange=0f..(if(selected)VoicePlayback.duration else message.durationMs).coerceAtLeast(1).toFloat(),enabled=call==null&&selected&&VoicePlayback.ready&&!loading)
        error?.let {Text(it,style=MaterialTheme.typography.labelSmall)}
    }
}
