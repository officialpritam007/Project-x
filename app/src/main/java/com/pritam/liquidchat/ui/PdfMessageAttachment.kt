package com.pritam.liquidchat.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pritam.liquidchat.AppViewModel
import com.pritam.liquidchat.data.Message
import com.pritam.liquidchat.data.PdfDocuments

/** Metadata-only document bubble: document bytes never pass through WebView. */
@Composable internal fun PdfMessageAttachment(
    model:AppViewModel,message:Message
) {
    val context=LocalContext.current
    val session by model.session.collectAsStateWithLifecycle()
    val busy by model.busy.collectAsStateWithLifecycle()
    val filename=PdfDocuments.safeName(message.body.removePrefix("Document: "))
    var feedback by remember(message.id) {mutableStateOf<String?>(null)}
    val export=rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) {destination->
        if(destination!=null)model.action {
            PdfDocuments.saveReceived(context,model.repository,message,destination)
            feedback="PDF saved."
        }
    }
    Surface(color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.65f),
        shape=Glass.compact) {
        Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Description,"PDF document",
                    tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(30.dp))
                Column(Modifier.weight(1f)) {
                    Text(filename,style=MaterialTheme.typography.bodyMedium,maxLines=2)
                    Text("PDF document",style=MaterialTheme.typography.labelSmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if(message.status=="SENT" && message.mediaId.isNotBlank() &&
                    message.deletedAt.isEmpty() && session?.userId==message.account) {
                    IconButton(onClick={export.launch(filename)},enabled=!busy) {
                        Icon(Icons.Outlined.FileDownload,"Save PDF")
                    }
                }
            }
            if(message.status!="SENT")
                Text(if(message.status=="FAILED")"Upload failed · Retry from message"
                    else "Preparing secure document delivery",
                    style=MaterialTheme.typography.labelSmall)
            feedback?.let {Text(it,style=MaterialTheme.typography.labelSmall)}
        }
    }
}
