package com.pritam.liquidchat.calls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Production call chrome, independent of media capture and controller lifetime. */
@Composable internal fun CallScreenChrome(
    call:CallView,
    displayName:String?,
    timer:String,
    activeVideo:Boolean,
    controlsVisible:Boolean,
    onMinimize:()->Unit,
    onAccept:()->Unit,
    onDecline:()->Unit,
    onMute:()->Unit,
    onAudio:()->Unit,
    onCamera:()->Unit,
    onSwitch:()->Unit,
    onHangup:()->Unit,
    onClose:()->Unit,
    onMenuVisibilityChanged:(Boolean)->Unit={}
) {
    val terminal=call.terminal
    val shouldRing=call.incoming&&call.status=="Incoming call"
    val videoLayout=call.video&&!shouldRing&&!terminal
    val showChrome=!activeVideo||controlsVisible
    val status=when {
        terminal->call.status
        shouldRing->"Incoming ${if(call.video)"video" else "voice"} call"
        call.held->"On hold"
        call.remoteHeld->"Other participant is on hold"
        !call.signalingAvailable->"Signaling unavailable · media may continue"
        call.connectedAt>0->if(call.status=="Connected")timer else "$timer · ${call.status}"
        call.status=="Ringing"->"Ringing"
        else->call.status
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val landscape=maxWidth>maxHeight
        if(videoLayout) {
            AnimatedVisibility(visible=showChrome,enter=fadeIn(),exit=fadeOut(),
                modifier=Modifier.align(Alignment.TopStart).systemBarsPadding().padding(16.dp)) {
                CallGlassSurface(shape=RoundedCornerShape(50),modifier=Modifier.widthIn(max=260.dp)) {
                    Row(Modifier.padding(start=18.dp,end=4.dp,top=4.dp,bottom=4.dp),
                        verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f,false).padding(end=10.dp)) {
                            Text(displayName ?: "Liquid Chat",color=Color.White,
                                style=MaterialTheme.typography.titleMedium,maxLines=1,overflow=TextOverflow.Ellipsis)
                            Text(status,color=Color.White.copy(alpha=.8f),
                                style=MaterialTheme.typography.labelMedium,maxLines=2,overflow=TextOverflow.Ellipsis)
                        }
                        IconButton(onClick=onMinimize) {Icon(Icons.Outlined.ExpandMore,"Minimize call",tint=Color.White)}
                    }
                }
            }
            AnimatedVisibility(visible=showChrome,enter=fadeIn(),exit=fadeOut(),
                modifier=Modifier.align(Alignment.BottomEnd).systemBarsPadding().padding(16.dp)) {
                key(call.id) {ActiveVideoControlStrip(
                    muted=call.muted,cameraEnabled=call.cameraEnabled,audioRoute=call.audioRoute,
                    onMute=onMute,onAudio=onAudio,onCamera=onCamera,onSwitch=onSwitch,onHangup=onHangup,
                    onMenuVisibilityChanged=onMenuVisibilityChanged
                )}
            }
        } else {
            if(!terminal)CallChromeButton("Minimize call",Icons.Outlined.ExpandMore,onMinimize,
                modifier=Modifier.align(Alignment.TopStart).systemBarsPadding().padding(16.dp),size=48.dp)
            Column(
                Modifier.align(if(landscape)Alignment.CenterStart else Alignment.TopCenter)
                    .widthIn(max=if(landscape)maxWidth*.5f else 420.dp)
                    .systemBarsPadding().padding(horizontal=if(landscape)32.dp else 64.dp,
                        vertical=if(landscape)16.dp else 76.dp),
                horizontalAlignment=if(landscape)Alignment.Start else Alignment.CenterHorizontally,
                verticalArrangement=Arrangement.spacedBy(8.dp)
            ) {
                Text(if(terminal)"Call finished" else if(call.video)"Liquid Chat Video" else "Liquid Chat Audio",
                    color=Color.White.copy(alpha=.72f),style=MaterialTheme.typography.bodyMedium)
                Text(displayName ?: "Liquid Chat",color=Color.White,fontSize=34.sp,lineHeight=40.sp,
                    fontWeight=FontWeight.SemiBold,maxLines=2,overflow=TextOverflow.Ellipsis,
                    textAlign=if(landscape)TextAlign.Start else TextAlign.Center)
                Text(status,color=Color.White.copy(alpha=.88f),style=MaterialTheme.typography.bodyLarge,
                    textAlign=if(landscape)TextAlign.Start else TextAlign.Center,maxLines=3,overflow=TextOverflow.Ellipsis)
            }
            Box(Modifier.align(if(landscape)Alignment.CenterEnd else Alignment.BottomCenter)
                .widthIn(max=if(landscape)maxWidth*.5f else 420.dp)
                .systemBarsPadding().padding(horizontal=if(landscape)12.dp else 20.dp,vertical=28.dp),
                contentAlignment=Alignment.Center) {
                when {
                    shouldRing->IncomingCallControls(onDecline=onDecline,onAccept=onAccept)
                    terminal->Column(horizontalAlignment=Alignment.CenterHorizontally) {
                        CallChromeButton("Close",Icons.Outlined.Close,onClose,
                            size=64.dp,containerColor=Color(0xFFFF3B30))
                        Text("Close",color=Color.White,modifier=Modifier.padding(top=8.dp))
                    }
                    else->VoiceCallControls(call.muted,call.audioRoute,onMute,onAudio,onHangup)
                }
            }
        }
    }
}
