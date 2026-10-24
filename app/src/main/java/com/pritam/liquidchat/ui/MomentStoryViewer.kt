package com.pritam.liquidchat.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pritam.liquidchat.AppViewModel
import com.pritam.liquidchat.data.Images
import com.pritam.liquidchat.data.SavedMedia
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal fun advancePhotoStory(progress:Float,elapsedMs:Long,paused:Boolean,ready:Boolean):Float =
    if(paused||!ready)progress else (progress+elapsedMs.coerceIn(0L,250L)/5000f).coerceAtMost(1f)

@Composable internal fun StoryViewer(
    model:AppViewModel,stories:List<Moment>,initialId:String,onDismiss:()->Unit
) {
    if(stories.isEmpty())return
    var currentId by remember(initialId) {mutableStateOf(initialId)}
    val index=stories.indexOfFirst {it.id==currentId}.coerceAtLeast(0)
    val current=stories[index]
    val context=LocalContext.current
    val clipboard=LocalClipboardManager.current
    val saveScope=rememberCoroutineScope()
    val playback=remember(context) {MbStoryPlaybackSettings.read(context)}
    var savingStory by remember(current.id) {mutableStateOf(false)}
    var saveStatus by remember(current.id) {mutableStateOf<String?>(null)}
    var paused by remember(current.id) {mutableStateOf(false)}
    var muted by remember {mutableStateOf(MbStoryPlaybackSettings.initiallyMuted(context,playback))}
    var progress by remember(current.id) {mutableFloatStateOf(0f)}
    var imageReady by remember(current.id) {mutableStateOf(false)}
    var replay by remember(current.id) {mutableIntStateOf(0)}
    var dragDistance by remember(current.id) {mutableFloatStateOf(0f)}
    val foreground by model.repository.foreground.collectAsStateWithLifecycle()
    val currentCall by model.calls.view.collectAsStateWithLifecycle()
    val effectivePause=paused||savingStory||!foreground||currentCall!=null
    // Photos advance after five seconds; MP4 progress is driven by the actual player.
    LaunchedEffect(current.id,effectivePause,imageReady,stories.size,current.mediaKind,replay) {
        if(current.mediaKind!="story_video") {
            var lastTick=android.os.SystemClock.elapsedRealtime()
            while(progress<1f) {
                delay(50)
                val now=android.os.SystemClock.elapsedRealtime()
                progress=advancePhotoStory(progress,now-lastTick,effectivePause,imageReady)
                lastTick=now
            }
            if(!effectivePause && playback.autoAdvance) {
                if(index<stories.lastIndex)currentId=stories[index+1].id else onDismiss()
            }
        }
    }
    androidx.compose.ui.window.Dialog(
        onDismissRequest=onDismiss,
        properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)
    ) {
        Surface(Modifier.fillMaxSize(),color=Color.Black) {
            Box(Modifier.fillMaxSize()) {
                if(current.mediaKind=="story_video") key(current.id,replay) {
                    StoryVideoPlayer(
                        backend=model.backend,media=current.media,
                        paused=effectivePause,muted=muted,
                        onProgress={progress=it},
                        onFinished={if(playback.autoAdvance) {
                            if(index<stories.lastIndex)currentId=stories[index+1].id else onDismiss()
                        }},
                        modifier=Modifier.fillMaxSize()
                    )
                } else ProtectedImage(model.backend,current.media,
                    description="${current.name}'s story",modifier=Modifier.fillMaxSize(),
                    scale=ContentScale.Fit,onLoadState={imageReady=it})
                Box(Modifier.fillMaxSize()
                    .pointerInput(current.id) {
                        detectVerticalDragGestures(
                            onDragStart={dragDistance=0f;paused=true},
                            onVerticalDrag={change,amount->
                                change.consume()
                                dragDistance=(dragDistance+amount).coerceAtLeast(0f)
                            },
                            onDragEnd={
                                val dismiss=dragDistance>size.height*.18f
                                dragDistance=0f
                                paused=false
                                if(dismiss)onDismiss()
                            },
                            onDragCancel={dragDistance=0f;paused=false}
                        )
                    }
                    .pointerInput(current.id,stories.size) {
                    detectTapGestures(
                        onPress={
                            paused=true
                            try {tryAwaitRelease()} finally {paused=false}
                        },
                        onTap={offset->
                            if(offset.x<size.width/2f) {
                                if(index>0)currentId=stories[index-1].id else {progress=0f;replay++}
                            } else if(index<stories.lastIndex)currentId=stories[index+1].id else onDismiss()
                        }
                    )
                })
                Column(
                    Modifier.fillMaxWidth().align(Alignment.TopCenter)
                        .statusBarsPadding().padding(14.dp),
                    verticalArrangement=Arrangement.spacedBy(12.dp)
                ) {
                    Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        stories.forEachIndexed {i,_->
                            LinearProgressIndicator(
                                progress={if(i<index)1f else if(i==index)progress else 0f},
                                modifier=Modifier.weight(1f).height(3.dp),
                                color=Color.White,trackColor=Color.White.copy(alpha=.4f)
                            )
                        }
                    }
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(current.name,color=Color.White,fontWeight=FontWeight.SemiBold)
                            Text("24-hour story",color=Color.White.copy(alpha=.78f),style=MaterialTheme.typography.labelSmall)
                        }
                        if(current.mediaKind=="story_video") {
                            IconButton(onClick={muted=!muted}) {
                                Icon(if(muted)Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp,
                                    if(muted)"Unmute story" else "Mute story",tint=Color.White)
                            }
                        }
                        if(current.mediaKind=="image" || current.mediaKind=="story_video") {
                            IconButton(onClick={
                                if(!savingStory) {
                                    savingStory=true
                                    saveStatus=null
                                    val story=current
                                    saveScope.launch {
                                        try {
                                            if(story.mediaKind=="story_video") {
                                                SavedMedia.saveStoryVideo(context,model.backend,
                                                    story.id,story.media,story.expires)
                                                saveStatus="Saved to Movies/Liquid Chat"
                                            } else {
                                                SavedMedia.saveStoryPhoto(context,model.backend,
                                                    story.id,story.media,story.expires)
                                                saveStatus="Saved to Pictures/Liquid Chat"
                                            }
                                        } catch(e:CancellationException) {throw e}
                                          catch(e:Exception) {
                                            saveStatus=e.message ?: "Could not save story."
                                        } finally {savingStory=false}
                                    }
                                }
                            },enabled=!savingStory) {
                                Icon(Icons.Outlined.Download,
                                    if(current.mediaKind=="story_video")"Save video story" else "Save photo story",
                                    tint=Color.White)
                            }
                        }
                        if(current.caption.isNotBlank()) {
                            IconButton(onClick={
                                clipboard.setText(AnnotatedString(current.caption))
                                saveStatus="Caption copied"
                            }) {
                                Icon(Icons.Outlined.ContentCopy,"Copy story caption",tint=Color.White)
                            }
                        }
                        IconButton(onClick=onDismiss) {Icon(Icons.Outlined.Close,"Close story",tint=Color.White)}
                    }
                }
                saveStatus?.let {status->
                    Text(status,Modifier.align(Alignment.Center).padding(16.dp),
                        color=Color.White,style=MaterialTheme.typography.bodyMedium)
                }
                if(!playback.autoAdvance && progress>=1f) {
                    TextButton(onClick={progress=0f;replay++},
                        modifier=Modifier.align(Alignment.Center)) {
                        Text("Replay story",color=Color.White)
                    }
                }
                if(current.caption.isNotBlank())
                    Text(current.caption,Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                        .padding(18.dp),color=Color.White)
            }
        }
    }
}

