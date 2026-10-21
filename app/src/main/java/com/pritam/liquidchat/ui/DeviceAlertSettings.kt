package com.pritam.liquidchat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pritam.liquidchat.data.DeviceAlertPreferences

/** These switches silence sound/vibration; they never discard message pushes. */
@Composable internal fun DeviceAlertSettings(account:String?) {
    val context=LocalContext.current
    var dnd by remember(account) {
        mutableStateOf(account?.let {
            DeviceAlertPreferences.quietDuringDnd(context,it)
        } ?: false)
    }
    var landscape by remember(account) {
        mutableStateOf(account?.let {
            DeviceAlertPreferences.quietDuringLandscape(context,it)
        } ?: false)
    }
    var error by remember(account) {mutableStateOf<String?>(null)}
    GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
        Text("Alert behavior",style=MaterialTheme.typography.titleMedium)
        Text("Control message notification sound and vibration on this phone. Incoming call alerts keep their normal Android channel behavior.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Quiet during Do Not Disturb")
                Text("Keep message notifications visible without a new sound.",
                    style=MaterialTheme.typography.bodySmall)
            }
            Switch(checked=dnd,enabled=account!=null,onCheckedChange={enabled->
                val owner=account
                if(owner!=null)runCatching {
                    DeviceAlertPreferences.update(context,owner,dnd=enabled)
                }.onSuccess {dnd=enabled;error=null}
                    .onFailure {error=it.message ?: "Unable to save"}
            })
        }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Quiet in landscape")
                Text("Prevent a message alert sound while viewing videos.",
                    style=MaterialTheme.typography.bodySmall)
            }
            Switch(checked=landscape,enabled=account!=null,onCheckedChange={enabled->
                val owner=account
                if(owner!=null)runCatching {
                    DeviceAlertPreferences.update(context,owner,landscape=enabled)
                }.onSuccess {landscape=enabled;error=null}
                    .onFailure {error=it.message ?: "Unable to save"}
            })
        }
        error?.let {Text(it,color=MaterialTheme.colorScheme.error,
            style=MaterialTheme.typography.bodySmall)}
        Text("Android's notification-channel settings, device DND policy and call controls remain authoritative.",
            style=MaterialTheme.typography.labelSmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
