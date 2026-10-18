package com.pritam.liquidchat.calls

import android.media.RingtoneManager
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.pritam.liquidchat.ui.Glass
import com.pritam.liquidchat.ui.GlassSurface
import com.pritam.liquidchat.ui.glassBackground
import org.webrtc.SurfaceViewRenderer

/** Call controls for a foreground-only beta. There is deliberately no fake background ringing. */
@Composable
fun CallOverlay(controller:CallController,displayName:String?) {
    val call by controller.view.collectAsState()
    val c=call ?: return
    BackHandler {if(c.id!=null)controller.hangup() else controller.dismissError()}
    val context=LocalContext.current
    val shouldRing=c.incoming && c.status=="Incoming call"
    DisposableEffect(c.id,shouldRing) {
        val ringtone=if(shouldRing)runCatching {
            val uri=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            RingtoneManager.getRingtone(context,uri)
        }.getOrNull() else null
        runCatching {ringtone?.play()}
        onDispose {runCatching {ringtone?.stop()}}
    }
    val needed=remember(c.video) {
        if(c.video) arrayOf(Manifest.permission.RECORD_AUDIO,Manifest.permission.CAMERA)
        else arrayOf(Manifest.permission.RECORD_AUDIO)
    }
    val request=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {result->
        if(needed.all {result[it]==true})controller.accept() else controller.decline()
    }
    val engine=controller.renderer()

    // An overlay (rather than a nested navigation route) keeps the existing conversation state intact.
    Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize().background(glassBackground())) {
            if(c.video && !c.incoming && engine!=null) {
                VideoRenderer(engine,false,Modifier.fillMaxSize())
                Box(Modifier.align(Alignment.TopEnd).padding(top=48.dp,end=20.dp)
                    .size(width=112.dp,height=154.dp)) {
                    VideoRenderer(engine,true,Modifier.fillMaxSize())
                }
            }
            Column(Modifier.fillMaxSize().systemBarsPadding().padding(18.dp),
                verticalArrangement=Arrangement.SpaceBetween,horizontalAlignment=Alignment.CenterHorizontally) {
                GlassSurface(Modifier.fillMaxWidth(),Glass.hero) {
                    Column(Modifier.fillMaxWidth().padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                        Text(displayName ?: "Liquid Chat",style=MaterialTheme.typography.headlineSmall,
                            color=MaterialTheme.colorScheme.onSurface)
                        Text(if(c.video)"Video call" else "Voice call",style=MaterialTheme.typography.bodyMedium)
                        Text(c.status,style=MaterialTheme.typography.titleMedium,
                            color=MaterialTheme.colorScheme.primary)
                        if(c.status=="Connection failed"||c.status=="Call unavailable")
                            Text("The network may require a TURN relay.",style=MaterialTheme.typography.bodySmall)
                    }
                }
                if(c.incoming) {
                    Row(horizontalArrangement=Arrangement.spacedBy(36.dp),verticalAlignment=Alignment.CenterVertically) {
                        FilledIconButton(onClick=controller::decline,modifier=Modifier.size(72.dp),
                            colors=IconButtonDefaults.filledIconButtonColors(containerColor=Color(0xFFC93B48))) {
                            Icon(Icons.Outlined.CallEnd,"Decline")
                        }
                        FilledIconButton(onClick={
                            if(needed.all {ContextCompat.checkSelfPermission(context,it)==PackageManager.PERMISSION_GRANTED})controller.accept()
                            else request.launch(needed)
                        },modifier=Modifier.size(72.dp),
                            colors=IconButtonDefaults.filledIconButtonColors(containerColor=Color(0xFF1A9C65))) {
                            Icon(Icons.Outlined.Call,"Accept")
                        }
                    }
                } else {
                    GlassSurface(Modifier.fillMaxWidth(),Glass.hero) {
                        Column(Modifier.padding(16.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) {
                                IconButton(onClick=controller::toggleMute) {
                                    Icon(if(c.muted)Icons.Outlined.MicOff else Icons.Outlined.Mic,
                                        if(c.muted)"Unmute" else "Mute")
                                }
                                IconButton(onClick=controller::toggleSpeaker) {
                                    Icon(if(c.speaker)Icons.Outlined.VolumeUp else Icons.Outlined.VolumeOff,
                                        "Toggle speaker")
                                }
                                if(c.video) {
                                    IconButton(onClick=controller::toggleCamera) {
                                        Icon(if(c.cameraEnabled)Icons.Outlined.Videocam else Icons.Outlined.VideocamOff,
                                            "Toggle camera")
                                    }
                                    IconButton(onClick=controller::switchCamera) {
                                        Icon(Icons.Outlined.Cameraswitch,"Switch camera")
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            FilledIconButton(onClick={
                                if(c.id==null)controller.dismissError() else controller.hangup()
                            },modifier=Modifier.size(66.dp),
                                colors=IconButtonDefaults.filledIconButtonColors(containerColor=Color(0xFFC93B48))) {
                                Icon(Icons.Outlined.CallEnd,"End call")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoRenderer(engine:CallEngine,local:Boolean,modifier:Modifier=Modifier) {
    val context=LocalContext.current
    val renderer=remember(engine,local) {SurfaceViewRenderer(context)}
    DisposableEffect(engine,renderer,local) {
        engine.attachRenderer(renderer,local)
        onDispose {engine.detachRenderer(renderer,local)}
    }
    AndroidView(factory={renderer},modifier=modifier)
}
