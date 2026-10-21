package com.pritam.liquidchat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Same account-local reply library used by ConversationScreen's /shortcuts.
 * Shortcuts are inserted as editable text: sending still requires an explicit
 * tap and goes through the existing authenticated Supabase outbox.
 */
@Composable internal fun MbQuickReplySettings(account:String?) {
    val context=LocalContext.current
    var entries by remember(account) {mutableStateOf(MbQuickReplies.list(context,account))}
    var editing by remember(account) {mutableStateOf(false)}
    var shortcut by remember(account) {mutableStateOf("")}
    var message by remember(account) {mutableStateOf("")}
    var feedback by remember(account) {mutableStateOf<String?>(null)}
    GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
        Text("Quick replies",style=MaterialTheme.typography.titleMedium)
        Text("Save /shortcuts for frequently used chat text. Replies are local to this account and are never sent without tapping Send.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text("${entries.size} / 24 saved",Modifier.weight(1f),
                style=MaterialTheme.typography.labelMedium)
            TextButton(onClick={entries=MbQuickReplies.list(context,account);editing=true},
                enabled=!account.isNullOrBlank()) {Text("Manage replies")}
        }
    }
    if(editing) LiquidAlertDialog(
        onDismissRequest={editing=false},
        title={Text("Manage quick replies")},
        text={Column(Modifier.fillMaxWidth().heightIn(max=450.dp)
            .verticalScroll(rememberScrollState()),
            verticalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(shortcut,{shortcut=it.take(25)},
                label={Text("Shortcut (e.g. /hello)")},
                modifier=Modifier.fillMaxWidth(),singleLine=true)
            OutlinedTextField(message,{if(it.length<=4000)message=it},
                label={Text("Reply text")},modifier=Modifier.fillMaxWidth(),maxLines=4)
            Button(onClick={
                if(account!=null)runCatching {
                    MbQuickReplies.save(context,account,shortcut,message)
                }.onSuccess {
                    entries=it
                    shortcut=""
                    message=""
                    feedback="Saved"
                }.onFailure {feedback=it.message ?: "Could not save reply"}
            },enabled=!account.isNullOrBlank(),modifier=Modifier.fillMaxWidth()) {
                Text("Save reply")
            }
            feedback?.let {Text(it,style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)}
            entries.forEach {entry->
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("/"+entry.shortcut,style=MaterialTheme.typography.titleSmall)
                        Text(entry.body,maxLines=2,overflow=TextOverflow.Ellipsis,
                            style=MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick={shortcut="/"+entry.shortcut;message=entry.body}) {Text("Edit")}
                    IconButton(onClick={
                        if(account!=null)entries=MbQuickReplies.delete(context,account,entry.shortcut)
                    }) {Icon(Icons.Outlined.DeleteOutline,"Delete quick reply")}
                }
                HorizontalDivider()
            }
        }},
        confirmButton={TextButton(onClick={editing=false}) {Text("Done")}}
    )
}
