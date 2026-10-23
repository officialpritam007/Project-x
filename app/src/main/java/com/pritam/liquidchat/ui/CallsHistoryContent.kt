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

/** Pure UI for the call-history list, independent of RPC and permission launchers. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun CallsHistoryContent(
    model:AppViewModel,chats:List<Chat>,account:String?,callInProgress:Boolean,
    filter:String,onFilter:(String)->Unit,visible:List<CallHistoryRow>,rowsEmpty:Boolean,
    loading:Boolean,loadedOnce:Boolean,appending:Boolean,more:Boolean,
    error:String?,permissionError:String?,
    onDismissPermissionError:()->Unit,onRetry:()->Unit,onRefresh:()->Unit,
    onLoadOlder:()->Unit,onDetails:(CallHistoryRow)->Unit,onRedial:(CallHistoryRow)->Unit
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal=12.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)
    ) {
        GlassHeader("Calls") {
            IconButton(onClick=onRefresh,enabled=!loading&&account!=null) {
                Icon(Icons.Outlined.Refresh,"Refresh call history")
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("All","Missed","Voice","Video").forEach {choice->
                MbReferenceFilterTab(label=choice,selected=filter==choice,onClick={onFilter(choice)})
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
                TextButton(onClick=onDismissPermissionError) {Text("Dismiss")}
            }
        }
        if(error!=null) GlassCard(Modifier.fillMaxWidth()) {
            Text(error.orEmpty(),color=MaterialTheme.colorScheme.error)
            TextButton(onClick=onRetry,enabled=!loading) {Text("Retry")}
        }
        LazyColumn(
            modifier=Modifier.fillMaxSize(),
            verticalArrangement=Arrangement.spacedBy(
                if(referenceListUsesFlatRows(LocalDisplayOptions.current))0.dp else 8.dp),
            contentPadding=PaddingValues(bottom=120.dp+WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
        ) {
            if(loading&&!loadedOnce&&rowsEmpty)item(key="loading") {LinearProgressIndicator(Modifier.fillMaxWidth().testTag("call-history-loading"))}
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
                        Modifier.fillMaxWidth().heightIn(
                            min=if(referenceListUsesFlatRows(LocalDisplayOptions.current))60.dp else 70.dp)
                            .clickable {onDetails(call)}
                            .padding(horizontal=12.dp,
                                vertical=if(referenceListUsesFlatRows(LocalDisplayOptions.current))6.dp else 10.dp),
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
                        IconButton(onClick={onRedial(call)},
                            enabled=callHistoryCanRedial(com.pritam.liquidchat.BuildConfig.ENABLE_EXPERIMENTAL_CALLS,
                                call.kind,account!=null,callInProgress)) {
                            Icon(if(call.kind=="video")Icons.Outlined.Videocam else Icons.Outlined.Call,
                                if(call.kind=="video")"Video call again" else "Voice call again",
                                tint=MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            if(more)item(key="more") {
                TextButton(onClick=onLoadOlder,enabled=!loading,modifier=Modifier.fillMaxWidth()) {
                    Text(if(appending)"Loading older calls…" else "Load older calls")
                }
            }
        }
    }
}
