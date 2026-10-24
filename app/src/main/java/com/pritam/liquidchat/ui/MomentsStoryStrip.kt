package com.pritam.liquidchat.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
            Column(Modifier.weight(1f)) {
                Text("Stories",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
                Text("24-hour moments from your contacts",
                    style=MaterialTheme.typography.labelSmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
    val reference=referenceListUsesFlatRows(LocalDisplayOptions.current)
    val shape=if(reference)androidx.compose.foundation.shape.CircleShape else Glass.card
    val cardWidth=if(reference)82.dp else 94.dp
    Column(
        Modifier.width(cardWidth)
            .semantics { contentDescription=if(moment!=null) "$title story" else if(mine) "Add your story" else "View stories" }
            .clickable(role=Role.Button,onClick=onClick),
        horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=Arrangement.spacedBy(6.dp)
    ) {
        // A ring indicates an available story, not an invented seen/unseen state.
        // Every tile still resolves only through the existing participant-scoped media viewer.
        Surface(
            modifier=if(reference)Modifier.size(82.dp) else Modifier.size(94.dp,116.dp),
            shape=shape,
            border=if(reference&&moment!=null)BorderStroke(
                2.dp,MaterialTheme.colorScheme.primary) else null,
            color=MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation=if(reference)0.dp else 1.dp
        ) {
            Box(Modifier.fillMaxSize().clip(shape),contentAlignment=Alignment.Center) {
                if(moment?.mediaKind=="story_video")
                    Icon(Icons.Outlined.Videocam,"Video story",
                        modifier=Modifier.size(32.dp),tint=MaterialTheme.colorScheme.primary)
                else if(moment!=null)
                    ProtectedImage(model.backend,moment.media,description="$title story",
                        modifier=Modifier.fillMaxSize(),scale=ContentScale.Crop)
                else
                    Icon(if(mine)Icons.Outlined.AddCircleOutline else Icons.Outlined.AutoStories,
                        null,modifier=Modifier.size(32.dp),tint=MaterialTheme.colorScheme.primary)
                if(mine)
                    Surface(
                        modifier=Modifier.align(Alignment.BottomEnd).padding(3.dp)
                            .clickable(role=Role.Button,onClick={onAdd?.invoke()}),
                        shape=RoundedCornerShape(50),color=MaterialTheme.colorScheme.primary
                    ) {Icon(Icons.Outlined.Add,"Add a story",
                        Modifier.padding(3.dp),tint=MaterialTheme.colorScheme.onPrimary)}
            }
        }
        Text(title,style=MaterialTheme.typography.labelSmall,maxLines=1,
            color=MaterialTheme.colorScheme.onSurface)
    }
}

