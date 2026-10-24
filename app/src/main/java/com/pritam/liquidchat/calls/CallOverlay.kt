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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pritam.liquidchat.data.Backend
import com.pritam.liquidchat.ui.*
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay
import androidx.compose.ui.input.pointer.PointerEventPass
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallOverlay(controller:CallController,displayName:String?,backend:Backend?=null,avatar:String="") {
    val call by controller.view.collectAsStateWithLifecycle()
    val c=call ?: return
    val context=LocalContext.current
    var seconds by remember(c.id,c.connectedAt) {mutableLongStateOf(0)}
    LaunchedEffect(c.connectedAt) {while(c.connectedAt>0){seconds=(SystemClock.elapsedRealtime()-c.connectedAt)/1000;delay(1000)}}
    val timer="%02d:%02d".format(seconds/60,seconds%60)
    val terminal=c.terminal
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
    var permissionPromptOpen by remember(c.conversation,c.peer) {mutableStateOf(false)}
    val request=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {result->
        permissionPromptOpen=false
        if(needed.all {result[it]==true})controller.accept() else permissionError=true
    }
    fun answer() {if(needed.all {ContextCompat.checkSelfPermission(context,it)==PackageManager.PERMISSION_GRANTED})controller.accept() else {permissionPromptOpen=true;request.launch(needed)}}
    LaunchedEffect(c.id,c.answerRequested) {if(c.answerRequested&&shouldRing)answer()}
    var routes by remember {mutableStateOf<List<Pair<Int,String>>?>(null)}
    LaunchedEffect(c.audioDevicesRevision) {if(routes!=null)routes=controller.audioRoutes()}
    var automaticPermissionRequested by remember(c.conversation,c.peer) {mutableStateOf(false)}
    val automaticPermissions=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {permissionPromptOpen=false;controller.refreshAudioRoutes()}
    LaunchedEffect(shouldRing,terminal) {
        if(!shouldRing&&!terminal&&!automaticPermissionRequested) {
            automaticPermissionRequested=true
            val optional=buildList {
                if(android.os.Build.VERSION.SDK_INT>=31)add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.READ_PHONE_STATE)
            }.filter {ContextCompat.checkSelfPermission(context,it)!=PackageManager.PERMISSION_GRANTED}
            if(optional.isNotEmpty()){permissionPromptOpen=true;automaticPermissions.launch(optional.toTypedArray())}
        }
    }
    val bluetoothPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {permissionPromptOpen=false;controller.refreshAudioRoutes();routes=controller.audioRoutes()}
    fun openAudioRoutes() {
        if(android.os.Build.VERSION.SDK_INT>=31 &&
            ContextCompat.checkSelfPermission(context,Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)
            {permissionPromptOpen=true;bluetoothPermission.launch(Manifest.permission.BLUETOOTH_CONNECT)}
        else routes=controller.audioRoutes()
    }
    // The compact pill is laid out by Scaffold; video can instead float over the chat.
    if(c.minimized) {
        val minimizedEngine=controller.renderer()
        if(c.video && c.renderVideo && !c.compact && minimizedEngine!=null)
            FloatingCallVideo(controller,minimizedEngine,c)
        return
    }
    val engine=controller.renderer()
    val activeVideo=c.renderVideo&&engine!=null&&!terminal
    var swapped by remember(c.id) {mutableStateOf(false)}
    LaunchedEffect(c.systemPip) {if(c.systemPip)swapped=false}
    var moreMenuOpen by remember(c.id) {mutableStateOf(false)}
    var controlsVisible by remember(c.id) {mutableStateOf(true)}
    var lastInteraction by remember(c.id) {mutableLongStateOf(SystemClock.elapsedRealtime())}
    val accessibility=context.getSystemService(android.view.accessibility.AccessibilityManager::class.java)
    var accessibilityEnabled by remember {mutableStateOf(accessibility.isEnabled)}
    DisposableEffect(accessibility) {
        val listener=android.view.accessibility.AccessibilityManager.AccessibilityStateChangeListener {accessibilityEnabled=it}
        accessibility.addAccessibilityStateChangeListener(listener)
        onDispose {accessibility.removeAccessibilityStateChangeListener(listener)}
    }
    LaunchedEffect(c.id,c.status,routes,permissionError,permissionPromptOpen,lastInteraction,c.systemPip,accessibilityEnabled,moreMenuOpen) {
        controlsVisible=true
        if(canAutoHideCallControls(c.status,routes!=null||moreMenuOpen,permissionPromptOpen||permissionError,accessibilityEnabled)) {
            delay(accessibility.getRecommendedTimeoutMillis(4000,android.view.accessibility.AccessibilityManager.FLAG_CONTENT_CONTROLS).toLong())
            if(!accessibility.isEnabled)controlsVisible=false
        }
    }

    var x by remember(c.id) {mutableFloatStateOf(0f)}
    var y by remember(c.id) {mutableFloatStateOf(-1f)}
    val density=LocalDensity.current
    val rootView=LocalView.current
    val callGlass=remember(c.id) {HazeState()}
    DisposableEffect(activeVideo,controlsVisible,context,rootView) {
        // Immersive mode hides Android chrome, not the draggable in-call self preview.
        var ownerContext:android.content.Context=context
        while(ownerContext !is Activity && ownerContext is ContextWrapper)ownerContext=ownerContext.baseContext
        val window=(ownerContext as? Activity)?.window
        val bars=window?.let {WindowCompat.getInsetsController(it,rootView)}
        val lightStatus=bars?.isAppearanceLightStatusBars
        val lightNavigation=bars?.isAppearanceLightNavigationBars
        bars?.isAppearanceLightStatusBars=false
        bars?.isAppearanceLightNavigationBars=false
        if(activeVideo&&!controlsVisible) {
            bars?.systemBarsBehavior=WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            bars?.hide(WindowInsetsCompat.Type.systemBars())
        } else bars?.show(WindowInsetsCompat.Type.systemBars())
        onDispose {
            bars?.show(WindowInsetsCompat.Type.systemBars())
            lightStatus?.let {bars?.isAppearanceLightStatusBars=it}
            lightNavigation?.let {bars?.isAppearanceLightNavigationBars=it}
        }
    }
    // Video controls retain their readable translucent material;
    // contact posters retain source-backed blur.
    CompositionLocalProvider(LocalGlassState provides if(activeVideo)null else callGlass,LocalGlassDark provides true) {
    Surface(Modifier.fillMaxSize().pointerInput(c.id) {
        awaitPointerEventScope {while(true) {
            awaitPointerEvent(PointerEventPass.Initial)
            lastInteraction=SystemClock.elapsedRealtime()
        }}
    },color=Color.Black) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val showChrome=(!activeVideo||controlsVisible)&&!c.systemPip
            val layoutDirection=LocalLayoutDirection.current
            if(!activeVideo) CallPoster(displayName,backend,avatar)
            val remoteStatus=when {
                c.remoteHeld->"Other participant is on hold"
                !c.remoteCamera->"Other participant turned camera off"
                c.status=="Reconnecting"->"Video unavailable · reconnecting"
                !c.remoteVideoReady&&c.remoteVideoSeen->"Video unavailable"
                !c.remoteVideoReady->"Waiting for remote video…"
                else->null
            }
            if(activeVideo&&engine!=null)key(engine,swapped) {
                if((swapped&&c.cameraEnabled&&!c.held)||(!swapped&&c.remoteCamera&&!c.remoteHeld))VideoRenderer(engine,swapped,Modifier.fillMaxSize())
                if(remoteStatus!=null&&!swapped) {
                    Text(remoteStatus,color=Color.White,textAlign=TextAlign.Center,
                        modifier=Modifier.fillMaxSize().background(Color.Black).wrapContentSize(Alignment.Center).padding(32.dp)
                            .background(Color.Black.copy(alpha=.55f),RoundedCornerShape(16.dp)).padding(12.dp))
                }
                if(swapped&&(!c.cameraEnabled||c.held))Text(if(c.held)"On hold" else "Camera off",color=Color.White,modifier=Modifier.align(Alignment.Center))
                // Transparent touch surface belongs behind the inset preview, never over it.
                Box(Modifier.fillMaxSize().clickable {controlsVisible=true;lastInteraction=SystemClock.elapsedRealtime()})
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
                    Color.Black.copy(alpha=.32f),Color.Transparent,Color.Black.copy(alpha=.28f)))))
                val previewWidth=if(maxWidth<360.dp)100.dp else 116.dp
                val previewHeight=if(maxHeight<400.dp)96.dp else if(maxHeight<620.dp)138.dp else 154.dp
                val widthPx=with(density){previewWidth.roundToPx()}
                val heightPx=with(density){previewHeight.roundToPx()}
                val marginPx=with(density){12.dp.roundToPx()}
                val safeTop=WindowInsets.statusBars.getTop(density)
                val safeBottom=WindowInsets.navigationBars.getBottom(density)
                val sideInsets=WindowInsets.systemBars.asPaddingValues()
                val safeStart=with(density){sideInsets.calculateStartPadding(layoutDirection).roundToPx()}
                val safeEnd=with(density){sideInsets.calculateEndPadding(layoutDirection).roundToPx()}
                // Keep the always-available End control clear of the native video surface.
                val horizontalControls=constraints.maxHeight-safeTop-safeBottom < with(density){270.dp.roundToPx()}
                val railPx=with(density){if(showChrome&&!horizontalControls)72.dp.roundToPx() else 0}
                val bottomControlsPx=with(density){if(showChrome&&horizontalControls)84.dp.roundToPx() else 0}
                val maxX=(constraints.maxWidth-widthPx-marginPx*2-safeStart-safeEnd-railPx).coerceAtLeast(0).toFloat()
                val maxY=(constraints.maxHeight-heightPx-safeTop-safeBottom-marginPx*2-bottomControlsPx).coerceAtLeast(0).toFloat()
                val resolvedX=if(x<0f)maxX else x.coerceIn(0f,maxX)
                val resolvedY=if(y<0f)maxY else y.coerceIn(0f,maxY)
                if(!c.systemPip)Box(Modifier
                    .offset {IntOffset(safeStart+marginPx+resolvedX.roundToInt(),safeTop+marginPx+resolvedY.roundToInt())}
                    .size(previewWidth,previewHeight)
                    .background(Color.Black,RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp,Color.White.copy(alpha=.38f),RoundedCornerShape(22.dp))
                    .testTag("call-self-preview")
                    .semantics {contentDescription=if(swapped)"Other participant preview; tap to swap video" else "Your video preview; tap to swap video"}
                    .pointerInput(c.id,maxX,maxY) {
                        detectDragGestures {change,drag->
                            change.consume()
                            x=((if(x<0f)maxX else x)+drag.x).coerceIn(0f,maxX)
                            y=((if(y<0f)resolvedY else y)+drag.y).coerceIn(0f,maxY)
                        }
                    }
                    .clickable {swapped=!swapped}) {
                    if((!swapped&&c.cameraEnabled&&!c.held)||(swapped&&c.remoteCamera&&!c.remoteHeld))VideoRenderer(engine,!swapped,Modifier.fillMaxSize(),22f)
                    if(!swapped&&(!c.cameraEnabled||c.held))Text(if(c.held)"On hold" else "Camera off",color=Color.White,modifier=Modifier.align(Alignment.Center))
                    if(swapped&&(!c.remoteCamera||c.remoteHeld||!c.remoteVideoReady))Text(if(c.remoteHeld)"On hold" else if(!c.remoteCamera)"Camera off" else if(c.remoteVideoSeen)"Video unavailable" else "Waiting for video",color=Color.White,modifier=Modifier.align(Alignment.Center).background(Color.Black).padding(8.dp))
                }
            }
            if(c.held&&!c.systemPip)TextButton(onClick=controller::resumeHeldCall,modifier=Modifier.align(Alignment.Center)) {Text("Resume call")}
            CallScreenChrome(
                call=c,displayName=displayName,timer=timer,
                activeVideo=activeVideo,controlsVisible=controlsVisible&&!c.systemPip,
                onMinimize=controller::minimize,onAccept=::answer,onDecline=controller::decline,
                onMute=controller::toggleMute,onAudio=::openAudioRoutes,
                onCamera=controller::toggleCamera,onSwitch=controller::switchCamera,
                onHangup=controller::hangup,onClose=controller::dismissError,
                onMenuVisibilityChanged={moreMenuOpen=it}
            )
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
/** The real account-scoped contact photo becomes the poster; absent photos keep a quiet fallback. */
@Composable private fun CallPoster(name:String?,backend:Backend?,avatar:String) {
    val backdrop=Brush.linearGradient(listOf(Color(0xFF263C43),Color(0xFF26272C),Color(0xFF493A34)))
    Box(Modifier.fillMaxSize().background(backdrop)) {
        if(backend!=null&&avatar.isNotBlank())
            ProtectedImage(backend,avatar,description="Caller photo",modifier=Modifier.fillMaxSize(),
                scale=androidx.compose.ui.layout.ContentScale.Crop)
        else Box(Modifier.fillMaxSize().background(backdrop).glassSource(zIndex=0f),contentAlignment=Alignment.Center) {
            Text(name?.take(1)?.uppercase() ?: "L",fontSize=140.sp,fontWeight=FontWeight.Light,
                color=Color.White.copy(alpha=.15f))
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
            Color.Black.copy(alpha=.55f),Color.Black.copy(alpha=.08f),Color.Black.copy(alpha=.12f),
            Color.Black.copy(alpha=.75f)))))
    }
}

@Composable internal fun IncomingCallControls(onDecline:()->Unit,onAccept:()->Unit) {
    Row(horizontalArrangement=Arrangement.spacedBy(56.dp),verticalAlignment=Alignment.CenterVertically) {
        Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
            CallChromeButton("Decline",Icons.Outlined.CallEnd,onDecline,size=64.dp,
                containerColor=Color(0xFFFF3B30))
            Text("Decline",color=Color.White,style=MaterialTheme.typography.labelLarge)
        }
        Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
            CallChromeButton("Accept",Icons.Outlined.Call,onAccept,size=64.dp,
                containerColor=Color(0xFF30B85A))
            Text("Accept",color=Color.White,style=MaterialTheme.typography.labelLarge)
        }
    }
}
/**
 * In-app Picture-in-Picture. Not Android system PiP: media stays in the existing foreground
 * call service and the window stays inside the app. Insets keep the camera above the IME.
 */
@Composable private fun FloatingCallVideo(controller:CallController,engine:CallEngine,call:CallView) {
    val callId=call.id
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
                if(call.remoteCamera&&!call.remoteHeld)VideoRenderer(engine,false,Modifier.fillMaxSize(),22f)
                if(!call.remoteVideoReady||call.remoteHeld||!call.remoteCamera)Text(if(call.remoteHeld)"On hold" else if(!call.remoteCamera)"Camera off" else if(call.remoteVideoSeen)"Video unavailable" else "Waiting for video",color=Color.White,modifier=Modifier.fillMaxSize().background(Color.Black).wrapContentSize(Alignment.Center))
                Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(width=36.dp,height=48.dp).clip(Glass.compact)) {
                    if(call.cameraEnabled&&!call.held)VideoRenderer(engine,true,Modifier.fillMaxSize(),12f)
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

@Composable private fun VideoRenderer(engine:CallEngine,local:Boolean,modifier:Modifier=Modifier,radius:Float=0f) {
    val context=LocalContext.current
    val renderer=remember(engine,local) {CallTextureRenderer(context)}
    DisposableEffect(engine,renderer,local) {engine.attachRenderer(renderer,local);onDispose {engine.detachRenderer(renderer,local)}}
    AndroidView(factory={renderer},modifier=modifier,update={view->
        view.clipToOutline=radius>0
        view.outlineProvider=object:android.view.ViewOutlineProvider() {
            override fun getOutline(v:android.view.View,outline:android.graphics.Outline) {
                outline.setRoundRect(0,0,v.width,v.height,radius*v.resources.displayMetrics.density)
            }
        }
        view.invalidateOutline()
    })
}
