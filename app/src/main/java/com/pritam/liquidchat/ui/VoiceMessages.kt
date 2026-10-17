package com.pritam.liquidchat.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.*
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
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pritam.liquidchat.AppViewModel
import com.pritam.liquidchat.data.Message
import com.pritam.liquidchat.data.VoiceRecorder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable fun VoiceCapture(model:AppViewModel,cid:String,busy:Boolean) {
    val context=LocalContext.current
    val account=model.backend.session.value?.userId.orEmpty()
    val capture=remember(cid,account) {VoiceRecorder(context,account)}
    var error by remember {mutableStateOf<String?>(null)}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {granted->error=if(granted)"Hold the microphone to record." else "Microphone permission is needed to record."}
    fun start(locked:Boolean=false) {
        if(busy||account.isBlank())return
        if(ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) {permission.launch(Manifest.permission.RECORD_AUDIO);return}
        runCatching {capture.start(locked)}.onFailure {error="Recording could not start. Try again."}
    }
    fun send() {
        val result=capture.finish() ?: return
        model.sendVoice(cid,result.first,result.second,account)
    }
    val lifecycle=LocalLifecycleOwner.current
    DisposableEffect(capture,lifecycle) {
        val observer=LifecycleEventObserver {_,event->if(event==Lifecycle.Event.ON_STOP)capture.cancel()}
        lifecycle.lifecycle.addObserver(observer)
        onDispose {lifecycle.lifecycle.removeObserver(observer);capture.cancel()}
    }
    LaunchedEffect(capture.recording) {while(capture.recording){capture.tick();if(capture.elapsed>=119500){send();break};delay(100)}}
    Box {
        Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary,CircleShape).semantics {
            contentDescription="Hold to record voice message";role=Role.Button
            onClick {start(true);true}
        }.pointerInput(capture,busy) {
            awaitEachGesture {
                val down=awaitFirstDown();if(busy)return@awaitEachGesture
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
                Text("Recording ${capture.elapsed/1000}s / 120s")
                Text(if(capture.locked)"Hands-free recording" else "Release to send · slide left to cancel · slide up to lock",style=MaterialTheme.typography.bodySmall)
                if(capture.locked)Row {TextButton(onClick=capture::cancel){Text("Discard")};TextButton(onClick={send()}){Text("Send voice")}}
            }
        }
    }
    error?.let {message->AlertDialog(onDismissRequest={error=null},text={Text(message)},confirmButton={TextButton(onClick={error=null}){Text("OK")}})}
}

private object VoicePlayback {
    var id by mutableStateOf<String?>(null);private set
    var playing by mutableStateOf(false);private set
    var position by mutableIntStateOf(0);private set
    var duration by mutableIntStateOf(1);private set
    var ready by mutableStateOf(false);private set
    private var player:MediaPlayer?=null
    private var manager:AudioManager?=null
    private var focus:AudioFocusRequest?=null
    private var generation=0
    fun begin(message:String):Int {stop();id=message;return generation}
    fun current(token:Int)=token==generation
    fun play(context:android.content.Context,message:String,source:String,token:Int,onError:()->Unit) {
        if(!current(token))return
        val audio=context.getSystemService(AudioManager::class.java)
        val attrs=AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
        val request=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(attrs).setOnAudioFocusChangeListener {change->if(change<=0)stop()}.build()
        manager=audio;focus=request
        val media=MediaPlayer();player=media
        try {
            media.setAudioAttributes(attrs);media.setDataSource(source)
            media.setOnPreparedListener {p->if(current(token)&&id==message&&audio.requestAudioFocus(request)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED){ready=true;duration=p.duration.coerceAtLeast(1);p.start();playing=true}else stop()}
            media.setOnCompletionListener {stop()}
            media.setOnErrorListener {_,_,_->if(current(token)){stop();onError()};true}
            media.prepareAsync()
        } catch(_:Exception) {stop();onError()}
    }
    fun pauseResume() {if(!ready)return;val p=player ?: return;if(playing){p.pause();playing=false}else {p.start();playing=true}}
    fun seek(ms:Int) {player?.seekTo(ms);position=ms}
    fun tick() {position=runCatching {player?.currentPosition ?: 0}.getOrDefault(0)}
    fun stop() {generation++;runCatching {player?.release()};player=null;focus?.let {manager?.abandonAudioFocusRequest(it)};manager=null;focus=null;id=null;playing=false;ready=false;position=0}
}
@Composable fun VoicePlaybackLifecycle(key:Any?) {
    val lifecycle=LocalLifecycleOwner.current
    DisposableEffect(key,lifecycle) {val observer=LifecycleEventObserver {_,e->if(e==Lifecycle.Event.ON_STOP)VoicePlayback.stop()};lifecycle.lifecycle.addObserver(observer);onDispose {lifecycle.lifecycle.removeObserver(observer);VoicePlayback.stop()}}
}
@Composable fun VoicePlayer(model:AppViewModel,message:Message) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    var loading by remember(message.id) {mutableStateOf(false)}
    var error by remember(message.id) {mutableStateOf<String?>(null)}
    val selected=VoicePlayback.id==message.id
    val playing=selected&&VoicePlayback.playing
    LaunchedEffect(selected,playing) {while(selected&&playing){VoicePlayback.tick();delay(200)}}
    DisposableEffect(message.id) {onDispose {if(VoicePlayback.id==message.id)VoicePlayback.stop()}}
    Column(Modifier.widthIn(min=210.dp,max=285.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            IconButton(enabled=!loading&&(!selected||VoicePlayback.ready),onClick={
                if(selected)VoicePlayback.pauseResume() else {
                    loading=true;error=null;val token=VoicePlayback.begin(message.id)
                    scope.launch {
                        try {
                            val source=if(message.localImage.isNotBlank()&&java.io.File(message.localImage).exists())message.localImage else JSONObject(model.backend.request("/functions/v1/media","POST",JSONObject().put("action","download").put("id",message.mediaId))).getString("url")
                            if(VoicePlayback.current(token))VoicePlayback.play(context,message.id,source,token){error="Playback failed. Tap to retry."}
                        } catch(e:CancellationException){throw e}catch(_:Exception){if(VoicePlayback.current(token)){VoicePlayback.stop();error="Voice unavailable. Tap to retry."}}finally {loading=false}
                    }
                }
            }) {if(loading||(selected&&!VoicePlayback.ready))CircularProgressIndicator(Modifier.size(20.dp)) else Icon(if(playing)Icons.Outlined.Pause else Icons.Outlined.PlayArrow,if(playing)"Pause voice message" else "Play voice message")}
            Text("Voice message · ${message.durationMs/1000}s",style=MaterialTheme.typography.labelLarge)
        }
        Slider(value=if(selected)VoicePlayback.position.toFloat() else 0f,onValueChange={VoicePlayback.seek(it.toInt())},valueRange=0f..(if(selected)VoicePlayback.duration else message.durationMs).coerceAtLeast(1).toFloat(),enabled=selected&&!loading)
        error?.let {Text(it,style=MaterialTheme.typography.labelSmall)}
    }
}
