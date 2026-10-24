package com.pritam.liquidchat.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
    val clipboard=LocalClipboardManager.current
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
    val visibleItems=remember(items,mine,uid) {momentsForAudience(items,mine,uid)}
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
            if(visibleItems.isEmpty()&&loadedOnce&&!busy&&error==null&&!more)item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(if(kind=="story")"No stories yet" else if(archived)"No archived posts" else "No posts yet",style=MaterialTheme.typography.titleLarge)
                    Text(if(mine)"Share a photo with people in your saved contacts." else "Stories and posts appear when their creators save you as a contact.")
                    if(mine&&!archived)TextButton(onClick={editor=true},enabled=uid!=null){Text(if(kind=="story")"Add a story" else "Add a post")}
                }
            }
            items(visibleItems,key={it.id}) {moment->
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(moment.name,fontWeight=FontWeight.Bold)
                    Text(remember(moment.created){runCatching{java.time.Instant.parse(moment.created).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("dd MMM · HH:mm"))}.getOrDefault("")},style=MaterialTheme.typography.labelSmall)
                    if(moment.mediaKind=="story_video") {
                        Box(Modifier.fillMaxWidth().height(190.dp)
                            .clip(Glass.compact)
                            .clickable {focused=moment},contentAlignment=Alignment.Center) {
                            ProtectedVideoThumbnail(model.backend,moment.media,
                                description="${moment.name}'s video story poster",
                                modifier=Modifier.fillMaxSize())
                            Icon(Icons.Outlined.PlayCircle,"Play video story",
                                modifier=Modifier.size(56.dp),tint=Color.White)
                            Text("Video story · ${moment.durationMs/1000}s",
                                Modifier.align(Alignment.BottomCenter).padding(12.dp),
                                color=Color.White)
                        }
                    } else ProtectedImage(model.backend,moment.media,description="${moment.name}'s ${if(kind=="story")"story" else "post"}",modifier=Modifier.fillMaxWidth().heightIn(max=320.dp).aspectRatio((moment.width.toFloat()/moment.height.coerceAtLeast(1)).coerceIn(.6f,1.8f)).clickable {focused=moment},scale=ContentScale.Crop)
                    if(moment.caption.isNotBlank()) {
                        Text(moment.caption)
                        TextButton(enabled=currentAccount(),onClick={
                            if(currentAccount())clipboard.setText(AnnotatedString(moment.caption))
                        }) {Text("Copy caption")}
                    }
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
        if(kind=="story" && visibleItems.any {it.id==moment.id}) {
            StoryViewer(model,visibleItems,moment.id,onDismiss={focused=null})
        } else {
            androidx.compose.ui.window.Dialog(onDismissRequest={focused=null},
                properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)) {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.systemBarsPadding()) {
                        TextButton(onClick={focused=null}){Text("Close")}
                        ProtectedImage(model.backend,moment.media,description="Full-size post",
                            modifier=Modifier.weight(1f).fillMaxWidth())
                        if(moment.caption.isNotBlank()) {
                            Row(Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment=Alignment.CenterVertically) {
                                Text(moment.caption,Modifier.weight(1f))
                                TextButton(onClick={
                                    if(currentAccount())clipboard.setText(
                                        AnnotatedString(moment.caption))
                                },enabled=currentAccount()) {Text("Copy")}
                            }
                        }
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
@Composable fun MomentsDialog(
    model:AppViewModel,mine:Boolean,onDismiss:()->Unit,
    initialStoryId:String?=null,openEditorInitially:Boolean=false
) {
    var tab by rememberSaveable {mutableIntStateOf(0)}
    androidx.compose.ui.window.Dialog(onDismissRequest=onDismiss,properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().systemBarsPadding().padding(12.dp)) {
                Row {TextButton(onClick=onDismiss){Text("Back")};Text(if(mine)"My stories" else "Contacts",style=MaterialTheme.typography.titleLarge,modifier=Modifier.padding(12.dp))}
                Row {listOf("Stories","Posts").forEachIndexed {i,label->
                    MbReferenceFilterTab(label=label,selected=tab==i,onClick={tab=i})
                }}
                MomentsPanel(model,if(tab==0)"story" else "post",mine,
                    modifier=Modifier.weight(1f),initialStoryId=initialStoryId,
                    openEditorInitially=openEditorInitially&&mine&&tab==0)
            }
        }
    }
}

