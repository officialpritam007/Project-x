package com.pritam.liquidchat.calls

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pritam.liquidchat.data.Backend
import com.pritam.liquidchat.ui.*
import kotlinx.coroutines.delay
import org.webrtc.SurfaceViewRenderer
import kotlin.math.roundToInt

@Composable
fun CallOverlay(controller:CallController,displayName:String?,backend:Backend?=null,avatar:String="") {
    val call by controller.view.collectAsStateWithLifecycle()
    val c=call ?: return
    val context=LocalContext.current
    var seconds by remember(c.id,c.connectedAt) {mutableLongStateOf(0)}
    LaunchedEffect(c.connectedAt) {while(c.connectedAt>0){seconds=(SystemClock.elapsedRealtime()-c.connectedAt)/1000;delay(1000)}}
    val timer="%02d:%02d".format(seconds/60,seconds%60)
    val terminal=c.id==null&&c.status!="Preparing"
    BackHandler {if(terminal)controller.dismissError() else controller.minimize()}
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
    if(c.minimized) {
        Box(Modifier.fillMaxSize().systemBarsPadding(),contentAlignment=Alignment.TopCenter) {
            GlassSurface(Modifier.padding(12.dp).fillMaxWidth().clickable {controller.expand()},Glass.capsule) {
                Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
                    Icon(if(c.video)Icons.Outlined.Videocam else Icons.Outlined.Call,null)
                    Text("${displayName ?: "Liquid Chat"} · ${if(c.connectedAt>0)timer else c.status}",Modifier.weight(1f).padding(horizontal=12.dp))
                    IconButton(onClick=controller::hangup) {Icon(Icons.Outlined.CallEnd,"End call",tint=MaterialTheme.colorScheme.error)}
                }
            }
        }
        return
    }
    val engine=controller.renderer()
    val activeVideo=c.video&&!c.incoming&&engine!=null&&!terminal
    var swapped by remember(c.id) {mutableStateOf(false)}
    var largePreview by remember(c.id) {mutableStateOf(false)}
    var x by remember(c.id) {mutableFloatStateOf(0f)}
    var y by remember(c.id) {mutableFloatStateOf(0f)}
    Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
        BoxWithConstraints(Modifier.fillMaxSize().background(glassBackground())) {
            if(activeVideo&&engine!=null)key(engine,swapped) {
                VideoRenderer(engine,swapped,Modifier.fillMaxSize())
                if(!c.remoteVideoReady&&!swapped)Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
                    Column(horizontalAlignment=Alignment.CenterHorizontally) {
                        CircularProgressIndicator();Spacer(Modifier.height(12.dp));Text("Waiting for remote video…")
                    }
                }
                val maxX=constraints.maxWidth.toFloat()/2
                val maxY=constraints.maxHeight.toFloat()/2
                Box(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top=96.dp,end=16.dp)
                    .offset {IntOffset(x.roundToInt(),y.roundToInt())}
                    .size(if(largePreview)168.dp else 112.dp,if(largePreview)220.dp else 154.dp)
                    .clip(Glass.card).pointerInput(c.id) {
                        detectDragGestures {change,drag->change.consume();x=(x+drag.x).coerceIn(-maxX,0f);y=(y+drag.y).coerceIn(0f,maxY)}
                    }) {
                    VideoRenderer(engine,!swapped,Modifier.fillMaxSize())
                    Row(Modifier.align(Alignment.BottomCenter).background(Color.Black.copy(alpha=.45f))) {
                        IconButton(onClick={swapped=!swapped}) {Icon(Icons.Outlined.SwapHoriz,"Swap video views",tint=Color.White)}
                        IconButton(onClick={largePreview=!largePreview}) {Icon(Icons.Outlined.OpenInFull,"Resize preview",tint=Color.White)}
                    }
                }
            }
            Column(Modifier.fillMaxSize().systemBarsPadding().padding(18.dp),verticalArrangement=Arrangement.SpaceBetween,horizontalAlignment=Alignment.CenterHorizontally) {
                GlassSurface(Modifier.fillMaxWidth(),Glass.hero) {
                    Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(displayName ?: "Liquid Chat",style=MaterialTheme.typography.titleLarge)
                            Text(if(c.video)"Video call" else "Voice call",style=MaterialTheme.typography.bodyMedium)
                            Text(if(c.connectedAt>0&&!terminal)"$timer · ${c.status}" else c.status,color=MaterialTheme.colorScheme.primary)
                        }
                        if(!terminal)IconButton(onClick=controller::minimize) {Icon(Icons.Outlined.ExpandMore,"Minimize call")}
                    }
                }
                if(!activeVideo)Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(20.dp)) {
                    if(backend!=null&&avatar.isNotBlank())ProtectedImage(backend,avatar,description="Caller photo",modifier=Modifier.size(136.dp).clip(CircleShape),scale=androidx.compose.ui.layout.ContentScale.Crop)
                    else Surface(shape=CircleShape,color=MaterialTheme.colorScheme.primaryContainer,modifier=Modifier.size(136.dp)) {
                        Box(contentAlignment=Alignment.Center) {Text(displayName?.take(1)?.uppercase() ?: "L",style=MaterialTheme.typography.displayLarge)}
                    }
                    Text(when {terminal->"Call finished";shouldRing->"Incoming ${if(c.video)"video" else "voice"} call";c.status=="Ringing"->"Calling…";else->c.status},style=MaterialTheme.typography.headlineSmall)
                }
                if(shouldRing)Row(horizontalArrangement=Arrangement.spacedBy(48.dp)) {
                    CallControl("Decline",Icons.Outlined.CallEnd,{controller.decline()},true)
                    CallControl("Accept",Icons.Outlined.Call,{answer()},false)
                } else GlassSurface(Modifier.fillMaxWidth(),Glass.hero) {
                    Column(Modifier.padding(12.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                        if(!terminal)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) {
                            IconButton(onClick=controller::toggleMute) {Icon(if(c.muted)Icons.Outlined.MicOff else Icons.Outlined.Mic,if(c.muted)"Unmute" else "Mute")}
                            IconButton(onClick={
                                if(android.os.Build.VERSION.SDK_INT>=31&&ContextCompat.checkSelfPermission(context,Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)bluetoothPermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
                                else routes=controller.audioRoutes()
                            }) {Icon(Icons.Outlined.Headset,"Select audio output")}
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
    routes?.let { availableRoutes ->
        AlertDialog(
            onDismissRequest = { routes = null },
            title = { Text("Audio output") },
            text = {
                Column {
                    availableRoutes.forEach { (id, name) ->
                        TextButton(onClick = {
                            controller.selectAudioRoute(id)
                            routes = null
                        }) {
                            Text(name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { routes = null }) { Text("Done") }
            }
        )
    }
    if (permissionError) {
        AlertDialog(
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
@Composable private fun CallControl(label:String,icon:androidx.compose.ui.graphics.vector.ImageVector,action:()->Unit,danger:Boolean) {
    Column(horizontalAlignment=Alignment.CenterHorizontally) {
        FilledIconButton(onClick=action,modifier=Modifier.size(64.dp),colors=IconButtonDefaults.filledIconButtonColors(containerColor=if(danger)Color(0xFFC93B48) else Color(0xFF16875A))) {Icon(icon,label,tint=Color.White)}
        Text(label,style=MaterialTheme.typography.labelMedium,modifier=Modifier.padding(top=6.dp))
    }
}
@Composable private fun VideoRenderer(engine:CallEngine,local:Boolean,modifier:Modifier=Modifier) {
    val context=LocalContext.current
    val renderer=remember(engine,local) {SurfaceViewRenderer(context)}
    DisposableEffect(engine,renderer,local) {engine.attachRenderer(renderer,local);onDispose {engine.detachRenderer(renderer,local)}}
    AndroidView(factory={renderer},modifier=modifier)
}
