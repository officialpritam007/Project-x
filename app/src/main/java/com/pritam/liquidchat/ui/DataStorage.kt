package com.pritam.liquidchat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pritam.liquidchat.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable internal fun DataStorage(model:AppViewModel) {
    val context=LocalContext.current
    val count by model.unsent.collectAsStateWithLifecycle()
    var bytes by remember {mutableLongStateOf(0)}
    var clearing by remember {mutableStateOf(false)}
    LaunchedEffect(Unit) {bytes=withContext(Dispatchers.IO) {coil.Coil.imageLoader(context).diskCache?.size ?: 0}}
    GlassCard(Modifier.fillMaxWidth()) {
        Text("Data and Storage",style=MaterialTheme.typography.titleMedium)
        Text("Image disk cache: ${android.text.format.Formatter.formatShortFileSize(context,bytes)}")
        Text("Private chat images use authorized, short-lived links. Images load when displayed; permanent public links are not stored.",style=MaterialTheme.typography.bodyMedium)
        Text("$count unsent messages are kept separately in private storage until sent or explicitly discarded.")
        TextButton(onClick={clearing=true;model.action {
            try {
                withContext(Dispatchers.IO) {coil.Coil.imageLoader(context).diskCache?.clear()}
                coil.Coil.imageLoader(context).memoryCache?.clear();bytes=0
            } finally {clearing=false}
        }},enabled=!clearing) {Text("Clear image cache")}
        HorizontalDivider(Modifier.padding(vertical=8.dp))
        Text("Media limits",style=MaterialTheme.typography.titleSmall)
        Text("Uploads enforce the existing 100 MB per-account media budget and the shared private-storage budget. Quota errors are shown before a failed upload can be treated as sent.")
        Text("Voice: up to 120 seconds / 2 MB. Choose gallery images below 10 MB; large images are resized before upload.")
    }
}
