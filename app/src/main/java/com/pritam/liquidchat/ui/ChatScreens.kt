package com.pritam.liquidchat.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.ui.platform.LocalContext
import com.pritam.liquidchat.data.Images
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
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
    val homeDisplay=LocalDisplayOptions.current
    Surface(
        shape=Glass.card,
        color=if(LocalGlassDark.current)Color(0xFF20384D).copy(alpha=.62f) else Color.White.copy(alpha=.56f),
        border=androidx.compose.foundation.BorderStroke(1.dp,Color.White.copy(alpha=if(LocalGlassDark.current).17f else .72f)),
        modifier=Modifier.fillMaxWidth().glassSource().combinedClickable(onClick=onClick,onLongClick=onLongClick)
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical=homeDisplay.homeRowPadding.dp,horizontal=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Avatar(chat.name,chat.avatarId,backend,size=homeDisplay.homeAvatarSize.dp)
            Column(Modifier.weight(1f)) {
                Text(chat.name,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium.copy(fontSize=homeDisplay.homeChatNameSize.sp),maxLines=1,overflow=TextOverflow.Ellipsis)
                Text(inboxSnippet(chat.preview,"Say hello to @${chat.username}",homeDisplay.hideHomePreviews),style=MaterialTheme.typography.bodyMedium.copy(fontSize=homeDisplay.homeChatPreviewSize.sp),maxLines=1,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment=Alignment.End,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                if(chat.lastAt.isNotEmpty()) Text(remember(chat.lastAt) {runCatching {java.time.Instant.parse(chat.lastAt).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}.getOrDefault("")},style=MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    if(homeDisplay.showPinBadges && chat.pinnedAt>0)Icon(Icons.Outlined.PushPin,"Pinned",Modifier.size(14.dp))
                    if(chat.favorite)Icon(Icons.Outlined.Star,"Favorite",Modifier.size(14.dp))
                    if(chat.muted)Icon(Icons.Outlined.NotificationsOff,"Muted",Modifier.size(14.dp))
                }
                if(chat.unread>0) Badge {Text(if(chat.unread>99)"99+" else chat.unread.toString())}
                else if(chat.manualUnread)Badge(Modifier.semantics {contentDescription="Marked unread"})
            }
        }
    }
}
@OptIn(ExperimentalFoundationApi::class,ExperimentalLayoutApi::class)
@Composable fun ChatsScreen(model:AppViewModel,chats:List<Chat>,onContacts:()->Unit={},onProfile:()->Unit={}) {
    var selectedChat by remember {mutableStateOf<Chat?>(null)}
    var deleteChat by remember {mutableStateOf<Chat?>(null)}
    var query by rememberSaveable {mutableStateOf("")};var filter by rememberSaveable {mutableIntStateOf(0)}
    var menu by remember {mutableStateOf(false)};var stories by remember {mutableStateOf(false)};var ownStories by remember {mutableStateOf(false)}
    var selectedStoryId by remember {mutableStateOf<String?>(null)}
    var selectedMyStoryId by remember {mutableStateOf<String?>(null)}
    var storyRefreshKey by remember {mutableIntStateOf(0)}
    var addStoryImmediately by remember {mutableStateOf(false)}
    val profile by model.profile.collectAsStateWithLifecycle()
    val connection by model.connection.collectAsStateWithLifecycle()
    val results by model.searchResults.collectAsStateWithLifecycle()
    val homePrivacy by model.repository.privateSettings.collectAsStateWithLifecycle()
    val signedInAccount by model.session.collectAsStateWithLifecycle()
    val homeOwner=signedInAccount?.userId
    val context=LocalContext.current
    var homePhotoRevision by remember(homeOwner) {mutableIntStateOf(0)}
    val homeForeground by model.repository.foreground.collectAsStateWithLifecycle()
    var homePickerAccount by remember {mutableStateOf<String?>(null)}
    var homeVideoPickerAccount by remember {mutableStateOf<String?>(null)}
    val homeVideo=remember(homeOwner,homePhotoRevision) {
        homeOwner?.let {owner->runCatching {
            com.pritam.liquidchat.data.PrivateVideoWallpaper.location(context,owner,
                com.pritam.liquidchat.data.PersonalWallpaper.HOME_CID)
        }.getOrNull()}
    }
    val homePhoto=remember(homeOwner,homePhotoRevision) {
        homeOwner?.let {owner->
            runCatching {
                com.pritam.liquidchat.data.PersonalWallpaper.location(context,owner,
                    com.pritam.liquidchat.data.PersonalWallpaper.HOME_CID)
            }.getOrNull()
        }
    }
    val homePhotoPicker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {uri->
        val owner=homePickerAccount
        homePickerAccount=null
        if(uri!=null && owner!=null && model.session.value?.userId==owner) model.action {
            val saved=com.pritam.liquidchat.data.PersonalWallpaper.save(context,owner,
                com.pritam.liquidchat.data.PersonalWallpaper.HOME_CID,uri)
            if(model.session.value?.userId==owner)homePhotoRevision++
            else saved.delete()
        }
    }
    val inboxDisplay=LocalDisplayOptions.current
    LaunchedEffect(inboxDisplay.hideHomeSearch) {
        // Disabling search must not leave an invisible query filtering the inbox.
        if(inboxDisplay.hideHomeSearch && query.isNotEmpty()) {
            query=""
            model.search("")
        }
    }
    LaunchedEffect(inboxDisplay.hideHomeFilters) {if(inboxDisplay.hideHomeFilters)filter=0}
    val activeFilter=if(inboxDisplay.hideHomeFilters || (inboxDisplay.hideArchiveAccess && filter==2))0 else filter
    LaunchedEffect(menu,signedInAccount?.userId) {
        if(menu && signedInAccount!=null) {
            try {model.repository.loadSettings()}
            catch(e:kotlinx.coroutines.CancellationException) {throw e}
            catch(_:Exception) { /* Privacy controls remain disabled while unavailable. */ }
        }
    }
    val homeVideoPicker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {uri->
        val owner=homeVideoPickerAccount
        homeVideoPickerAccount=null
        if(uri!=null && owner!=null && model.session.value?.userId==owner)model.action {
            val saved=com.pritam.liquidchat.data.PrivateVideoWallpaper.save(context,owner,
                com.pritam.liquidchat.data.PersonalWallpaper.HOME_CID,uri)
            if(model.session.value?.userId==owner)homePhotoRevision++ else saved.delete()
        }
    }
    Box(Modifier.fillMaxSize()) {
    if(homeVideo?.isFile==true) {
        LocalVideoWallpaper(homeVideo,homeForeground,homePhotoRevision,Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(
            if(LocalGlassDark.current)Color.Black.copy(alpha=wallpaperDimAlpha(.56f,inboxDisplay.homeWallpaperDim))
            else Color.White.copy(alpha=wallpaperDimAlpha(.45f,inboxDisplay.homeWallpaperDim))))
    } else if(homePhoto?.isFile==true) {
        val imageRequest=remember(homePhoto,homePhotoRevision) {
            coil.request.ImageRequest.Builder(context).data(homePhoto)
                .memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                .diskCachePolicy(coil.request.CachePolicy.DISABLED).build()
        }
        coil.compose.AsyncImage(model=imageRequest,contentDescription=null,
            contentScale=androidx.compose.ui.layout.ContentScale.Crop,
            modifier=Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(
            if(LocalGlassDark.current)Color.Black.copy(alpha=wallpaperDimAlpha(.56f,inboxDisplay.homeWallpaperDim))
            else Color.White.copy(alpha=wallpaperDimAlpha(.45f,inboxDisplay.homeWallpaperDim))))
    }
    Column(Modifier.fillMaxSize().padding(horizontal=12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        GlassHeader(if(inboxDisplay.showProfileAsTitle)profile?.optString("display_name").orEmpty().ifBlank {"Liquid Chat"} else "Liquid Chat",brand=true) {
            IconButton(onClick=onContacts) {Icon(Icons.Outlined.People,"Open contacts")}
            IconButton(onClick=onProfile) {Icon(Icons.Outlined.AccountCircle,"Open profile")}
            Box {IconButton(onClick={menu=true}) {Icon(Icons.Outlined.MoreVert,"Chat options")}
                LiquidDropdownMenu(menu,{menu=false}) {
                    // The four filters already have visible, accessible chips below.
                    // Keep overflow for an action rather than duplicating navigation.
                    DropdownMenuItem(text={Text("Refresh conversations")},onClick={model.repository.refresh();menu=false})
                    if(inboxDisplay.hideStoriesCarousel) {
                        DropdownMenuItem(text={Text("View stories")},onClick={stories=true;menu=false})
                        DropdownMenuItem(text={Text("Add my story")},onClick={addStoryImmediately=true;ownStories=true;menu=false})
                    }
                    DropdownMenuItem(text={Text("Choose home wallpaper")},onClick={
                        menu=false
                        homePickerAccount=homeOwner
                        homePhotoPicker.launch("image/*")
                    },enabled=homeOwner!=null)
                    DropdownMenuItem(text={Text("Choose home video wallpaper")},onClick={
                        menu=false
                        homeVideoPickerAccount=homeOwner
                        homeVideoPicker.launch("video/mp4")
                    },enabled=homeOwner!=null)
                    if((homePhoto?.isFile==true || homeVideo?.isFile==true) && homeOwner!=null)
                        DropdownMenuItem(text={Text("Remove home wallpaper")},onClick={
                            menu=false
                            val oldOwner=homeOwner
                            model.action {
                                if(model.session.value?.userId==oldOwner) {
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        com.pritam.liquidchat.data.PrivateVideoWallpaper.clear(
                                            context,oldOwner,com.pritam.liquidchat.data.PersonalWallpaper.HOME_CID)
                                        val path=com.pritam.liquidchat.data.PersonalWallpaper.location(
                                            context,oldOwner,com.pritam.liquidchat.data.PersonalWallpaper.HOME_CID)
                                        require(!path.exists() || path.delete()) {"Unable to remove wallpaper."}
                                    }
                                    homePhotoRevision++
                                }
                            }
                        })
                    val privacyLoaded=homePrivacy.has("show_presence") && homePrivacy.has("read_receipts") && homePrivacy.has("notification_previews")
                    val ghost=ghostModeActive(homePrivacy.optBoolean("show_presence",true),homePrivacy.optBoolean("read_receipts",true))
                    DropdownMenuItem(text={Text(if(ghost)"Disable Ghost Mode" else "Enable Ghost Mode")},
                        enabled=privacyLoaded,onClick={
                            menu=false
                            model.action {
                                val next=ghostModeSettings(!ghost)
                                model.repository.settings(next.first,next.second,homePrivacy.optBoolean("notification_previews",true))
                            }
                        })
                }
            }
        }
        if(!inboxDisplay.hideStoriesCarousel) HomeStoriesRow(
            model,onMine={addStoryImmediately=true;ownStories=true},onStories={stories=true},
            refreshKey=storyRefreshKey,
            onOpenStory={id,mine->
                if(mine){selectedMyStoryId=id;ownStories=true}
                else {selectedStoryId=id;stories=true}
            }
        )
        if(!inboxDisplay.hideHomeSearch) GlassSearch(query,{query=it;model.search(it)},"Search Chats")
        if(!inboxDisplay.hideHomeFilters) FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            listOf(0 to "All",1 to "Favorite",2 to "Archive",3 to "Unread")
                .filter {it.first!=2 || !inboxDisplay.hideArchiveAccess}
                .forEach { (index,label)->FilterChip(selected=activeFilter==index,onClick={filter=index},label={Text(label)},shape=Glass.capsule)}
        }
        if(connection!="Connected"&&connection!="Online") Text(connection,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        val visible=chats.filter {when(activeFilter){1->it.favorite&&!it.archived;2->it.archived;3->(it.unread>0||it.manualUnread)&&!it.archived;else->!it.archived}}
            .filter {query.isBlank()||it.name.contains(query,true)||it.username.contains(query,true)}
        Row(Modifier.fillMaxWidth().padding(horizontal=7.dp,vertical=1.dp),
            verticalAlignment=Alignment.CenterVertically) {
            Text(when(activeFilter){1->"Favorites";2->"Archived chats";3->"Unread chats";else->"Conversations"},
                Modifier.weight(1f),style=MaterialTheme.typography.titleMedium,
                fontWeight=FontWeight.SemiBold)
            Text("${visible.size} shown",style=MaterialTheme.typography.labelMedium,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=132.dp)) {
            if(visible.isEmpty()&&query.isBlank()) item {GlassCard(Modifier.fillMaxWidth()) {Text("No conversations here yet.");Text("Open Contacts to find someone by username.",style=MaterialTheme.typography.bodyMedium)}}
            items(visible,key={"chat-${it.id}"}) {chat->
                // ChatRow already provides the full combined clickable surface.
                // Wrapping it in a second clickable creates overlapping semantics
                // and can trigger duplicate navigation / long-press actions.
                ChatRow(chat,model.backend,onClick={model.open(chat.id)},onLongClick={selectedChat=chat})
                if(inboxDisplay.showHomeDividers) HorizontalDivider(
                    modifier=Modifier.padding(start=78.dp),
                    color=MaterialTheme.colorScheme.outlineVariant)
            }
            if(query.isNotBlank()) {
                val visibleResults=results.filter {it.deletedAt.isEmpty()}
                item {Text("Downloaded message history",style=MaterialTheme.typography.labelLarge)}
                items(visibleResults,key={"result-${it.id}"}) {m->
                    val chat=chats.firstOrNull {it.id==m.conversation}
                    if(chat!=null) Surface(onClick={model.open(chat.id,m.id)},shape=Glass.compact,color=MaterialTheme.colorScheme.surface,modifier=Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {Text("${chat.name} · ${if(m.sender==m.account)"You" else chat.name}",style=MaterialTheme.typography.labelLarge);Text(m.body,maxLines=2,overflow=TextOverflow.Ellipsis)}
                    }
                }
                if(visibleResults.isEmpty()) item {Text("No downloaded messages match.",style=MaterialTheme.typography.bodyMedium)}
            }
        }
    }
    Column(Modifier.align(Alignment.BottomEnd).padding(end=17.dp,bottom=78.dp+WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
        if(!inboxDisplay.hidePhotoShortcut) GlassSurface(Modifier.size(48.dp),CircleShape) {IconButton(onClick=onContacts) {Icon(Icons.Outlined.PhotoCamera,"Choose a contact to share a photo")}}
        FloatingActionButton(onClick=onContacts,shape=CircleShape,containerColor=Color(0xFF00A6E8),contentColor=Color.White,modifier=Modifier.size(52.dp)) {Icon(Icons.Outlined.Add,"New chat")}
    }
    }
    selectedChat?.let {c->
        val previewFlow=remember(model,c.id) {model.repository.messages(c.id)}
        val previewMessages by previewFlow.collectAsStateWithLifecycle(initialValue=emptyList())
        LiquidActionSheet(title=c.name,onDismiss={selectedChat=null}) {
        Text("Conversation preview",style=MaterialTheme.typography.titleSmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(horizontal=12.dp))
        GlassCard(Modifier.fillMaxWidth(),Glass.compact) {
            val preview=previewMessages.filter {it.deletedAt.isEmpty()}.takeLast(3)
            if(preview.isEmpty()) Text("No downloaded messages to preview.",
                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            else preview.forEach {m->
                Text(if(m.sender==m.account)"You" else c.name,
                    style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
                Text(if(m.mediaKind=="voice")"Voice message" else if(m.mediaId.isNotEmpty() || m.localImage.isNotEmpty())"Photo message" else m.body,
                    maxLines=2,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodyMedium)
            }
        }
        GlassActionRow("Open conversation",Icons.Outlined.ChatBubbleOutline,
            onClick={selectedChat=null;model.open(c.id)})
        HorizontalDivider(Modifier.padding(vertical=8.dp))
        GlassActionRow(if(c.pinnedAt>0)"Unpin" else "Pin",Icons.Outlined.PushPin,
            onClick={selectedChat=null;model.action {model.repository.chatAction(c.id,"pin",c.pinnedAt==0L)}})
        GlassActionRow("Mark unread",Icons.Outlined.MarkChatUnread,
            onClick={selectedChat=null;model.action {model.repository.chatAction(c.id,"unread",true)}})
        GlassActionRow(if(c.favorite)"Remove favorite" else "Favorite",Icons.Outlined.StarOutline,
            onClick={selectedChat=null;model.updateChat(c,favorite=!c.favorite)})
        GlassActionRow(if(c.archived)"Unarchive" else "Archive",Icons.Outlined.Archive,
            onClick={selectedChat=null;model.updateChat(c,archived=!c.archived)})
        GlassActionRow(if(c.muted)"Unmute" else "Mute",Icons.Outlined.NotificationsOff,
            onClick={selectedChat=null;model.updateChat(c,muted=!c.muted)})
        HorizontalDivider(Modifier.padding(vertical=8.dp),color=MaterialTheme.colorScheme.outlineVariant)
        GlassActionRow("Delete history for me",Icons.Outlined.DeleteOutline,
            onClick={selectedChat=null;deleteChat=c},destructive=true)
    }}
    deleteChat?.let {c->LiquidAlertDialog(onDismissRequest={deleteChat=null},title={Text("Delete your chat history?")},text={Text("This removes your existing history on every device. The other person keeps their copy.")},confirmButton={TextButton(onClick={deleteChat=null;model.updateChat(c,delete=true)}) {Text("Delete for me")}},dismissButton={TextButton(onClick={deleteChat=null}) {Text("Cancel")}})}
    if(stories) MomentsDialog(model,mine=false,
        onDismiss={stories=false;selectedStoryId=null;storyRefreshKey++},
        initialStoryId=selectedStoryId)
    if(ownStories) MomentsDialog(model,mine=true,
        onDismiss={
            ownStories=false;selectedMyStoryId=null;addStoryImmediately=false;storyRefreshKey++
        },initialStoryId=selectedMyStoryId,openEditorInitially=addStoryImmediately)
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
        GlassHeader("Contacts") {
            IconButton(onClick={showFind=true}) {Icon(Icons.Outlined.PersonSearch,"Find friends")}
            IconButton(onClick={alphabetical=!alphabetical}) {Icon(Icons.Outlined.SortByAlpha,"Change contact sort order")}
        }
        GlassSearch(username,{username=it.lowercase().take(24)},"Search Contacts")
        GlassSurface(Modifier.fillMaxWidth(),Glass.compact) {
            Row(Modifier.fillMaxWidth().padding(horizontal=15.dp,vertical=12.dp),
                verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Saved contacts",style=MaterialTheme.typography.titleSmall)
                    Text(if(alphabetical)"Sorted A to Z" else "Sorted Z to A",
                        style=MaterialTheme.typography.bodySmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${filtered.size}",style=MaterialTheme.typography.titleLarge,
                    color=MaterialTheme.colorScheme.primary)
            }
        }
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
    if(showFind) LiquidAlertDialog(onDismissRequest={showFind=false},title={Text("Find Friends")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Find someone by their exact username.")
        OutlinedTextField(username,{username=it.lowercase().take(24)},label={Text("Username")},singleLine=true)
        Button(onClick={model.find(username)},enabled=!busy&&username.matches(Regex("[a-z0-9_]{3,24}"))) {Text("Find contact")}
        found?.takeIf {it.optString("username").equals(username,true)}?.let {p->
            Text(p.optString("display_name"),fontWeight=FontWeight.Bold);Text("@${p.optString("username")}")
            Row {TextButton(onClick={showFind=false;model.start(p.getString("id"))},enabled=!busy){Text("Start chat")};TextButton(onClick={model.action {model.repository.saveContact(p.getString("id"),true)}},enabled=!busy){Text("Save contact")}}
        }
    }},confirmButton={TextButton(onClick={showFind=false}){Text("Done")}})
}
/** Never offer text forwarding for deleted, failed, or media-only messages. */
internal fun canForwardTextMessage(message:Message,account:String?):Boolean =
    account!=null && message.account==account && message.status=="SENT" &&
    message.deletedAt.isEmpty() && message.mediaId.isEmpty() &&
    message.localImage.isEmpty() && message.mediaKind=="image" && message.body.isNotBlank()

/** Unknown server state must never produce contradictory block/unblock actions. */
internal fun blockActionLabel(ownedBlocked:Boolean?):String =
    when(ownedBlocked) { true->"Unblock contact"; false->"Block contact"; null->"Checking block status…" }

internal enum class SettingsPanel { APPEARANCE, POWER_SAVING, OTHER }
internal fun settingsPanelFor(section:String?):SettingsPanel = when(section) {
    "Chat Settings","Liquid Chat General Settings"->SettingsPanel.APPEARANCE
    "Power Saving"->SettingsPanel.POWER_SAVING
    else->SettingsPanel.OTHER
}

/** Display category order for the one-to-one message action sheet. */
internal fun messageActionCategories(message:Message):List<String> {
    val result=mutableListOf<String>()
    if(message.deletedAt.isEmpty()) {
        if(message.status=="SENT")result+="Quick reactions"
        result+="Message actions"
    }
    if((message.status=="SENT" && message.sender!=message.account && message.deletedAt.isEmpty()) ||
        message.status in listOf("SENT","QUEUED","FAILED"))result+="Safety"
    return result
}

internal fun toggleTextSelection(ids:Set<String>,id:String):Set<String> =
    if(id in ids) ids-id else ids+id

@Composable fun ConversationScreen(model:AppViewModel,cid:String,chats:List<Chat>,busy:Boolean) {
    val chat=chats.firstOrNull {it.id==cid}
    val allMessages by model.messages.collectAsStateWithLifecycle()
    val session by model.session.collectAsStateWithLifecycle()
    val messagesReady by model.messagesReady.collectAsStateWithLifecycle()
    val messages=remember(allMessages,cid,session?.userId) {allMessages.filter {it.conversation==cid&&it.account==session?.userId}}
    val scheduledFlow=remember(model,cid) {model.repository.scheduledTexts(cid)}
    val scheduledTexts by scheduledFlow.collectAsStateWithLifecycle(initialValue=emptyList())
    val foreground by model.repository.foreground.collectAsStateWithLifecycle()
    val focus by model.focusMessage.collectAsStateWithLifecycle()
    val contact by model.repository.contactState.collectAsStateWithLifecycle()
    var input by remember(cid) {mutableStateOf(TextFieldValue(""))}
    var draftReady by remember(cid) {mutableStateOf(false)}
    var queueing by remember(cid) {mutableStateOf(false)}
    var draftVersion by remember(cid) {mutableLongStateOf(0L)}
    var inputFocused by remember(cid) {mutableStateOf(false)}
    var emojiPicker by remember(cid) {mutableStateOf(false)}
    var palette by remember(cid) {mutableStateOf("Ice")}
    var appearance by remember(cid) {mutableStateOf(false)}
    var wallpaperRevision by remember(cid,session?.userId) {mutableIntStateOf(0)}
    var scheduleOpen by remember(cid) {mutableStateOf(false)}
    var schedulesOpen by remember(cid) {mutableStateOf(false)}
    var scheduleEditingId by remember(cid) {mutableStateOf<String?>(null)}
    var scheduleBody by remember(cid) {mutableStateOf("")}
    var scheduleReplyTo by remember(cid) {mutableStateOf("")}
    var scheduleDraftVersion by remember(cid) {mutableLongStateOf(0L)}
    var scheduleAt by remember(cid) {mutableLongStateOf(System.currentTimeMillis()+3_600_000L)}
    var searchOpen by remember(cid) {mutableStateOf(false)}
    var chatQuery by remember(cid) {mutableStateOf("")}
    var searchIndex by remember(cid) {mutableIntStateOf(0)}
    var searchSender by remember(cid) {mutableStateOf(SearchSender.ANY)}
    var searchMedia by remember(cid) {mutableStateOf(SearchMedia.ANY)}
    var searchPeriod by remember(cid) {mutableStateOf(SearchPeriod.ANY)}
    val chatResults=remember(messages,chatQuery,session?.userId,searchSender,searchMedia,searchPeriod) {
        filterMessageHistory(messages,chatQuery,session?.userId,searchSender,searchMedia,searchPeriod)
    }
    var actionMessage by remember(cid) {mutableStateOf<Message?>(null)}
    var forwardMessages by remember(cid,session?.userId) {mutableStateOf<List<Message>>(emptyList())}
    var selectedIds by remember(cid,session?.userId) {mutableStateOf<Set<String>>(emptySet())}
    var forwardSearch by remember(cid,session?.userId) {mutableStateOf("")}
    val clipboard=LocalClipboardManager.current
    var replyTo by remember(cid) {mutableStateOf("")}
    var editMessage by remember(cid) {mutableStateOf<Message?>(null)}
    var editBody by remember(cid) {mutableStateOf("")}
    var deleteMessage by remember(cid) {mutableStateOf<Message?>(null)}
    var starsOpen by remember(cid) {mutableStateOf(false)}
    val body=input.text
    fun updateInput(value:TextFieldValue) {val changed=input.text!=value.text;input=value;if(changed)draftVersion=model.repository.saveDraft(cid,value.text,replyTo)}
    LaunchedEffect(cid,session?.userId) {
        // Even without a live session, initialize an editable empty draft.
        // The account key re-runs this effect when a different user signs in,
        // preventing cross-account draft/wallpaper reuse.
        val draft=model.repository.draft(cid)
        input=TextFieldValue(draft?.body.orEmpty(),TextRange(draft?.body?.length ?: 0));replyTo=draft?.replyTo.orEmpty();draftVersion=draft?.updatedAt ?: 0;draftReady=true
        palette=model.repository.palette(cid)
    }
    val context=LocalContext.current
    var wallpaperPickerAccount by remember(cid) {mutableStateOf<String?>(null)}
    var videoWallpaperPickerAccount by remember(cid) {mutableStateOf<String?>(null)}
    val wallpaperPicker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {uri->
        val expected=wallpaperPickerAccount
        wallpaperPickerAccount=null
        if(uri!=null && expected!=null && model.session.value?.userId==expected)model.action {
            model.repository.setCustomWallpaper(cid,uri,expected)
            if(model.session.value?.userId==expected) {
                palette="Photo"
                wallpaperRevision++
                appearance=false
            }
        }
    }
    val videoWallpaperPicker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {uri->
        val expected=videoWallpaperPickerAccount
        videoWallpaperPickerAccount=null
        if(uri!=null && expected!=null && model.session.value?.userId==expected)model.action {
            model.repository.setCustomVideoWallpaper(cid,uri,expected)
            if(model.session.value?.userId==expected) {
                palette="Video"
                wallpaperRevision++
                appearance=false
            }
        }
    }
    var notificationsOpen by remember(cid) {mutableStateOf(false)}
    var customNotificationEnabled by remember(cid,session?.userId) {
        mutableStateOf(session?.userId?.let {
            com.pritam.liquidchat.data.ConversationNotificationChannels.enabled(context,it,cid)
        } ?: false)
    }
    var hideNotificationBody by remember(cid,session?.userId) {
        mutableStateOf(session?.userId?.let {
            com.pritam.liquidchat.data.ConversationNotificationChannels.hiddenPreview(context,it,cid)
        } ?: false)
    }
    var hideNotificationSender by remember(cid,session?.userId) {
        mutableStateOf(session?.userId?.let {
            com.pritam.liquidchat.data.ConversationNotificationChannels.hiddenSender(context,it,cid)
        } ?: false)
    }
    var notificationError by remember(cid) {mutableStateOf<String?>(null)}
    var requestedCallKind by remember(cid) {mutableStateOf("audio")}
    val callPermissionLauncher=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {granted->
        val needed=if(requestedCallKind=="video")listOf(android.Manifest.permission.RECORD_AUDIO,android.Manifest.permission.CAMERA) else listOf(android.Manifest.permission.RECORD_AUDIO)
        if(needed.all {granted[it]==true})chat?.let {model.calls.start(cid,it.peer,requestedCallKind)}
    }
    fun requestCall(kind:String) {
        val needed=if(kind=="video")arrayOf(android.Manifest.permission.RECORD_AUDIO,android.Manifest.permission.CAMERA) else arrayOf(android.Manifest.permission.RECORD_AUDIO)
        requestedCallKind=kind
        if(needed.all {androidx.core.content.ContextCompat.checkSelfPermission(context,it)==android.content.pm.PackageManager.PERMISSION_GRANTED})
            chat?.let {model.calls.start(cid,it.peer,kind)}
        else callPermissionLauncher.launch(needed)
    }
    var prepared by remember(cid) {mutableStateOf<Images.Prepared?>(null)}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {uri->if(uri!=null)model.action {
        val account=model.backend.session.value?.userId ?: return@action
        val image=Images.prepare(context,uri,account)
        if(model.backend.session.value?.userId==account)prepared=image else image.file.delete()
    }}
    var attachments by remember(cid) {mutableStateOf(false)}
    var cameraPath by rememberSaveable(cid) {mutableStateOf("")}
    var cameraAccount by rememberSaveable(cid) {mutableStateOf("")}
    val camera=rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {ok->
        val file=java.io.File(cameraPath)
        if(ok)model.action {
            try {
                if(model.backend.session.value?.userId==cameraAccount) {
                    val uri=androidx.core.content.FileProvider.getUriForFile(context,context.packageName+".files",file)
                    val image=Images.prepare(context,uri,cameraAccount)
                    if(model.backend.session.value?.userId==cameraAccount)prepared=image else image.file.delete()
                }
            } finally {file.delete();cameraPath=""}
        } else {file.delete();cameraPath=""}
    }
    val display=LocalDisplayOptions.current
    var chatAccent by remember(cid,session?.userId) {
        mutableStateOf(session?.userId?.let {MbConversationAccent.read(context,it,cid)} ?: "Follow")
    }
    val chatAccentColor=conversationInkColor(chatAccent,LocalGlassDark.current,
        MaterialTheme.colorScheme.primary)
    val sendScale by androidx.compose.animation.core.animateFloatAsState(if(body.isNotBlank())1.04f else 1f,animationSpec=if(display.reducedMotion)androidx.compose.animation.core.tween(0) else androidx.compose.animation.core.spring(stiffness=when(display.animation){"Normal"->400f;"Extra"->250f;else->650f}),label="Send readiness")
    var menu by remember {mutableStateOf(false)}
    var ownedBlock by remember(cid,session?.userId) {mutableStateOf<Boolean?>(null)}
    LaunchedEffect(menu,cid,session?.userId,chat?.peer) {
        val expected=session?.userId
        val peer=chat?.peer
        if(menu && expected!=null && peer!=null) {
            ownedBlock=null
            try {
                val value=model.repository.ownBlockState(peer)
                if(model.session.value?.userId==expected)ownedBlock=value
            } catch(e:kotlinx.coroutines.CancellationException) {throw e}
              catch(_:Exception) {ownedBlock=null}
        }
    }
    var confirmDelete by remember {mutableStateOf(false)}
    var report by remember {mutableStateOf(false)}
    var reportMessage by remember {mutableStateOf<Message?>(null)}
    var details by remember {mutableStateOf(false)}
    BackHandler {model.closeChat()}
    // Read acknowledgments are emitted only after a foreground conversation frame exists.
    LaunchedEffect(cid,messages.lastOrNull()?.id,foreground) {
        if(foreground) {withFrameNanos { };runCatching {model.repository.acknowledge(cid,true)}}
    }
    LaunchedEffect(cid,foreground,display.hideOutgoingTyping) {
        var lastSent=0L
        if(foreground && !display.hideOutgoingTyping) try {
            snapshotFlow {if(inputFocused&&draftReady)input.text else ""}.drop(1).collectLatest { text ->
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
    Box(Modifier.matchParentSize().glassSource(.5f).background(
        wallpaper(if(palette=="Photo" || palette=="Video")"Ice" else palette,LocalGlassDark.current))) {
        val owner=session?.userId
        if(palette=="Video" && owner!=null) {
            val videoFile=remember(owner,cid,wallpaperRevision) {
                runCatching {com.pritam.liquidchat.data.PrivateVideoWallpaper.location(context,owner,cid)}
                    .getOrNull()
            }
            if(videoFile?.isFile==true) {
                LocalVideoWallpaper(videoFile,foreground,wallpaperRevision,Modifier.matchParentSize())
                Box(Modifier.matchParentSize().background(
                    if(LocalGlassDark.current)Color.Black.copy(alpha=wallpaperDimAlpha(.46f,display.chatWallpaperDim))
                    else Color.White.copy(alpha=wallpaperDimAlpha(.30f,display.chatWallpaperDim))))
            }
        }
        if(palette=="Photo" && owner!=null) {
            val photoFile=remember(owner,cid,wallpaperRevision) {
                runCatching {com.pritam.liquidchat.data.PersonalWallpaper.location(context,owner,cid)}.getOrNull()
            }
            if(photoFile?.isFile==true) {
                val request=remember(photoFile,wallpaperRevision) {
                    coil.request.ImageRequest.Builder(context).data(photoFile)
                        .memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                        .diskCachePolicy(coil.request.CachePolicy.DISABLED).build()
                }
                coil.compose.AsyncImage(model=request,contentDescription=null,
                    contentScale=androidx.compose.ui.layout.ContentScale.Crop,
                    modifier=Modifier.matchParentSize())
                Box(Modifier.matchParentSize().background(
                    if(LocalGlassDark.current)Color.Black.copy(alpha=wallpaperDimAlpha(.46f,display.chatWallpaperDim))
                    else Color.White.copy(alpha=wallpaperDimAlpha(.30f,display.chatWallpaperDim))))
            }
        }
    }
    Column(Modifier.fillMaxSize().imePadding()) {
        // Visual-only presentation choice; keep the existing actions and call state intact.
        val headerIconTint=if(chatAccent!="Follow")chatAccentColor else when(display.chatHeaderIconColor) {
            "Accent"->MaterialTheme.colorScheme.primary
            "Text"->MaterialTheme.colorScheme.onSurface
            else->LocalContentColor.current
        }
        GlassSurface(Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=3.dp),Glass.capsule) {
        Row(Modifier.fillMaxWidth().padding(horizontal=4.dp,vertical=2.dp),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick=model::closeChat) {Icon(Icons.AutoMirrored.Outlined.ArrowBack,"Back to chats",tint=headerIconTint)}
            if(chat!=null) Box(Modifier.clickable {details=true}) {Avatar(chat.name,chat.avatarId,model.backend)}
            Column(Modifier.weight(1f).padding(horizontal=8.dp).clickable {details=true}) {Text(chat?.name ?: "Conversation",maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.titleMedium.copy(fontSize=display.chatHeaderNameSize.sp));Text(when {contact.optBoolean("online")->"Online";!contact.isNull("last_seen")&&contact.has("last_seen")->"Last seen "+runCatching {java.time.Instant.parse(contact.getString("last_seen")).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("dd MMM, HH:mm"))}.getOrDefault("");else->chat?.username?.let {"@$it"}.orEmpty()},style=MaterialTheme.typography.bodySmall,maxLines=1,overflow=TextOverflow.Ellipsis)}
            if(chat!=null && com.pritam.liquidchat.BuildConfig.ENABLE_EXPERIMENTAL_CALLS) {
                IconButton(onClick={requestCall("audio")}) {Icon(Icons.Outlined.Call,"Start voice call",tint=headerIconTint)}
                IconButton(onClick={requestCall("video")}) {Icon(Icons.Outlined.Videocam,"Start video call",tint=headerIconTint)}
            }
            Box {IconButton(onClick={menu=true}) {Icon(Icons.Outlined.MoreVert,"Conversation options",tint=headerIconTint)}
                if(menu)LiquidActionSheet(title=chat?.name ?: "Conversation",onDismiss={menu=false}) {
                    chat?.let { c->
                        GlassActionRow("Contact details & shared media",Icons.Outlined.Person,
                            onClick={menu=false;details=true})
                        GlassActionRow("Search conversation",Icons.Outlined.Search,
                            onClick={menu=false;searchOpen=true})
                        GlassActionRow("Schedule message",Icons.Outlined.Schedule,
                            onClick={
                                menu=false
                                scheduleEditingId=null
                                scheduleBody=body
                                scheduleReplyTo=replyTo
                                scheduleDraftVersion=draftVersion
                                scheduleAt=System.currentTimeMillis()+3_600_000L
                                scheduleOpen=true
                            })
                        GlassActionRow("Scheduled messages (${scheduledTexts.size})",Icons.Outlined.Event,
                            onClick={menu=false;schedulesOpen=true})
                        GlassActionRow("Starred messages",Icons.Outlined.StarOutline,
                            onClick={menu=false;starsOpen=true})
                        GlassActionRow("Custom notifications",Icons.Outlined.Notifications,
                            onClick={menu=false;notificationsOpen=true})
                        HorizontalDivider(Modifier.padding(vertical=8.dp))
                        GlassActionRow(if(c.pinnedAt>0)"Unpin" else "Pin",Icons.Outlined.PushPin,
                            onClick={menu=false;model.action {model.repository.chatAction(c.id,"pin",c.pinnedAt==0L)}})
                        GlassActionRow("Wallpaper",Icons.Outlined.Wallpaper,onClick={menu=false;appearance=true})
                        GlassActionRow(if(c.favorite)"Remove favorite" else "Favorite",Icons.Outlined.StarOutline,
                            onClick={menu=false;model.updateChat(c,favorite=!c.favorite)})
                        GlassActionRow(if(c.archived)"Unarchive" else "Archive",Icons.Outlined.Archive,
                            onClick={menu=false;model.updateChat(c,archived=!c.archived)})
                        GlassActionRow(if(c.muted)"Unmute" else "Mute",Icons.Outlined.NotificationsOff,
                            onClick={menu=false;model.updateChat(c,muted=!c.muted)})
                        HorizontalDivider(Modifier.padding(vertical=8.dp))
                        GlassActionRow(blockActionLabel(ownedBlock),Icons.Outlined.Block,
                            enabled=ownedBlock!=null && !busy,onClick={
                                val blocked=ownedBlock
                                if(blocked!=null) {menu=false;ownedBlock=null;model.block(c.peer,!blocked)}
                            })
                        GlassActionRow("Report contact",Icons.Outlined.Flag,
                            onClick={menu=false;reportMessage=null;report=true})
                        GlassActionRow("Delete history for me",Icons.Outlined.DeleteOutline,
                            destructive=true,onClick={menu=false;confirmDelete=true})
                    }
                }
            }
        }
        }
        if(searchOpen) GlassSurface(Modifier.fillMaxWidth().padding(horizontal=8.dp)) {
            Column(Modifier.padding(8.dp)) {
                OutlinedTextField(chatQuery,{chatQuery=it;searchIndex=0},placeholder={Text("Search this chat")},singleLine=true,modifier=Modifier.fillMaxWidth(),trailingIcon={IconButton(onClick={searchOpen=false;chatQuery=""}) {Icon(Icons.Outlined.Close,"Close search")}})
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    SearchFilterMenu("Sender",searchSender.label,SearchSender.entries.map {it.label}) {
                        searchSender=SearchSender.entries[it];searchIndex=0
                    }
                    SearchFilterMenu("Type",searchMedia.label,SearchMedia.entries.map {it.label}) {
                        searchMedia=SearchMedia.entries[it];searchIndex=0
                    }
                    SearchFilterMenu("Date",searchPeriod.label,SearchPeriod.entries.map {it.label}) {
                        searchPeriod=SearchPeriod.entries[it];searchIndex=0
                    }
                }
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("Downloaded messages only · ${chatResults.size} matches",Modifier.weight(1f),style=MaterialTheme.typography.labelSmall)
                    IconButton(onClick={if(chatResults.isNotEmpty()){searchIndex=(searchIndex-1+chatResults.size)%chatResults.size;model.focusMessage.value=chatResults[searchIndex].id}},enabled=chatResults.isNotEmpty()) {Icon(Icons.Outlined.KeyboardArrowUp,"Previous match")}
                    IconButton(onClick={if(chatResults.isNotEmpty()){model.focusMessage.value=chatResults[searchIndex%chatResults.size].id;searchIndex=(searchIndex+1)%chatResults.size}},enabled=chatResults.isNotEmpty()) {Icon(Icons.Outlined.KeyboardArrowDown,"Next match")}
                }
            }
        }
        if(messagesReady!=cid)Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center) {CircularProgressIndicator(Modifier.size(24.dp))}
        else MessageTimeline(cid,messages,contact.optBoolean("typing"),focus,{model.focusMessage.value=null},loadOlder={model.repository.loadOlder(cid)},modifier=Modifier.weight(1f).fillMaxWidth()) {m->
            MessageBubble(m,chat,model,onRetry={model.retry(m.id)},highlight=m.id==focus||m.id in selectedIds,
                onLongClick={if(selectedIds.isNotEmpty() && canForwardTextMessage(m,session?.userId))
                    selectedIds=toggleTextSelection(selectedIds,m.id) else actionMessage=m},
                onReply={replyTo=m.id;draftVersion=model.repository.saveDraft(cid,body,replyTo)},
                quoted=messages.firstOrNull {it.id==m.replyTo},
                onQuoteClick={if(m.replyTo.isNotEmpty())model.focusMessage.value=m.replyTo},
                selectionMode=selectedIds.isNotEmpty(),
                onSelect={if(canForwardTextMessage(m,session?.userId))
                    selectedIds=toggleTextSelection(selectedIds,m.id)})
        }
        if(selectedIds.isNotEmpty()) GlassSurface(Modifier.fillMaxWidth().padding(horizontal=8.dp),Glass.compact) {
            val selectedText=messages.filter {it.id in selectedIds && canForwardTextMessage(it,session?.userId)}
            Row(Modifier.fillMaxWidth().padding(6.dp),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                Text("${selectedText.size} selected",Modifier.weight(1f),
                    style=MaterialTheme.typography.labelLarge)
                TextButton(onClick={
                    clipboard.setText(AnnotatedString(selectedText.joinToString("\n") {it.body}))
                    selectedIds=emptySet()
                },enabled=selectedText.isNotEmpty()) {Text("Copy")}
                TextButton(onClick={
                    forwardMessages=selectedText
                    forwardSearch=""
                    selectedIds=emptySet()
                },enabled=selectedText.isNotEmpty()) {Text("Forward")}
                IconButton(onClick={selectedIds=emptySet()}) {Icon(Icons.Outlined.Close,"Cancel selection")}
            }
        }
        if(body.length>=3800)Text("${body.length} / 4000",Modifier.align(Alignment.End).padding(end=20.dp),style=MaterialTheme.typography.labelSmall)
        if(replyTo.isNotEmpty()) GlassSurface(Modifier.fillMaxWidth().padding(horizontal=12.dp),Glass.compact) {
            Row(Modifier.padding(start=14.dp),verticalAlignment=Alignment.CenterVertically) {
                val original=messages.firstOrNull {it.id==replyTo}
                Column(Modifier.weight(1f)) {Text("Replying to ${if(original?.sender==original?.account)"yourself" else chat?.name.orEmpty()}",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary);Text(original?.takeIf {it.deletedAt.isEmpty()}?.body ?: "Original message unavailable",maxLines=1,overflow=TextOverflow.Ellipsis)}
                IconButton(onClick={replyTo="";draftVersion=model.repository.saveDraft(cid,body)}) {Icon(Icons.Outlined.Close,"Cancel reply")}
            }
        }
        GlassSurface(Modifier.fillMaxWidth().padding(8.dp),Glass.capsule) {
            Row(Modifier.fillMaxWidth().padding(4.dp),verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                IconButton(onClick={emojiPicker=true},enabled=draftReady) {Icon(Icons.Outlined.EmojiEmotions,"Open emoji picker")}
                androidx.compose.foundation.text.BasicTextField(input,{if(it.text.length<=4000)updateInput(it)},enabled=draftReady,keyboardOptions=KeyboardOptions(capitalization=KeyboardCapitalization.Sentences,keyboardType=KeyboardType.Text),modifier=Modifier.weight(1f).heightIn(min=48.dp,max=140.dp).padding(vertical=14.dp).onFocusChanged {inputFocused=it.isFocused}.testTag("message-input").semantics {contentDescription="Message"},textStyle=MaterialTheme.typography.bodyLarge.copy(color=MaterialTheme.colorScheme.onSurface),maxLines=5,decorationBox={inner->Box {if(body.isEmpty())Text("Message",color=MaterialTheme.colorScheme.onSurfaceVariant);inner()}})
                IconButton(onClick={attachments=true},enabled=!busy&&replyTo.isEmpty()) {Icon(Icons.Outlined.AttachFile,"Attach image")}
                if(body.isBlank()&&replyTo.isEmpty())VoiceCapture(model,cid,busy) else FilledIconButton(onClick={if(body.isNotBlank()&&!queueing){
                    val submittedBody=body
                    val submittedReply=replyTo
                    val submittedVersion=draftVersion
                    queueing=true
                    model.send(cid,submittedBody,submittedReply,submittedVersion){
                        // Preserve any new draft typed while the previous message was being queued.
                        if(draftVersion==submittedVersion&&input.text==submittedBody&&replyTo==submittedReply){input=TextFieldValue("");replyTo=""}
                    }.invokeOnCompletion {queueing=false}
                }},enabled=body.isNotBlank()&&!queueing&&draftReady,
                    colors=IconButtonDefaults.filledIconButtonColors(containerColor=chatAccentColor),
                    modifier=Modifier.size(48.dp).graphicsLayer {scaleX=sendScale;scaleY=sendScale}) {
                    Icon(Icons.AutoMirrored.Outlined.Send,"Send message",
                        tint=if(chatAccent=="Follow")MaterialTheme.colorScheme.onPrimary
                             else if(LocalGlassDark.current)Color.Black else Color.White)
                }
            }
        }
    }

    }
    if(attachments)LiquidActionSheet(title="Attach",onDismiss={attachments=false}) {
        GlassActionRow("Gallery",Icons.Outlined.PhotoLibrary,onClick={attachments=false;picker.launch("image/*")})
        GlassActionRow("Camera",Icons.Outlined.PhotoCamera,onClick={attachments=false
            val account=model.backend.session.value?.userId
            if(account!=null) {
                cameraAccount=account
                val file=java.io.File(context.cacheDir,"camera/${java.util.UUID.randomUUID()}.jpg");file.parentFile?.mkdirs();cameraPath=file.absolutePath
                runCatching {camera.launch(androidx.core.content.FileProvider.getUriForFile(context,context.packageName+".files",file))}.onFailure {file.delete();cameraPath="";model.action {throw java.io.IOException("No camera app is available. Choose Gallery.")}}
            }})
    }
    if(emojiPicker)EmojiPicker(onEmoji={emoji->
        val start=minOf(input.selection.start,input.selection.end);val end=maxOf(input.selection.start,input.selection.end)
        val text=body.replaceRange(start,end,emoji)
        if(text.length<=4000)updateInput(TextFieldValue(text,TextRange(start+emoji.length)))
    },onDismiss={emojiPicker=false})
    if(scheduleOpen) LiquidAlertDialog(
        onDismissRequest={scheduleOpen=false;scheduleEditingId=null},
        title={Text(if(scheduleEditingId==null)"Schedule a message" else "Edit scheduled message")},
        text={Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement=Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(scheduleBody,{if(it.length<=4000)scheduleBody=it},
                label={Text("Message")},modifier=Modifier.fillMaxWidth(),maxLines=6,
                keyboardOptions=KeyboardOptions(capitalization=KeyboardCapitalization.Sentences))
            Text("Scheduled for " + java.time.Instant.ofEpochMilli(scheduleAt)
                .atZone(java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")))
            OutlinedButton(onClick={
                val selected=java.util.Calendar.getInstance().apply {timeInMillis=scheduleAt}
                android.app.DatePickerDialog(context,{_,year,month,day->
                    android.app.TimePickerDialog(context,{_,hour,minute->
                        scheduleAt=java.util.Calendar.getInstance().apply {
                            set(year,month,day,hour,minute)
                            set(java.util.Calendar.SECOND,0)
                            set(java.util.Calendar.MILLISECOND,0)
                        }.timeInMillis
                    },selected.get(java.util.Calendar.HOUR_OF_DAY),selected.get(java.util.Calendar.MINUTE),
                        android.text.format.DateFormat.is24HourFormat(context)).show()
                },selected.get(java.util.Calendar.YEAR),selected.get(java.util.Calendar.MONTH),
                    selected.get(java.util.Calendar.DAY_OF_MONTH)).apply {
                    datePicker.minDate=System.currentTimeMillis()-86_400_000L
                    datePicker.maxDate=System.currentTimeMillis()+180L*86_400_000L
                }.show()
            }) {Text("Choose date and time")}
            Text("Stored on this device until due. Android may delay delivery; signing out or deleting the chat cancels local schedules.",
                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }},
        confirmButton={TextButton(onClick={
            val text=scheduleBody
            val expectedReply=scheduleReplyTo
            val version=scheduleDraftVersion
            val sendAt=scheduleAt
            val editId=scheduleEditingId
            val consumeVersion=version.takeIf {editId==null&&it>0L&&body==text&&replyTo==expectedReply&&draftVersion==it}
            model.action {
                if(editId!=null) {
                    model.repository.editScheduledText(cid,editId,text,sendAt)
                } else {
                    model.repository.scheduleText(cid,text,expectedReply,sendAt,consumeVersion)
                }
                scheduleOpen=false
                scheduleEditingId=null
                if(consumeVersion!=null && body==text && replyTo==expectedReply && draftVersion==version) {
                    input=TextFieldValue("")
                    replyTo=""
                }
            }
        },enabled=!busy&&scheduleBody.isNotBlank()&&scheduleAt>=System.currentTimeMillis()+60_000L) {
            Text(if(scheduleEditingId==null)"Schedule" else "Save changes")
        }},
        dismissButton={TextButton(onClick={scheduleOpen=false;scheduleEditingId=null}) {Text("Cancel")}}
    )
    if(schedulesOpen) LiquidActionSheet(title="Scheduled messages",onDismiss={schedulesOpen=false}) {
        if(scheduledTexts.isEmpty()) Text("No scheduled messages for this conversation.",
            style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        scheduledTexts.forEach {item ->
            Row(Modifier.fillMaxWidth().padding(vertical=8.dp),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(item.body,maxLines=3,overflow=TextOverflow.Ellipsis)
                    Text(java.time.Instant.ofEpochMilli(item.sendAt)
                        .atZone(java.time.ZoneId.systemDefault())
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")),
                        style=MaterialTheme.typography.bodySmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick={
                    schedulesOpen=false
                    scheduleEditingId=item.id
                    scheduleBody=item.body
                    scheduleReplyTo=item.replyTo
                    scheduleDraftVersion=0L
                    scheduleAt=item.sendAt
                    scheduleOpen=true
                },enabled=!busy) {Icon(Icons.Outlined.Edit,"Edit scheduled message")}
                IconButton(onClick={model.action {model.repository.cancelScheduledText(item.id)}},
                    enabled=!busy) {Icon(Icons.Outlined.Close,"Cancel scheduled message")}
            }
            HorizontalDivider()
        }
    }
    if(notificationsOpen && chat!=null && session?.userId!=null) {
        val notificationAccount=session!!.userId
        LiquidAlertDialog(
            onDismissRequest={notificationsOpen=false;notificationError=null},
            title={Text("Custom notifications")},
            text={Column(Modifier.heightIn(max=430.dp).verticalScroll(rememberScrollState()),
                verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Separate notification channel",style=MaterialTheme.typography.titleSmall)
                        Text("Use a dedicated Android channel for ${chat.name}.",
                            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked=customNotificationEnabled,
                        onCheckedChange={on->
                            try {
                                com.pritam.liquidchat.data.ConversationNotificationChannels
                                    .setEnabled(context,notificationAccount,cid,chat.name,on)
                                customNotificationEnabled=on
                                notificationError=null
                            } catch(e:Exception) {
                                notificationError=e.message ?: "Unable to change notification channel."
                            }
                        })
                }
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Hide notification message text",style=MaterialTheme.typography.titleSmall)
                        Text("Only show 'New message' for this conversation on this phone, including expanded alerts.",
                            style=MaterialTheme.typography.bodySmall,
                            color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked=hideNotificationBody,onCheckedChange={hide->
                        try {
                            com.pritam.liquidchat.data.ConversationNotificationChannels
                                .setHiddenPreview(context,notificationAccount,cid,hide)
                            hideNotificationBody=hide
                            notificationError=null
                        } catch(e:Exception) {
                            notificationError=e.message ?: "Could not save message preview setting."
                        }
                    })
                }
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Hide contact name in notifications",
                            style=MaterialTheme.typography.titleSmall)
                        Text("Future message alerts show Liquid Chat instead of the sender and hide the message text too. Does not affect call invitations.",
                            style=MaterialTheme.typography.bodySmall,
                            color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked=hideNotificationSender,onCheckedChange={hide->
                        try {
                            com.pritam.liquidchat.data.ConversationNotificationChannels
                                .setHiddenSender(context,notificationAccount,cid,chat.name,hide)
                            hideNotificationSender=hide
                            notificationError=null
                        } catch(e:Exception) {
                            notificationError=e.message ?: "Could not save sender privacy."
                        }
                    })
                }
                if(customNotificationEnabled) FilledTonalButton(onClick={
                    try {
                        com.pritam.liquidchat.data.ConversationNotificationChannels
                            .createChannel(context,notificationAccount,cid,
                                if(hideNotificationSender)"Conversation" else chat.name)
                        context.startActivity(
                            com.pritam.liquidchat.data.ConversationNotificationChannels
                                .settingsIntent(context,notificationAccount,cid))
                        notificationError=null
                    } catch(e:Exception) {
                        notificationError=e.message ?: "Android channel settings unavailable."
                    }
                },modifier=Modifier.fillMaxWidth()) {Text("Choose sound and vibration in Android")}
                Text("Android controls sounds, vibration, importance and permission. This phone's per-chat redaction is independent of Mute and can only make your server preview privacy stricter.",
                    style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                notificationError?.let {Text(it,color=MaterialTheme.colorScheme.error,
                    style=MaterialTheme.typography.bodySmall)}
            }},
            confirmButton={TextButton(onClick={notificationsOpen=false;notificationError=null}) {
                Text("Done")
            }}
        )
    }
    if(appearance)LiquidAlertDialog(
        onDismissRequest={appearance=false},
        title={Text("Chat wallpaper")},
        text={Column(Modifier.fillMaxWidth().heightIn(max=430.dp).verticalScroll(rememberScrollState()),
            verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Choose a built-in palette, private photo or muted MP4 video from this phone.",
                style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Chat accent · header actions and send button",
                style=MaterialTheme.typography.titleSmall)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                MbConversationAccent.choices.forEach {choice->
                    FilterChip(selected=chatAccent==choice,
                        onClick={
                            session?.userId?.let {account->
                                MbConversationAccent.save(context,account,cid,choice)
                                chatAccent=choice
                            }
                        },
                        label={Text(choice)})
                }
            }
            Text("Stored only on this device and isolated by account. Messages and calls are unchanged.",
                style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
            listOf("Ice","Lilac","Mint","Sand").forEach {choice->
                TextButton(onClick={model.action {
                    model.repository.setPalette(cid,choice)
                    palette=choice
                    appearance=false
                }}) {Text(if(choice==palette)"✓ $choice" else choice)}
            }
            FilledTonalButton(
                onClick={wallpaperPickerAccount=session?.userId;wallpaperPicker.launch("image/*")},
                modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {
                Icon(Icons.Outlined.AddPhotoAlternate,null)
                Spacer(Modifier.width(8.dp))
                Text("Choose photo wallpaper")
            }
            FilledTonalButton(
                onClick={videoWallpaperPickerAccount=session?.userId;videoWallpaperPicker.launch("video/mp4")},
                modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {
                Icon(Icons.Outlined.Videocam,null)
                Spacer(Modifier.width(8.dp))
                Text("Choose MP4 video wallpaper")
            }
            val owner=session?.userId
            if(owner!=null && remember(owner,cid,wallpaperRevision) {
                runCatching {com.pritam.liquidchat.data.PersonalWallpaper.has(context,owner,cid)}
                    .getOrDefault(false)
            }) {
                TextButton(onClick={model.action {
                    model.repository.setPalette(cid,"Photo")
                    palette="Photo"
                    appearance=false
                }}) {Text(if(palette=="Photo")"✓ My photo" else "My saved photo")}
            }
            if(owner!=null && remember(owner,cid,wallpaperRevision) {
                runCatching {com.pritam.liquidchat.data.PrivateVideoWallpaper.has(context,owner,cid)}
                    .getOrDefault(false)
            }) {
                TextButton(onClick={model.action {
                    model.repository.setPalette(cid,"Video")
                    palette="Video"
                    appearance=false
                }}) {Text(if(palette=="Video")"✓ My video" else "My saved video")}
            }
            Text("Photos and videos stay private on this phone. MP4 is limited to 30s / 24 MB, up to 1080p; playback loops silently and pauses in background or reduced motion. Files are erased on logout and chat deletion.",
                style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
        }},
        confirmButton={TextButton(onClick={appearance=false}) {Text("Done")}}
    )
    actionMessage?.let {m->
        val categories=messageActionCategories(m)
        LiquidAlertDialog(
            onDismissRequest={actionMessage=null},
            title={Text("Message actions")},
            text={
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    if(m.deletedAt.isNotEmpty()) {
                        Text("This message was deleted",
                            color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if("Quick reactions" in categories) {
                        Text("Quick reactions",style=MaterialTheme.typography.labelMedium,
                            color=MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.SpaceEvenly) {
                            MbQuickReactions.selected(display.quickReactions).forEach {emoji->
                                TextButton(
                                    modifier=Modifier.size(48.dp),
                                    onClick={
                                        actionMessage=null
                                        model.action {
                                            val old=org.json.JSONObject(m.reactions).optString(m.account)
                                            model.repository.messageAction(m.id,"react",if(old==emoji)"" else emoji)
                                        }
                                    },
                                    contentPadding=PaddingValues(0.dp)
                                ) {Text(emoji)}
                            }
                        }
                        HorizontalDivider()
                    }
                    if("Message actions" in categories) {
                        Text("Message actions",style=MaterialTheme.typography.labelMedium,
                            color=MaterialTheme.colorScheme.onSurfaceVariant)
                        if(m.status=="SENT") {
                            TextButton(onClick={replyTo=m.id;draftVersion=model.repository.saveDraft(cid,body,replyTo);actionMessage=null},
                                modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text("Reply")}
                            TextButton(onClick={
                                actionMessage=null
                                model.action {model.repository.messageAction(m.id,"star",(!m.starred).toString())}
                            },modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text(if(m.starred)"Unstar" else "Star")}
                        }
                        if(canForwardTextMessage(m,session?.userId)) {
                            TextButton(onClick={actionMessage=null;forwardMessages=listOf(m);forwardSearch=""},
                                modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {
                                Icon(Icons.Outlined.Forward,null)
                                Spacer(Modifier.width(8.dp))
                                Text("Forward to chat")
                            }
                            TextButton(onClick={actionMessage=null;selectedIds=setOf(m.id)},
                                modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {
                                Icon(Icons.Outlined.CheckBox,null)
                                Spacer(Modifier.width(8.dp))
                                Text("Select multiple")
                            }
                        }
                        if(m.mediaKind!="voice")TextButton(onClick={
                            clipboard.setText(AnnotatedString(m.body));actionMessage=null
                        },modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text("Copy text")}
                        TextButton(onClick={
                            actionMessage=null
                            context.startActivity(android.content.Intent.createChooser(
                                android.content.Intent(android.content.Intent.ACTION_SEND)
                                    .setType("text/plain")
                                    .putExtra(android.content.Intent.EXTRA_TEXT,m.body),
                                "Share message"
                            ))
                        },modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text("Share text")}
                        val age=runCatching {
                            java.time.Duration.between(java.time.Instant.parse(m.serverTime),java.time.Instant.now()).seconds
                        }.getOrDefault(Long.MAX_VALUE)
                        if(m.sender==m.account&&m.status=="SENT"&&m.mediaId.isEmpty()&&age<=900)
                            TextButton(onClick={editMessage=m;editBody=m.body;actionMessage=null},
                                modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text("Edit")}
                    }
                    if("Safety" in categories) {
                        HorizontalDivider()
                        Text("Safety",style=MaterialTheme.typography.labelMedium,
                            color=MaterialTheme.colorScheme.onSurfaceVariant)
                        if(m.deletedAt.isEmpty()&&m.status=="SENT"&&m.sender!=m.account)
                            TextButton(onClick={reportMessage=m;actionMessage=null;report=true},
                                modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text("Report message")}
                        if(m.status in listOf("SENT","QUEUED","FAILED"))
                            TextButton(onClick={deleteMessage=m;actionMessage=null},
                                modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {
                                Text("Delete",color=MaterialTheme.colorScheme.error)
                            }
                    }
                    Text("${if(m.sender==m.account)"You" else chat?.name.orEmpty()} · ${
                        java.time.Instant.ofEpochMilli(m.localTime)
                            .atZone(java.time.ZoneId.systemDefault())
                            .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
                    }",style=MaterialTheme.typography.bodySmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton={TextButton(onClick={actionMessage=null}) {Text("Done")}}
        )
    }
    forwardMessages.takeIf {it.isNotEmpty()}?.let {batch->
        val source=batch.first()
        val account=session?.userId
        val destinations=chats.filter {it.account==account && it.id!=source.conversation &&
            (forwardSearch.isBlank() || it.name.contains(forwardSearch,true) ||
                it.username.contains(forwardSearch,true))}
        LiquidAlertDialog(onDismissRequest={forwardMessages=emptyList()},
            title={Text(if(batch.size==1)"Forward message" else "Forward ${batch.size} messages")},
            text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(if(batch.size==1)source.body else batch.joinToString("\n") {it.body},
                    maxLines=3,overflow=TextOverflow.Ellipsis,
                    style=MaterialTheme.typography.bodyMedium,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(value=forwardSearch,onValueChange={forwardSearch=it.take(60)},
                    label={Text("Find a conversation")},singleLine=true,
                    modifier=Modifier.fillMaxWidth())
                if(destinations.isEmpty())Text("No other conversations found",
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
                LazyColumn(Modifier.fillMaxWidth().heightIn(max=280.dp)) {
                    items(destinations,key={it.id}) {target->
                        Row(Modifier.fillMaxWidth().clickable(enabled=!busy) {
                            val selected=batch
                            forwardMessages=emptyList()
                            model.action {selected.forEach {model.repository.forwardText(it,target.id)}}
                        }.padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically,
                            horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            Avatar(target.name,target.avatarId,model.backend,size=40.dp)
                            Column {Text(target.name);Text("@${target.username}",
                                style=MaterialTheme.typography.labelSmall)}
                        }
                    }
                }
            }},
            confirmButton={TextButton(onClick={forwardMessages=emptyList()}) {Text("Cancel")}})
    }
    editMessage?.let {m->LiquidAlertDialog(onDismissRequest={editMessage=null},title={Text("Edit message")},text={OutlinedTextField(editBody,{if(it.length<=4000)editBody=it},keyboardOptions=KeyboardOptions(capitalization=KeyboardCapitalization.Sentences),modifier=Modifier.fillMaxWidth())},confirmButton={TextButton(onClick={model.action {model.repository.messageAction(m.id,"edit",editBody);editMessage=null}},enabled=!busy&&editBody.isNotBlank()) {Text("Save")}},dismissButton={TextButton(onClick={editMessage=null}) {Text("Cancel")}})}
    deleteMessage?.let {m->LiquidAlertDialog(onDismissRequest={deleteMessage=null},title={Text("Delete message?")},text={Column {
        Text(if(m.status=="SENT")"Delete for me hides this message on your devices. The other person keeps their copy. Saved copies and screenshots cannot be erased." else "Remove this unsent message and its local attachment from this device.")
        val age=runCatching {java.time.Duration.between(java.time.Instant.parse(m.serverTime),java.time.Instant.now()).seconds}.getOrDefault(Long.MAX_VALUE)
        if(m.sender==m.account&&age<=172800&&m.deletedAt.isEmpty())TextButton(onClick={model.action {model.repository.messageAction(m.id,"delete");deleteMessage=null}},enabled=!busy) {Text("Delete for everyone")}
    }},confirmButton={TextButton(onClick={model.action {if(m.status=="SENT")model.repository.messageAction(m.id,"hide") else model.repository.discardUnsent(m.id);deleteMessage=null}},enabled=!busy) {Text(if(m.status=="SENT")"Delete for me" else "Remove unsent")}},dismissButton={TextButton(onClick={deleteMessage=null}) {Text("Cancel")}})}
    if(starsOpen&&chat!=null)ContactMediaDialog(model,chat,messages,initialKind="star") {starsOpen=false}
    if(details&&chat!=null) ContactMediaDialog(model,chat,messages) {details=false}
    if(report&&chat!=null)ReportContentDialog(if(reportMessage==null)"Report contact" else "Report message",if(reportMessage==null)"Your explanation will be sent privately for review." else "Your explanation and this message's reference will be sent privately for review.",onSubmit={reason->model.report(chat.peer,reason,reportMessage?.let {listOf(it.id)} ?: emptyList());report=false;reportMessage=null},onDismiss={report=false;reportMessage=null})
    prepared?.let {image->LiquidAlertDialog(onDismissRequest={image.file.delete();prepared=null},title={Text("Send image")},text={Column {
        coil.compose.AsyncImage(image.file,"Image preview",Modifier.fillMaxWidth().heightIn(max=260.dp).aspectRatio(image.width.toFloat()/image.height))
        OutlinedTextField(input,{if(it.text.length<=4000)updateInput(it)},keyboardOptions=KeyboardOptions(capitalization=KeyboardCapitalization.Sentences,keyboardType=KeyboardType.Text),label={Text("Caption (optional)")},modifier=Modifier.fillMaxWidth())
    }},confirmButton={TextButton(onClick={val caption=body;val version=draftVersion;model.action {model.repository.sendImage(cid,image,caption,version);prepared=null;if(draftVersion==version){input=TextFieldValue("");replyTo=""}}},enabled=!busy) {Text("Send")}},dismissButton={TextButton(onClick={image.file.delete();prepared=null}) {Text("Cancel")}})}
    if(confirmDelete&&chat!=null) LiquidAlertDialog(onDismissRequest={confirmDelete=false},title={Text("Delete your chat history?")},text={Text("This hides existing history for your account on every device. The other person keeps their copy. Unsent local copies will be removed; messages already sending may still finish.")},confirmButton={TextButton(onClick={confirmDelete=false;model.updateChat(chat,delete=true)}) {Text("Delete for me")}},dismissButton={TextButton(onClick={confirmDelete=false}) {Text("Cancel")}})
}

@Composable private fun SearchFilterMenu(title:String,selected:String,options:List<String>,onSelect:(Int)->Unit) {
    var expanded by remember {mutableStateOf(false)}
    Box {
        TextButton(onClick={expanded=true}) {Text("$title: $selected")}
        LiquidDropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
            options.forEachIndexed {index,label->DropdownMenuItem(text={Text(label)},onClick={onSelect(index);expanded=false})}
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable internal fun MessageBubble(message:Message,chat:Chat?,model:AppViewModel,onRetry:()->Unit,highlight:Boolean,onLongClick:()->Unit={},onReply:()->Unit={},quoted:Message?=null,onQuoteClick:()->Unit={},selectionMode:Boolean=false,onSelect:()->Unit={}) {
    val uploadFlow=remember(model,message.account,message.id) {model.repository.uploadProgress(message.account,message.id)}
    val uploadPercent by uploadFlow.collectAsStateWithLifecycle(initialValue=null)
    val mine=message.sender==message.account
    val bubbleDisplay=LocalDisplayOptions.current
    var swipe by remember(message.id) {mutableFloatStateOf(0f)}
    val threshold=with(LocalDensity.current) {72.dp.toPx()}
    val replyCallback by rememberUpdatedState(onReply)
    val rtl=LocalLayoutDirection.current==LayoutDirection.Rtl
    val haptic=LocalHapticFeedback.current
    var crossed by remember(message.id) {mutableStateOf(false)}
    val drag=rememberDraggableState {delta->
        swipe=(swipe+if(rtl)-delta else delta).coerceIn(0f,threshold*1.3f)
        if(swipe>=threshold&&!crossed) {crossed=true;haptic.performHapticFeedback(HapticFeedbackType.LongPress)}
    }
    var expanded by remember(message.id) {mutableStateOf(false)}
    var truncated by remember(message.id) {mutableStateOf(false)}
    BoxWithConstraints(Modifier.fillMaxWidth()) {
    val bubbleWidth=minOf(maxWidth*.86f,480.dp)
    Row(Modifier.fillMaxWidth().offset {androidx.compose.ui.unit.IntOffset((if(rtl)-swipe else swipe).toInt(),0)}.draggable(drag,Orientation.Horizontal,enabled=message.status=="SENT"&&message.deletedAt.isEmpty(),onDragStopped={if(swipe>=threshold)replyCallback();swipe=0f;crossed=false}),horizontalArrangement=if(mine)Arrangement.End else Arrangement.Start) {
        Surface(
            shape=conversationBubbleShape(bubbleDisplay.bubbleStyle,mine),
            color=if(highlight)MaterialTheme.colorScheme.tertiaryContainer
                else bubbleSurfaceColor(bubbleDisplay,mine,LocalGlassDark.current,
                    if(LocalGlassDark.current) {if(mine)Color(0xFF1F5268) else Color(0xFF293F53)}
                    else if(mine)Color(0xFFD0EDFB) else Color(0xFFFAFDFF)),
            border=androidx.compose.foundation.BorderStroke(1.dp,Color.White.copy(alpha=if(LocalGlassDark.current).19f else .8f)),
            shadowElevation=2.dp,
            modifier=Modifier.widthIn(max=bubbleWidth).glassSource()
                .combinedClickable(onClick={if(selectionMode)onSelect()},
                    onLongClick={if(selectionMode)onSelect() else onLongClick()})
        ) {
            Column(Modifier.padding(horizontal=11.dp,vertical=9.dp)) {
                if(message.deletedAt.isNotEmpty()) {
                    Text("This message was deleted",
                        style=MaterialTheme.typography.bodyMedium,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                if(message.replyTo.isNotEmpty()) Surface(shape=Glass.compact,color=MaterialTheme.colorScheme.primary.copy(alpha=.1f),modifier=Modifier.fillMaxWidth().clickable(onClick=onQuoteClick)) {
                    Column(Modifier.padding(8.dp)) {Text("Reply",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary);Text(quoted?.takeIf {it.deletedAt.isEmpty()}?.body ?: "Original message unavailable",maxLines=2,overflow=TextOverflow.Ellipsis,
                        color=conversationInkColor(bubbleDisplay.quotedInk,LocalGlassDark.current,
                            resolvedBubbleTextColor(bubbleDisplay,mine,LocalGlassDark.current,MaterialTheme.colorScheme.onSurface),bubbleDisplay.quotedInkHex),
                        style=MaterialTheme.typography.bodySmall)}
                }
                if(message.mediaId.isNotEmpty()||message.localImage.isNotEmpty()) {
                    if(message.mediaKind=="voice")VoicePlayer(model,message) else SharedImagePreview(model,message)
                    Spacer(Modifier.height(8.dp))
                }
                if(message.mediaKind!="voice")MessageBodyText(message.body,bubbleDisplay.messageFontSize,expanded,
                    linksEnabled=!selectionMode,
                    textColor=conversationInkColor(if(mine)bubbleDisplay.outgoingInk else bubbleDisplay.incomingInk,
                        LocalGlassDark.current,resolvedBubbleTextColor(bubbleDisplay,mine,LocalGlassDark.current,MaterialTheme.colorScheme.onSurface),
                        if(mine)bubbleDisplay.outgoingInkHex else bubbleDisplay.incomingInkHex),
                    onTextLayout={truncated=it.hasVisualOverflow})
                if(message.status=="SENDING")uploadPercent?.let {percent->
                    LinearProgressIndicator(progress={percent/100f},modifier=Modifier.width(160.dp))
                    Text(if(percent<100)"Uploading · $percent%" else "Processing attachment…",style=MaterialTheme.typography.labelSmall)
                }
                if(truncated||expanded) TextButton(onClick={expanded=!expanded}) {Text(if(expanded)"Show less" else "Read more")}
                }
                val time=remember(message.localTime) {java.time.Instant.ofEpochMilli(message.localTime).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}
                Row(Modifier.align(Alignment.End),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    if(message.starred&&message.deletedAt.isEmpty())Icon(Icons.Outlined.Star,"Starred",Modifier.size(12.dp))
                    if(message.editedAt.isNotEmpty()&&message.deletedAt.isEmpty())Text("edited",style=MaterialTheme.typography.labelSmall)
                    Text(time,style=MaterialTheme.typography.labelSmall,
                        color=conversationInkColor(bubbleDisplay.timestampInk,LocalGlassDark.current,
                            resolvedBubbleTextColor(bubbleDisplay,mine,LocalGlassDark.current,MaterialTheme.colorScheme.onSurfaceVariant),bubbleDisplay.timestampInkHex))
                    if(mine)Receipt(message.status,message.seq,chat?.peerDelivered ?: 0,chat?.peerRead ?: 0)
                }
                if(message.reactions!="{}"&&message.deletedAt.isEmpty()) {
                    val reactions=remember(message.reactions) {runCatching {val j=org.json.JSONObject(message.reactions);j.keys().asSequence().map {j.getString(it)}.groupingBy {it}.eachCount()}.getOrDefault(emptyMap())}
                    val own=remember(message.reactions,message.account) {runCatching {org.json.JSONObject(message.reactions).optString(message.account)}.getOrDefault("")}
                    Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {reactions.forEach {(emoji,count)->Surface(shape=Glass.capsule,border=if(emoji==own)androidx.compose.foundation.BorderStroke(1.dp,MaterialTheme.colorScheme.primary) else null,color=MaterialTheme.colorScheme.primaryContainer) {Text("$emoji $count",Modifier.padding(horizontal=8.dp,vertical=3.dp),style=MaterialTheme.typography.labelMedium)}}}
                }
                if(message.status=="FAILED") {Text(message.error,style=MaterialTheme.typography.bodySmall);TextButton(onClick=onRetry) {Text("Retry")}}
            }
        }
    }
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable fun SettingsScreen(model:AppViewModel,busy:Boolean) {
    var section by rememberSaveable {mutableStateOf<String?>(null)}
    var fromPreferences by rememberSaveable {mutableStateOf(false)}
    var unavailable by remember {mutableStateOf<String?>(null)}
    if(section==null) {
        SettingsHome(model) {title->
            val destination=title
            if(destination in listOf("Devices","Language","Chat Folders")) unavailable=destination else {
                fromPreferences=false
                section=destination
            }
        }
        unavailable?.let {title->LiquidAlertDialog(onDismissRequest={unavailable=null},title={Text(title)},text={Text(when(title){"Language"->"English is the current app language.";"Chat Folders"->"Use All, Favorite, Archive and Unread in the Chats menu.";"Data and Storage"->"Shared images are loaded on demand. Automatic downloads are not enabled.";else->"Device management is not available in this version yet."})},confirmButton={TextButton(onClick={unavailable=null}){Text("OK")}})}
        return
    }
    BackHandler {
        section=if(fromPreferences && section!="Liquid Chat Preferences")"Liquid Chat Preferences"
            else if(section in listOf("Liquid Chat General Settings","Liquid Chat Conversation Screen","Liquid Chat Home Screen","Liquid Chat Themes","Liquid Chat Privacy","Liquid Chat Notifications"))"Liquid Chat Preferences"
            else null
        if(section==null)fromPreferences=false
    }

    if(section=="Liquid Chat Preferences") {
        MbPreferencesLanding(
            onBack={section=null;fromPreferences=false},
            onNavigate={destination->fromPreferences=true;section=destination}
        )
        return
    }

    val profile by model.profile.collectAsStateWithLifecycle()
    val settings by model.repository.privateSettings.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {model.action {model.repository.loadSettings()}}
    val unsent by model.unsent.collectAsStateWithLifecycle()
    val display by model.display.collectAsStateWithLifecycle()
    val context=LocalContext.current
    var themeFeedback by remember {mutableStateOf<String?>(null)}
    var resetThemeDialog by remember {mutableStateOf(false)}
    val exportTheme=rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(MbThemeTransfer.MIME_TYPE)) {uri->
        if(uri!=null)model.action {
            val json=MbThemeTransfer.export(model.display.value)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val stream=context.contentResolver.openOutputStream(uri) ?: error("Cannot open theme file for writing.")
                stream.use {it.write(json.toByteArray(Charsets.UTF_8));it.flush()}
            }
            themeFeedback="Theme exported successfully."
        }
    }
    val importTheme=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {uri->
        if(uri!=null)model.action {
            val json=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val stream=context.contentResolver.openInputStream(uri) ?: error("Cannot open selected file.")
                stream.use {input ->
                    val buffer=ByteArray(4096)
                    val collected=java.io.ByteArrayOutputStream()
                    while(true) {
                        val n=input.read(buffer)
                        if(n<0)break
                        if(n==0)continue
                        require(collected.size()+n<=MbThemeTransfer.MAX_IMPORT_BYTES) {"Theme file is too large."}
                        collected.write(buffer,0,n)
                    }
                    collected.toString(Charsets.UTF_8.name())
                }
            }
            val next=MbThemeTransfer.decode(json,model.display.value)
            model.setDisplay(next)
            themeFeedback="Theme imported and applied."
        }
    }
    if(resetThemeDialog) LiquidAlertDialog(
        onDismissRequest={resetThemeDialog=false},
        title={Text("Restore default appearance?")},
        text={Text("Your custom colors, type sizes, wallpaper dimming and quick reactions will reset. Screenshots, outgoing typing, hidden inbox previews, accessibility options, messages and saved photos are unchanged.")},
        confirmButton={TextButton(onClick={
            model.setDisplay(MbThemeTransfer.resetAppearance(model.display.value))
            themeFeedback="Default appearance restored."
            resetThemeDialog=false
        }) {Text("Restore defaults")}},
        dismissButton={TextButton(onClick={resetThemeDialog=false}) {Text("Cancel")}}
    )
    val photoPicker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {uri->if(uri!=null)model.action {
        val id=model.backend.session.value?.userId ?: return@action
        val image=Images.prepare(context,uri,id)
        try {val uploaded=model.backend.uploadImage(image.file,null);model.setAvatar(uploaded.getString("id"))} finally {image.file.delete()}
    }}
    var confirm by remember {mutableStateOf(false)}
    var edit by remember {mutableStateOf(false)}
    var deleteAccount by remember {mutableStateOf(false)}
    var showPolicy by remember {mutableStateOf(false)}
    var password by remember {mutableStateOf("")}
    if(edit) {BackHandler {edit=false};Column {TextButton(onClick={edit=false}) {Text("Back to Settings")};ProfileScreen(model,busy)};return}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=16.dp,vertical=12.dp)
        .padding(bottom=98.dp+WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {IconButton(onClick={section=if(section in listOf("Liquid Chat General Settings","Liquid Chat Conversation Screen","Liquid Chat Home Screen","Liquid Chat Themes","Liquid Chat Privacy","Liquid Chat Notifications"))"Liquid Chat Preferences" else null}){Icon(Icons.AutoMirrored.Outlined.ArrowBack,"Back to Settings")};Text(section.orEmpty().removePrefix("Liquid Chat "),style=MaterialTheme.typography.headlineSmall)}
        if(section=="Data and Storage") DataStorage(model)
        if(section=="Liquid Chat Themes") GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
            Text("Themes",style=MaterialTheme.typography.titleLarge)
            Text("Save your current Liquid Chat appearance to a JSON theme, or import a saved theme.",style=MaterialTheme.typography.bodyMedium)
            Text("Imported themes change only this device's colors, navigation and chat appearance. No messages, accounts or contacts are exported.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider()
            FilledTonalButton(onClick={exportTheme.launch("LiquidChat-Theme.json")},modifier=Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.FileUpload,null)
                Spacer(Modifier.width(8.dp))
                Text("Export current theme")
            }
            OutlinedButton(onClick={importTheme.launch(arrayOf("application/json","text/plain"))},modifier=Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.FileDownload,null)
                Spacer(Modifier.width(8.dp))
                Text("Import Liquid Chat theme")
            }
            HorizontalDivider()
            OutlinedButton(onClick={resetThemeDialog=true},modifier=Modifier.fillMaxWidth()) {
                Text("Restore default appearance")
            }
            Text("This resets colors, fonts, bubble and navigation appearance. It preserves privacy settings, reduced-motion/transparency preferences, local photo files and messages.",
                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Import only Liquid Chat theme .json files.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)

            HorizontalDivider()
            Text("Saved themes on this device",style=MaterialTheme.typography.titleMedium)
            var bookmarkName by rememberSaveable {mutableStateOf("")}
            var savedThemeNames by remember {mutableStateOf(LiquidThemeLibrary.names(context))}
            var removeThemeName by remember {mutableStateOf<String?>(null)}
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value=bookmarkName,onValueChange={bookmarkName=it.take(36)},
                    label={Text("Theme name")},singleLine=true,modifier=Modifier.weight(1f))
                FilledTonalButton(enabled=bookmarkName.trim().isNotEmpty(),onClick={
                    runCatching {LiquidThemeLibrary.save(context,bookmarkName,model.display.value)}
                        .onSuccess {
                            savedThemeNames=LiquidThemeLibrary.names(context)
                            bookmarkName=""
                            themeFeedback="Theme saved on this device."
                        }
                        .onFailure {themeFeedback=it.message ?: "Unable to save theme."}
                }) {Text("Save")}
            }
            if(savedThemeNames.isEmpty()) Text("No saved themes yet.",
                style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
            savedThemeNames.forEach {name->
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text(name,Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis)
                    TextButton(onClick={
                        runCatching {
                            model.setDisplay(LiquidThemeLibrary.restore(context,name,model.display.value))
                        }.onSuccess {themeFeedback="Theme applied: $name"}
                            .onFailure {themeFeedback=it.message ?: "Unable to apply theme."}
                    }) {Text("Apply")}
                    IconButton(onClick={removeThemeName=name}) {
                        Icon(Icons.Outlined.DeleteOutline,"Delete saved theme: $name")
                    }
                }
            }
            removeThemeName?.let {name->
                LiquidAlertDialog(onDismissRequest={removeThemeName=null},
                    title={Text("Delete saved theme?")},
                    text={Text(name)},
                    confirmButton={TextButton(onClick={
                        LiquidThemeLibrary.delete(context,name)
                        savedThemeNames=LiquidThemeLibrary.names(context)
                        removeThemeName=null
                        themeFeedback="Saved theme deleted."
                    }) {Text("Delete")}},
                    dismissButton={TextButton(onClick={removeThemeName=null}) {Text("Cancel")}})
            }

            themeFeedback?.let {Text(it,color=MaterialTheme.colorScheme.primary)}
        }
        if(section=="Account") GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
            val avatar=profile?.optString("avatar_id").orEmpty()
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                Avatar(profile?.optString("display_name").orEmpty(),
                    avatar.takeUnless {it=="null"}.orEmpty(),model.backend,self=true,size=72.dp)
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text(profile?.optString("display_name").orEmpty().ifBlank {"My profile"},
                        style=MaterialTheme.typography.titleLarge,maxLines=2)
                    Text("@${profile?.optString("username").orEmpty()}",
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                    profile?.optString("bio")?.takeIf {it.isNotBlank()}?.let {
                        Text(it,style=MaterialTheme.typography.bodySmall,maxLines=3)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            FilledTonalButton(onClick={edit=true},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {
                Text("Edit profile")
            }
            OutlinedButton(
                onClick={photoPicker.launch("image/*")},enabled=!busy,
                modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)
            ) {Text(if(avatar.isBlank()||avatar=="null")"Add profile photo" else "Change profile photo")}
            if(avatar.isNotBlank()&&avatar!="null") TextButton(
                onClick={model.action {model.setAvatar()}},enabled=!busy,
                modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)
            ) {Text("Remove photo")}
        }
        if(section=="Privacy & Security") GlassCard(Modifier.fillMaxWidth()) {Text("Your privacy matters",fontWeight=FontWeight.SemiBold);Text("Chat access is restricted to participants. This version does not use end-to-end encryption.")}
        if(settingsPanelFor(section)==SettingsPanel.APPEARANCE) {
            if(section=="Liquid Chat General Settings") GlassSurface(Modifier.fillMaxWidth(),Glass.hero) {
                Column(Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("General settings",style=MaterialTheme.typography.titleLarge)
                    Text("Customize appearance, motion and storage using working Liquid Chat controls.",
                        style=MaterialTheme.typography.bodySmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
                Text(if(section=="Liquid Chat General Settings")"Theme & typography" else "Appearance",
                    style=MaterialTheme.typography.titleMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {listOf("System","Light","Dark").forEach {value->FilterChip(selected=display.appearance==value,onClick={model.setDisplay(display.copy(appearance=value))},label={Text(value)})}}
            Text("Main theme color",style=MaterialTheme.typography.labelLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf("Cyan","Blue","Green","Purple","Rose","Amber").forEach {value->
                    FilterChip(selected=display.accentColor==value,onClick={model.setDisplay(display.copy(accentColor=value))},label={Text(value)})
                }
            }
            Text("Custom accent color · #RRGGBB",style=MaterialTheme.typography.labelLarge)
            var accentHexInput by rememberSaveable {mutableStateOf(display.customAccentHex)}
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value=accentHexInput,
                    onValueChange={accentHexInput=it.take(7)},
                    label={Text("Hex color")},
                    singleLine=true,
                    isError=accentHexInput.isNotBlank() && normalizedAccentHex(accentHexInput)==null,
                    modifier=Modifier.weight(1f)
                )
                FilledTonalButton(
                    enabled=normalizedAccentHex(accentHexInput)!=null,
                    onClick={
                        normalizedAccentHex(accentHexInput)?.let {hex->
                            model.setDisplay(display.copy(accentColor="Custom",customAccentHex=hex))
                        }
                    }
                ) {Text("Apply")}
            }
            if(display.accentColor=="Custom") Text(
                "Custom accent applied across Liquid Chat · ${display.customAccentHex}",
                style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.primary
            )
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Random accent on next launch")
                    Text("Choose a different Liquid Chat color on each new app session.",
                        style=MaterialTheme.typography.bodySmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked=display.randomAccentOnLaunch,
                    onCheckedChange={model.setDisplay(display.copy(randomAccentOnLaunch=it))})
            }
            Text("App font family",style=MaterialTheme.typography.labelLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf("System","Serif","Mono","Cursive").forEach {value ->
                    FilterChip(selected=display.fontPreset==value,
                        onClick={model.setDisplay(display.copy(fontPreset=value))},label={Text(value)})
                }
            }
            Text("Uses Android's built-in fonts; imports no private font files.",
                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
                Text("Motion & accessibility",style=MaterialTheme.typography.titleMedium)
            Text("Animation level",style=MaterialTheme.typography.labelLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {listOf("Normal","Balanced","Extra").forEach {value->FilterChip(selected=display.animation==value,onClick={model.setDisplay(display.copy(animation=value))},enabled=!display.reducedMotion,label={Text(value)})}}
            Row(verticalAlignment=Alignment.CenterVertically) {Text("Reduced motion",Modifier.weight(1f));Switch(display.reducedMotion,{model.setDisplay(display.copy(reducedMotion=it))})}
            }
            GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
                Text("Liquid Glass effects",style=MaterialTheme.typography.titleMedium)
            Row(verticalAlignment=Alignment.CenterVertically) {Text("Reduced transparency",Modifier.weight(1f));Switch(display.reducedTransparency,{model.setDisplay(display.copy(reducedTransparency=it))})}
            HorizontalDivider()
            Text("Liquid Glass blur intensity",style=MaterialTheme.typography.titleSmall)
            Text("${display.blurIntensity}%",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(value=display.blurIntensity.toFloat(),onValueChange={value->model.setDisplay(display.copy(blurIntensity=value.toInt()))},valueRange=0f..100f,enabled=!display.reducedTransparency,modifier=Modifier.fillMaxWidth())
            Text("Lower values reduce blur; Reduced transparency overrides this setting.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        }
        // MB General's cache control reuses the already-safe Coil-only cleanup.
        // Never expose "clear all" actions that would erase the durable Room outbox.
        if(section=="Liquid Chat General Settings") DataStorage(model)
        
if(section=="Liquid Chat Home Screen") {
            GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
                Text("Home Screen",style=MaterialTheme.typography.titleLarge)
                Text("Personalize your Liquid Chat inbox; all settings work with existing conversations.", 
                    style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
                Text("Bottom navigation & tabs",style=MaterialTheme.typography.titleMedium)
                Text("Navigation bar",style=MaterialTheme.typography.titleSmall)
                Row(Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    listOf("Reference" to "iOS navigation", "Liquid" to "Liquid capsule").forEach { (value,label)->
                        FilterChip(selected=display.navBarStyle==value,
                            onClick={model.setDisplay(display.copy(navBarStyle=value))},
                            label={Text(label)})
                    }
                }
            Text("Reorder tabs · all five destinations remain accessible",
                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            MbNavigationOrder.parse(display.navigationOrder).forEachIndexed {position,destination ->
                Row(Modifier.fillMaxWidth().heightIn(min=44.dp),
                    verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    Text(MbNavigationOrder.titles[destination],Modifier.weight(1f),
                        style=MaterialTheme.typography.bodyMedium)
                    IconButton(
                        enabled=position>0,
                        onClick={model.setDisplay(display.copy(navigationOrder=
                            MbNavigationOrder.move(display.navigationOrder,destination,-1)))}) {
                        Icon(Icons.Outlined.KeyboardArrowUp,
                            "Move ${MbNavigationOrder.titles[destination]} left")
                    }
                    IconButton(
                        enabled=position<4,
                        onClick={model.setDisplay(display.copy(navigationOrder=
                            MbNavigationOrder.move(display.navigationOrder,destination,1)))}) {
                        Icon(Icons.Outlined.KeyboardArrowDown,
                            "Move ${MbNavigationOrder.titles[destination]} right")
                    }
                }
            }
            TextButton(onClick={model.setDisplay(display.copy(navigationOrder=MbNavigationOrder.DEFAULT))},
                enabled=display.navigationOrder!=MbNavigationOrder.DEFAULT) {
                Text("Reset navigation order")
            }
            HorizontalDivider()

            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("Show navigation labels",Modifier.weight(1f))
                Switch(display.showNavLabels,{model.setDisplay(display.copy(showNavLabels=it))},
                    enabled=!display.hideNavIcons)
            }
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Hide navigation icons")
                    Text("Labels stay visible so every tab remains identifiable.",
                        style=MaterialTheme.typography.bodySmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked=display.hideNavIcons,
                    onCheckedChange={model.setDisplay(display.copy(hideNavIcons=it))})
            }
            Text("Navigation icon size · ${display.navIconSize} dp",style=MaterialTheme.typography.titleSmall)
            Slider(value=display.navIconSize.toFloat(),onValueChange={model.setDisplay(display.copy(navIconSize=it.toInt()))},
                valueRange=20f..50f,steps=29,modifier=Modifier.fillMaxWidth())
            Text("Navigation label size · ${display.navLabelSize} sp",style=MaterialTheme.typography.titleSmall)
            Slider(value=display.navLabelSize.toFloat(),onValueChange={model.setDisplay(display.copy(navLabelSize=it.toInt()))},
                valueRange=8f..20f,steps=11,enabled=showEffectiveNavigationLabels(display),modifier=Modifier.fillMaxWidth())
            }
            GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
                Text("Inbox header & filters",style=MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text("Hide filter chips",Modifier.weight(1f))
                    Switch(checked=display.hideHomeFilters,
                        onCheckedChange={model.setDisplay(display.copy(hideHomeFilters=it))})
                }
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Hide Stories carousel")
                        Text("Stories remain available from the Home options menu.",
                            style=MaterialTheme.typography.bodySmall,
                            color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked=display.hideStoriesCarousel,
                        onCheckedChange={model.setDisplay(display.copy(hideStoriesCarousel=it))})
                }
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Hide home search",style=MaterialTheme.typography.titleSmall)
                        Text("The search field disappears but can be restored here.",
                            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked=display.hideHomeSearch,
                        onCheckedChange={model.setDisplay(display.copy(hideHomeSearch=it))})
                }
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text("Hide photo shortcut",Modifier.weight(1f))
                    Switch(checked=display.hidePhotoShortcut,
                        onCheckedChange={model.setDisplay(display.copy(hidePhotoShortcut=it))})
                }
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("Show my profile name as title",Modifier.weight(1f))
                Switch(display.showProfileAsTitle,{model.setDisplay(display.copy(showProfileAsTitle=it))})
            }
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Hide Archive filter")
                    Text("Archived conversations remain saved.",style=MaterialTheme.typography.bodySmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(display.hideArchiveAccess,{model.setDisplay(display.copy(hideArchiveAccess=it))})
            }
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Hide chat-list message previews",style=MaterialTheme.typography.titleSmall)
                    Text("Shows a generic label instead of the last message in Chats. Does not change push notification previews.",
                        style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked=display.hideHomePreviews,
                    onCheckedChange={model.setDisplay(display.copy(hideHomePreviews=it))})
            }
            HorizontalDivider()
            Text("Header title size · ${display.headerTitleSize} sp",style=MaterialTheme.typography.titleSmall)
            Slider(value=display.headerTitleSize.toFloat(),onValueChange={model.setDisplay(display.copy(headerTitleSize=it.toInt()))},
                valueRange=20f..30f,steps=9,modifier=Modifier.fillMaxWidth())
            }
            GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
                Text("Home wallpaper",style=MaterialTheme.typography.titleMedium)
                Text("Photo wallpaper dimming · ${display.homeWallpaperDim}%",style=MaterialTheme.typography.titleSmall)
            Slider(value=display.homeWallpaperDim.toFloat(),onValueChange={model.setDisplay(display.copy(homeWallpaperDim=it.toInt()))},
                valueRange=0f..100f,modifier=Modifier.fillMaxWidth())
            Text("50% keeps the previous Home wallpaper appearance. Changes apply to locally selected photos.",
                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
                Text("Chat list & typography",style=MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Show chat dividers",style=MaterialTheme.typography.titleSmall)
                        Text("Show separators between conversation rows.",
                            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked=display.showHomeDividers,
                        onCheckedChange={model.setDisplay(display.copy(showHomeDividers=it))})
                }
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text("Show pinned chat indicators",Modifier.weight(1f))
                    Switch(checked=display.showPinBadges,
                        onCheckedChange={model.setDisplay(display.copy(showPinBadges=it))})
                }
                Text("Chat avatar size · ${display.homeAvatarSize} dp",style=MaterialTheme.typography.titleSmall)
            Slider(value=display.homeAvatarSize.toFloat(),onValueChange={model.setDisplay(display.copy(homeAvatarSize=it.toInt()))},
                valueRange=40f..68f,steps=27,modifier=Modifier.fillMaxWidth())
            Text("Conversation row spacing · ${display.homeRowPadding} dp",style=MaterialTheme.typography.titleSmall)
            Slider(value=display.homeRowPadding.toFloat(),onValueChange={model.setDisplay(display.copy(homeRowPadding=it.toInt()))},
                valueRange=6f..20f,steps=13,modifier=Modifier.fillMaxWidth())
            Text("Adjusts avatar diameter and vertical padding without removing unread badges, previews or chat actions.",
                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider()
            Text("Conversation name size · ${display.homeChatNameSize} sp",style=MaterialTheme.typography.titleSmall)
            Slider(value=display.homeChatNameSize.toFloat(),onValueChange={model.setDisplay(display.copy(homeChatNameSize=it.toInt()))},
                valueRange=10f..30f,steps=19,modifier=Modifier.fillMaxWidth())
            Text("Conversation preview size · ${display.homeChatPreviewSize} sp",style=MaterialTheme.typography.titleSmall)
            Slider(value=display.homeChatPreviewSize.toFloat(),onValueChange={model.setDisplay(display.copy(homeChatPreviewSize=it.toInt()))},
                valueRange=12f..18f,steps=5,modifier=Modifier.fillMaxWidth())

            }
        }
                if(section=="Liquid Chat Conversation Screen") {
            GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
                Text("Conversation Screen",style=MaterialTheme.typography.titleLarge)
                Text("Customize private chats without changing messages, delivery or call behavior.",
                    style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
                Text("Conversation header",style=MaterialTheme.typography.titleMedium)
                Text("Contact name size · ${display.chatHeaderNameSize} sp",style=MaterialTheme.typography.titleSmall)
                Slider(value=display.chatHeaderNameSize.toFloat(),
                    onValueChange={model.setDisplay(display.copy(chatHeaderNameSize=it.toInt()))},
                    valueRange=12f..24f,steps=11,modifier=Modifier.fillMaxWidth())
                Text("Call, video and menu icon colors",style=MaterialTheme.typography.titleSmall)
                Row(Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    listOf("Theme","Accent","Text").forEach {mode->
                        FilterChip(selected=display.chatHeaderIconColor==mode,
                            onClick={model.setDisplay(display.copy(chatHeaderIconColor=mode))},
                            label={Text(mode)})
                    }
                }
                Text("Changing icon colors never changes contact actions or calling availability.",
                    style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
                Text("Wallpaper & contrast",style=MaterialTheme.typography.titleMedium)
                Text("Photo wallpaper dimming · ${display.chatWallpaperDim}%",style=MaterialTheme.typography.titleSmall)
            Slider(value=display.chatWallpaperDim.toFloat(),onValueChange={model.setDisplay(display.copy(chatWallpaperDim=it.toInt()))},
                valueRange=0f..100f,modifier=Modifier.fillMaxWidth())
            Text("50% preserves the original conversation wallpaper contrast. Only locally selected photos use this control.",
                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
                Text("Message bubbles",style=MaterialTheme.typography.titleMedium)
            Text("Bubble opacity · ${display.bubbleOpacity}%",style=MaterialTheme.typography.titleSmall)
            Slider(value=display.bubbleOpacity.toFloat(),
                onValueChange={model.setDisplay(display.copy(bubbleOpacity=it.toInt()))},
                valueRange=45f..100f,steps=54,modifier=Modifier.fillMaxWidth())
                Text("Message bubble style",style=MaterialTheme.typography.titleSmall)
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf("Tail","Rounded","Compact").forEach {style ->
                    FilterChip(selected=display.bubbleStyle==style,onClick={model.setDisplay(display.copy(bubbleStyle=style))},label={Text(style)})
                }
            }
            Text("Message bubble color",style=MaterialTheme.typography.titleSmall)
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf("Original","Green","Lavender","Rose","Night").forEach {palette ->
                    FilterChip(selected=display.bubbleColor==palette,onClick={model.setDisplay(display.copy(bubbleColor=palette))},label={Text(palette)})
                }
            }
            Text("Incoming and outgoing bubble colors",style=MaterialTheme.typography.titleSmall)
            Text("Follow uses the shared bubble palette above; independent colors override it.",
                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            listOf(false,true).forEach {mine->
                val selected=if(mine)display.outgoingBubblePreset else display.incomingBubblePreset
                Text(if(mine)"Outgoing bubble" else "Incoming bubble",style=MaterialTheme.typography.labelMedium)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    listOf("Follow","Original","Green","Lavender","Rose","Night").forEach {palette->
                        FilterChip(selected=selected==palette,
                            onClick={model.setDisplay(if(mine)display.copy(outgoingBubblePreset=palette)
                                else display.copy(incomingBubblePreset=palette))},label={Text(palette)})
                    }
                }
            }

            Text("Pick independent bubble colors",style=MaterialTheme.typography.titleSmall)
            listOf(true,false).forEach {mine->
                val current=if(mine)display.outgoingBubbleHex else display.incomingBubbleHex
                var input by rememberSaveable(mine) {mutableStateOf(current)}
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value=input,
                        onValueChange={input=it.take(7)},
                        label={Text(if(mine)"Outgoing #RRGGBB" else "Incoming #RRGGBB")},
                        isError=input.isNotEmpty() && normalizedAccentHex(input)==null,
                        singleLine=true,
                        modifier=Modifier.weight(1f)
                    )
                    FilledTonalButton(
                        enabled=normalizedAccentHex(input)!=null,
                        onClick={
                            normalizedAccentHex(input)?.let {hex->
                                model.setDisplay(if(mine)
                                    display.copy(outgoingBubbleHex=hex,outgoingBubblePreset="Custom")
                                else display.copy(incomingBubbleHex=hex,incomingBubblePreset="Custom"))
                            }
                        }
                    ) {Text("Apply")}
                }
            }
            Text("Message ink & timestamps",style=MaterialTheme.typography.titleSmall)
            listOf("Outgoing text","Incoming text","Quoted text","Message time").forEach {target->
                val selected=when(target) {
                    "Outgoing text"->display.outgoingInk
                    "Incoming text"->display.incomingInk
                    "Quoted text"->display.quotedInk
                    else->display.timestampInk
                }
                Text(target,style=MaterialTheme.typography.labelMedium)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    listOf("Theme","Blue","Green","Purple","Rose","Amber","Custom").forEach {palette->
                        FilterChip(selected=selected==palette,
                            onClick={model.setDisplay(when(target) {
                                "Outgoing text"->display.copy(outgoingInk=palette)
                                "Incoming text"->display.copy(incomingInk=palette)
                                "Quoted text"->display.copy(quotedInk=palette)
                                else->display.copy(timestampInk=palette)
                            })},label={Text(palette)})
                    }
                }
                val currentHex=when(target) {
                    "Outgoing text"->display.outgoingInkHex
                    "Incoming text"->display.incomingInkHex
                    "Quoted text"->display.quotedInkHex
                    else->display.timestampInkHex
                }
                var hexInput by rememberSaveable(target) {mutableStateOf(currentHex)}
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value=hexInput,
                        onValueChange={hexInput=it.take(7)},
                        label={Text("Custom #RRGGBB")},
                        singleLine=true,
                        isError=hexInput.isNotEmpty()&&normalizedAccentHex(hexInput)==null,
                        modifier=Modifier.weight(1f)
                    )
                    FilledTonalButton(
                        enabled=normalizedAccentHex(hexInput)!=null,
                        onClick={
                            normalizedAccentHex(hexInput)?.let {hex->
                                model.setDisplay(when(target) {
                                    "Outgoing text"->display.copy(outgoingInk="Custom",outgoingInkHex=hex)
                                    "Incoming text"->display.copy(incomingInk="Custom",incomingInkHex=hex)
                                    "Quoted text"->display.copy(quotedInk="Custom",quotedInkHex=hex)
                                    else->display.copy(timestampInk="Custom",timestampInkHex=hex)
                                })
                            }
                        }
                    ) {Text("Apply")}
                }
            }
            }
            GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
                Text("Reactions & text",style=MaterialTheme.typography.titleMedium)
                Text("Quick reaction emojis · choose 1–6",style=MaterialTheme.typography.titleSmall)
            Text("Only reactions accepted by the current messaging server can be selected.",
                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            androidx.compose.foundation.layout.FlowRow(
                modifier=Modifier.fillMaxWidth(),
                horizontalArrangement=Arrangement.spacedBy(8.dp),
                verticalArrangement=Arrangement.spacedBy(4.dp)) {
                val selected=MbQuickReactions.selected(display.quickReactions)
                MbQuickReactions.available.forEach {emoji->
                    FilterChip(selected=emoji in selected,
                        onClick={model.setDisplay(display.copy(
                            quickReactions=MbQuickReactions.toggle(display.quickReactions,emoji)))},
                        label={Text(emoji)},
                        enabled=(emoji in selected && selected.size>1) ||
                                (emoji !in selected && selected.size<6))
                }
            }
            TextButton(onClick={model.setDisplay(display.copy(
                quickReactions=MbQuickReactions.defaultSetting))}) {Text("Reset quick reactions")}
            Text("Web link color",style=MaterialTheme.typography.titleSmall)
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf("Theme","Blue","Green","Purple","Rose","Amber").forEach {palette->
                    FilterChip(selected=display.linkColor==palette,
                        onClick={model.setDisplay(display.copy(linkColor=palette))},label={Text(palette)})
                }
            }
            Text("Applies to real HTTPS/HTTP links in message text without changing the link or browser action.",
                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Message font size",style=MaterialTheme.typography.titleSmall)
            Text("${display.messageFontSize} sp",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(value=display.messageFontSize.toFloat(),onValueChange={value->model.setDisplay(display.copy(messageFontSize=value.toInt()))},
                valueRange=10f..30f,steps=19,modifier=Modifier.fillMaxWidth())
            // MB 3.3.4 mb_emoji_sizer: changes the real emoji picker, not message content.
            Text("Emoji picker size · ${display.emojiPickerSize} sp",style=MaterialTheme.typography.titleSmall)
            Slider(value=display.emojiPickerSize.toFloat(),
                onValueChange={value->model.setDisplay(display.copy(emojiPickerSize=value.toInt()))},
                valueRange=18f..32f,steps=13,modifier=Modifier.fillMaxWidth())
            }
            GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
                Text("Live message preview",style=MaterialTheme.typography.titleMedium)
                Text("Preview",style=MaterialTheme.typography.titleSmall)
            listOf(false,true).forEach {mine ->
                Row(Modifier.fillMaxWidth(),horizontalArrangement=if(mine)Arrangement.End else Arrangement.Start) {
                    Surface(shape=conversationBubbleShape(display.bubbleStyle,mine),
                        color=bubbleSurfaceColor(display,mine,LocalGlassDark.current,
                            if(LocalGlassDark.current) {if(mine)Color(0xFF1F5268) else Color(0xFF293F53)}
                            else if(mine)Color(0xFFD0EDFB) else Color(0xFFFAFDFF))) {
                        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                            Text("↪ Original message",
                                style=MaterialTheme.typography.labelSmall,
                                color=conversationInkColor(display.quotedInk,LocalGlassDark.current,
                                    resolvedBubbleTextColor(display,mine,LocalGlassDark.current,MaterialTheme.colorScheme.onSurfaceVariant),display.quotedInkHex))
                            Text(if(mine)"Outgoing message" else "Incoming message",
                                style=MaterialTheme.typography.bodyLarge.copy(fontSize=display.messageFontSize.sp),
                                color=conversationInkColor(if(mine)display.outgoingInk else display.incomingInk,
                                    LocalGlassDark.current,
                                    resolvedBubbleTextColor(display,mine,LocalGlassDark.current,MaterialTheme.colorScheme.onSurface),
                                    if(mine)display.outgoingInkHex else display.incomingInkHex))
                            Text("12:34",modifier=Modifier.align(Alignment.End),
                                style=MaterialTheme.typography.labelSmall,
                                color=conversationInkColor(display.timestampInk,LocalGlassDark.current,
                                    resolvedBubbleTextColor(display,mine,LocalGlassDark.current,MaterialTheme.colorScheme.onSurfaceVariant),display.timestampInkHex))
                        }
                    }
                }
            }

            }
        }
        if(settingsPanelFor(section)==SettingsPanel.POWER_SAVING) GlassCard(Modifier.fillMaxWidth()) {
            Text("Power Saving",style=MaterialTheme.typography.titleMedium)
            Text("Reduce animations to lower visual activity and improve battery comfort.",
                style=MaterialTheme.typography.bodyMedium,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("Reduced motion",Modifier.weight(1f))
                Switch(checked=display.reducedMotion,onCheckedChange={
                    model.setDisplay(display.copy(reducedMotion=it))
                })
            }
            HorizontalDivider()
            Text("Media downloads",style=MaterialTheme.typography.titleSmall)
            Text("Photos and voice messages load on demand. This app does not enable automatic background media downloads.",
                color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Background delivery",style=MaterialTheme.typography.titleSmall)
            Text("Android battery restrictions may delay notifications and incoming calls. The operating system controls these settings.",
                color=MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick={
                val intent=android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.parse("package:${context.packageName}"))
                runCatching {context.startActivity(intent)}
            }) {Text("Open Android app settings")}
        }
        if(section in listOf("Privacy & Security","Notifications","Liquid Chat Privacy","Liquid Chat Notifications")) {
            val privacyPage=section=="Privacy & Security" || section=="Liquid Chat Privacy"
            if(privacyPage) GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
                Text("Device privacy",style=MaterialTheme.typography.titleMedium)
                Text("Local screen and typing protection for this device.",
                    style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)

                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Block screenshots and app previews",style=MaterialTheme.typography.titleSmall)
                        Text("Android blocks screenshots and app-switcher thumbnails on this phone; a separate camera can still photograph the screen.",
                            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked=display.preventScreenshots,
                        onCheckedChange={model.setDisplay(display.copy(preventScreenshots=it))})
                }
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Hide outgoing typing",style=MaterialTheme.typography.titleSmall)
                        Text("This device will not advertise typing to contacts. Other signed-in devices have separate settings.",
                            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked=display.hideOutgoingTyping,
                        onCheckedChange={model.setDisplay(display.copy(hideOutgoingTyping=it))})
                }
                HorizontalDivider()

            }
            GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
                Text(if(privacyPage)"Online presence & read receipts" else "Notification previews",
                    style=MaterialTheme.typography.titleMedium)
                if(privacyPage) {
                val loaded=settings.has("show_presence") && settings.has("read_receipts") && settings.has("notification_previews")
                val enabled=ghostModeActive(settings.optBoolean("show_presence",true),settings.optBoolean("read_receipts",true))
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Ghost Mode",style=MaterialTheme.typography.titleSmall)
                        Text("Hide online/last seen and new read receipts for your account.",
                            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked=enabled,enabled=loaded&&!busy,onCheckedChange={checked ->
                        model.action {
                            val next=ghostModeSettings(checked)
                            model.repository.settings(next.first,next.second,settings.optBoolean("notification_previews",true))
                        }
                    })
                }
                Text("Ghost Mode uses server-enforced account privacy. Switching it off restores both privacy controls; it cannot retract receipts already sent.",
                    style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider()

                }
            if(!settings.has("show_presence")) {Text("Connect to load your privacy settings.");TextButton(onClick={model.action {model.repository.loadSettings()}},enabled=!busy) {Text("Retry")}}
            listOf("show_presence" to "Online & last seen","read_receipts" to "Read receipts","notification_previews" to "Notification previews").filter {if(section=="Notifications" || section=="Liquid Chat Notifications")it.first=="notification_previews" else it.first!="notification_previews"}.forEach { (key,label)->
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text(label,Modifier.weight(1f))
                    Switch(checked=settings.optBoolean(key,true),enabled=!busy&&settings.has(key),onCheckedChange={checked->model.action {
                        model.repository.settings(if(key=="show_presence")checked else settings.optBoolean("show_presence",true),if(key=="read_receipts")checked else settings.optBoolean("read_receipts",true),if(key=="notification_previews")checked else settings.optBoolean("notification_previews",true))
                    }})
                }
            }

            }
        }
        if(section=="Notifications" || section=="Liquid Chat Notifications") GlassCard(
            Modifier.fillMaxWidth(),Glass.hero
        ) {
            Text("Notification icon",style=MaterialTheme.typography.titleMedium)
            Text("Changes the icon on future Liquid Chat message and call notifications.",
                style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf("Classic","Outline").forEach {style->
                    FilterChip(selected=display.notificationIconStyle==style,
                        onClick={model.setDisplay(display.copy(notificationIconStyle=style))},
                        label={Text(style)})
                }
            }
        }
        if(section=="Notifications" || section=="Liquid Chat Notifications") NotificationSystemSettings()
        if(section=="Account") OutlinedButton(onClick={confirm=true},enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text("Sign out")}
        if(section=="Account") TextButton(onClick={deleteAccount=true},enabled=!busy) {Text("Delete account",color=MaterialTheme.colorScheme.error)}
        TextButton(onClick={showPolicy=true}) {Text("Privacy & User Policy")}
        Text("Developed by Pritam Pal\n© 2026 Liquid Chat",style=MaterialTheme.typography.bodySmall)
    }
    if(showPolicy)LiquidAlertDialog(onDismissRequest={showPolicy=false},title={Text("Privacy & User Policy")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){UserPolicyText()}},confirmButton={TextButton(onClick={showPolicy=false}){Text("Done")}})
    if(deleteAccount) LiquidAlertDialog(onDismissRequest={deleteAccount=false;password=""},title={Text("Permanently delete account?")},text={Column(Modifier.verticalScroll(rememberScrollState())) {
        Text("Your profile, sessions, device tokens and owned media will be removed. Sent text remains in the other person's history under Deleted account. $unsent unsent messages will be discarded.")
        Text("Enter your password to verify. For Google accounts, sign in with Google again, then return here and confirm within five minutes.")
        PasswordField(password,{password=it},"Password (email accounts)")
        TextButton(onClick={model.action {val url=model.backend.googleUrl();context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse(url)))}}) {Text("Verify with Google")}
    }},confirmButton={TextButton(onClick={model.deleteAccount(password);password="";deleteAccount=false},enabled=!busy) {Text("Delete permanently")}},dismissButton={TextButton(onClick={deleteAccount=false;password=""}) {Text("Cancel")}})
    if(confirm) LiquidAlertDialog(onDismissRequest={confirm=false},title={Text("Sign out?")},text={Text(if(unsent>0)"$unsent unsent messages will be discarded. Saved local history, drafts and jobs will be removed from this device." else "Saved local history and account data will be removed from this device.")},confirmButton={TextButton(onClick={confirm=false;model.finishLogout()}) {Text("Sign out")}},dismissButton={TextButton(onClick={confirm=false}) {Text("Cancel")}})
}

@Composable internal fun SharedImagePreview(model:AppViewModel,message:Message,thumbnail:Boolean=false) {
    var open by remember(message.id) {mutableStateOf(false)}
    ProtectedImage(model.backend,message.mediaId,message.localImage,"Shared photo",Modifier.fillMaxWidth().aspectRatio(if(thumbnail)1f else (if(message.imageHeight>0&&message.imageWidth>0)message.imageWidth.toFloat()/message.imageHeight else 1f).coerceIn(.5f,2f)).clip(Glass.compact).clickable {open=true},scale=if(thumbnail)androidx.compose.ui.layout.ContentScale.Crop else androidx.compose.ui.layout.ContentScale.Fit)
    if(open) androidx.compose.ui.window.Dialog(onDismissRequest={open=false},properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
            Column(Modifier.statusBarsPadding().navigationBarsPadding()) {
                TextButton(onClick={open=false}) {Text("Close image")}
                var zoom by remember(message.id) {mutableFloatStateOf(1f)}
                var pan by remember(message.id) {mutableStateOf(Offset.Zero)}
                var size by remember {mutableStateOf(IntSize.Zero)}
                val transform=rememberTransformableState {scale,move,_->
                    zoom=(zoom*scale).coerceIn(1f,4f)
                    val limitX=size.width*(zoom-1)/2;val limitY=size.height*(zoom-1)/2
                    pan=Offset((pan.x+move.x).coerceIn(-limitX,limitX),(pan.y+move.y).coerceIn(-limitY,limitY))
                }
                Box(Modifier.weight(1f).fillMaxWidth().clip(androidx.compose.ui.graphics.RectangleShape).onSizeChanged {size=it}.transformable(transform)) {
                    ProtectedImage(model.backend,message.mediaId,message.localImage,"Full-size shared photo",Modifier.fillMaxSize().graphicsLayer {scaleX=zoom;scaleY=zoom;translationX=pan.x;translationY=pan.y})
                }
                TextButton(onClick={zoom=1f;pan=Offset.Zero}) {Text("Reset zoom")}
            }
        }
    }
}
