package com.pritam.liquidchat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pritam.liquidchat.AppViewModel

/** Honest read-only service diagnostics and a user-requested refresh.
 * No fake service switch: messaging, FCM, WebRTC and Supabase cannot be
 * disabled/restarted safely by pretending a settings preference controls them.
 */
@Composable internal fun MbServiceStatus(model:AppViewModel) {
    val session by model.session.collectAsStateWithLifecycle()
    val connection by model.connection.collectAsStateWithLifecycle()
    GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
        Text("Service status",style=MaterialTheme.typography.titleLarge)
        Text("Check the current connection without exposing credentials or private account details.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider()
        Text("Session",style=MaterialTheme.typography.titleSmall)
        Text(if(session?.userId.isNullOrBlank())"Signed out" else "Signed in",
            style=MaterialTheme.typography.bodyMedium)
        Text("Connection",style=MaterialTheme.typography.titleSmall)
        Text(connection,style=MaterialTheme.typography.bodyMedium)
        OutlinedButton(
            onClick={model.repository.refresh()},
            enabled=!session?.userId.isNullOrBlank(),
            modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)
        ) {
            Icon(Icons.Outlined.Refresh,contentDescription=null)
            Spacer(Modifier.width(8.dp))
            Text("Refresh conversations")
        }
        Text("Refreshing requests the latest authorized conversation state. It does not restart remote services or change message retention.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
    NotificationSystemSettings()
    GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
        Text("Calling and media",style=MaterialTheme.typography.titleMedium)
        Text("Voice/video calling and private media depend on network conditions, Android permissions and their authenticated backend. This screen does not claim a successful two-phone connection test.",
            style=MaterialTheme.typography.bodySmall)
    }
}
