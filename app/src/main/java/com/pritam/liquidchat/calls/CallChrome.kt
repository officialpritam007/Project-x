package com.pritam.liquidchat.calls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pritam.liquidchat.ui.LocalDisplayOptions
import com.pritam.liquidchat.ui.LocalGlassState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

private val CallEndColor=Color(0xFFFF3B30)
private val CallActiveFill=Color(0xFFF5F5F7)
private val CallCameraGreen=Color(0xFF30C759)
private val CallMutedOrange=Color(0xFFFF9500)

/** One restrained glass layer, separate from the caller photo and video content. */
@Composable internal fun CallGlassSurface(
    modifier:Modifier=Modifier,
    shape:Shape=CircleShape,
    content:@Composable BoxScope.()->Unit
) {
    val options=LocalDisplayOptions.current
    val state=LocalGlassState.current
    val glassColor=Color(0xFF252629)
    val opaque=options.reducedTransparency
    val useBlur=!opaque && options.blurIntensity>0 && state!=null
    val effect=if(useBlur)Modifier.hazeEffect(
        state!!,
        style=HazeStyle(
            backgroundColor=Color(0xFF171819),
            tint=HazeTint(glassColor.copy(alpha=.46f)),
            blurRadius=(options.blurIntensity.coerceIn(0,100)*.25f).dp,
            noiseFactor=.012f,
            // WebRTC SurfaceView pixels are outside Compose's captured layer.
            // Keep its controls legible even when only a fallback can be drawn.
            fallbackTint=HazeTint(glassColor.copy(alpha=.76f))
        )
    ) else Modifier
    Surface(
        modifier=modifier.shadow(3.dp,shape),
        shape=shape,
        color=if(opaque)glassColor else Color.Transparent,
        border=BorderStroke(.75.dp,Brush.linearGradient(listOf(
            Color.White.copy(alpha=if(opaque).16f else .25f),
            Color.White.copy(alpha=.055f)
        ))),
        tonalElevation=0.dp
    ) {
        Box(
            Modifier.clip(shape).then(effect)
                .background(if(opaque)Color.Transparent
                    else glassColor.copy(alpha=if(useBlur).18f else .64f))
                .background(Brush.linearGradient(listOf(
                    Color.White.copy(alpha=if(opaque)0f else .075f),
                    Color.Transparent,
                    Color.Black.copy(alpha=if(opaque)0f else .045f)
                ))),
            content=content
        )
    }
}

@Composable internal fun CallChromeButton(
    description:String,
    icon:ImageVector,
    onClick:()->Unit,
    modifier:Modifier=Modifier,
    size:Dp=56.dp,
    containerColor:Color?=null,
    tint:Color=Color.White,
    stateLabel:String?=null
) {
    Box(
        modifier.size(size.coerceAtLeast(48.dp))
            .clip(CircleShape)
            .semantics(mergeDescendants=true) {
                contentDescription=description
                if(stateLabel!=null)stateDescription=stateLabel
            }
            .clickable(role=Role.Button,onClick=onClick),
        contentAlignment=Alignment.Center
    ) {
        if(containerColor==null) {
            CallGlassSurface(Modifier.fillMaxSize()) {
                Icon(icon,contentDescription=null,tint=tint,
                    modifier=Modifier.align(Alignment.Center).size(27.dp))
            }
        } else {
            Surface(Modifier.fillMaxSize(),shape=CircleShape,
                color=containerColor,tonalElevation=0.dp) {
                Box(contentAlignment=Alignment.Center) {
                    Icon(icon,contentDescription=null,tint=tint,
                        modifier=Modifier.size(27.dp))
                }
            }
        }
    }
}

private fun audioOutputDescription(route:String)=
    "Select audio output" + if(route.isBlank())"" else ", current: $route"

@Composable internal fun VoiceCallControls(
    muted:Boolean,
    audioRoute:String,
    onMute:()->Unit,
    onAudio:()->Unit,
    onHangup:()->Unit,
    modifier:Modifier=Modifier
) {
    Row(
        modifier.fillMaxWidth().testTag("voice-call-controls"),
        horizontalArrangement=Arrangement.spacedBy(24.dp,Alignment.CenterHorizontally),
        verticalAlignment=Alignment.Top
    ) {
        VoiceControlLabel("Audio") {
            CallChromeButton(
                description=audioOutputDescription(audioRoute),
                icon=if(audioRoute.contains("Bluetooth",ignoreCase=true))
                    Icons.Outlined.BluetoothAudio else Icons.Outlined.VolumeUp,
                onClick=onAudio,size=64.dp,
                stateLabel=audioRoute.takeIf {it.isNotBlank()}
            )
        }
        VoiceControlLabel(if(muted)"Unmute" else "Mute") {
            CallChromeButton(
                description=if(muted)"Unmute microphone" else "Mute microphone",
                icon=if(muted)Icons.Outlined.MicOff else Icons.Outlined.Mic,
                onClick=onMute,size=64.dp,
                containerColor=if(muted)CallActiveFill else null,
                tint=if(muted)Color(0xFF1C1C1E) else Color.White,
                stateLabel=if(muted)"Microphone muted" else "Microphone on"
            )
        }
        VoiceControlLabel("End") {
            CallChromeButton("End call",Icons.Outlined.CallEnd,onHangup,
                size=64.dp,containerColor=CallEndColor)
        }
    }
}

@Composable private fun VoiceControlLabel(label:String,content:@Composable ()->Unit) {
    Column(Modifier.width(64.dp),horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=Arrangement.spacedBy(8.dp)) {
        content()
        Text(label,color=Color.White.copy(alpha=.94f),
            style=MaterialTheme.typography.labelMedium,
            fontWeight=FontWeight.Medium,
            textAlign=TextAlign.Center,
            modifier=Modifier.clearAndSetSemantics {})
    }
}

/** The native-style rail keeps End immediately available; secondary actions live in More. */
@Composable internal fun ActiveVideoControlStrip(
    muted:Boolean,cameraEnabled:Boolean,audioRoute:String,
    onMute:()->Unit,onAudio:()->Unit,onCamera:()->Unit,onSwitch:()->Unit,onHangup:()->Unit,
    modifier:Modifier=Modifier,onMenuVisibilityChanged:(Boolean)->Unit={}
) {
    var moreOpen by remember {mutableStateOf(false)}
    val menuListener by rememberUpdatedState(onMenuVisibilityChanged)
    LaunchedEffect(moreOpen) {menuListener(moreOpen)}
    DisposableEffect(Unit) {onDispose {menuListener(false)}}
    BoxWithConstraints(modifier) {
        val horizontal=maxHeight<238.dp
        val buttonSize=if(horizontal&&maxWidth<238.dp)48.dp else 52.dp
        val gap=if(buttonSize==48.dp)4.dp else 10.dp
        val controls:@Composable ()->Unit = {
        CallChromeButton(
            description=if(cameraEnabled)"Turn camera off" else "Turn camera on",
            icon=if(cameraEnabled)Icons.Outlined.Videocam else Icons.Outlined.VideocamOff,
            onClick=onCamera,size=buttonSize,
            containerColor=if(cameraEnabled)CallActiveFill else null,
            tint=if(cameraEnabled)CallCameraGreen else Color.White,
            stateLabel=if(cameraEnabled)"Camera on" else "Camera off"
        )
        CallChromeButton(
            description=if(muted)"Unmute microphone" else "Mute microphone",
            icon=if(muted)Icons.Outlined.MicOff else Icons.Outlined.Mic,
            onClick=onMute,size=buttonSize,
            containerColor=if(muted)CallActiveFill else null,
            tint=if(muted)CallMutedOrange else Color.White,
            stateLabel=if(muted)"Microphone muted" else "Microphone on"
        )
        Box {
            CallChromeButton("More call options",Icons.Outlined.MoreHoriz,
                {moreOpen=true},size=buttonSize,
                stateLabel=if(moreOpen)"Expanded" else "Collapsed")
            DropdownMenu(
                expanded=moreOpen,onDismissRequest={moreOpen=false},
                shape=RoundedCornerShape(22.dp),
                containerColor=Color(0xFF252629),
                tonalElevation=0.dp,
                modifier=Modifier.widthIn(min=220.dp).testTag("call-more-menu")
            ) {
                DropdownMenuItem(
                    text={Column {
                        Text("Audio output")
                        if(audioRoute.isNotBlank())Text(audioRoute,
                            style=MaterialTheme.typography.bodySmall,
                            color=Color.White.copy(alpha=.68f))
                    }},
                    leadingIcon={Icon(Icons.Outlined.VolumeUp,contentDescription=null)},
                    modifier=Modifier.semantics {
                        contentDescription=audioOutputDescription(audioRoute)
                    },
                    colors=MenuDefaults.itemColors(textColor=Color.White,
                        leadingIconColor=Color.White),
                    onClick={moreOpen=false;onAudio()}
                )
                DropdownMenuItem(
                    text={Text("Switch camera")},
                    leadingIcon={Icon(Icons.Outlined.Cameraswitch,contentDescription=null)},
                    modifier=Modifier.semantics {contentDescription="Switch camera"},
                    colors=MenuDefaults.itemColors(textColor=Color.White,
                        leadingIconColor=Color.White),
                    onClick={moreOpen=false;onSwitch()}
                )
            }
        }
        CallChromeButton("End call",Icons.Outlined.Close,onHangup,
            size=buttonSize,containerColor=CallEndColor)
        }
        if(horizontal)Row(
            Modifier.testTag("video-call-controls"),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(gap)
        ) {controls()}
        else Column(
            Modifier.width(52.dp).testTag("video-call-controls"),
            horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.spacedBy(gap)
        ) {controls()}
    }
}
