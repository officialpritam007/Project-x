package com.pritam.liquidchat.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
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

@Composable internal fun MomentEditor(model:AppViewModel,kind:String,onDismiss:()->Unit,onPublished:()->Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val session by model.session.collectAsStateWithLifecycle()
    val owner=session?.userId.orEmpty()
    var publishId by remember {mutableStateOf(UUID.randomUUID().toString())}
    var photo by remember {mutableStateOf<Images.Prepared?>(null)}
    var video by remember {mutableStateOf<android.net.Uri?>(null)}
    var textMode by remember {mutableStateOf(false)}
    var textStory by rememberSaveable {mutableStateOf("")}
    var textBackground by rememberSaveable {mutableStateOf("Indigo")}
    var videoDuration by remember {mutableLongStateOf(0L)}
    var trimStart by remember {mutableLongStateOf(0L)}
    var progress by remember {mutableIntStateOf(0)}
    var stage by remember {mutableStateOf("")}
    var caption by rememberSaveable {mutableStateOf("")}
    var media by remember {mutableStateOf<String?>(null)}
    var busy by remember {mutableStateOf(false)}
    var error by remember {mutableStateOf<String?>(null)}

    val photoPicker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {uri->
        if(uri!=null&&!busy) {
            busy=true;error=null
            scope.launch {
                try {
                    val prepared=Images.prepare(context,uri,owner)
                    if(model.session.value?.userId!=owner) {
                        prepared.file.delete()
                        throw CancellationException("Account changed")
                    }
                    photo?.file?.delete()
                    photo=prepared
                    video=null
                    textMode=false
                    media=null
                    publishId=UUID.randomUUID().toString()
                } catch(e:CancellationException) {throw e}
                catch(_:Exception) {error="Could not open this photo. Choose another image."}
                finally {busy=false}
            }
        }
    }
    val videoPicker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {uri->
        if(uri!=null&&!busy&&kind=="story") {
            busy=true;error=null
            scope.launch {
                try {
                    val duration=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        com.pritam.liquidchat.data.StoryVideo.durationMs(context,uri)
                    }
                    if(model.session.value?.userId!=owner)throw CancellationException("Account changed")
                    video=uri
                    videoDuration=duration
                    trimStart=0L
                    photo?.file?.delete();photo=null
                    textMode=false
                    media=null
                    publishId=UUID.randomUUID().toString()
                } catch(e:CancellationException) {throw e}
                catch(_:Exception) {error="Could not read video. Choose a playable MP4 file."}
                finally {busy=false}
            }
        }
    }
    DisposableEffect(Unit) {onDispose {photo?.file?.delete()}}
    LiquidAlertDialog(
        onDismissRequest={if(!busy)onDismiss()},
        title={Text(if(kind=="story")"New story" else "New post")},
        text={
            Column(Modifier.fillMaxWidth().heightIn(max=530.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("Visible only to your saved contacts. Blocked contacts cannot view it.",
                    style=MaterialTheme.typography.bodySmall)
                if(kind=="story")Text("Stories disappear after 24 hours.",
                    style=MaterialTheme.typography.labelMedium)
                photo?.let {selected->
                    coil.compose.AsyncImage(selected.file,"Photo preview",
                        Modifier.fillMaxWidth().heightIn(max=200.dp)
                            .aspectRatio(selected.width.toFloat()/selected.height.coerceAtLeast(1)))
                }
                if(textMode && kind=="story") {
                    val cardColor=when(textBackground) {
                        "Emerald"->Color(0xFF106051)
                        "Rose"->Color(0xFF7E2D4D)
                        "Midnight"->Color(0xFF132233)
                        else->Color(0xFF272B67)
                    }
                    Surface(shape=RoundedCornerShape(24.dp),color=cardColor,
                        modifier=Modifier.fillMaxWidth().height(180.dp)) {
                        Box(Modifier.padding(20.dp),contentAlignment=Alignment.Center) {
                            Text(textStory.ifBlank {"Your story text"},
                                color=Color.White,fontWeight=FontWeight.Bold,
                                maxLines=6,style=MaterialTheme.typography.titleLarge)
                        }
                    }
                    OutlinedTextField(value=textStory,
                        onValueChange={textStory=it.take(280);media=null},
                        label={Text("Text story (max 280 characters)")},
                        enabled=!busy,modifier=Modifier.fillMaxWidth(),maxLines=5)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        com.pritam.liquidchat.data.TextStory.palettes.forEach {option->
                            FilterChip(selected=textBackground==option,
                                onClick={textBackground=option;media=null},
                                label={Text(option)})
                        }
                    }
                }
                if(video!=null) {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Videocam,"Selected video",tint=MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Text("Video selected · "+(videoDuration/1000)+" seconds")
                        }
                        val maximum=(videoDuration-30_000L).coerceAtLeast(0L)
                        if(maximum>0L) {
                            Text("Choose the beginning of a 30-second story clip",
                                style=MaterialTheme.typography.bodySmall)
                            Slider(value=trimStart.toFloat(),onValueChange={trimStart=it.toLong()},
                                valueRange=0f..maximum.toFloat(),enabled=!busy)
                        }
                        Text("Start: "+(trimStart/1000)+"s · End: "+
                            ((trimStart+30_000L).coerceAtMost(videoDuration)/1000)+"s",
                            style=MaterialTheme.typography.labelSmall)
                        Text("Export: H.264/AAC MP4, up to 720p and 8 MB.",
                            style=MaterialTheme.typography.labelSmall)
                    }
                }
                if(media==null) {
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick={photoPicker.launch("image/*")},enabled=!busy) {
                            Icon(Icons.Outlined.PhotoLibrary,null)
                            Spacer(Modifier.width(4.dp))
                            Text(if(photo==null)"Photo" else "Change photo")
                        }
                        if(kind=="story")TextButton(onClick={
                            photo?.file?.delete();photo=null
                            video=null;media=null
                            textMode=true
                            publishId=UUID.randomUUID().toString()
                        },enabled=!busy) {
                            Icon(Icons.Outlined.TextFields,null)
                            Spacer(Modifier.width(4.dp))
                            Text("Text")
                        }
                        if(kind=="story")TextButton(onClick={videoPicker.launch("video/*")},enabled=!busy) {
                            Icon(Icons.Outlined.VideoLibrary,null)
                            Spacer(Modifier.width(4.dp))
                            Text(if(video==null)"Video" else "Change video")
                        }
                    }
                }
                OutlinedTextField(caption,{caption=it.take(1000)},label={Text("Caption (optional)")},
                    enabled=!busy&&media==null,modifier=Modifier.fillMaxWidth(),maxLines=3)
                if(busy) {
                    if(stage=="Preparing video"||stage=="Uploading")
                        LinearProgressIndicator(progress={progress/100f},modifier=Modifier.fillMaxWidth())
                    else LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(if(stage=="Preparing video"||stage=="Uploading")"$stage · $progress%" else "Publishing…",
                        style=MaterialTheme.typography.labelSmall)
                }
                error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
            }
        },
        confirmButton={
            TextButton(enabled=(photo!=null||video!=null||(kind=="story"&&textMode&&textStory.isNotBlank()))&&!busy&&owner.isNotEmpty(),onClick={
                val selectedPhoto=photo
                val selectedVideo=video
                val selectedText=if(textMode&&kind=="story")textStory.trim() else ""
                val selectedBackground=textBackground
                if(selectedPhoto==null&&selectedVideo==null&&selectedText.isEmpty())return@TextButton
                busy=true;error=null;progress=0;stage="Publishing"
                scope.launch {
                    try {
                        if(model.session.value?.userId!=owner)throw CancellationException("Account changed")
                        if(media!=null) {
                            try {
                                model.backend.rpc("image_access",JSONObject().put("p_id",media))
                            } catch(e:com.pritam.liquidchat.data.ApiException) {
                                if(e.status==403||e.code=="42501")media=null else throw e
                            }
                        }
                        if(media==null) {
                            media=if(selectedVideo!=null) {
                                stage="Preparing video"
                                val encoded=com.pritam.liquidchat.data.StoryVideo.export(
                                    context,selectedVideo,trimStart,onProgress={progress=it})
                                stage="Uploading";progress=0
                                try {model.backend.uploadStoryVideo(encoded) {value->scope.launch {progress=value}}.getString("id")}
                                finally {encoded.delete()}
                            } else if(selectedPhoto!=null) {
                                stage="Uploading";progress=0
                                model.backend.uploadImage(selectedPhoto.file,null,onProgress={value->scope.launch {progress=value}}).getString("id")
                            } else {
                                stage="Preparing text story";progress=0
                                val prepared=com.pritam.liquidchat.data.TextStory.prepare(
                                    context,owner,selectedText,selectedBackground)
                                try {
                                    stage="Uploading"
                                    model.backend.uploadImage(prepared.file,null,
                                        onProgress={value->scope.launch {progress=value}}).getString("id")
                                } finally {prepared.file.delete()}
                            }
                        }
                        stage="Publishing"
                        model.backend.rpc("publish_moment",JSONObject()
                            .put("p_id",publishId).put("p_media",media)
                            .put("p_kind",kind).put("p_caption",
                                if(selectedText.isNotEmpty())(selectedText+
                                    if(caption.isBlank())"" else "\n"+caption.trim()).take(1000)
                                else caption.trim()))
                        onPublished()
                    } catch(e:CancellationException) {throw e}
                    catch(e:Exception) {
                        error=e.message?.take(120) ?: "Could not publish. Your selected media is safe. Retry."
                    } finally {busy=false}
                }
            }) {Text(if(error==null)"Publish" else "Retry")}
        },
        dismissButton={TextButton(onClick=onDismiss,enabled=!busy) {Text("Cancel")}}
    )
}
