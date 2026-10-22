package com.pritam.liquidchat.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pritam.liquidchat.AppViewModel
import com.pritam.liquidchat.data.Chat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Call summaries are loaded only from the existing authenticated, participant-scoped RPC. */
internal data class CallHistoryRow(
    val id:String,
    val conversation:String,
    val peer:String,
    val name:String,
    val avatar:String,
    val kind:String,
    val outcome:String,
    val created:String,
    val answered:String?,
    val ended:String?,
    val outgoing:Boolean
) {
    val missed get()=outcome=="missed" && !outgoing
    val durationSeconds:Long? get() {
        val start=answered?.let {runCatching {Instant.parse(it)}.getOrNull()} ?: return null
        val end=ended?.let {runCatching {Instant.parse(it)}.getOrNull()} ?: return null
        return Duration.between(start,end).seconds.takeIf {it>=0}
    }
    val direction:String get()=when {
        missed->"Missed"
        outcome=="missed"->"No answer"
        outcome=="declined"->"Declined"
        outgoing->"Outgoing"
        else->"Incoming"
    }
    val timeLabel:String get()=runCatching {
        Instant.parse(created).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd MMM · h:mm a"))
    }.getOrDefault("")
    companion object {
        fun parse(data:JSONArray,account:String):List<CallHistoryRow> = (0 until data.length()).mapNotNull {i->
            runCatching {
                val j=data.getJSONObject(i)
                CallHistoryRow(
                    id=j.getString("id"),
                    conversation=j.getString("conversation_id"),
                    peer=j.getString("peer_id"),
                    name=j.optString("display_name").ifBlank {"Contact"},
                    avatar=j.optString("avatar_id").takeUnless {it=="null"}.orEmpty(),
                    kind=j.optString("kind","audio"),
                    outcome=j.optString("outcome",j.optString("state")),
                    created=j.getString("created_at"),
                    answered=j.optString("answered_at").takeUnless {it.isBlank()||it=="null"},
                    ended=j.optString("ended_at").takeUnless {it.isBlank()||it=="null"},
                    outgoing=j.getString("caller_id")==account
                )
            }.getOrNull()
        }
    }
}

/** Keep the server cursor/count separate from rows that cannot be rendered. */
internal data class CallHistoryPage(val rows:List<CallHistoryRow>,val before:String?,val more:Boolean)
internal fun callHistoryPage(data:JSONArray,account:String)=CallHistoryPage(
    CallHistoryRow.parse(data,account),
    data.optJSONObject(data.length()-1)?.optString("created_at")?.takeIf {it.isNotBlank()},
    data.length()>=50
)

/** In-memory only; never modifies the server page cursor or filtered row history. */
internal fun filterCallHistory(rows:List<CallHistoryRow>,selection:String):List<CallHistoryRow> = when(selection) {
    "Missed"->rows.filter {it.missed}
    "Voice"->rows.filter {it.kind=="audio"}
    "Video"->rows.filter {it.kind=="video"}
    else->rows
}

/** Same runtime microphone/camera permissions as the conversation call buttons. */
internal fun callHistoryCanRedial(callsEnabled:Boolean,kind:String,hasAccount:Boolean,callInProgress:Boolean):Boolean =
    callsEnabled && hasAccount && !callInProgress && kind in listOf("audio","video")

internal fun callHistoryRequiredPermissions(kind:String):Array<String> =
    if(kind=="video") arrayOf(Manifest.permission.RECORD_AUDIO,Manifest.permission.CAMERA)
    else arrayOf(Manifest.permission.RECORD_AUDIO)

private fun elapsedLabel(seconds:Long):String {
    val hours=seconds/3600
    val minutes=(seconds%3600)/60
    return if(hours>0) "${hours}h ${minutes}m" else if(minutes>0) "${minutes}m ${seconds%60}s" else "${seconds}s"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CallsScreen(model:AppViewModel,chats:List<Chat>) {
    val session by model.session.collectAsStateWithLifecycle()
    val currentCall by model.calls.view.collectAsStateWithLifecycle()
    val foreground by model.repository.foreground.collectAsStateWithLifecycle()
    val sessionEpoch by model.backend.uiReads.sessionEpoch.collectAsStateWithLifecycle()
    val revision by model.backend.uiReads.revision.collectAsStateWithLifecycle()
    val scope=rememberCoroutineScope()
    val account=session?.userId
    val cacheKey="calls:recent"
    val version=remember(account,revision) {model.backend.uiReads.version(account,cacheKey)}
    val cachedPage=remember(account,sessionEpoch) {
        model.backend.uiReads.peek(account,cacheKey,allowStale=true)
            ?.let {runCatching {callHistoryPage(JSONArray(it),account.orEmpty())}.getOrNull()}
    }
    var rows by remember(account,sessionEpoch) {mutableStateOf(cachedPage?.rows ?: emptyList())}
    var firstPage by remember(account,sessionEpoch) {mutableStateOf(cachedPage?.rows)}
    var appliedVersion by remember(account,sessionEpoch) {mutableStateOf(version)}
    var before by remember(account,sessionEpoch) {mutableStateOf(cachedPage?.before)}
    var loadedOnce by remember(account,sessionEpoch) {
        mutableStateOf(cachedPage!=null||account==null)
    }
    var loading by remember(account,sessionEpoch) {mutableStateOf(false)}
    var appending by remember(account,sessionEpoch) {mutableStateOf(false)}
    var more by remember(account,sessionEpoch) {mutableStateOf(cachedPage?.more ?: false)}
    var error by remember(account,sessionEpoch) {mutableStateOf<String?>(null)}
    var retryAppend by remember(account,sessionEpoch) {mutableStateOf(false)}
    var filter by remember(account,sessionEpoch) {mutableStateOf("All")}
    var details by remember(account,sessionEpoch) {mutableStateOf<CallHistoryRow?>(null)}
    val loadMutex=remember(account,sessionEpoch) {Mutex()}
    val context=LocalContext.current
    var pendingRedial by remember(account,sessionEpoch) {mutableStateOf<CallHistoryRow?>(null)}
    var permissionError by remember(account,sessionEpoch) {mutableStateOf<String?>(null)}

    fun currentAccount()=account!=null&&model.backend.session.value?.userId==account&&model.backend.uiReads.epoch(account)==sessionEpoch
    val callPermissions=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {granted->
        val call=pendingRedial
        pendingRedial=null
        if(call!=null&&currentAccount()&&model.calls.view.value==null) {
            if(callHistoryRequiredPermissions(call.kind).all {granted[it]==true})
                model.calls.start(call.conversation,call.peer,call.kind)
            else permissionError=if(call.kind=="video")
                "Microphone and camera permissions are required for video calls."
                else "Microphone permission is required for voice calls."
        }
    }
    fun redial(call:CallHistoryRow) {
        if(!currentAccount() || !callHistoryCanRedial(
            com.pritam.liquidchat.BuildConfig.ENABLE_EXPERIMENTAL_CALLS,
            call.kind,account!=null,model.calls.view.value!=null))return
        permissionError=null
        val required=callHistoryRequiredPermissions(call.kind)
        if(required.all {ContextCompat.checkSelfPermission(context,it)==PackageManager.PERMISSION_GRANTED}) {
            model.calls.start(call.conversation,call.peer,call.kind)
        } else {
            pendingRedial=call
            callPermissions.launch(required)
        }
    }
    suspend fun refresh(append:Boolean=false,force:Boolean=false)=loadMutex.withLock {
        if(!currentAccount()||(append&&(!more||before==null)))return@withLock
        val requestVersion=model.backend.uiReads.version(account,cacheKey)
        loading=true;appending=append
        error=null
        try {
            val params=JSONObject()
            if(append)params.put("p_before",before)
            val result=if(append)model.backend.rpc("list_call_history",params)
                else model.backend.uiReads.load(account!!,cacheKey,60_000L,force=force) {
                    model.backend.rpc("list_call_history",params)
                }
            val page=callHistoryPage(JSONArray(result),account!!)
            if(currentAccount()&&(!append||model.backend.uiReads.version(account,cacheKey)==requestVersion)) {
                val unchanged=!append&&!force&&firstPage==page.rows&&appliedVersion==requestVersion
                rows=if(append)(rows+page.rows).distinctBy {it.id} else if(unchanged)rows else page.rows
                if(!unchanged) {before=page.before;more=page.more&&page.before!=null}
                if(!append) {firstPage=page.rows;appliedVersion=model.backend.uiReads.version(account,cacheKey)}
                loadedOnce=true
                details=details?.let {open->rows.firstOrNull {it.id==open.id}}
            }
        } catch(e:CancellationException) {throw e}
        catch(_:Exception) {if(currentAccount()) {retryAppend=append;error="Could not load ${if(append)"older calls" else "call history"}. Check your connection and retry."}}
        finally {loading=false;appending=false}
    }
    LaunchedEffect(account,sessionEpoch,version,foreground) {if(foreground)refresh()}
    var hadCall by remember(account,sessionEpoch) {mutableStateOf(false)}
    LaunchedEffect(account,sessionEpoch,currentCall!=null) {
        if(currentCall!=null)hadCall=true
        else if(hadCall&&currentAccount()) {
            hadCall=false
            // A peer can end an incoming call without this device issuing a mutation RPC.
            model.backend.uiReads.invalidate(account,cacheKey)
        }
    }
    val visible=remember(rows,filter) {filterCallHistory(rows,filter)}
    Column(
        Modifier.fillMaxSize().padding(horizontal=12.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)
    ) {
        GlassHeader("Calls") {
            IconButton(onClick={scope.launch {refresh(force=true)}},enabled=!loading&&account!=null) {
                Icon(Icons.Outlined.Refresh,"Refresh call history")
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("All","Missed","Voice","Video").forEach {choice->
                FilterChip(selected=filter==choice,onClick={filter=choice},label={Text(choice)},shape=Glass.capsule)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=8.dp),
            verticalAlignment=Alignment.CenterVertically) {
            Text(when(filter){"Missed"->"Missed calls";"Voice"->"Voice calls";"Video"->"Video calls";else->"Recent calls"},
                Modifier.weight(1f),style=MaterialTheme.typography.titleMedium,
                fontWeight=FontWeight.SemiBold)
            Text("${visible.size} loaded",style=MaterialTheme.typography.labelMedium,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if(permissionError!=null) GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text(permissionError.orEmpty(),Modifier.weight(1f),color=MaterialTheme.colorScheme.error)
                TextButton(onClick={permissionError=null}) {Text("Dismiss")}
            }
        }
        if(error!=null) GlassCard(Modifier.fillMaxWidth()) {
            Text(error.orEmpty(),color=MaterialTheme.colorScheme.error)
            TextButton(onClick={scope.launch {refresh(append=retryAppend,force=true)}},enabled=!loading) {Text("Retry")}
        }
        LazyColumn(
            modifier=Modifier.fillMaxSize(),
            verticalArrangement=Arrangement.spacedBy(
                if(referenceListUsesFlatRows(LocalDisplayOptions.current))0.dp else 8.dp),
            contentPadding=PaddingValues(bottom=120.dp+WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
        ) {
            if(loading&&!loadedOnce&&rows.isEmpty())item(key="loading") {LinearProgressIndicator(Modifier.fillMaxWidth().testTag("call-history-loading"))}
            if(loadedOnce&&!loading&&error==null&&visible.isEmpty()&&!more) item(key="empty") {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(when(filter) {"Missed"->"No missed calls";"Voice"->"No voice calls";"Video"->"No video calls";else->"No recent calls"},style=MaterialTheme.typography.titleMedium)
                    Text("Your voice and video call history will appear here.",color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(visible,key={it.id}) {call->
                val peerChat=chats.firstOrNull {it.account==account&&it.id==call.conversation}
                ReferenceListRow(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min=70.dp).clickable {details=call}.padding(horizontal=12.dp,vertical=10.dp),
                        verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(12.dp)
                    ) {
                        Avatar(peerChat?.name ?: call.name,peerChat?.avatarId ?: call.avatar,model.backend,size=46.dp)
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                            Text(peerChat?.name ?: call.name,fontWeight=FontWeight.SemiBold,
                                color=if(call.missed)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                                Icon(if(call.missed)Icons.Outlined.CallMissed else if(call.outgoing)Icons.Outlined.CallMade else Icons.Outlined.CallReceived,
                                    null,Modifier.size(15.dp),tint=if(call.missed)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                                Text("${call.direction} · ${if(call.kind=="video")"Video" else "Voice"}",
                                    style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(call.timeLabel,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick={redial(call)},
                            enabled=callHistoryCanRedial(com.pritam.liquidchat.BuildConfig.ENABLE_EXPERIMENTAL_CALLS,
                                call.kind,account!=null,currentCall!=null)) {
                            Icon(if(call.kind=="video")Icons.Outlined.Videocam else Icons.Outlined.Call,
                                if(call.kind=="video")"Video call again" else "Voice call again",
                                tint=MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            if(more)item(key="more") {
                TextButton(onClick={scope.launch {refresh(append=true)}},enabled=!loading,modifier=Modifier.fillMaxWidth()) {
                    Text(if(appending)"Loading older calls…" else "Load older calls")
                }
            }
        }
    }
    details?.let {call->
        ModalBottomSheet(onDismissRequest={details=null}) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal=24.dp).padding(bottom=24.dp),
                verticalArrangement=Arrangement.spacedBy(12.dp)
            ) {
                Text(call.name,style=MaterialTheme.typography.headlineSmall)
                Text("${call.direction} ${if(call.kind=="video")"video" else "voice"} call")
                Text(call.timeLabel,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Duration: ${call.durationSeconds?.let(::elapsedLabel) ?: "Not connected"}")
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick={details=null;model.open(call.conversation)}) {
                        Icon(Icons.Outlined.ChatBubbleOutline,null)
                        Spacer(Modifier.width(8.dp))
                        Text("Message")
                    }
                    Button(onClick={details=null;redial(call)},
                        enabled=callHistoryCanRedial(com.pritam.liquidchat.BuildConfig.ENABLE_EXPERIMENTAL_CALLS,
                            call.kind,account!=null,currentCall!=null)) {
                        Icon(if(call.kind=="video")Icons.Outlined.Videocam else Icons.Outlined.Call,null)
                        Spacer(Modifier.width(8.dp))
                        Text("Call again")
                    }
                }
            }
        }
    }
}
