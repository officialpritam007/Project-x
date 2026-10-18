package com.pritam.liquidchat.ui

import android.os.SystemClock
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CallEnd
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pritam.liquidchat.calls.CallController
import com.pritam.liquidchat.calls.CallView
import kotlinx.coroutines.delay

/**
 * A single, in-layout call pill in the Scaffold topBar slot. Scaffold reserves its height
 * before laying out chat headers, lists, and the keyboard-safe message composer.
 */
@Composable fun MinimizedCallPill(controller:CallController,call:CallView,name:String?) {
    var seconds by remember(call.id,call.connectedAt) {mutableLongStateOf(0L)}
    LaunchedEffect(call.connectedAt) {
        while(call.connectedAt>0L) {
            seconds=((SystemClock.elapsedRealtime()-call.connectedAt)/1000L).coerceAtLeast(0)
            delay(1000L)
        }
    }
    val subtitle=if(call.connectedAt>0) "%02d:%02d".format(seconds/60,seconds%60) else call.status
    GlassSurface(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal=12.dp,vertical=4.dp),Glass.capsule) {
        Row(
            Modifier.fillMaxWidth().heightIn(min=52.dp).clickable(onClick=controller::expand).padding(horizontal=12.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(10.dp)
        ) {
            Icon(if(call.video)Icons.Outlined.Videocam else Icons.Outlined.Call,null,tint=MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(name ?: "Liquid Chat",style=MaterialTheme.typography.titleSmall,maxLines=1)
                Text("${if(call.video)"Video" else "Voice"} · $subtitle",
                    style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick=controller::expand) {
                Icon(Icons.Outlined.OpenInFull,"Expand call",tint=MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick=controller::hangup) {
                Icon(Icons.Outlined.CallEnd,"End call",tint=MaterialTheme.colorScheme.error)
            }
        }
    }
}
