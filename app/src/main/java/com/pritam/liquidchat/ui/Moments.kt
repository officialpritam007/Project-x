package com.pritam.liquidchat.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pritam.liquidchat.AppViewModel
import com.pritam.liquidchat.data.Images
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

@Immutable data class Moment(
    val id:String,val owner:String,val media:String,val name:String,val caption:String,
    val created:String,val expires:String?,val width:Int,val height:Int,
    val mediaKind:String="image",val durationMs:Long=0L
)
/** Shared cache key for the same participant-scoped RPC across Home and the Story/Post sheet. */
internal fun momentsCacheKey(kind:String,mine:Boolean,archived:Boolean=false):String =
    "moments:$kind:${if(mine)"mine" else "contacts"}:${if(archived)"archived" else "active"}"

internal fun unexpiredMoments(
    kind:String,items:List<Moment>,now:java.time.Instant=java.time.Instant.now()
):List<Moment> {
    if(kind!="story")return items
    return items.filter {moment->
        moment.expires==null||runCatching {
            java.time.Instant.parse(moment.expires).isAfter(now)
        }.getOrDefault(false)
    }
}

/** The next cursor belongs to the server page, even when some stories expire locally. */
internal data class MomentPage(val items:List<Moment>,val cursor:Moment?,val more:Boolean)
internal fun momentPage(kind:String,raw:List<Moment>,now:java.time.Instant=java.time.Instant.now()) =
    MomentPage(unexpiredMoments(kind,raw,now),raw.lastOrNull(),raw.size>=30)

internal fun parseMoments(array:JSONArray):List<Moment> =(0 until array.length()).map {index->
    val j=array.getJSONObject(index)
    Moment(j.getString("id"),j.getString("owner_id"),j.getString("media_id"),
        j.getString("display_name"),j.getString("caption"),j.getString("created_at"),
        if(j.isNull("expires_at"))null else j.getString("expires_at"),j.getInt("width"),j.getInt("height"),
        j.optString("media_kind","image"),j.optLong("duration_ms",0L))
}

/**
 * Option C: compact, horizontal story cards on the existing Chats tab.
 * This is a client of the existing participant-scoped list_moments RPC; no public feed.
 * Selecting a story opens the original authenticated story flow.
 */
@Composable fun HomeStoriesRow(
    model:AppViewModel,onMine:()->Unit,onStories:()->Unit,
    onOpenStory:(id:String,mine:Boolean)->Unit,
    refreshKey:Int=0
) {
    val session by model.session.collectAsStateWithLifecycle()
    val foreground by model.repository.foreground.collectAsStateWithLifecycle()
    val sessionEpoch by model.backend.uiReads.sessionEpoch.collectAsStateWithLifecycle()
    val revision by model.backend.uiReads.revision.collectAsStateWithLifecycle()
    val uid=session?.userId
    val feedKey=remember {momentsCacheKey("story",false)}
    val version=remember(uid,revision) {model.backend.uiReads.version(uid,feedKey)}
    var stories by remember(uid,sessionEpoch) {
        mutableStateOf(
            model.backend.uiReads.peek(uid,feedKey,allowStale=true)
                ?.let {runCatching {unexpiredMoments("story",parseMoments(JSONArray(it)))}.getOrNull()}
                ?: emptyList()
        )
    }
    var lastRefreshKey by remember(uid,sessionEpoch) {mutableIntStateOf(refreshKey)}
    LaunchedEffect(uid,sessionEpoch,version,foreground,refreshKey) {
        if(uid.isNullOrBlank()||!foreground)return@LaunchedEffect
        val force=lastRefreshKey!=refreshKey
        lastRefreshKey=refreshKey
        try {
            val result=model.backend.uiReads.load(uid,feedKey,20_000L,force=force) {
                val body=JSONObject().put("p_kind","story").put("p_mine",false)
                    .put("p_archived",false)
                model.backend.rpc("list_moments",body)
            }
            if(model.backend.session.value?.userId==uid&&model.backend.uiReads.epoch(uid)==sessionEpoch)
                stories=unexpiredMoments("story",parseMoments(JSONArray(result)))
        } catch(e:CancellationException){throw e}
        catch(_:Exception){/* Keep the last rendered cards available offline. */}
    }
    LaunchedEffect(uid,sessionEpoch,foreground) {
        if(foreground)while(true) {
            delay(1000)
            stories=unexpiredMoments("story",stories)
        }
    }
    val selfId=session?.userId
    val myMoment=stories.firstOrNull {it.owner==selfId}
    val contactMoments=remember(stories,selfId) {
        stories.filter {it.owner!=selfId}.distinctBy {it.owner}.take(12)
    }
    Column(verticalArrangement=Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal=6.dp),verticalAlignment=Alignment.CenterVertically) {
            Text("Stories",Modifier.weight(1f),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
            TextButton(onClick=onStories) {Text("See all")}
        }
        LazyRow(
            horizontalArrangement=Arrangement.spacedBy(10.dp),
            contentPadding=PaddingValues(horizontal=6.dp)
        ) {
            item(key="my-story") {
                StoryCard(
                    title="My Story",moment=myMoment,model=model,
                    mine=true,onClick={
                        if(myMoment!=null)onOpenStory(myMoment.id,true) else onMine()
                    },onAdd=onMine
                )
            }
            items(contactMoments,key={it.owner}) {moment->
                StoryCard(title=moment.name,moment=moment,model=model,mine=false,
                    onClick={onOpenStory(moment.id,false)})
            }
            if(contactMoments.isEmpty())item(key="stories-shortcut") {
                StoryCard(title="View Stories",moment=null,model=model,mine=false,onClick=onStories)
            }
        }
    }
}
@Composable private fun StoryCard(
    title:String,moment:Moment?,model:AppViewModel,mine:Boolean,
    onClick:()->Unit,onAdd:(()->Unit)?=null
) {
    Column(
        Modifier.width(94.dp).clickable(onClick=onClick),
        horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=Arrangement.spacedBy(6.dp)
    ) {
        GlassSurface(Modifier.size(width=94.dp,height=116.dp),Glass.card) {
            Box(Modifier.fillMaxSize().clip(Glass.card),contentAlignment=Alignment.Center) {
                if(moment?.mediaKind=="story_video")
                    Icon(Icons.Outlined.Videocam,"Video story",modifier=Modifier.size(32.dp),tint=MaterialTheme.colorScheme.primary)
                else if(moment!=null)
                    ProtectedImage(model.backend,moment.media,description="$title story",
                        modifier=Modifier.fillMaxSize(),scale=ContentScale.Crop)
                else
                    Icon(if(mine)Icons.Outlined.AddCircleOutline else Icons.Outlined.AutoStories,
                        null,modifier=Modifier.size(32.dp),tint=MaterialTheme.colorScheme.primary)
                if(mine)
                    Surface(
                        modifier=Modifier.align(Alignment.BottomEnd).padding(5.dp)
                            .clickable {onAdd?.invoke()},
                        shape=RoundedCornerShape(50),color=MaterialTheme.colorScheme.primary
                    ) {Icon(Icons.Outlined.Add,"Add a story",Modifier.padding(3.dp),tint=MaterialTheme.colorScheme.onPrimary)}
            }
        }
        Text(title,style=MaterialTheme.typography.labelSmall,maxLines=1,color=MaterialTheme.colorScheme.onSurface)
    }
}

/** No public feed: audiences are the creator's saved contacts, rechecked on every media request. */
@Composable fun MomentsPanel(
    model:AppViewModel,kind:String,mine:Boolean,archived:Boolean=false,
    modifier:Modifier=Modifier,bottomPadding:Dp=16.dp,
    header:(@Composable ()->Unit)?=null, initialStoryId:String?=null,
    openEditorInitially:Boolean=false
) {
    val account by model.session.collectAsStateWithLifecycle()
    val foreground by model.repository.foreground.collectAsStateWithLifecycle()
    val sessionEpoch by model.backend.uiReads.sessionEpoch.collectAsStateWithLifecycle()
    val revision by model.backend.uiReads.revision.collectAsStateWithLifecycle()
    val scope=rememberCoroutineScope()
    val uid=account?.userId
    val cacheKey=remember(kind,mine,archived) {momentsCacheKey(kind,mine,archived)}
    val version=remember(uid,cacheKey,revision) {model.backend.uiReads.version(uid,cacheKey)}
    val cachedPage=remember(uid,sessionEpoch,cacheKey) {
        model.backend.uiReads.peek(uid,cacheKey,allowStale=true)
            ?.let {runCatching {momentPage(kind,parseMoments(JSONArray(it)))}.getOrNull()}
    }
    var items by remember(uid,sessionEpoch,cacheKey){mutableStateOf(cachedPage?.items ?: emptyList())}
    var firstPage by remember(uid,sessionEpoch,cacheKey){mutableStateOf(cachedPage?.items)}
    var appliedVersion by remember(uid,sessionEpoch,cacheKey){mutableStateOf(version)}
    var cursor by remember(uid,sessionEpoch,cacheKey){mutableStateOf(cachedPage?.cursor)}
    var loadedOnce by remember(uid,sessionEpoch,cacheKey){mutableStateOf(cachedPage!=null||uid==null)}
    var loading by remember(uid,sessionEpoch,cacheKey){mutableStateOf(false)}
    var appending by remember(uid,sessionEpoch,cacheKey){mutableStateOf(false)}
    var mutating by remember(uid,sessionEpoch,cacheKey){mutableStateOf(false)}
    var error by remember(uid,sessionEpoch,cacheKey){mutableStateOf<String?>(null)}
    var retryAppend by remember(uid,sessionEpoch,cacheKey){mutableStateOf(false)}
    var retryChange by remember(uid,sessionEpoch,cacheKey){mutableStateOf<Pair<Moment,String>?>(null)}
    var more by remember(uid,sessionEpoch,cacheKey){mutableStateOf(cachedPage?.more ?: false)}
    val loadMutex=remember(uid,sessionEpoch,cacheKey) {Mutex()}
    var editor by remember(uid,sessionEpoch,openEditorInitially,kind) {mutableStateOf(openEditorInitially && kind=="story")}
    var focused by remember(uid,sessionEpoch,cacheKey) {mutableStateOf<Moment?>(null)}
    var pendingStoryId by remember(uid,sessionEpoch,initialStoryId) {mutableStateOf(initialStoryId)}
    var deleting by remember(uid,sessionEpoch,cacheKey) {mutableStateOf<Moment?>(null)}
    var reporting by remember(uid,sessionEpoch,cacheKey) {mutableStateOf<Moment?>(null)}
    fun currentAccount()=uid!=null&&model.backend.session.value?.userId==uid&&model.backend.uiReads.epoch(uid)==sessionEpoch
    suspend fun load(append:Boolean=false,force:Boolean=false)=loadMutex.withLock {
        if(!currentAccount()||(append&&(!more||cursor==null)))return@withLock
        val requestVersion=model.backend.uiReads.version(uid,cacheKey)
        loading=true;appending=append;error=null;retryChange=null
        try {
            val params=JSONObject().put("p_kind",kind).put("p_mine",mine)
                .put("p_archived",archived)
            if(append)cursor?.let {
                params.put("p_before",it.created).put("p_before_id",it.id)
            }
            val json=if(append)model.backend.rpc("list_moments",params)
                else model.backend.uiReads.load(uid!!,cacheKey,if(kind=="story")20_000L else 45_000L,force=force) {
                    model.backend.rpc("list_moments",params)
                }
            val page=momentPage(kind,parseMoments(JSONArray(json)))
            if(currentAccount()&&(!append||model.backend.uiReads.version(uid,cacheKey)==requestVersion)) {
                val unchanged=!append&&!force&&firstPage==page.items&&appliedVersion==requestVersion
                items=when {
                    append->unexpiredMoments(kind,(items+page.items).distinctBy {it.id})
                    unchanged->unexpiredMoments(kind,items)
                    else->page.items
                }
                if(!unchanged) {cursor=page.cursor;more=page.more}
                if(!append) {firstPage=page.items;appliedVersion=model.backend.uiReads.version(uid,cacheKey)}
                loadedOnce=true
                focused=focused?.let {open->items.firstOrNull {it.id==open.id}}
                if(kind=="story"&&pendingStoryId!=null) {
                    items.firstOrNull {it.id==pendingStoryId}?.let {
                        focused=it;pendingStoryId=null
                    }
                }
            }
        } catch(e:CancellationException){throw e}
        catch(_:Exception) {
            if(currentAccount()) {
                retryAppend=append
                error="Could not ${if(append)"load older" else "refresh"} ${if(kind=="story")"stories" else "posts"}. Check your connection and retry."
            }
        } finally {loading=false;appending=false}
    }
    suspend fun changeMoment(moment:Moment,action:String) {
        if(!currentAccount()||mutating)return
        mutating=true;error=null;retryChange=null
        try {
            model.backend.rpc("change_moment",JSONObject().put("p_id",moment.id).put("p_action",action))
            if(currentAccount()) {
                items=items.filterNot {it.id==moment.id}
                if(focused?.id==moment.id)focused=null
            }
        } catch(e:CancellationException){throw e}
        catch(_:Exception){if(currentAccount()){retryAppend=false;retryChange=moment to action;error="Could not update the ${if(kind=="story")"story" else "post"}. Retry."}}
        finally {mutating=false}
    }
    LaunchedEffect(cacheKey,uid,sessionEpoch,version,foreground) {
        if(foreground)load()
    }
    LaunchedEffect(pendingStoryId,items) {
        if(kind=="story"&&pendingStoryId!=null)items.firstOrNull {it.id==pendingStoryId}?.let {
            focused=it;pendingStoryId=null
        }
    }
    // Expiry is enforced server-side. Remove an expired tile while this screen stays open too.
    LaunchedEffect(uid,sessionEpoch,cacheKey,foreground) {if(foreground&&kind=="story")while(true){delay(1000);items=unexpiredMoments(kind,items);focused?.let {if(items.none {m->m.id==it.id})focused=null}}}
    val busy=loading||mutating
    LazyColumn(modifier=modifier.testTag("moments-list"),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=bottomPadding)) {
        header?.let { item(key="profile-header") {it()} }
        item(key="moment-controls") { Row(Modifier.fillMaxWidth()) {
            Text(if(kind=="story")"Stories · 24 hours" else if(archived)"Archived posts" else "Posts",Modifier.weight(1f),fontWeight=FontWeight.Bold)
            IconButton(onClick={scope.launch {load(force=true)}},enabled=!busy&&uid!=null){Icon(Icons.Outlined.Refresh,"Refresh ${if(kind=="story")"stories" else "posts"}")}
            if(mine&&!archived)IconButton(onClick={editor=true},enabled=account!=null&&!busy){Icon(Icons.Outlined.Add,if(kind=="story")"Add a story" else "Add a post")}
        }
        }
        if(loading&&!loadedOnce&&items.isEmpty())item {LinearProgressIndicator(Modifier.fillMaxWidth().testTag("moments-loading"))}
        error?.let {item {Column {Text(it,color=MaterialTheme.colorScheme.error);TextButton(onClick={scope.launch {
            val change=retryChange
            if(change!=null)changeMoment(change.first,change.second) else load(append=retryAppend,force=true)
        }},enabled=!busy){Text("Retry")}}}}
            if(items.isEmpty()&&loadedOnce&&!busy&&error==null&&!more)item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(if(kind=="story")"No stories yet" else if(archived)"No archived posts" else "No posts yet...",style=MaterialTheme.typography.titleLarge)
                    Text(if(mine)"Share a photo with people in your saved contacts." else "Stories and posts appear when their creators save you as a contact.")
                    if(mine&&!archived)TextButton(onClick={editor=true},enabled=uid!=null){Text(if(kind=="story")"Add a story" else "Add a post")}
                }
            }
            items(items,key={it.id}) {moment->
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(moment.name,fontWeight=FontWeight.Bold)
                    Text(remember(moment.created){runCatching{java.time.Instant.parse(moment.created).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("dd MMM · HH:mm"))}.getOrDefault("")},style=MaterialTheme.typography.labelSmall)
                    if(moment.mediaKind=="story_video") {
                        Box(Modifier.fillMaxWidth().height(190.dp).clickable {focused=moment},contentAlignment=Alignment.Center) {
                            Icon(Icons.Outlined.PlayCircle,"Play video story",modifier=Modifier.size(56.dp),tint=MaterialTheme.colorScheme.primary)
                            Text("Video story · ${(moment.durationMs/1000)}s",Modifier.align(Alignment.BottomCenter).padding(12.dp))
                        }
                    } else ProtectedImage(model.backend,moment.media,description="${moment.name}'s ${if(kind=="story")"story" else "post"}",modifier=Modifier.fillMaxWidth().heightIn(max=320.dp).aspectRatio((moment.width.toFloat()/moment.height.coerceAtLeast(1)).coerceIn(.6f,1.8f)).clickable {focused=moment},scale=ContentScale.Crop)
                    if(moment.caption.isNotBlank())Text(moment.caption)
                    if(moment.owner!=account?.userId)Row {
                        TextButton(onClick={reporting=moment},enabled=!busy){Text("Report ${if(kind=="story")"story" else "post"}")}
                        TextButton(onClick={model.action {model.repository.block(moment.owner,true)}},enabled=!busy){Text("Block user")}
                    }
                    if(moment.owner==account?.userId)Row {
                        if(kind=="post")TextButton(enabled=!busy,onClick={scope.launch {changeMoment(moment,if(archived)"restore" else "archive")}}){Text(if(archived)"Restore" else "Archive")}
                        TextButton(onClick={deleting=moment},enabled=!busy){Text("Delete")}
                    }
                }
            }
            if(more)item(key="moment-more") {TextButton(onClick={scope.launch {load(true)}},enabled=!busy){Text(if(appending)"Loading older…" else "Load older")}}
    }
    if(editor)key(uid,sessionEpoch,kind) {MomentEditor(model,kind,onDismiss={editor=false},onPublished={editor=false})}
    focused?.let {moment->
        if(kind=="story") {
            StoryViewer(model,items,moment.id,onDismiss={focused=null})
        } else {
            androidx.compose.ui.window.Dialog(onDismissRequest={focused=null},
                properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)) {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.systemBarsPadding()) {
                        TextButton(onClick={focused=null}){Text("Close")}
                        ProtectedImage(model.backend,moment.media,description="Full-size post",
                            modifier=Modifier.weight(1f).fillMaxWidth())
                        Text(moment.caption,Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
    reporting?.let {moment->ReportContentDialog("Report ${if(kind=="story")"story" else "post"}","Your explanation and this content's reference will be sent privately for review.",onSubmit={model.reportMoment(moment.id,it);reporting=null},onDismiss={reporting=null})}
    deleting?.let {moment->LiquidAlertDialog(onDismissRequest={deleting=null},title={Text("Delete this ${if(kind=="story")"story" else "post"}?")},text={Text("It will disappear for you and your contacts.")},confirmButton={TextButton(enabled=!busy,onClick={deleting=null;scope.launch {changeMoment(moment,"delete")}}){Text("Delete")}},dismissButton={TextButton(onClick={deleting=null}){Text("Cancel")}})}
}

/**
 * Real photo-story playback state. Progress advances only while the touch surface is released.
 * No viewer counts or viewed receipts are faked; those require a separate authorized backend.
 */
internal fun advancePhotoStory(progress:Float,elapsedMs:Long,paused:Boolean,ready:Boolean):Float =
    if(paused||!ready)progress else (progress+elapsedMs.coerceIn(0L,250L)/5000f).coerceAtMost(1f)

@Composable private fun StoryViewer(
    model:AppViewModel,stories:List<Moment>,initialId:String,onDismiss:()->Unit
) {
    if(stories.isEmpty())return
    var currentId by remember(initialId) {mutableStateOf(initialId)}
    val index=stories.indexOfFirst {it.id==currentId}.coerceAtLeast(0)
    val current=stories[index]
    var paused by remember(current.id) {mutableStateOf(false)}
    var muted by remember {mutableStateOf(true)}
    var progress by remember(current.id) {mutableFloatStateOf(0f)}
    var imageReady by remember(current.id) {mutableStateOf(false)}
    var replay by remember(current.id) {mutableIntStateOf(0)}
    val foreground by model.repository.foreground.collectAsStateWithLifecycle()
    val currentCall by model.calls.view.collectAsStateWithLifecycle()
    val effectivePause=paused||!foreground||currentCall!=null
    // Photos advance after five seconds; MP4 progress is driven by the actual player.
    LaunchedEffect(current.id,effectivePause,imageReady,stories.size,current.mediaKind) {
        if(current.mediaKind!="story_video") {
            var lastTick=android.os.SystemClock.elapsedRealtime()
            while(progress<1f) {
                delay(50)
                val now=android.os.SystemClock.elapsedRealtime()
                progress=advancePhotoStory(progress,now-lastTick,effectivePause,imageReady)
                lastTick=now
            }
            if(!effectivePause) {
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
                        onFinished={if(index<stories.lastIndex)currentId=stories[index+1].id else onDismiss()},
                        modifier=Modifier.fillMaxSize()
                    )
                } else ProtectedImage(model.backend,current.media,
                    description="${current.name}'s story",modifier=Modifier.fillMaxSize(),
                    scale=ContentScale.Fit,onLoadState={imageReady=it})
                Box(Modifier.fillMaxSize().pointerInput(current.id,stories.size) {
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
                        IconButton(onClick=onDismiss) {Icon(Icons.Outlined.Close,"Close story",tint=Color.White)}
                    }
                }
                if(current.caption.isNotBlank())
                    Text(current.caption,Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                        .padding(18.dp),color=Color.White)
            }
        }
    }
}

@Composable fun MomentsDialog(
    model:AppViewModel,mine:Boolean,onDismiss:()->Unit,
    initialStoryId:String?=null,openEditorInitially:Boolean=false
) {
    var tab by rememberSaveable {mutableIntStateOf(0)}
    androidx.compose.ui.window.Dialog(onDismissRequest=onDismiss,properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().systemBarsPadding().padding(12.dp)) {
                Row {TextButton(onClick=onDismiss){Text("Back")};Text(if(mine)"My stories" else "Contacts",style=MaterialTheme.typography.titleLarge,modifier=Modifier.padding(12.dp))}
                Row {listOf("Stories","Posts").forEachIndexed {i,label->FilterChip(tab==i,{tab=i},label={Text(label)})}}
                MomentsPanel(model,if(tab==0)"story" else "post",mine,
                    modifier=Modifier.weight(1f),initialStoryId=initialStoryId,
                    openEditorInitially=openEditorInitially&&mine&&tab==0)
            }
        }
    }
}

@Composable private fun MomentEditor(model:AppViewModel,kind:String,onDismiss:()->Unit,onPublished:()->Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val session by model.session.collectAsStateWithLifecycle()
    val owner=session?.userId.orEmpty()
    var publishId by remember {mutableStateOf(UUID.randomUUID().toString())}
    var photo by remember {mutableStateOf<Images.Prepared?>(null)}
    var video by remember {mutableStateOf<android.net.Uri?>(null)}
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
            Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("Visible only to your saved contacts. Blocked contacts cannot view it.",
                    style=MaterialTheme.typography.bodySmall)
                if(kind=="story")Text("Stories disappear after 24 hours.",
                    style=MaterialTheme.typography.labelMedium)
                photo?.let {selected->
                    coil.compose.AsyncImage(selected.file,"Photo preview",
                        Modifier.fillMaxWidth().heightIn(max=200.dp)
                            .aspectRatio(selected.width.toFloat()/selected.height.coerceAtLeast(1)))
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
            TextButton(enabled=(photo!=null||video!=null)&&!busy&&owner.isNotEmpty(),onClick={
                val selectedPhoto=photo
                val selectedVideo=video
                if(selectedPhoto==null&&selectedVideo==null)return@TextButton
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
                            } else {
                                stage="Uploading";progress=0
                                model.backend.uploadImage(selectedPhoto!!.file,null,onProgress={value->scope.launch {progress=value}}).getString("id")
                            }
                        }
                        stage="Publishing"
                        model.backend.rpc("publish_moment",JSONObject()
                            .put("p_id",publishId).put("p_media",media)
                            .put("p_kind",kind).put("p_caption",caption.trim()))
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
