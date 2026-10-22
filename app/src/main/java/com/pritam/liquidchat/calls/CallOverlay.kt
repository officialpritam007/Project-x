package com.pritam.liquidchat.calls

import com.pritam.liquidchat.ui.LiquidAlertDialog

import android.Manifest
import android.app.Activity
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pritam.liquidchat.data.Backend
import com.pritam.liquidchat.ui.*
import kotlinx.coroutines.delay
import org.webrtc.SurfaceViewRenderer
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallOverlay(controller:CallController,displayName:String?,backend:Backend?=null,avatar:String="") {
    val call by controller.view.collectAsStateWithLifecycle()
    val c=call ?: return
    val referenceCall=referenceListUsesFlatRows(LocalDisplayOptions.current)
    val context=LocalContext.current
    var seconds by remember(c.id,c.connectedAt) {mutableLongStateOf(0)}
    LaunchedEffect(c.connectedAt) {while(c.connectedAt>0){seconds=(SystemClock.elapsedRealtime()-c.connectedAt)/1000;delay(1000)}}
    val timer="%02d:%02d".format(seconds/60,seconds%60)
    val terminal=c.id==null&&c.status!="Preparing"
    BackHandler(enabled=!c.minimized) {if(terminal)controller.dismissError() else controller.minimize()}
    val shouldRing=c.incoming&&c.status=="Incoming call"
    DisposableEffect(c.id,shouldRing) {
        val audio=context.getSystemService(AudioManager::class.java)
        val ringtone=if(shouldRing&&audio.ringerMode==AudioManager.RINGER_MODE_NORMAL)runCatching {
            RingtoneManager.getRingtone(context,RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)).apply {isLooping=true;play()}
        }.getOrNull() else null
        val vibrator=context.getSystemService(android.os.Vibrator::class.java)
        if(shouldRing&&audio.ringerMode!=AudioManager.RINGER_MODE_SILENT)runCatching {vibrator.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0,700,800),0))}
        onDispose {runCatching {ringtone?.stop()};if(shouldRing)vibrator.cancel()}
    }
    DisposableEffect(c.id,c.video,c.speaker,c.audioRoute,c.connectedAt) {
        val manager=context.getSystemService(android.os.PowerManager::class.java)
        val proximity=if(!c.video&&!c.speaker&&c.connectedAt>0&&c.audioRoute in listOf("","Earpiece")&&manager.isWakeLockLevelSupported(android.os.PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK))
            manager.newWakeLock(android.os.PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,"LiquidChat:proximity").apply {acquire(60*60*1000L)} else null
        onDispose {if(proximity?.isHeld==true)proximity.release()}
    }
    val needed=remember(c.video) {if(c.video)arrayOf(Manifest.permission.RECORD_AUDIO,Manifest.permission.CAMERA) else arrayOf(Manifest.permission.RECORD_AUDIO)}
    var permissionError by remember(c.id) {mutableStateOf(false)}
    val request=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {result->
        if(needed.all {result[it]==true})controller.accept() else permissionError=true
    }
    fun answer() {if(needed.all {ContextCompat.checkSelfPermission(context,it)==PackageManager.PERMISSION_GRANTED})controller.accept() else request.launch(needed)}
    LaunchedEffect(c.id,c.answerRequested) {if(c.answerRequested&&shouldRing)answer()}
    var routes by remember {mutableStateOf<List<Pair<Int,String>>?>(null)}
    val bluetoothPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {routes=controller.audioRoutes()}
    fun openAudioRoutes() {
        if(android.os.Build.VERSION.SDK_INT>=31 &&
            ContextCompat.checkSelfPermission(context,Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)
            bluetoothPermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
        else routes=controller.audioRoutes()
    }
    // The compact pill is laid out by Scaffold; video can instead float over the chat.
    if(c.minimized) {
        val minimizedEngine=controller.renderer()
        if(c.video && c.renderVideo && !c.compact && minimizedEngine!=null)
            FloatingCallVideo(controller,minimizedEngine,c.id)
        return
    }
    val engine=controller.renderer()
    val activeVideo=c.renderVideo&&engine!=null&&!terminal
    var swapped by remember(c.id) {mutableStateOf(false)}
    var controlsVisible by remember(c.id) {mutableStateOf(true)}
    var x by remember(c.id) {mutableFloatStateOf(0f)}
    var y by remember(c.id) {mutableFloatStateOf(-1f)}
    val density=LocalDensity.current
    val rootView=LocalView.current
    DisposableEffect(activeVideo,controlsVisible,context,rootView) {
        // Immersive mode hides Android chrome, not the draggable in-call self preview.
        var ownerContext:android.content.Context=context
        while(ownerContext !is Activity && ownerContext is ContextWrapper)ownerContext=ownerContext.baseContext
        val window=(ownerContext as? Activity)?.window
        val bars=window?.let {WindowCompat.getInsetsController(it,rootView)}
        if(activeVideo&&!controlsVisible) {
            bars?.systemBarsBehavior=WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            bars?.hide(WindowInsetsCompat.Type.systemBars())
        } else bars?.show(WindowInsetsCompat.Type.systemBars())
        onDispose {bars?.show(WindowInsetsCompat.Type.systemBars())}
    }
    Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
        BoxWithConstraints(Modifier.fillMaxSize().background(glassBackground())) {
            if(activeVideo&&engine!=null)key(engine,swapped) {
                VideoRenderer(engine,swapped,Modifier.fillMaxSize())
                // Transparent touch surface belongs behind the inset preview, never over it.
                Box(Modifier.fillMaxSize().clickable {controlsVisible=!controlsVisible})
                if(!c.remoteVideoReady&&!swapped && controlsVisible)
                    Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
                        Text("Waiting for remote video…",color=Color.White)
                    }
                val previewWidth=if(maxWidth<360.dp)100.dp else 116.dp
                val previewHeight=if(maxHeight<620.dp)138.dp else 154.dp
                val widthPx=with(density){previewWidth.roundToPx()}
                val heightPx=with(density){previewHeight.roundToPx()}
                val marginPx=with(density){12.dp.roundToPx()}
                val safeTop=WindowInsets.statusBars.getTop(density)
                val safeBottom=WindowInsets.navigationBars.getBottom(density)
                val maxX=(constraints.maxWidth-widthPx-marginPx*2).coerceAtLeast(0).toFloat()
                val maxY=(constraints.maxHeight-heightPx-safeTop-safeBottom-marginPx*2).coerceAtLeast(0).toFloat()
                val resolvedX=if(x<0f)maxX else x.coerceIn(0f,maxX)
                val resolvedY=if(y<0f)with(density){84.dp.toPx()}.coerceAtMost(maxY) else y.coerceIn(0f,maxY)
                Box(Modifier
                    .offset {IntOffset(marginPx+resolvedX.roundToInt(),safeTop+marginPx+resolvedY.roundToInt())}
                    .size(previewWidth,previewHeight)
                    .clip(Glass.card)
                    .pointerInput(c.id,maxX,maxY) {
                        detectDragGestures {change,drag->
                            change.consume()
                            x=((if(x<0f)maxX else x)+drag.x).coerceIn(0f,maxX)
                            y=((if(y<0f)resolvedY else y)+drag.y).coerceIn(0f,maxY)
                        }
                    }
                    .clickable {swapped=!swapped}) {
                    VideoRenderer(engine,!swapped,Modifier.fillMaxSize())
                }
            }
            Column(Modifier.fillMaxSize().systemBarsPadding().padding(18.dp),verticalArrangement=Arrangement.SpaceBetween,horizontalAlignment=Alignment.CenterHorizontally) {
                AnimatedVisibility(visible=!activeVideo||controlsVisible,enter=fadeIn(),exit=fadeOut()) {
                GlassSurface(Modifier.fillMaxWidth(),
                    if(referenceCall)Glass.compact else Glass.hero) {
                    Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(displayName ?: "Liquid Chat",style=MaterialTheme.typography.titleLarge)
                            Text(if(c.video)"Video call" else "Voice call",style=MaterialTheme.typography.bodyMedium)
                            Text(if(c.connectedAt>0&&!terminal)"$timer · ${c.status}" else c.status,color=MaterialTheme.colorScheme.primary)
                        }
                        if(!terminal)IconButton(onClick=controller::minimize) {Icon(Icons.Outlined.ExpandMore,"Minimize call")}
                    }
                }
                }
                if(!activeVideo)Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(20.dp)) {
                    if(backend!=null&&avatar.isNotBlank())ProtectedImage(backend,avatar,description="Caller photo",modifier=Modifier.size(136.dp).clip(CircleShape),scale=androidx.compose.ui.layout.ContentScale.Crop)
                    else Surface(shape=CircleShape,color=MaterialTheme.colorScheme.primaryContainer,modifier=Modifier.size(136.dp)) {
                        Box(contentAlignment=Alignment.Center) {Text(displayName?.take(1)?.uppercase() ?: "L",style=MaterialTheme.typography.displayLarge)}
                    }
                    Text(when {terminal->"Call finished";shouldRing->"Incoming ${if(c.video)"video" else "voice"} call";c.status=="Ringing"->"Calling…";else->c.status},style=MaterialTheme.typography.headlineSmall)
                }
                if(activeVideo&&!controlsVisible) Spacer(Modifier.height(1.dp))
                else if(shouldRing)Row(horizontalArrangement=Arrangement.spacedBy(48.dp)) {
                    CallControl("Decline",Icons.Outlined.CallEnd,{controller.decline()},true)
                    CallControl("Accept",Icons.Outlined.Call,{answer()},false)
                } else if(activeVideo) {
                    // Do not cover a large part of the actual remote video with a
                    // two-row toolbar. This narrow strip fits ordinary phones and
                    // retains every capture/output control and End call action.
                    ActiveVideoControlStrip(
                        muted=c.muted,cameraEnabled=c.cameraEnabled,audioRoute=c.audioRoute,
                        onMute=controller::toggleMute,onAudio=::openAudioRoutes,
                        onCamera=controller::toggleCamera,onSwitch=controller::switchCamera,
                        onHangup=controller::hangup
                    )
                } else GlassSurface(Modifier.fillMaxWidth(),
                    if(referenceCall)Glass.compact else Glass.hero) {
                    Column(Modifier.padding(12.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                        if(!terminal)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) {
                            IconButton(onClick=controller::toggleMute) {Icon(if(c.muted)Icons.Outlined.MicOff else Icons.Outlined.Mic,if(c.muted)"Unmute" else "Mute")}
                            IconButton(onClick=::openAudioRoutes) {Icon(Icons.Outlined.Headset,"Select audio output")}
                            if(c.video) {
                                IconButton(onClick=controller::toggleCamera) {Icon(if(c.cameraEnabled)Icons.Outlined.Videocam else Icons.Outlined.VideocamOff,"Toggle camera")}
                                IconButton(onClick=controller::switchCamera) {Icon(Icons.Outlined.Cameraswitch,"Switch camera")}
                            }
                        }
                        if(c.audioRoute.isNotBlank())Text(c.audioRoute,style=MaterialTheme.typography.labelSmall)
                        CallControl(if(terminal)"Close" else "End call",Icons.Outlined.CallEnd,{if(terminal)controller.dismissError() else controller.hangup()},true)
                    }
                }
            }
        }
    }
    routes?.let {availableRoutes->
        // The same reference action sheet now serves call audio routing too.
        // Keep Bluetooth permission request, selected device and controller actions.
        LiquidActionSheet(title="Audio output",onDismiss={routes=null}) {
            if(availableRoutes.isEmpty())
                Text("No output devices available",
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
            availableRoutes.forEach {(id,name)->
                GlassActionRow(
                    if(c.audioRoute==name)"$name · Selected" else name,
                    if(name.contains("Bluetooth",true))Icons.Outlined.BluetoothAudio
                    else if(name.contains("Speaker",true))Icons.Outlined.VolumeUp
                    else Icons.Outlined.Headset,
                    onClick={
                        controller.selectAudioRoute(id)
                        routes=null
                    })
            }
        }
    }
    if (permissionError) {
        LiquidAlertDialog(
            onDismissRequest = { permissionError = false },
            title = { Text("Permission needed") },
            text = {
                Text(
                    "Allow microphone${if (c.video) " and camera" else ""} access to answer this call. " +
                        "You can enable it in Android Settings."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    permissionError = false
                    context.startActivity(
                        android.content.Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            android.net.Uri.parse("package:${context.packageName}")
                        )
                    )
                }) { Text("Settings") }
            },
            dismissButton = {
                TextButton(onClick = {
                    permissionError = false
                    controller.decline()
                }) { Text("Decline") }
            }
        )
    }
}
@Composable internal fun ActiveVideoControlStrip(
    muted:Boolean,cameraEnabled:Boolean,audioRoute:String,
    onMute:()->Unit,onAudio:()->Unit,onCamera:()->Unit,onSwitch:()->Unit,onHangup:()->Unit
) {
    GlassSurface(Modifier.fillMaxWidth(),
        if(referenceListUsesFlatRows(LocalDisplayOptions.current))Glass.compact else Glass.capsule) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal=6.dp,vertical=4.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.SpaceEvenly
        ) {
            IconButton(onClick=onMute) {
                Icon(if(muted)Icons.Outlined.MicOff else Icons.Outlined.Mic,
                    if(muted)"Unmute microphone" else "Mute microphone")
            }
            IconButton(onClick=onAudio) {
                Icon(Icons.Outlined.Headset,
                    "Select audio output" + if(audioRoute.isBlank())"" else ", current: $audioRoute")
            }
            IconButton(onClick=onCamera) {
                Icon(if(cameraEnabled)Icons.Outlined.Videocam else Icons.Outlined.VideocamOff,
                    if(cameraEnabled)"Turn camera off" else "Turn camera on")
            }
            IconButton(onClick=onSwitch) {
                Icon(Icons.Outlined.Cameraswitch,"Switch camera")
            }
            FilledIconButton(
                onClick=onHangup,
                modifier=Modifier.size(48.dp),
                colors=IconButtonDefaults.filledIconButtonColors(containerColor=Color(0xFFC93B48))
            ) {
                Icon(Icons.Outlined.CallEnd,"End call",tint=Color.White)
            }
        }
    }
}

@Composable private fun CallControl(label:String,icon:androidx.compose.ui.graphics.vector.ImageVector,action:()->Unit,danger:Boolean) {
    Column(horizontalAlignment=Alignment.CenterHorizontally) {
        FilledIconButton(onClick=action,modifier=Modifier.size(64.dp),colors=IconButtonDefaults.filledIconButtonColors(containerColor=if(danger)Color(0xFFC93B48) else Color(0xFF16875A))) {Icon(icon,label,tint=Color.White)}
        Text(label,style=MaterialTheme.typography.labelMedium,modifier=Modifier.padding(top=6.dp))
    }
}
/**
 * In-app Picture-in-Picture. Not Android system PiP: media stays in the existing foreground
 * call service and the window stays inside the app. Insets keep the camera above the IME.
 */
@Composable private fun FloatingCallVideo(controller:CallController,engine:CallEngine,callId:String?) {
    val density=LocalDensity.current
    var x by remember(callId) {mutableFloatStateOf(-1f)}
    var y by remember(callId) {mutableFloatStateOf(-1f)}
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width=if(maxWidth<360.dp)120.dp else 136.dp
        val height=if(maxHeight<630.dp)166.dp else 186.dp
        val edge=with(density){12.dp.roundToPx()}
        val widthPx=with(density){width.roundToPx()}
        val heightPx=with(density){height.roundToPx()}
        val safeTop=WindowInsets.statusBars.getTop(density)
        val keyboardBottom=maxOf(WindowInsets.ime.getBottom(density),WindowInsets.navigationBars.getBottom(density))
        val maxX=(constraints.maxWidth-widthPx-2*edge).coerceAtLeast(0).toFloat()
        val maxY=(constraints.maxHeight-heightPx-safeTop-keyboardBottom-2*edge).coerceAtLeast(0).toFloat()
        val actualX=if(x<0f)maxX else x.coerceIn(0f,maxX)
        val actualY=if(y<0f)with(density){100.dp.toPx()}.coerceAtMost(maxY) else y.coerceIn(0f,maxY)
        GlassSurface(
            Modifier.offset {IntOffset(edge+actualX.roundToInt(),safeTop+edge+actualY.roundToInt())}
                .size(width,height)
                .pointerInput(callId,maxX,maxY) {
                    detectDragGestures {change,drag->
                        change.consume()
                        x=((if(x<0f)maxX else x)+drag.x).coerceIn(0f,maxX)
                        y=((if(y<0f)actualY else y)+drag.y).coerceIn(0f,maxY)
                    }
                }
                .clickable {controller.expand()},Glass.card
        ) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                VideoRenderer(engine,false,Modifier.fillMaxSize())
                Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(width=36.dp,height=48.dp).clip(Glass.compact)) {
                    VideoRenderer(engine,true,Modifier.fillMaxSize())
                }
                FilledIconButton(
                    onClick=controller::toggleCompact,
                    modifier=Modifier.align(Alignment.BottomEnd).padding(6.dp).size(38.dp),
                    colors=IconButtonDefaults.filledIconButtonColors(containerColor=Color.Black.copy(alpha=.64f))
                ) {
                    Icon(Icons.Outlined.Remove,"Minimize to call pill",tint=Color.White)
                }
            }
        }
    }
}

@Composable private fun VideoRenderer(engine:CallEngine,local:Boolean,modifier:Modifier=Modifier) {
    val context=LocalContext.current
    val renderer=remember(engine,local) {SurfaceViewRenderer(context)}
    DisposableEffect(engine,renderer,local) {engine.attachRenderer(renderer,local);onDispose {engine.detachRenderer(renderer,local)}}
    AndroidView(factory={renderer},modifier=modifier)
}
