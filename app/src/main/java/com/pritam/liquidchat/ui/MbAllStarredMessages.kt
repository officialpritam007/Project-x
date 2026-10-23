package com.pritam.liquidchat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pritam.liquidchat.AppViewModel
import com.pritam.liquidchat.data.Chat
import com.pritam.liquidchat.data.Message
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Account-isolated, cached-only starred history. No new server permission is assumed. */
@Composable internal fun MbAllStarredMessages(
    model:AppViewModel,chats:List<Chat>,account:String,onDismiss:()->Unit
) {
    val source=remember(model,account) {model.repository.dao.starredMessages(account)}
    val stored by source.collectAsStateWithLifecycle(initialValue=emptyList())
    val availableChats=remember(chats,account) {
        chats.filter {it.account==account}.associateBy {it.id}
    }
    val visible=remember(stored,availableChats,account) {
        stored.filter {
            it.account==account && it.conversation in availableChats &&
                it.starred && it.status=="SENT" && it.deletedAt.isEmpty()
        }
    }
    var search by remember(account) {mutableStateOf("")}
    val found=remember(visible,availableChats,search) {
        val term=search.trim()
        if(term.isEmpty())visible else visible.filter {message->
            val chat=availableChats[message.conversation]
            chat!=null && (chat.name.contains(term,ignoreCase=true) ||
                chat.username.contains(term,ignoreCase=true) ||
                starredMessagePreview(message).contains(term,ignoreCase=true))
        }
    }
    val format=remember {
        DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
    }
    LiquidActionSheet(title="Starred messages",onDismiss=onDismiss) {
        Text("Starred messages saved on this device. Older or uncached messages " +
            "may not appear until their conversations have been loaded.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value=search,
            onValueChange={search=it.take(100)},
            label={Text("Find starred messages")},
            leadingIcon={Icon(Icons.Outlined.Search,contentDescription=null)},
            trailingIcon={
                if(search.isNotEmpty())IconButton(onClick={search=""}) {
                    Icon(Icons.Outlined.Close,"Clear starred search")
                }
            },
            singleLine=true,
            modifier=Modifier.fillMaxWidth()
        )
        Text("${found.size} of ${visible.size} downloaded starred messages",
            style=MaterialTheme.typography.labelSmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(found.isEmpty()) {
            Text(if(search.isBlank())"No downloaded starred messages."
                else "No starred messages match this search.",
                style=MaterialTheme.typography.bodyMedium,
                modifier=Modifier.padding(vertical=16.dp))
        }
        found.forEach {message->
            val chat=availableChats[message.conversation] ?: return@forEach
            Column(Modifier.fillMaxWidth()
                .clickable(role=Role.Button) {
                    onDismiss()
                    model.open(message.conversation,message.id)
                }
                .padding(horizontal=6.dp,vertical=12.dp),
                verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(chat.name,style=MaterialTheme.typography.titleSmall)
                Text(starredMessagePreview(message),maxLines=3,
                    overflow=TextOverflow.Ellipsis,
                    style=MaterialTheme.typography.bodyMedium)
                Text(runCatching {
                    Instant.ofEpochMilli(message.localTime)
                        .atZone(ZoneId.systemDefault()).format(format)
                }.getOrDefault("Saved message"),
                    style=MaterialTheme.typography.labelSmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider()
        }
    }
}

/** Display a type label rather than a cached local file path or signed media URL. */
internal fun starredMessagePreview(message:Message):String =
    message.body.takeIf {it.isNotBlank()} ?: when(message.mediaKind) {
        "voice"->"Voice message"
        "chat_video"->"Video"
        "sticker"->"Sticker"
        "document"->"Document"
        else->if(message.mediaId.isNotBlank())"Photo" else "Message"
    }
