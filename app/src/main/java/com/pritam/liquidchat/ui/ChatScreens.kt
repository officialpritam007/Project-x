package com.pritam.liquidchat.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pritam.liquidchat.AppViewModel
import com.pritam.liquidchat.data.Chat
import com.pritam.liquidchat.data.Message

@Composable fun Avatar(name:String) {
    Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer,CircleShape),contentAlignment=Alignment.Center) {
        Text(name.take(1).uppercase(),style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.onPrimaryContainer)
    }
}
@Composable fun ChatRow(chat:Chat,onClick:()->Unit) {
    Surface(onClick=onClick,shape=Glass.card,color=MaterialTheme.colorScheme.surface,modifier=Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Avatar(chat.name)
            Column(Modifier.weight(1f)) {
                Text(chat.name,style=MaterialTheme.typography.titleMedium,maxLines=1,overflow=TextOverflow.Ellipsis)
                Text(chat.preview.ifEmpty {"Say hello to @${chat.username}"},style=MaterialTheme.typography.bodyMedium,maxLines=1,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if(chat.unread>0) Badge {Text(if(chat.unread>99)"99+" else chat.unread.toString())}
        }
    }
}
@Composable fun ChatsScreen(model:AppViewModel,chats:List<Chat>) {
    var query by rememberSaveable {mutableStateOf("")};var filter by rememberSaveable {mutableIntStateOf(0)}
    val connection by model.connection.collectAsStateWithLifecycle()
    val results by model.searchResults.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {Text("Liquid Chat",style=MaterialTheme.typography.headlineLarge,modifier=Modifier.weight(1f));IconButton(onClick={model.repository.refresh()}) {Icon(Icons.Outlined.Refresh,"Refresh")}}
        Text(connection,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(query,{query=it;model.search(it)},label={Text("Search contacts & downloaded messages")},singleLine=true,shape=Glass.capsule,modifier=Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            listOf("All","Favorite","Archive","Unread").forEachIndexed {i,title->FilterChip(selected=filter==i,onClick={filter=i},label={Text(title)})}
        }
        val visible=chats.filter {when(filter){1->it.favorite&&!it.archived;2->it.archived;3->it.unread>0&&!it.archived;else->!it.archived}}
            .filter {query.isBlank()||it.name.contains(query,true)||it.username.contains(query,true)}
        LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=16.dp)) {
            if(visible.isEmpty()&&query.isBlank()) item {GlassCard(Modifier.fillMaxWidth()) {Text("No conversations here yet.");Text("Open Contacts to find someone by username.",style=MaterialTheme.typography.bodyMedium)}}
            items(visible,key={"chat-${it.id}"}) {chat->ChatRow(chat){model.open(chat.id)}}
            if(query.isNotBlank()) {
                item {Text("Downloaded message history",style=MaterialTheme.typography.labelLarge)}
                items(results,key={"result-${it.id}"}) {m->
                    val chat=chats.firstOrNull {it.id==m.conversation}
                    if(chat!=null) Surface(onClick={model.open(chat.id,m.id)},shape=Glass.compact,color=MaterialTheme.colorScheme.surface,modifier=Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {Text("${chat.name} · ${if(m.sender==m.account)"You" else chat.name}",style=MaterialTheme.typography.labelLarge);Text(m.body,maxLines=2,overflow=TextOverflow.Ellipsis)}
                    }
                }
                if(results.isEmpty()) item {Text("No downloaded messages match.",style=MaterialTheme.typography.bodyMedium)}
            }
        }
    }
}
@Composable fun ContactsScreen(model:AppViewModel,chats:List<Chat>,busy:Boolean) {
    var username by rememberSaveable {mutableStateOf("")}
    val found by model.found.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text("Contacts",style=MaterialTheme.typography.headlineLarge)
        OutlinedTextField(username,{username=it.lowercase().take(24)},label={Text("Exact username")},shape=Glass.capsule,singleLine=true,modifier=Modifier.fillMaxWidth())
        Button(onClick={model.find(username)},enabled=!busy&&username.matches(Regex("[a-z0-9_]{3,24}")),modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text("Find contact")}
        found?.let {p->GlassCard(Modifier.fillMaxWidth()) {
            Text(p.optString("display_name"),style=MaterialTheme.typography.titleLarge);Text("@${p.optString("username")}")
            Button(onClick={model.start(p.getString("id"))},enabled=!busy) {Text("Start conversation")}
        }}
        Text("Your conversations",style=MaterialTheme.typography.titleMedium)
        LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)) {items(chats,key={it.id}) {chat->ChatRow(chat){model.open(chat.id)}}}
    }
}
@Composable fun ConversationScreen(model:AppViewModel,cid:String,chats:List<Chat>,busy:Boolean) {
    val chat=chats.firstOrNull {it.id==cid}
    val messages by model.messages.collectAsStateWithLifecycle()
    val foreground by model.repository.foreground.collectAsStateWithLifecycle()
    val focus by model.focusMessage.collectAsStateWithLifecycle()
    val list=rememberLazyListState()
    var body by remember(cid) {mutableStateOf("")}
    var menu by remember {mutableStateOf(false)}
    var confirmDelete by remember {mutableStateOf(false)}
    BackHandler {model.closeChat()}
    // Read acknowledgments are emitted only after a foreground conversation frame exists.
    LaunchedEffect(cid,messages.lastOrNull()?.id,foreground) {
        if(foreground) {withFrameNanos { };runCatching {model.repository.acknowledge(cid,true)}}
    }
    LaunchedEffect(cid,focus,messages.size) {
        if(focus!=null) {
            val i=messages.indexOfFirst {it.id==focus}
            if(i>=0) {list.scrollToItem(i+1);model.focusMessage.value=null}
        } else if(messages.isNotEmpty() && list.firstVisibleItemIndex+list.layoutInfo.visibleItemsInfo.size>=messages.size-2) {
            list.scrollToItem(messages.size)
        }
    }
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick=model::closeChat) {Icon(Icons.AutoMirrored.Outlined.ArrowBack,"Back to chats")}
            if(chat!=null) Avatar(chat.name)
            Column(Modifier.weight(1f).padding(8.dp)) {Text(chat?.name ?: "Conversation",style=MaterialTheme.typography.titleMedium);Text(chat?.username?.let {"@$it"}.orEmpty(),style=MaterialTheme.typography.bodySmall)}
            Box {IconButton(onClick={menu=true}) {Icon(Icons.Outlined.MoreVert,"Conversation options")}
                DropdownMenu(expanded=menu,onDismissRequest={menu=false}) {
                    chat?.let { c->
                        DropdownMenuItem(text={Text(if(c.favorite)"Remove favorite" else "Favorite")},onClick={menu=false;model.updateChat(c,favorite=!c.favorite)})
                        DropdownMenuItem(text={Text(if(c.archived)"Unarchive" else "Archive")},onClick={menu=false;model.updateChat(c,archived=!c.archived)})
                        DropdownMenuItem(text={Text(if(c.muted)"Unmute" else "Mute")},onClick={menu=false;model.updateChat(c,muted=!c.muted)})
                        DropdownMenuItem(text={Text("Block contact")},onClick={menu=false;model.block(c.peer,true)})
                        DropdownMenuItem(text={Text("Unblock contact")},onClick={menu=false;model.block(c.peer,false)})
                        DropdownMenuItem(text={Text("Delete history for me")},onClick={menu=false;confirmDelete=true})
                    }
                }
            }
        }
        HorizontalDivider()
        LazyColumn(state=list,modifier=Modifier.weight(1f).fillMaxWidth(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            item(key="older") {TextButton(onClick={model.older(cid)},enabled=!busy) {Text("Load older messages")}}
            if(messages.isEmpty()) item(key="empty") {Text("Say hello. Your messages will appear here.",color=MaterialTheme.colorScheme.onSurfaceVariant)}
            items(messages.filter {it.conversation==cid},key={it.id}) {m->MessageBubble(m,chat,onRetry={model.retry(m.id)},highlight=m.id==focus)}
        }
        Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(body,{body=it.take(4000)},label={Text("Message")},modifier=Modifier.weight(1f),shape=Glass.card,maxLines=5)
            FilledIconButton(onClick={if(body.isNotBlank()){model.send(cid,body);body=""}},enabled=body.isNotBlank(),modifier=Modifier.size(52.dp)) {Icon(Icons.AutoMirrored.Outlined.Send,"Send message")}
        }
    }
    if(confirmDelete&&chat!=null) AlertDialog(onDismissRequest={confirmDelete=false},title={Text("Delete your chat history?")},text={Text("This hides existing history for your account on every device. The other person keeps their copy. Send or retry any unsent messages before deleting.")},confirmButton={TextButton(onClick={confirmDelete=false;model.updateChat(chat,delete=true)}) {Text("Delete for me")}},dismissButton={TextButton(onClick={confirmDelete=false}) {Text("Cancel")}})
}
@Composable private fun MessageBubble(message:Message,chat:Chat?,onRetry:()->Unit,highlight:Boolean) {
    val mine=message.sender==message.account
    var expanded by remember(message.id) {mutableStateOf(false)}
    val label=when {
        message.status!="SENT"->message.status.lowercase().replaceFirstChar {it.uppercase()}
        message.seq>0&&(chat?.peerRead ?: 0)>=message.seq->"Read"
        message.seq>0&&(chat?.peerDelivered ?: 0)>=message.seq->"Delivered"
        else->"Sent"
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=if(mine)Arrangement.End else Arrangement.Start) {
        Surface(shape=Glass.card,color=if(highlight)MaterialTheme.colorScheme.tertiaryContainer else if(mine)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,modifier=Modifier.widthIn(max=320.dp)) {
            Column(Modifier.padding(horizontal=16.dp,vertical=12.dp)) {
                Text(message.body,style=MaterialTheme.typography.bodyLarge,maxLines=if(expanded)Int.MAX_VALUE else 12,overflow=TextOverflow.Ellipsis)
                if(message.body.length>700) TextButton(onClick={expanded=!expanded}) {Text(if(expanded)"Show less" else "Read more")}
                if(mine) Text(label,style=MaterialTheme.typography.labelSmall,modifier=Modifier.align(Alignment.End))
                if(message.status=="FAILED") {Text(message.error,style=MaterialTheme.typography.bodySmall);TextButton(onClick=onRetry) {Text("Retry")}}
            }
        }
    }
}
@Composable fun SettingsScreen(model:AppViewModel,busy:Boolean) {
    val profile by model.profile.collectAsStateWithLifecycle()
    val unsent by model.unsent.collectAsStateWithLifecycle()
    var confirm by remember {mutableStateOf(false)}
    var edit by remember {mutableStateOf(false)}
    if(edit) {BackHandler {edit=false};Column {TextButton(onClick={edit=false}) {Text("Back to Settings")};ProfileScreen(model,busy)};return}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        Text("Settings",style=MaterialTheme.typography.headlineLarge)
        GlassCard(Modifier.fillMaxWidth()) {
            Avatar(profile?.optString("display_name").orEmpty());Spacer(Modifier.height(12.dp))
            Text(profile?.optString("display_name").orEmpty(),style=MaterialTheme.typography.titleLarge)
            Text("@${profile?.optString("username").orEmpty()}");Text(profile?.optString("bio").orEmpty())
            TextButton(onClick={edit=true}) {Text("Edit profile")}
        }
        GlassCard(Modifier.fillMaxWidth()) {Text("Your privacy matters",fontWeight=FontWeight.SemiBold);Text("Chat access is restricted to participants. This version does not use end-to-end encryption.")}
        OutlinedButton(onClick={confirm=true},enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text("Sign out")}
        Text("Developed by Pritam Pal\n© 2026 Liquid Chat",style=MaterialTheme.typography.bodySmall)
    }
    if(confirm) AlertDialog(onDismissRequest={confirm=false},title={Text("Sign out?")},text={Text(if(unsent>0)"$unsent unsent messages will be discarded. Saved local history, drafts and jobs will be removed from this device." else "Saved local history and account data will be removed from this device.")},confirmButton={TextButton(onClick={confirm=false;model.logout()}) {Text("Sign out")}},dismissButton={TextButton(onClick={confirm=false}) {Text("Cancel")}})
}
