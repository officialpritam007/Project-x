package com.pritam.liquidchat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pritam.liquidchat.AppViewModel
import com.pritam.liquidchat.data.Chat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
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

private fun elapsedLabel(seconds:Long):String {
    val hours=seconds/3600
    val minutes=(seconds%3600)/60
    return if(hours>0) "${hours}h ${minutes}m" else if(minutes>0) "${minutes}m ${seconds%60}s" else "${seconds}s"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CallsScreen(model:AppViewModel,chats:List<Chat>) {
    val session by model.session.collectAsStateWithLifecycle()
    val currentCall by model.calls.view.collectAsStateWithLifecycle()
    val scope=rememberCoroutineScope()
    val account=session?.userId
    var rows by remember(account) {mutableStateOf(emptyList<CallHistoryRow>())}
    var loading by remember(account) {mutableStateOf(false)}
    var more by remember(account) {mutableStateOf(false)}
    var error by remember(account) {mutableStateOf<String?>(null)}
    var filter by remember(account) {mutableStateOf("All")}
    var details by remember {mutableStateOf<CallHistoryRow?>(null)}

    suspend fun refresh(append:Boolean=false) {
        if(loading||account==null)return
        loading=true
        error=null
        try {
            val params=JSONObject()
            if(append)rows.lastOrNull()?.let {params.put("p_before",it.created)}
            val page=CallHistoryRow.parse(JSONArray(model.backend.rpc("list_call_history",params)),account)
            if(model.backend.session.value?.userId==account) {
                rows=if(append)(rows+page).distinctBy {it.id} else page
                more=page.size>=50
            }
        } catch(e:CancellationException) {throw e}
        catch(_:Exception) {error="Could not load call history. Check your connection and retry."}
        finally {loading=false}
    }
    LaunchedEffect(account) {if(account!=null)refresh()}
    val visible=remember(rows,filter) {if(filter=="Missed")rows.filter {it.missed} else rows}
    Column(
        Modifier.fillMaxSize().padding(horizontal=12.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)
    ) {
        GlassHeader("Calls") {
            IconButton(onClick={scope.launch {refresh()}},enabled=!loading) {
                Icon(Icons.Outlined.Refresh,"Refresh call history")
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("All","Missed").forEach {choice->
                FilterChip(selected=filter==choice,onClick={filter=choice},label={Text(choice)},shape=Glass.capsule)
            }
        }
        if(error!=null) GlassCard(Modifier.fillMaxWidth()) {
            Text(error.orEmpty(),color=MaterialTheme.colorScheme.error)
            TextButton(onClick={scope.launch {refresh()}},enabled=!loading) {Text("Retry")}
        }
        LazyColumn(
            modifier=Modifier.fillMaxSize(),
            verticalArrangement=Arrangement.spacedBy(8.dp),
            contentPadding=PaddingValues(bottom=120.dp+WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
        ) {
            if(loading&&rows.isEmpty())item(key="loading") {LinearProgressIndicator(Modifier.fillMaxWidth())}
            if(!loading&&error==null&&visible.isEmpty()) item(key="empty") {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(if(filter=="Missed")"No missed calls" else "No recent calls",style=MaterialTheme.typography.titleMedium)
                    Text("Your voice and video call history will appear here.",color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(visible,key={it.id}) {call->
                val peerChat=chats.firstOrNull {it.id==call.conversation}
                GlassSurface(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min=70.dp).clickable {details=call}.padding(horizontal=12.dp,vertical=10.dp),
                        verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(12.dp)
                    ) {
                        Avatar(call.name,call.avatar,model.backend,size=46.dp)
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
                        IconButton(onClick={model.calls.start(call.conversation,call.peer,call.kind)},
                            enabled=currentCall==null && account!=null) {
                            Icon(if(call.kind=="video")Icons.Outlined.Videocam else Icons.Outlined.Call,
                                if(call.kind=="video")"Video call again" else "Voice call again",
                                tint=MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            if(more && filter=="All")item(key="more") {
                TextButton(onClick={scope.launch {refresh(append=true)}},enabled=!loading,modifier=Modifier.fillMaxWidth()) {
                    Text(if(loading)"Loading…" else "Load older calls")
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
                    Button(onClick={details=null;model.calls.start(call.conversation,call.peer,call.kind)},enabled=currentCall==null) {
                        Icon(if(call.kind=="video")Icons.Outlined.Videocam else Icons.Outlined.Call,null)
                        Spacer(Modifier.width(8.dp))
                        Text("Call again")
                    }
                }
            }
        }
    }
}
