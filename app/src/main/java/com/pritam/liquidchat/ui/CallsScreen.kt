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
    CallsHistoryContent(
        model=model,chats=chats,account=account,callInProgress=currentCall!=null,
        filter=filter,onFilter={filter=it},visible=visible,rowsEmpty=rows.isEmpty(),
        loading=loading,loadedOnce=loadedOnce,appending=appending,more=more,
        error=error,permissionError=permissionError,
        onDismissPermissionError={permissionError=null},
        onRetry={scope.launch {refresh(append=retryAppend,force=true)}},
        onRefresh={scope.launch {refresh(force=true)}},
        onLoadOlder={scope.launch {refresh(append=true)}},
        onDetails={details=it},onRedial=::redial
    )
    details?.let {call->
        // Use the same reference bottom-sheet hierarchy and close affordance
        // as conversation actions, without changing live call permissions.
        LiquidActionSheet(title=call.name,onDismiss={details=null}) {
            Text("${call.direction} ${if(call.kind=="video")"video" else "voice"} call")
            Text(call.timeLabel,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Duration: ${call.durationSeconds?.let(::elapsedLabel) ?: "Not connected"}")
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick={details=null;model.open(call.conversation)},
                    modifier=Modifier.weight(1f)) {
                    Icon(Icons.Outlined.ChatBubbleOutline,null)
                    Spacer(Modifier.width(8.dp))
                    Text("Message")
                }
                Button(onClick={details=null;redial(call)},
                    modifier=Modifier.weight(1f),
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
