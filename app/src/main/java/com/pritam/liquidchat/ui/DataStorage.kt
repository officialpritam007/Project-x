package com.pritam.liquidchat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pritam.liquidchat.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Clearing the Coil image cache is local-only. Never offer an action which
 * could accidentally discard durable unsent messages or remote media.
 */
@Composable internal fun DataStorage(model:AppViewModel) {
    val context=LocalContext.current
    val unsentCount by model.unsent.collectAsStateWithLifecycle()
    val busy by model.busy.collectAsStateWithLifecycle()
    var imageBytes by remember {mutableLongStateOf(0)}
    var automaticCleanup by remember(context) {
        mutableStateOf(ImageCacheAutoCleanup.preference(context))
    }
    var maintenanceError by remember {mutableStateOf<String?>(null)}
    var confirmClear by remember {mutableStateOf(false)}
    var clearing by remember {mutableStateOf(false)}
    LaunchedEffect(context) {
        imageBytes=withContext(Dispatchers.IO) {
            coil.Coil.imageLoader(context).diskCache?.size ?: 0L
        }
    }
    GlassCard(Modifier.fillMaxWidth()) {
        Text("Storage overview",style=MaterialTheme.typography.titleMedium)
        HorizontalDivider()
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text("Cached images",style=MaterialTheme.typography.bodyMedium)
            Text(android.text.format.Formatter.formatShortFileSize(context,imageBytes),
                style=MaterialTheme.typography.labelLarge)
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text("Messages waiting to send",style=MaterialTheme.typography.bodyMedium)
            Text(unsentCount.toString(),style=MaterialTheme.typography.labelLarge)
        }
        Text("Images are loaded on demand using authorized, short-lived links. They are not saved as permanent public URLs.",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick={confirmClear=true},enabled=!busy&&!clearing,
            modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {
            Text(if(clearing)"Clearing images…" else "Clear cached images")
        }
        Text("Clearing cached images only frees local image-cache space. Chat history, sent media, drafts and unsent messages are not deleted.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
    GlassCard(Modifier.fillMaxWidth()) {
        Text("Automatic image cache cleanup",style=MaterialTheme.typography.titleMedium)
        Text("Optional background cleanup of locally cached thumbnails. It does not delete chat messages, drafts, uploads or your saved photos and wallpapers.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),
            horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            ImageCacheAutoCleanup.choices.forEach {period->
                FilterChip(selected=automaticCleanup==period,
                    onClick={
                        runCatching {ImageCacheAutoCleanup.update(context,period)}
                            .onSuccess {automaticCleanup=period;maintenanceError=null}
                            .onFailure {maintenanceError=it.message ?: "Could not schedule cleanup."}
                    },
                    label={Text(period)})
            }
        }
        Text("Android may delay background maintenance to conserve battery.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        maintenanceError?.let {Text(it,color=MaterialTheme.colorScheme.error,
            style=MaterialTheme.typography.bodySmall)}
    }
    GlassCard(Modifier.fillMaxWidth()) {
        Text("Media limits",style=MaterialTheme.typography.titleMedium)
        Text("Photos: choose images under 10 MB; large images are resized before upload.")
        Text("Voice messages: up to 120 seconds and 2 MB.")
        Text("Media storage is protected by a 100 MB per-account quota and a shared private-storage budget.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        Text("If an upload is over quota, it remains unsuccessful rather than appearing as sent.",
            style=MaterialTheme.typography.bodySmall)
    }
    if(confirmClear) LiquidAlertDialog(
        onDismissRequest={confirmClear=false},
        title={Text("Clear cached images?")},
        text={Text("This removes only locally cached pictures. Shared media, chats, drafts and pending messages remain unchanged. Pictures may download again when you open them.")},
        dismissButton={TextButton(onClick={confirmClear=false}){Text("Cancel")}},
        confirmButton={TextButton(onClick={
            confirmClear=false
            clearing=true
            model.action {
                try {
                    withContext(Dispatchers.IO) {
                        coil.Coil.imageLoader(context).diskCache?.clear()
                    }
                    coil.Coil.imageLoader(context).memoryCache?.clear()
                    imageBytes=withContext(Dispatchers.IO) {
                        coil.Coil.imageLoader(context).diskCache?.size ?: 0L
                    }
                } finally {clearing=false}
            }
        },enabled=!busy&&!clearing) {Text("Clear images")}
        }
    )
}
