package com.pritam.liquidchat.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.ui.platform.LocalContext
import com.pritam.liquidchat.data.Images
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pritam.liquidchat.AppViewModel
import com.pritam.liquidchat.data.Chat
import com.pritam.liquidchat.data.Message
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop

@Composable fun Avatar(name:String,mediaId:String="",backend:com.pritam.liquidchat.data.Backend?=null,self:Boolean=false,size:androidx.compose.ui.unit.Dp=48.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(if(self)Color(0xFF07B4D8) else Color(0xFFB94799),CircleShape),contentAlignment=Alignment.Center) {
        if(mediaId.isNotEmpty()&&backend!=null) ProtectedImage(backend,mediaId,description="Profile photo for $name",modifier=Modifier.fillMaxSize(),scale=androidx.compose.ui.layout.ContentScale.Crop)
        else Text(if(self)name.split(" ").filter {it.firstOrNull()?.isLetter()==true}.take(2).joinToString(""){it.take(1)}.uppercase() else name.take(1).uppercase(),style=MaterialTheme.typography.titleLarge,color=Color.White)
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable fun ChatRow(chat:Chat,backend:com.pritam.liquidchat.data.Backend,onClick:()->Unit,onLongClick:()->Unit={}) {
    Surface(shape=Glass.card,color=Color.Transparent,modifier=Modifier.fillMaxWidth().glassSource().combinedClickable(onClick=onClick,onLongClick=onLongClick)) {
        Row(Modifier.padding(vertical=8.dp,horizontal=1.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Avatar(chat.name,chat.avatarId,backend,size=52.dp)
            Column(Modifier.weight(1f)) {
                Text(chat.name,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium,maxLines=1,overflow=TextOverflow.Ellipsis)
                Text(chat.preview.ifEmpty {"Say hello to @${chat.username}"},style=MaterialTheme.typography.bodyMedium,maxLines=1,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment=Alignment.End,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                if(chat.lastAt.isNotEmpty()) Text(remember(chat.lastAt) {runCatching {java.time.Instant.parse(chat.lastAt).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}.getOrDefault("")},style=MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    if(chat.pinnedAt>0)Icon(Icons.Outlined.PushPin,"Pinned",Modifier.size(14.dp))
                    if(chat.favorite)Icon(Icons.Outlined.Star,"Favorite",Modifier.size(14.dp))
                    if(chat.muted)Icon(Icons.Outlined.NotificationsOff,"Muted",Modifier.size(14.dp))
                }
                if(chat.unread>0) Badge {Text(if(chat.unread>99)"99+" else chat.unread.toString())}
            }
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable fun ChatsScreen(model:AppViewModel,chats:List<Chat>,onContacts:()->Unit={},onProfile:()->Unit={}) {
    var selectedChat by remember {mutableStateOf<Chat?>(null)}
    var deleteChat by remember {mutableStateOf<Chat?>(null)}
    var query by rememberSaveable {mutableStateOf("")};var filter by rememberSaveable {mutableIntStateOf(0)}
    var menu by remember {mutableStateOf(false)};var stories by remember {mutableStateOf(false)};var ownStories by remember {mutableStateOf(false)}
    val profile by model.profile.collectAsStateWithLifecycle()
    val connection by model.connection.collectAsStateWithLifecycle()
    val results by model.searchResults.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().padding(horizontal=12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        GlassHeader("Liquid Chat",brand=true) {
            Box {IconButton(onClick={menu=true}) {Icon(Icons.Outlined.MoreVert,"Chat options")}
                DropdownMenu(menu,{menu=false}) {
                    listOf("All","Favorite","Archive","Unread").forEachIndexed {i,title->DropdownMenuItem(text={Text(title)},onClick={filter=i;menu=false})}
                    DropdownMenuItem(text={Text("Refresh")},onClick={model.repository.refresh();menu=false})
                }
            }
        }
        Row(Modifier.padding(top=10.dp,start=4.dp,bottom=8.dp),horizontalArrangement=Arrangement.spacedBy(28.dp)) {
            Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.clickable {ownStories=true}) {
                Box {
                    Avatar(profile?.optString("display_name").orEmpty(),profile?.optString("avatar_id").orEmpty().takeUnless {it=="null"}.orEmpty(),model.backend,self=true,size=50.dp)
                    Surface(Modifier.align(Alignment.BottomEnd).size(21.dp),shape=CircleShape,color=Color(0xFF00A6E8)) {Box(contentAlignment=Alignment.Center){Text("+",color=Color.White)}}
                }
                Spacer(Modifier.height(12.dp));Text("My Story",fontSize=12.sp,fontWeight=FontWeight.Bold)
            }
            Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.clickable {stories=true}) {
                Box(Modifier.size(50.dp).background(Color(0xFF00A6E8),CircleShape),contentAlignment=Alignment.Center){Text("➤",color=Color.White,fontSize=24.sp)}
                Spacer(Modifier.height(12.dp));Text("Stories",fontSize=12.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        GlassSearch(query,{query=it;model.search(it)},"Search Chats")
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("All","Favorite","Archive","Unread").forEachIndexed {index,label->FilterChip(selected=filter==index,onClick={filter=index},label={Text(label)},shape=Glass.capsule)}
        }
        if(connection!="Connected"&&connection!="Online") Text(connection,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        val visible=chats.filter {when(filter){1->it.favorite&&!it.archived;2->it.archived;3->it.unread>0&&!it.archived;else->!it.archived}}
            .filter {query.isBlank()||it.name.contains(query,true)||it.username.contains(query,true)}
        LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=132.dp)) {
            if(visible.isEmpty()&&query.isBlank()) item {GlassCard(Modifier.fillMaxWidth()) {Text("No conversations here yet.");Text("Open Contacts to find someone by username.",style=MaterialTheme.typography.bodyMedium)}}
            items(visible,key={"chat-${it.id}"}) {chat->
                Box(Modifier.combinedClickable(onClick={model.open(chat.id)},onLongClick={selectedChat=chat})) {
                    ChatRow(chat,model.backend,onClick={model.open(chat.id)},onLongClick={selectedChat=chat})
                }
            }
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
    Column(Modifier.align(Alignment.BottomEnd).padding(end=17.dp,bottom=78.dp+WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
        GlassSurface(Modifier.size(48.dp),CircleShape) {IconButton(onClick=onContacts) {Icon(Icons.Outlined.PhotoCamera,"Choose a contact to share a photo")}}
        FloatingActionButton(onClick=onContacts,shape=CircleShape,containerColor=Color(0xFF00A6E8),contentColor=Color.White,modifier=Modifier.size(52.dp)) {Icon(Icons.Outlined.Add,"New chat")}
    }
    }
    selectedChat?.let {c->AlertDialog(onDismissRequest={selectedChat=null},title={Text(c.name)},text={Column {
        TextButton(onClick={selectedChat=null;model.action {model.repository.chatAction(c.id,"pin",c.pinnedAt==0L)}}) {Text(if(c.pinnedAt>0)"Unpin" else "Pin")}
        TextButton(onClick={selectedChat=null;model.action {model.repository.chatAction(c.id,"unread",true)}}) {Text("Mark unread")}
        TextButton(onClick={selectedChat=null;model.updateChat(c,favorite=!c.favorite)}) {Text(if(c.favorite)"Remove favorite" else "Favorite")}
        TextButton(onClick={selectedChat=null;model.updateChat(c,archived=!c.archived)}) {Text(if(c.archived)"Unarchive" else "Archive")}
        TextButton(onClick={selectedChat=null;model.updateChat(c,muted=!c.muted)}) {Text(if(c.muted)"Unmute" else "Mute")}
        TextButton(onClick={selectedChat=null;deleteChat=c}) {Text("Delete history for me")}
    }},confirmButton={TextButton(onClick={selectedChat=null}) {Text("Done")}})}
    deleteChat?.let {c->AlertDialog(onDismissRequest={deleteChat=null},title={Text("Delete your chat history?")},text={Text("This removes your existing history on every device. The other person keeps their copy.")},confirmButton={TextButton(onClick={deleteChat=null;model.updateChat(c,delete=true)}) {Text("Delete for me")}},dismissButton={TextButton(onClick={deleteChat=null}) {Text("Cancel")}})}
    if(stories) MomentsDialog(model,mine=false,onDismiss={stories=false})
    if(ownStories) MomentsDialog(model,mine=true,onDismiss={ownStories=false})
}
@Composable fun ContactsScreen(model:AppViewModel,chats:List<Chat>,busy:Boolean) {
    var username by rememberSaveable {mutableStateOf("")}
    var showFind by rememberSaveable {mutableStateOf(false)}
    var alphabetical by rememberSaveable {mutableStateOf(true)}
    val context=LocalContext.current
    val contacts by model.contacts.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {model.action {model.repository.loadContacts()}}
    val found by model.found.collectAsStateWithLifecycle()
    val filtered=remember(contacts,username,alphabetical) {contacts.filter {username.isBlank()||it.name.contains(username,true)||it.username.contains(username,true)}.let {if(alphabetical)it.sortedBy {c->c.name.lowercase()} else it.reversed()}}
    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().padding(horizontal=12.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        GlassHeader("Contacts") {IconButton(onClick={alphabetical=!alphabetical}) {Icon(Icons.Outlined.SortByAlpha,"Change contact sort order")}}
        GlassSearch(username,{username=it.lowercase().take(24)},"Search Contacts")
        GlassCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().heightIn(min=46.dp).clickable {
                val intent=android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT,"Find me on Liquid Chat: @${model.profile.value?.optString("username").orEmpty()}")
                context.startActivity(android.content.Intent.createChooser(intent,"Invite Friends"))
            },verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(34.dp).background(Color(0xFF00A6E8),CircleShape),contentAlignment=Alignment.Center){Text("+",color=Color.White,fontWeight=FontWeight.Bold,fontSize=20.sp)}
                Text("Invite Friends",fontSize=18.sp,fontWeight=FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth().heightIn(min=46.dp).clickable {showFind=true},verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(34.dp).background(Color(0xFF11C748),CircleShape),contentAlignment=Alignment.Center){Text("↗",color=Color.White,fontSize=22.sp)}
                Text("Find Friends",fontSize=18.sp,fontWeight=FontWeight.Bold)
            }
        }
        LazyColumn(contentPadding=PaddingValues(bottom=140.dp+WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            item {Text(if(alphabetical)"Sorted by name" else "Reverse name order",Modifier.padding(horizontal=16.dp),color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold,fontSize=15.sp)}
            if(filtered.isEmpty()) item {GlassCard(Modifier.fillMaxWidth()){Text(if(username.isBlank())"Find someone by username and save them here." else "No saved contacts match.")}}
            items(filtered,key={"contact-${it.id}"}) {contact->
                GlassSurface(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                        Row(Modifier.weight(1f).clickable {val chat=chats.firstOrNull {it.peer==contact.id};if(chat!=null)model.open(chat.id) else model.start(contact.id)},verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            Avatar(contact.name,contact.avatarId,model.backend,size=43.dp)
                            Column {Text(contact.name,fontSize=17.sp,fontWeight=FontWeight.Bold);Text("@${contact.username}",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                        }
                        IconButton(onClick={model.action {model.repository.saveContact(contact.id,false)}},enabled=!busy) {Icon(Icons.Outlined.PersonRemove,"Remove saved contact")}
                    }
                }
            }
        }
    }
    GlassSurface(Modifier.align(Alignment.BottomEnd).padding(end=17.dp,bottom=78.dp+WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()).size(52.dp),CircleShape) {IconButton(onClick={showFind=true},modifier=Modifier.fillMaxSize()) {Icon(Icons.Outlined.Add,"Add contact",tint=MaterialTheme.colorScheme.primary)}}
    }
    if(showFind) AlertDialog(onDismissRequest={showFind=false},title={Text("Find Friends")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Find someone by their exact username.")
        OutlinedTextField(username,{username=it.lowercase().take(24)},label={Text("Username")},singleLine=true)
        Button(onClick={model.find(username)},enabled=!busy&&username.matches(Regex("[a-z0-9_]{3,24}"))) {Text("Find contact")}
        found?.takeIf {it.optString("username").equals(username,true)}?.let {p->
            Text(p.optString("display_name"),fontWeight=FontWeight.Bold);Text("@${p.optString("username")}")
            Row {TextButton(onClick={showFind=false;model.start(p.getString("id"))},enabled=!busy){Text("Start chat")};TextButton(onClick={model.action {model.repository.saveContact(p.getString("id"),true)}},enabled=!busy){Text("Save contact")}}
        }
    }},confirmButton={TextButton(onClick={showFind=false}){Text("Done")}})
}
@Composable fun ConversationScreen(model:AppViewModel,cid:String,chats:List<Chat>,busy:Boolean) {
    val chat=chats.firstOrNull {it.id==cid}
    val allMessages by model.messages.collectAsStateWithLifecycle()
    val messages=remember(allMessages,cid) {allMessages.filter {it.conversation==cid}}
    val foreground by model.repository.foreground.collectAsStateWithLifecycle()
    val focus by model.focusMessage.collectAsStateWithLifecycle()
    val contact by model.repository.contactState.collectAsStateWithLifecycle()
    var input by remember(cid) {mutableStateOf(TextFieldValue(""))}
    var draftReady by remember(cid) {mutableStateOf(false)}
    var queueing by remember(cid) {mutableStateOf(false)}
    var emojiPicker by remember(cid) {mutableStateOf(false)}
    var palette by remember(cid) {mutableStateOf("Ice")}
    var appearance by remember(cid) {mutableStateOf(false)}
    var searchOpen by remember(cid) {mutableStateOf(false)}
    var chatQuery by remember(cid) {mutableStateOf("")}
    var searchIndex by remember(cid) {mutableIntStateOf(0)}
    val chatResults=remember(messages,chatQuery) {if(chatQuery.isBlank())emptyList() else messages.filter {it.body.contains(chatQuery,true)}}
    var actionMessage by remember(cid) {mutableStateOf<Message?>(null)}
    val clipboard=LocalClipboardManager.current
    var replyTo by remember(cid) {mutableStateOf("")}
    var editMessage by remember(cid) {mutableStateOf<Message?>(null)}
    var editBody by remember(cid) {mutableStateOf("")}
    var deleteMessage by remember(cid) {mutableStateOf<Message?>(null)}
    var starsOpen by remember(cid) {mutableStateOf(false)}
    val body=input.text
    fun updateInput(value:TextFieldValue) {input=value;model.repository.saveDraft(cid,value.text,replyTo)}
    LaunchedEffect(cid) {
        val draft=model.repository.draft(cid)
        input=TextFieldValue(draft?.body.orEmpty(),TextRange(draft?.body?.length ?: 0));replyTo=draft?.replyTo.orEmpty();draftReady=true
        palette=model.repository.palette(cid)
    }
    val context=LocalContext.current
    var prepared by remember(cid) {mutableStateOf<Images.Prepared?>(null)}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {uri->if(uri!=null)model.action {
        val account=model.backend.session.value?.userId ?: return@action
        val image=Images.prepare(context,uri,account)
        if(model.backend.session.value?.userId==account)prepared=image else image.file.delete()
    }}
    val display=LocalDisplayOptions.current
    val sendScale by androidx.compose.animation.core.animateFloatAsState(if(body.isNotBlank())1.04f else 1f,animationSpec=if(display.reducedMotion)androidx.compose.animation.core.tween(0) else androidx.compose.animation.core.spring(stiffness=when(display.animation){"Normal"->400f;"Extra"->250f;else->650f}),label="Send readiness")
    var menu by remember {mutableStateOf(false)}
    var confirmDelete by remember {mutableStateOf(false)}
    var report by remember {mutableStateOf(false)}
    var details by remember {mutableStateOf(false)}
    var reason by remember {mutableStateOf("")}
    BackHandler {model.closeChat()}
    // Read acknowledgments are emitted only after a foreground conversation frame exists.
    LaunchedEffect(cid,messages.lastOrNull()?.id,foreground) {
        if(foreground) {withFrameNanos { };runCatching {model.repository.acknowledge(cid,true)}}
    }
    LaunchedEffect(cid,foreground) {
        var lastSent=0L
        if(foreground) try {
            snapshotFlow {input.text}.drop(1).collectLatest { text ->
                val keyTime=android.os.SystemClock.elapsedRealtime()
                if(text.isBlank()) runCatching {model.repository.typing(cid,false)}
                else {
                    kotlinx.coroutines.delay(maxOf(0L,2500-(keyTime-lastSent)))
                    runCatching {model.repository.typing(cid,true)}
                    lastSent=android.os.SystemClock.elapsedRealtime()
                    kotlinx.coroutines.delay(maxOf(0L,4500-(lastSent-keyTime)))
                    runCatching {model.repository.typing(cid,false)}
                }
            }
        } finally {kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {runCatching {model.repository.typing(cid,false)}}}
    }
    Box(Modifier.fillMaxSize()) {
    Box(Modifier.matchParentSize().glassSource(.5f).background(wallpaper(palette,LocalGlassDark.current)))
    Column(Modifier.fillMaxSize().imePadding()) {
        GlassSurface(Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=3.dp),Glass.capsule) {
        Row(Modifier.fillMaxWidth().padding(horizontal=4.dp,vertical=2.dp),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick=model::closeChat) {Icon(Icons.AutoMirrored.Outlined.ArrowBack,"Back to chats")}
            if(chat!=null) Box(Modifier.clickable {details=true}) {Avatar(chat.name,chat.avatarId,model.backend)}
            Column(Modifier.weight(1f).padding(horizontal=8.dp)) {Text(chat?.name ?: "Conversation",style=MaterialTheme.typography.titleMedium);Text(when {contact.optBoolean("online")->"Online";!contact.isNull("last_seen")&&contact.has("last_seen")->"Last seen "+runCatching {java.time.Instant.parse(contact.getString("last_seen")).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("dd MMM, HH:mm"))}.getOrDefault("");else->chat?.username?.let {"@$it"}.orEmpty()},style=MaterialTheme.typography.bodySmall)}
            Box {IconButton(onClick={menu=true}) {Icon(Icons.Outlined.MoreVert,"Conversation options")}
                DropdownMenu(expanded=menu,onDismissRequest={menu=false}) {
                    chat?.let { c->
                        DropdownMenuItem(text={Text("Contact details & shared media")},onClick={menu=false;details=true})
                        DropdownMenuItem(text={Text("Search conversation")},onClick={menu=false;searchOpen=true})
                        DropdownMenuItem(text={Text("Starred messages")},onClick={menu=false;starsOpen=true})
                        DropdownMenuItem(text={Text(if(c.pinnedAt>0)"Unpin" else "Pin")},onClick={menu=false;model.action {model.repository.chatAction(c.id,"pin",c.pinnedAt==0L)}})
                        DropdownMenuItem(text={Text("Wallpaper")},onClick={menu=false;appearance=true})
                        DropdownMenuItem(text={Text(if(c.favorite)"Remove favorite" else "Favorite")},onClick={menu=false;model.updateChat(c,favorite=!c.favorite)})
                        DropdownMenuItem(text={Text(if(c.archived)"Unarchive" else "Archive")},onClick={menu=false;model.updateChat(c,archived=!c.archived)})
                        DropdownMenuItem(text={Text(if(c.muted)"Unmute" else "Mute")},onClick={menu=false;model.updateChat(c,muted=!c.muted)})
                        DropdownMenuItem(text={Text("Block contact")},onClick={menu=false;model.block(c.peer,true)})
                        DropdownMenuItem(text={Text("Unblock contact")},onClick={menu=false;model.block(c.peer,false)})
                        DropdownMenuItem(text={Text("Report contact")},onClick={menu=false;report=true})
                        DropdownMenuItem(text={Text("Delete history for me")},onClick={menu=false;confirmDelete=true})
                    }
                }
            }
        }
        }
        if(searchOpen) GlassSurface(Modifier.fillMaxWidth().padding(horizontal=8.dp)) {
            Column(Modifier.padding(8.dp)) {
                OutlinedTextField(chatQuery,{chatQuery=it;searchIndex=0},placeholder={Text("Search this chat")},singleLine=true,modifier=Modifier.fillMaxWidth(),trailingIcon={IconButton(onClick={searchOpen=false;chatQuery=""}) {Icon(Icons.Outlined.Close,"Close search")}})
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("Downloaded messages only · ${chatResults.size} matches",Modifier.weight(1f),style=MaterialTheme.typography.labelSmall)
                    IconButton(onClick={if(chatResults.isNotEmpty()){searchIndex=(searchIndex-1+chatResults.size)%chatResults.size;model.focusMessage.value=chatResults[searchIndex].id}},enabled=chatResults.isNotEmpty()) {Icon(Icons.Outlined.KeyboardArrowUp,"Previous match")}
                    IconButton(onClick={if(chatResults.isNotEmpty()){model.focusMessage.value=chatResults[searchIndex%chatResults.size].id;searchIndex=(searchIndex+1)%chatResults.size}},enabled=chatResults.isNotEmpty()) {Icon(Icons.Outlined.KeyboardArrowDown,"Next match")}
                }
            }
        }
        MessageTimeline(cid,messages,contact.optBoolean("typing"),focus,{model.focusMessage.value=null},loadOlder={model.repository.loadOlder(cid)},modifier=Modifier.weight(1f).fillMaxWidth()) {m->
            MessageBubble(m,chat,model,onRetry={model.retry(m.id)},highlight=m.id==focus,onLongClick={actionMessage=m},quoted=messages.firstOrNull {it.id==m.replyTo},onQuoteClick={if(m.replyTo.isNotEmpty())model.focusMessage.value=m.replyTo})
        }
        if(replyTo.isNotEmpty()) GlassSurface(Modifier.fillMaxWidth().padding(horizontal=12.dp),Glass.compact) {
            Row(Modifier.padding(start=14.dp),verticalAlignment=Alignment.CenterVertically) {
                val original=messages.firstOrNull {it.id==replyTo}
                Column(Modifier.weight(1f)) {Text("Replying to ${if(original?.sender==original?.account)"yourself" else chat?.name.orEmpty()}",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary);Text(original?.takeIf {it.deletedAt.isEmpty()}?.body ?: "Original message unavailable",maxLines=1,overflow=TextOverflow.Ellipsis)}
                IconButton(onClick={replyTo="";model.repository.saveDraft(cid,body)}) {Icon(Icons.Outlined.Close,"Cancel reply")}
            }
        }
        GlassSurface(Modifier.fillMaxWidth().padding(8.dp),Glass.capsule) {
            Row(Modifier.fillMaxWidth().padding(4.dp),verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                IconButton(onClick={emojiPicker=true},enabled=draftReady) {Icon(Icons.Outlined.EmojiEmotions,"Open emoji picker")}
                androidx.compose.foundation.text.BasicTextField(input,{if(it.text.length<=4000)updateInput(it)},enabled=draftReady&&!queueing,keyboardOptions=KeyboardOptions(capitalization=KeyboardCapitalization.Sentences,keyboardType=KeyboardType.Text),modifier=Modifier.weight(1f).heightIn(min=48.dp,max=140.dp).padding(vertical=14.dp).testTag("message-input").semantics {contentDescription="Message"},textStyle=MaterialTheme.typography.bodyLarge.copy(color=MaterialTheme.colorScheme.onSurface),maxLines=5,decorationBox={inner->Box {if(body.isEmpty())Text("Message",color=MaterialTheme.colorScheme.onSurfaceVariant);inner()}})
                IconButton(onClick={picker.launch("image/*")},enabled=!busy&&replyTo.isEmpty()) {Icon(Icons.Outlined.AttachFile,"Attach image")}
                if(body.isBlank()&&replyTo.isEmpty())VoiceCapture(model,cid,busy) else FilledIconButton(onClick={if(body.isNotBlank()&&!queueing){queueing=true;model.send(cid,body,replyTo){input=TextFieldValue("");replyTo=""}.invokeOnCompletion {queueing=false}}},enabled=body.isNotBlank()&&!queueing&&draftReady,modifier=Modifier.size(48.dp).graphicsLayer {scaleX=sendScale;scaleY=sendScale}) {Icon(Icons.AutoMirrored.Outlined.Send,"Send message")}
            }
        }
    }

    }
    if(body.length>=3800) Text("${body.length} / 4000",style=MaterialTheme.typography.labelSmall)
    if(emojiPicker)EmojiPicker(onEmoji={emoji->
        val start=minOf(input.selection.start,input.selection.end);val end=maxOf(input.selection.start,input.selection.end)
        val text=body.replaceRange(start,end,emoji)
        if(text.length<=4000)updateInput(TextFieldValue(text,TextRange(start+emoji.length)))
    },onDismiss={emojiPicker=false})
    if(appearance)AlertDialog(onDismissRequest={appearance=false},title={Text("Chat wallpaper")},text={Column {listOf("Ice","Lilac","Mint","Sand").forEach {choice->TextButton(onClick={model.action {model.repository.setPalette(cid,choice);palette=choice;appearance=false}}) {Text(if(choice==palette)"✓ $choice" else choice)}}}},confirmButton={TextButton(onClick={appearance=false}) {Text("Done")}})
    actionMessage?.let {m->AlertDialog(onDismissRequest={actionMessage=null},title={Text("Message")},text={Column(Modifier.verticalScroll(rememberScrollState())) {
        if(m.deletedAt.isEmpty()) {
            if(m.status=="SENT") {
                Row(Modifier.horizontalScroll(rememberScrollState())) {listOf("👍","❤️","😂","😮","😢","🙏").forEach {emoji->TextButton(onClick={actionMessage=null;model.action {val old=org.json.JSONObject(m.reactions).optString(m.account);model.repository.messageAction(m.id,"react",if(old==emoji)"" else emoji)}}) {Text(emoji)}}}
                TextButton(onClick={replyTo=m.id;model.repository.saveDraft(cid,body,replyTo);actionMessage=null}) {Text("Reply")}
                TextButton(onClick={actionMessage=null;model.action {model.repository.messageAction(m.id,"star",(!m.starred).toString())}}) {Text(if(m.starred)"Unstar" else "Star")}
            }
            TextButton(onClick={clipboard.setText(AnnotatedString(m.body));actionMessage=null}) {Text("Copy text")}
            TextButton(onClick={actionMessage=null;context.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT,m.body),"Share message"))}) {Text("Share text")}
            val age=runCatching {java.time.Duration.between(java.time.Instant.parse(m.serverTime),java.time.Instant.now()).seconds}.getOrDefault(Long.MAX_VALUE)
            if(m.sender==m.account&&m.status=="SENT"&&m.mediaId.isEmpty()&&age<=900)TextButton(onClick={editMessage=m;editBody=m.body;actionMessage=null}) {Text("Edit")}
        }
        if(m.status=="SENT")TextButton(onClick={deleteMessage=m;actionMessage=null}) {Text("Delete")}
        Text("${if(m.sender==m.account)"You" else chat?.name.orEmpty()} · ${java.time.Instant.ofEpochMilli(m.localTime).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))}",style=MaterialTheme.typography.bodySmall)
    }},confirmButton={TextButton(onClick={actionMessage=null}) {Text("Done")}})}
    editMessage?.let {m->AlertDialog(onDismissRequest={editMessage=null},title={Text("Edit message")},text={OutlinedTextField(editBody,{if(it.length<=4000)editBody=it},keyboardOptions=KeyboardOptions(capitalization=KeyboardCapitalization.Sentences),modifier=Modifier.fillMaxWidth())},confirmButton={TextButton(onClick={model.action {model.repository.messageAction(m.id,"edit",editBody);editMessage=null}},enabled=!busy&&editBody.isNotBlank()) {Text("Save")}},dismissButton={TextButton(onClick={editMessage=null}) {Text("Cancel")}})}
    deleteMessage?.let {m->AlertDialog(onDismissRequest={deleteMessage=null},title={Text("Delete message?")},text={Column {
        Text("Delete for me hides this message on your devices. The other person keeps their copy.")
        val age=runCatching {java.time.Duration.between(java.time.Instant.parse(m.serverTime),java.time.Instant.now()).seconds}.getOrDefault(Long.MAX_VALUE)
        if(m.sender==m.account&&age<=172800&&m.deletedAt.isEmpty())TextButton(onClick={model.action {model.repository.messageAction(m.id,"delete");deleteMessage=null}},enabled=!busy) {Text("Delete for everyone")}
    }},confirmButton={TextButton(onClick={model.action {model.repository.messageAction(m.id,"hide");deleteMessage=null}},enabled=!busy) {Text("Delete for me")}},dismissButton={TextButton(onClick={deleteMessage=null}) {Text("Cancel")}})}
    if(starsOpen)AlertDialog(onDismissRequest={starsOpen=false},title={Text("Starred messages")},text={Column(Modifier.verticalScroll(rememberScrollState())) {
        Text("Downloaded starred messages in this chat",style=MaterialTheme.typography.labelSmall)
        val stars=messages.filter {it.starred&&it.deletedAt.isEmpty()}
        if(stars.isEmpty())Text("No downloaded starred messages.")
        stars.forEach {m->TextButton(onClick={starsOpen=false;model.focusMessage.value=m.id}) {Text(m.body,maxLines=3,overflow=TextOverflow.Ellipsis)}}
    }},confirmButton={TextButton(onClick={starsOpen=false}) {Text("Done")}})
    if(details&&chat!=null) AlertDialog(onDismissRequest={details=false},title={Text(chat.name)},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Avatar(chat.name,chat.avatarId,model.backend);Text("@${chat.username}")
        TextButton(onClick={model.action {model.repository.saveContact(chat.peer,true)}},enabled=!busy) {Text("Save contact")}
        Text("Downloaded shared images",style=MaterialTheme.typography.titleMedium)
        messages.filter {it.mediaId.isNotEmpty()&&it.mediaKind!="voice"}.forEach {m->ImagePreview(model,m)}
        if(messages.none {it.mediaId.isNotEmpty()&&it.mediaKind!="voice"})Text("No downloaded shared images.")
    }},confirmButton={TextButton(onClick={details=false}) {Text("Done")}})
    if(report&&chat!=null) AlertDialog(onDismissRequest={report=false},title={Text("Report contact")},text={Column {Text("Only your explanation is included. No messages are attached.");OutlinedTextField(reason,{reason=it.take(1000)},label={Text("Reason")})}},confirmButton={TextButton(onClick={report=false;model.report(chat.peer,reason)},enabled=reason.isNotBlank()) {Text("Submit report")}},dismissButton={TextButton(onClick={report=false}) {Text("Cancel")}})
    prepared?.let {image->AlertDialog(onDismissRequest={image.file.delete();prepared=null},title={Text("Send image")},text={Column {
        coil.compose.AsyncImage(image.file,"Image preview",Modifier.fillMaxWidth().heightIn(max=260.dp).aspectRatio(image.width.toFloat()/image.height))
        OutlinedTextField(input,{if(it.text.length<=4000)updateInput(it)},keyboardOptions=KeyboardOptions(capitalization=KeyboardCapitalization.Sentences,keyboardType=KeyboardType.Text),label={Text("Caption (optional)")},modifier=Modifier.fillMaxWidth())
    }},confirmButton={TextButton(onClick={model.action {model.repository.sendImage(cid,image,body);prepared=null;input=TextFieldValue("")}},enabled=!busy) {Text("Send")}},dismissButton={TextButton(onClick={image.file.delete();prepared=null}) {Text("Cancel")}})}
    if(confirmDelete&&chat!=null) AlertDialog(onDismissRequest={confirmDelete=false},title={Text("Delete your chat history?")},text={Text("This hides existing history for your account on every device. The other person keeps their copy. Unsent local copies will be removed; messages already sending may still finish.")},confirmButton={TextButton(onClick={confirmDelete=false;model.updateChat(chat,delete=true)}) {Text("Delete for me")}},dismissButton={TextButton(onClick={confirmDelete=false}) {Text("Cancel")}})
}
@OptIn(ExperimentalFoundationApi::class)
@Composable internal fun MessageBubble(message:Message,chat:Chat?,model:AppViewModel,onRetry:()->Unit,highlight:Boolean,onLongClick:()->Unit={},quoted:Message?=null,onQuoteClick:()->Unit={}) {
    val mine=message.sender==message.account
    var expanded by remember(message.id) {mutableStateOf(false)}
    var truncated by remember(message.id) {mutableStateOf(false)}
    val label=com.pritam.liquidchat.data.Delivery.label(message.status,message.seq,chat?.peerDelivered ?: 0,chat?.peerRead ?: 0)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
    val bubbleWidth=maxWidth*.86f
    Row(Modifier.fillMaxWidth(),horizontalArrangement=if(mine)Arrangement.End else Arrangement.Start) {
        Surface(shape=androidx.compose.foundation.shape.RoundedCornerShape(21.dp),color=if(highlight)MaterialTheme.colorScheme.tertiaryContainer else if(LocalGlassDark.current) {if(mine)Color(0xFF204F65) else Color(0xFF263C4A)} else if(mine)Color(0xFFD6F1FD) else Color(0xFFF8FCFF),border=androidx.compose.foundation.BorderStroke(1.dp,Color.White.copy(alpha=if(LocalGlassDark.current).16f else .8f)),shadowElevation=2.dp,modifier=Modifier.widthIn(max=bubbleWidth).glassSource().combinedClickable(onClick={},onLongClick=onLongClick)) {
            Column(Modifier.padding(horizontal=11.dp,vertical=9.dp)) {
                if(message.replyTo.isNotEmpty()&&message.deletedAt.isEmpty()) Surface(shape=Glass.compact,color=MaterialTheme.colorScheme.primary.copy(alpha=.1f),modifier=Modifier.fillMaxWidth().clickable(onClick=onQuoteClick)) {
                    Column(Modifier.padding(8.dp)) {Text("Reply",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary);Text(quoted?.takeIf {it.deletedAt.isEmpty()}?.body ?: "Original message unavailable",maxLines=2,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodySmall)}
                }
                if(message.mediaId.isNotEmpty()||message.localImage.isNotEmpty()) {
                    if(message.mediaKind=="voice")VoicePlayer(model,message) else ImagePreview(model,message)
                    Spacer(Modifier.height(8.dp))
                }
                if(message.mediaKind!="voice")Text(message.body,style=MaterialTheme.typography.bodyLarge,maxLines=if(expanded)Int.MAX_VALUE else 12,overflow=TextOverflow.Ellipsis,onTextLayout={truncated=it.hasVisualOverflow})
                if(truncated||expanded) TextButton(onClick={expanded=!expanded}) {Text(if(expanded)"Show less" else "Read more")}
                val time=remember(message.localTime) {java.time.Instant.ofEpochMilli(message.localTime).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}
                Row(Modifier.align(Alignment.End),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    if(message.starred)Icon(Icons.Outlined.Star,"Starred",Modifier.size(12.dp))
                    if(message.editedAt.isNotEmpty()&&message.deletedAt.isEmpty())Text("edited",style=MaterialTheme.typography.labelSmall)
                    Text(time,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    if(mine)Receipt(message.status,message.seq,chat?.peerDelivered ?: 0,chat?.peerRead ?: 0)
                }
                if(message.reactions!="{}"&&message.deletedAt.isEmpty()) {
                    val reactions=remember(message.reactions) {runCatching {val j=org.json.JSONObject(message.reactions);j.keys().asSequence().map {j.getString(it)}.groupingBy {it}.eachCount()}.getOrDefault(emptyMap())}
                    Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {reactions.forEach {(emoji,count)->Surface(shape=Glass.capsule,color=MaterialTheme.colorScheme.primaryContainer) {Text("$emoji $count",Modifier.padding(horizontal=8.dp,vertical=3.dp),style=MaterialTheme.typography.labelMedium)}}}
                }
                if(message.status=="FAILED") {Text(message.error,style=MaterialTheme.typography.bodySmall);TextButton(onClick=onRetry) {Text("Retry")}}
            }
        }
    }
    }
}
@Composable fun SettingsScreen(model:AppViewModel,busy:Boolean) {
    var section by rememberSaveable {mutableStateOf<String?>(null)}
    var unavailable by remember {mutableStateOf<String?>(null)}
    if(section==null) {
        SettingsHome(model) {title->
            if(title in listOf("Devices","Data and Storage","Language","Chat Folders")) unavailable=title else section=title
        }
        unavailable?.let {title->AlertDialog(onDismissRequest={unavailable=null},title={Text(title)},text={Text(when(title){"Language"->"English is the current app language.";"Chat Folders"->"Use All, Favorite, Archive and Unread in the Chats menu.";"Data and Storage"->"Shared images are loaded on demand. Automatic downloads are not enabled.";else->"Device management is not available in this version yet."})},confirmButton={TextButton(onClick={unavailable=null}){Text("OK")}})}
        return
    }
    BackHandler {section=null}

    val profile by model.profile.collectAsStateWithLifecycle()
    val settings by model.repository.privateSettings.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {model.action {model.repository.loadSettings()}}
    val unsent by model.unsent.collectAsStateWithLifecycle()
    val display by model.display.collectAsStateWithLifecycle()
    val context=LocalContext.current
    val photoPicker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {uri->if(uri!=null)model.action {
        val id=model.backend.session.value?.userId ?: return@action
        val image=Images.prepare(context,uri,id)
        try {val uploaded=model.backend.uploadImage(image.file,null);model.backend.rpc("set_avatar",org.json.JSONObject().put("p_media",uploaded.getString("id")));model.loadProfile()} finally {image.file.delete()}
    }}
    var confirm by remember {mutableStateOf(false)}
    var edit by remember {mutableStateOf(false)}
    var deleteAccount by remember {mutableStateOf(false)}
    var password by remember {mutableStateOf("")}
    if(edit) {BackHandler {edit=false};Column {TextButton(onClick={edit=false}) {Text("Back to Settings")};ProfileScreen(model,busy)};return}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {IconButton(onClick={section=null}){Icon(Icons.AutoMirrored.Outlined.ArrowBack,"Back to Settings")};Text(section.orEmpty(),style=MaterialTheme.typography.headlineSmall)}
        if(section=="Account") GlassCard(Modifier.fillMaxWidth()) {
            val avatar=profile?.optString("avatar_id")
            if(!avatar.isNullOrEmpty()&&avatar!="null") ProtectedImage(model.backend,avatar,description="Your profile photo",modifier=Modifier.size(72.dp).clip(CircleShape),scale=androidx.compose.ui.layout.ContentScale.Crop)
            else Avatar(profile?.optString("display_name").orEmpty(),self=true)
            Spacer(Modifier.height(12.dp))
            Text(profile?.optString("display_name").orEmpty(),style=MaterialTheme.typography.titleLarge)
            Text("@${profile?.optString("username").orEmpty()}");Text(profile?.optString("bio").orEmpty())
            Row {TextButton(onClick={photoPicker.launch("image/*")},enabled=!busy) {Text("Edit photo")};TextButton(onClick={model.action {model.backend.rpc("set_avatar");model.loadProfile()}},enabled=!busy) {Text("Remove photo")}}
            TextButton(onClick={edit=true}) {Text("Edit profile")}
        }
        if(section=="Privacy & Security") GlassCard(Modifier.fillMaxWidth()) {Text("Your privacy matters",fontWeight=FontWeight.SemiBold);Text("Chat access is restricted to participants. This version does not use end-to-end encryption.")}
        if(section in listOf("Chat Settings","Power Saving")) GlassCard(Modifier.fillMaxWidth()) {
            Text("Appearance",style=MaterialTheme.typography.titleMedium)
            Row(Modifier.horizontalScroll(rememberScrollState())) {listOf("System","Light","Dark").forEach {value->FilterChip(selected=display.appearance==value,onClick={model.setDisplay(display.copy(appearance=value))},label={Text(value)})}}
            Text("Animation level",style=MaterialTheme.typography.labelLarge)
            Row(Modifier.horizontalScroll(rememberScrollState())) {listOf("Normal","Balanced","Extra").forEach {value->FilterChip(selected=display.animation==value,onClick={model.setDisplay(display.copy(animation=value))},enabled=!display.reducedMotion,label={Text(value)})}}
            Row(verticalAlignment=Alignment.CenterVertically) {Text("Reduced motion",Modifier.weight(1f));Switch(display.reducedMotion,{model.setDisplay(display.copy(reducedMotion=it))})}
            Row(verticalAlignment=Alignment.CenterVertically) {Text("Reduced transparency",Modifier.weight(1f));Switch(display.reducedTransparency,{model.setDisplay(display.copy(reducedTransparency=it))})}
        }
        if(section in listOf("Privacy & Security","Notifications")) GlassCard(Modifier.fillMaxWidth()) {
            Text("Privacy & notifications",style=MaterialTheme.typography.titleMedium)
            if(!settings.has("show_presence")) {Text("Connect to load your privacy settings.");TextButton(onClick={model.action {model.repository.loadSettings()}},enabled=!busy) {Text("Retry")}}
            listOf("show_presence" to "Online & last seen","read_receipts" to "Read receipts","notification_previews" to "Notification previews").filter {if(section=="Notifications")it.first=="notification_previews" else it.first!="notification_previews"}.forEach { (key,label)->
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text(label,Modifier.weight(1f))
                    Switch(checked=settings.optBoolean(key,true),enabled=!busy&&settings.has(key),onCheckedChange={checked->model.action {
                        model.repository.settings(if(key=="show_presence")checked else settings.optBoolean("show_presence",true),if(key=="read_receipts")checked else settings.optBoolean("read_receipts",true),if(key=="notification_previews")checked else settings.optBoolean("notification_previews",true))
                    }})
                }
            }
        }
        if(section=="Account") OutlinedButton(onClick={confirm=true},enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text("Sign out")}
        if(section=="Account") TextButton(onClick={deleteAccount=true},enabled=!busy) {Text("Delete account",color=MaterialTheme.colorScheme.error)}
        Text("Developed by Pritam Pal\n© 2026 Liquid Chat",style=MaterialTheme.typography.bodySmall)
    }
    if(deleteAccount) AlertDialog(onDismissRequest={deleteAccount=false;password=""},title={Text("Permanently delete account?")},text={Column(Modifier.verticalScroll(rememberScrollState())) {
        Text("Your profile, sessions, device tokens and owned media will be removed. Sent text remains in the other person's history under Deleted account. $unsent unsent messages will be discarded.")
        Text("Enter your password to verify. For Google accounts, sign in with Google again, then return here and confirm within five minutes.")
        PasswordField(password,{password=it},"Password (email accounts)")
        TextButton(onClick={model.action {val url=model.backend.googleUrl();context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse(url)))}}) {Text("Verify with Google")}
    }},confirmButton={TextButton(onClick={model.deleteAccount(password);password="";deleteAccount=false},enabled=!busy) {Text("Delete permanently")}},dismissButton={TextButton(onClick={deleteAccount=false;password=""}) {Text("Cancel")}})
    if(confirm) AlertDialog(onDismissRequest={confirm=false},title={Text("Sign out?")},text={Text(if(unsent>0)"$unsent unsent messages will be discarded. Saved local history, drafts and jobs will be removed from this device." else "Saved local history and account data will be removed from this device.")},confirmButton={TextButton(onClick={confirm=false;model.finishLogout()}) {Text("Sign out")}},dismissButton={TextButton(onClick={confirm=false}) {Text("Cancel")}})
}

@Composable private fun ImagePreview(model:AppViewModel,message:Message) {
    var open by remember(message.id) {mutableStateOf(false)}
    ProtectedImage(model.backend,message.mediaId,message.localImage,"Shared photo",Modifier.widthIn(max=280.dp).fillMaxWidth().aspectRatio((if(message.imageHeight>0&&message.imageWidth>0)message.imageWidth.toFloat()/message.imageHeight else 1f).coerceIn(.5f,2f)).clickable {open=true})
    if(open) androidx.compose.ui.window.Dialog(onDismissRequest={open=false},properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
            Column(Modifier.statusBarsPadding().navigationBarsPadding()) {
                TextButton(onClick={open=false}) {Text("Close image")}
                ProtectedImage(model.backend,message.mediaId,message.localImage,"Full-size shared photo",Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}
