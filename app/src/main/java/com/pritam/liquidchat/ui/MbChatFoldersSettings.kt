package com.pritam.liquidchat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pritam.liquidchat.AppViewModel

/**
 * Working device-local folders over authenticated current chats. Message
 * membership, archived flags and Supabase participant ACLs stay untouched.
 */
@Composable internal fun MbChatFoldersSettings(model:AppViewModel) {
    val context=LocalContext.current
    val session by model.session.collectAsStateWithLifecycle()
    val chats by model.chats.collectAsStateWithLifecycle()
    val account=session?.userId
    var folders by remember(account) {mutableStateOf(MbChatFolders.list(context,account))}
    var title by remember(account) {mutableStateOf("")}
    var feedback by remember(account) {mutableStateOf<String?>(null)}
    var renamingId by remember(account) {mutableStateOf<String?>(null)}
    var renameText by remember(account) {mutableStateOf("")}
    val ownedChats=chats.filter {it.account==account}
    GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
        Text("Chat Folders",style=MaterialTheme.typography.titleLarge)
        Text("Keep selected one-to-one conversations together. Folder membership is private to this account on this phone; your Supabase chats are unchanged.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(title,onValueChange={title=it.take(24)},
                label={Text("Folder name")},singleLine=true,
                modifier=Modifier.weight(1f))
            Button(onClick={
                val owner=account
                if(owner!=null)runCatching {
                    MbChatFolders.create(context,owner,title)
                }.onSuccess {
                    folders=it
                    title=""
                    feedback=null
                }.onFailure {feedback=it.message ?: "Could not create folder."}
            },enabled=account!=null && title.isNotBlank()) {Text("Create")}
        }
        Text("${folders.size} / 12 custom folders",
            style=MaterialTheme.typography.labelMedium)
        feedback?.let {Text(it,color=MaterialTheme.colorScheme.error,
            style=MaterialTheme.typography.bodySmall)}
    }
    if(folders.isEmpty()) GlassCard(Modifier.fillMaxWidth()) {
        Text("No custom folders yet.")
        Text("Create a folder and select the contacts to show inside it. You can also add chats from a conversation's long-press menu.",
            style=MaterialTheme.typography.bodySmall)
    }
    if(renamingId!=null) AlertDialog(
        onDismissRequest={renamingId=null},
        title={Text("Rename chat list")},
        text={
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value=renameText,
                    onValueChange={renameText=it.take(24)},
                    label={Text("List name")},
                    singleLine=true,
                    modifier=Modifier.fillMaxWidth())
                Text("Renaming keeps all chats in this list.",
                    style=MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton={
            TextButton(
                enabled=account!=null && renameText.isNotBlank(),
                onClick={
                    val owner=account
                    val id=renamingId
                    if(owner!=null && id!=null)runCatching {
                        MbChatFolders.rename(context,owner,id,renameText)
                    }.onSuccess {folders=it;feedback=null;renamingId=null}
                        .onFailure {feedback=it.message ?: "Cannot rename list."}
                }) {Text("Save")}
        },
        dismissButton={TextButton(onClick={renamingId=null}) {Text("Cancel")}}
    )
    folders.forEach {folder->
        GlassCard(Modifier.fillMaxWidth(),Glass.compact) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(folder.name,style=MaterialTheme.typography.titleMedium)
                    Text("${folder.members.size} chats",
                        style=MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick={renamingId=folder.id;renameText=folder.name},
                    enabled=account!=null) {
                    Icon(Icons.Outlined.Edit,"Rename folder")
                }
                IconButton(onClick={
                    val owner=account
                    if(owner!=null)folders=MbChatFolders.remove(context,owner,folder.id)
                },enabled=account!=null) {
                    Icon(Icons.Outlined.DeleteOutline,"Remove folder")
                }
            }
            if(ownedChats.isEmpty()) Text("Your conversations will appear here.",
                style=MaterialTheme.typography.bodySmall)
            ownedChats.forEach {chat->
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Checkbox(checked=chat.id in folder.members,
                        onCheckedChange={enabled->
                            val owner=account
                            if(owner!=null)runCatching {
                                MbChatFolders.membership(context,owner,folder.id,chat.id,enabled)
                            }.onSuccess {folders=it;feedback=null}
                                .onFailure {feedback=it.message ?: "Cannot update folder."}
                        },enabled=account!=null)
                    Column {
                        Text(chat.name,style=MaterialTheme.typography.bodyMedium)
                        Text("@${chat.username}",style=MaterialTheme.typography.labelSmall,
                            color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
