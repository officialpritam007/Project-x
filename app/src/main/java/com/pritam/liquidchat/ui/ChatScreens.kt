package com.pritam.liquidchat.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.ui.platform.LocalContext
import com.pritam.liquidchat.data.Images
import com.pritam.liquidchat.data.StickerFactory
import com.pritam.liquidchat.data.StickerLibrary
import com.pritam.liquidchat.data.PdfDocuments
import com.pritam.liquidchat.data.PreparedPdf
import com.pritam.liquidchat.data.PreparedChatVideo
import com.pritam.liquidchat.data.ChatVideo
import com.pritam.liquidchat.data.StoryVideo
import com.pritam.liquidchat.data.SavedMedia
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.pritam.liquidchat.AppViewModel
import com.pritam.liquidchat.data.Chat
import com.pritam.liquidchat.data.Message
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

@Composable fun Avatar(name:String,mediaId:String="",backend:com.pritam.liquidchat.data.Backend?=null,self:Boolean=false,size:androidx.compose.ui.unit.Dp=48.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(if(self)Color(0xFF07B4D8) else Color(0xFFB94799),CircleShape),contentAlignment=Alignment.Center) {
        if(mediaId.isNotEmpty()&&backend!=null) ProtectedImage(backend,mediaId,description="Profile photo for $name",modifier=Modifier.fillMaxSize(),scale=androidx.compose.ui.layout.ContentScale.Crop)
        else Text(if(self)name.split(" ").filter {it.firstOrNull()?.isLetter()==true}.take(2).joinToString(""){it.take(1)}.uppercase() else name.take(1).uppercase(),style=MaterialTheme.typography.titleLarge,color=Color.White)
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable fun ChatRow(chat:Chat,backend:com.pritam.liquidchat.data.Backend,onClick:()->Unit,onLongClick:()->Unit={}) {
    val homeDisplay=LocalDisplayOptions.current
    val reference=referenceListUsesFlatRows(homeDisplay)
    Surface(
        shape=if(reference)androidx.compose.ui.graphics.RectangleShape else Glass.card,
        color=if(reference)MaterialTheme.colorScheme.surface.copy(
            alpha=if(LocalGlassDark.current).72f else .86f)
            else if(LocalGlassDark.current)Color(0xFF20384D).copy(alpha=.62f)
            else Color.White.copy(alpha=.56f),
        border=if(reference)null else androidx.compose.foundation.BorderStroke(
            1.dp,Color.White.copy(alpha=if(LocalGlassDark.current).17f else .72f)),
        modifier=Modifier.fillMaxWidth()
            .then(if(reference)Modifier else Modifier.glassSource())
            .combinedClickable(onClick=onClick,onLongClick=onLongClick)
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
    var allStarsOpen by remember(homeOwner) {mutableStateOf(false)}
    val context=LocalContext.current
    var customFolders by remember(homeOwner) {
        mutableStateOf(MbChatFolders.list(context,homeOwner))
    }
    var activeFolder by remember(homeOwner) {mutableStateOf<String?>(null)}
    var folderChat by remember(homeOwner) {mutableStateOf<Chat?>(null)}
    var homePhotoRevision by remember(homeOwner) {mutableIntStateOf(0)}
    val homeForeground by model.repository.foreground.collectAsStateWithLifecycle()
    LaunchedEffect(homeOwner,homeForeground) {
        customFolders=MbChatFolders.list(context,homeOwner)
        if(customFolders.none {it.id==activeFolder})activeFolder=null
    }
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
                    DropdownMenuItem(text={Text("Starred messages")},onClick={
                        allStarsOpen=true;menu=false
                    },enabled=homeOwner!=null)
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
        if(customFolders.isNotEmpty() && !inboxDisplay.hideHomeFilters) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                FilterChip(selected=activeFolder==null,
                    onClick={activeFolder=null},label={Text("All folders")})
                customFolders.forEach {folder->
                    FilterChip(selected=activeFolder==folder.id,
                        onClick={activeFolder=folder.id},
                        label={Text(folder.name)})
                }
            }
        }
        if(connection!="Connected"&&connection!="Online") Text(connection,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        val selectedFolder=customFolders.firstOrNull {it.id==activeFolder}
        val authorizedChatIds=remember(chats,homeOwner) {
            if(homeOwner==null)emptySet() else chats.asSequence()
                .filter {it.account==homeOwner}.map {it.id}.toSet()
        }
        // The local inbox is always scoped to the active authenticated identity,
        // including during session transitions and account switching.
        val visible=chats.filter {it.account==homeOwner && homeOwner!=null}
            .filter {when(activeFilter){1->it.favorite&&!it.archived;2->it.archived;3->(it.unread>0||it.manualUnread)&&!it.archived;else->!it.archived}}
            .filter {selectedFolder==null || it.id in selectedFolder.members}
            .filter {query.isBlank() ||
                it.name.contains(query,true) || it.username.contains(query,true) ||
                (!inboxDisplay.hideHomePreviews && it.preview.contains(query,true))}
        Row(Modifier.fillMaxWidth().padding(horizontal=7.dp,vertical=1.dp),
            verticalAlignment=Alignment.CenterVertically) {
            Text(selectedFolder?.name ?: when(activeFilter){1->"Favorites";2->"Archived chats";3->"Unread chats";else->"Conversations"},
                Modifier.weight(1f),style=MaterialTheme.typography.titleMedium,
                fontWeight=FontWeight.SemiBold)
            Text("${visible.size} shown",style=MaterialTheme.typography.labelMedium,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(
            verticalArrangement=Arrangement.spacedBy(
                if(referenceListUsesFlatRows(inboxDisplay))0.dp else 8.dp),
            contentPadding=PaddingValues(bottom=132.dp)
        ) {
            if(visible.isEmpty()&&query.isBlank()) item {GlassCard(Modifier.fillMaxWidth()) {Text("No conversations here yet.");Text("Open Contacts to find someone by username.",style=MaterialTheme.typography.bodyMedium)}}
            if(visible.isEmpty()&&query.isNotBlank()) item {
                Text("No matching downloaded conversations.",
                    Modifier.fillMaxWidth().padding(20.dp),
                    style=MaterialTheme.typography.bodyMedium,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
                // Never display cached search hits from a different login,
                // a conversation absent from this session, or revoked content.
                val visibleResults=results.filter {
                    homeOwner!=null && it.account==homeOwner &&
                        it.conversation in authorizedChatIds &&
                        it.deletedAt.isEmpty() && it.status=="SENT"
                }
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
    if(!inboxDisplay.hideHomeFloatingButtons) Column(Modifier.align(Alignment.BottomEnd).padding(end=17.dp,bottom=78.dp+WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
        if(!inboxDisplay.hidePhotoShortcut) GlassSurface(Modifier.size(48.dp),CircleShape) {IconButton(onClick=onContacts) {Icon(Icons.Outlined.PhotoCamera,"Choose a contact to share a photo")}}
        // Reference mode already exposes the 56dp new-chat button directly
        // beside the bottom navigation. Avoid rendering a duplicate floating FAB.
        if(inboxDisplay.navBarStyle!="Reference") FloatingActionButton(
            onClick=onContacts,shape=CircleShape,
            containerColor=Color(0xFF00A6E8),contentColor=Color.White,
            modifier=Modifier.size(52.dp)) {Icon(Icons.Outlined.Add,"New chat")}
    }
    }
    if(allStarsOpen && homeOwner!=null) MbAllStarredMessages(
        model,chats,homeOwner,onDismiss={allStarsOpen=false})
    selectedChat?.let {c->
        val previewFlow=remember(model,c.id) {model.repository.messages(c.id)}
        val previewMessages by previewFlow.collectAsStateWithLifecycle(initialValue=emptyList())
        LiquidActionSheet(title=c.name,onDismiss={selectedChat=null}) {
        if(inboxDisplay.showLongPressPreview) {
        Text("Conversation preview",style=MaterialTheme.typography.titleSmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(horizontal=12.dp))
        GlassCard(Modifier.fillMaxWidth(),Glass.compact) {
            val preview=previewMessages.filter {
                homeOwner!=null && c.account==homeOwner &&
                    model.session.value?.userId==homeOwner &&
                    it.account==homeOwner && it.conversation==c.id &&
                    it.status=="SENT" && it.deletedAt.isEmpty()
            }.takeLast(3)
            if(preview.isEmpty()) Text("No downloaded messages to preview.",
                style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
            else preview.forEach {m->
                MbConversationPeekRow(m,c.name)
            }
        }
        }
        GlassActionRow("Open conversation",Icons.Outlined.ChatBubbleOutline,
            onClick={selectedChat=null;model.open(c.id)})
        if(customFolders.isNotEmpty())GlassActionRow("Add to Chat Folder",
            Icons.Outlined.FolderOpen,
            onClick={selectedChat=null;folderChat=c})
        HorizontalDivider(Modifier.padding(vertical=8.dp))
        GlassActionRow(if(c.pinnedAt>0)"Unpin" else "Pin",Icons.Outlined.PushPin,
            onClick={selectedChat=null;model.action {model.repository.chatAction(c.id,"pin",c.pinnedAt==0L)}})
        val unreadNow=c.unread>0 || c.manualUnread
        GlassActionRow(if(unreadNow)"Mark as read" else "Mark unread",
            if(unreadNow)Icons.Outlined.DoneAll else Icons.Outlined.MarkChatUnread,
            onClick={
                selectedChat=null
                model.action {
                    if(unreadNow)model.repository.markChatRead(c.id)
                    else model.repository.chatAction(c.id,"unread",true)
                }
            })
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
    folderChat?.let {c->
        LiquidAlertDialog(onDismissRequest={folderChat=null},
            title={Text("Chat folders")},
            text={Column(Modifier.fillMaxWidth(),
                verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(c.name,style=MaterialTheme.typography.titleSmall)
                Text("Select the folders where this chat should appear.",
                    style=MaterialTheme.typography.bodySmall)
                customFolders.forEach {folder->
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Checkbox(checked=c.id in folder.members,
                            onCheckedChange={checked->
                                if(homeOwner!=null && c.account==homeOwner)
                                    runCatching {
                                        MbChatFolders.membership(context,homeOwner,
                                            folder.id,c.id,checked)
                                    }.onSuccess {customFolders=it}
                            })
                        Text(folder.name)
                    }
                }
            }},
            confirmButton={TextButton(onClick={folderChat=null}) {Text("Done")}}
        )
    }
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
        LazyColumn(
            contentPadding=PaddingValues(bottom=140.dp+WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            verticalArrangement=Arrangement.spacedBy(
                if(referenceListUsesFlatRows(LocalDisplayOptions.current))0.dp else 8.dp)
        ) {
            item {Text(if(alphabetical)"Sorted by name" else "Reverse name order",Modifier.padding(horizontal=16.dp),color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold,fontSize=15.sp)}
            if(filtered.isEmpty()) item {GlassCard(Modifier.fillMaxWidth()){Text(if(username.isBlank())"Find someone by username and save them here." else "No saved contacts match.")}}
            items(filtered,key={"contact-${it.id}"}) {contact->
                ReferenceListRow(Modifier.fillMaxWidth()) {
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
    if(id in ids) ids-id else if(ids.size<30) ids+id else ids

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
    var actionMessage by remember(cid,session?.userId) {mutableStateOf<Message?>(null)}
    var speechCandidate by remember(cid,session?.userId) {mutableStateOf<Message?>(null)}
    var spokenMessageId by remember(cid,session?.userId) {mutableStateOf<String?>(null)}
    // Long-press menus must close as soon as a message is revoked, hidden,
    // deleted, or an active account switches; never preserve stale payloads.
    LaunchedEffect(messages,actionMessage?.id,session?.userId) {
        val opened=actionMessage
        if(opened!=null && messages.none {
            it.account==session?.userId && it.id==opened.id &&
                it.deletedAt.isEmpty()
        }) actionMessage=null
    }
    var historyMessage by remember(cid,session?.userId) {mutableStateOf<Message?>(null)}
    var historyRevisions by remember(cid,session?.userId) {
        mutableStateOf(emptyList<MessageEditVersion>())
    }
    var historyLoading by remember(cid,session?.userId) {mutableStateOf(false)}
    var historyError by remember(cid,session?.userId) {mutableStateOf<String?>(null)}
    // Never keep a prior version on screen after the source message becomes
    // hidden, deleted, or disappears on an account/active-chat change.
    LaunchedEffect(messages,historyMessage?.id,session?.userId) {
        val visibleId=historyMessage?.id
        if(visibleId!=null && messages.none {
            it.id==visibleId && it.account==session?.userId &&
                it.status=="SENT" && it.deletedAt.isEmpty()
        }) {
            historyMessage=null
            historyRevisions=emptyList()
            historyLoading=false
            historyError=null
        }
    }
    var forwardMessages by remember(cid,session?.userId) {mutableStateOf<List<Message>>(emptyList())}
    var selectedIds by remember(cid,session?.userId) {mutableStateOf<Set<String>>(emptySet())}
    var forwardSearch by remember(cid,session?.userId) {mutableStateOf("")}
    val clipboard=LocalClipboardManager.current
    fun copiedText(message:Message,includeDetails:Boolean):String {
        if(!includeDetails)return message.body
        val from=if(message.sender==message.account)"You" else chat?.name.orEmpty().ifBlank {"Contact"}
        val timestamp=java.time.Instant.ofEpochMilli(message.localTime)
            .atZone(java.time.ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
        return "$from · $timestamp:\n${message.body}"
    }
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
    // Android TTS may use the installed engine's cloud provider: speaking is
    // never automatic, and a separate confirmation is required per message.
    val messageSpeaker=remember(context.applicationContext,cid,session?.userId) {
        MbMessageReadAloud(context.applicationContext)
    }
    val speechLifecycleOwner=LocalLifecycleOwner.current
    DisposableEffect(messageSpeaker,speechLifecycleOwner) {
        val observer=LifecycleEventObserver {_,event->
            if(event==Lifecycle.Event.ON_STOP) {
                messageSpeaker.stop()
                spokenMessageId=null
            }
        }
        speechLifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            speechLifecycleOwner.lifecycle.removeObserver(observer)
            messageSpeaker.close()
        }
    }
    LaunchedEffect(messages,speechCandidate?.id,spokenMessageId,session?.userId) {
        val requested=speechCandidate?.id
        if(requested!=null && messages.none {
            it.id==requested && it.account==session?.userId && it.deletedAt.isEmpty()
        }) speechCandidate=null
        val speaking=spokenMessageId
        if(speaking!=null && messages.none {
            it.id==speaking && it.account==session?.userId && it.deletedAt.isEmpty()
        }) {
            messageSpeaker.stop()
            spokenMessageId=null
        }
    }
    val playOutgoingConversationCue=rememberConversationSendCue(
        model,cid,session?.userId,messages,messagesReady==cid,
        chat?.lastSeq ?: 0L,chat?.muted!=false,foreground)
    var exportOwner by remember(cid) {mutableStateOf<String?>(null)}
    var exportFeedback by remember(cid) {mutableStateOf<String?>(null)}
    val exportChatText=rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")) {uri->
        val owner=exportOwner
        exportOwner=null
        if(uri!=null && owner!=null && model.backend.session.value?.userId==owner)
            model.action {
                val content=ChatTextExport.render(model.messages.value,owner,cid,
                    chat?.name.orEmpty())
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    require(model.backend.session.value?.userId==owner) {
                        "Account changed before exporting."
                    }
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(content.toByteArray(Charsets.UTF_8))
                        it.flush()
                    } ?: error("Cannot write selected document.")
                }
                exportFeedback="Downloaded chat text exported."
            }
    }
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
    var prepared by remember(cid,session?.userId) {mutableStateOf<Images.Prepared?>(null)}
    var photoUndo by remember(cid,session?.userId) {
        mutableStateOf<List<Images.Prepared>>(emptyList())
    }
    var photoRedo by remember(cid,session?.userId) {
        mutableStateOf<List<Images.Prepared>>(emptyList())
    }
    fun replacePhotoDraft(next:Images.Prepared) {
        prepared?.file?.delete()
        photoUndo.forEach {it.file.delete()}
        photoUndo=emptyList()
        photoRedo.forEach {it.file.delete()}
        photoRedo=emptyList()
        prepared=next
    }
    fun clearPhotoDraft() {
        prepared?.file?.delete()
        photoUndo.forEach {it.file.delete()}
        photoUndo=emptyList()
        photoRedo.forEach {it.file.delete()}
        photoRedo=emptyList()
        prepared=null
    }
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {uri->if(uri!=null)model.action {
        val account=model.backend.session.value?.userId ?: return@action
        val image=Images.prepare(context,uri,account,hd=model.display.value.photoQuality=="HD")
        if(model.backend.session.value?.userId==account)replacePhotoDraft(image) else image.file.delete()
    }}
    // The reference app allows multi-select. Each image is still an independent
    // authorized and retriable Liquid Chat message (no invented album backend).
    var preparedAlbum by remember(cid,session?.userId) {
        mutableStateOf<List<Images.Prepared>>(emptyList())
    }
    var albumSending by remember(cid,session?.userId) {mutableStateOf(false)}
    var albumCaption by remember(cid,session?.userId) {mutableStateOf("")}
    var albumEditIndex by remember(cid,session?.userId) {mutableIntStateOf(0)}
    var albumEditing by remember(cid,session?.userId) {mutableStateOf(false)}
    val albumPicker=rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()) {uris->
        if(uris.isNotEmpty())model.action {
            val owner=model.backend.session.value?.userId ?: return@action
            require(uris.size<=10) {"Choose up to ten photos at a time."}
            val staged=mutableListOf<Images.Prepared>()
            try {
                uris.distinct().forEach {uri->
                    check(model.backend.session.value?.userId==owner) {
                        "The signed-in account changed while preparing photos."
                    }
                    staged+=Images.prepare(context,uri,owner,
                        hd=model.display.value.photoQuality=="HD")
                }
                if(model.backend.session.value?.userId==owner) {
                    preparedAlbum.forEach {it.file.delete()}
                    preparedAlbum=staged.toList()
                    albumCaption=""
                    albumEditIndex=0
                } else error("The signed-in account changed.")
            } catch(error:Exception) {
                staged.forEach {it.file.delete()}
                throw error
            }
        }
    }
    var videoPickerOwner by remember(cid,session?.userId) {mutableStateOf<String?>(null)}
    var selectedVideo by remember(cid,session?.userId) {
        mutableStateOf<android.net.Uri?>(null)
    }
    var selectedVideoLength by remember(cid,session?.userId) {mutableLongStateOf(0L)}
    var videoStartMs by remember(cid,session?.userId) {mutableFloatStateOf(0f)}
    var preparingVideo by remember(cid,session?.userId) {mutableStateOf(false)}
    var preparedChatVideo by remember(cid,session?.userId) {
        mutableStateOf<PreparedChatVideo?>(null)
    }
    var videoCaption by remember(cid,session?.userId) {mutableStateOf("")}
    var videoSending by remember(cid,session?.userId) {mutableStateOf(false)}
    val videoPicker=rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()) {uri->
        val requestedOwner=videoPickerOwner
        videoPickerOwner=null
        if(uri!=null && requestedOwner!=null &&
            model.backend.session.value?.userId==requestedOwner) model.action {
            val length=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                StoryVideo.durationMs(context,uri)
            }
            if(model.backend.session.value?.userId==requestedOwner) {
                selectedVideo=uri
                selectedVideoLength=length
                videoStartMs=0f
            }
        }
    }
    var preparedPdf by remember(cid,session?.userId) {mutableStateOf<PreparedPdf?>(null)}
    var selectedPdfBatch by remember(cid,session?.userId) {
        mutableStateOf<List<PreparedPdf>>(emptyList())
    }
    var sendingPdfBatch by remember(cid,session?.userId) {mutableStateOf(false)}
    val pdfBatchPicker=rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()) {uris->
        if(uris.isNotEmpty())model.action {
            val owner=model.backend.session.value?.userId ?: return@action
            require(uris.size<=5 && uris.distinct().size==uris.size) {
                "Choose up to five different PDF documents."
            }
            val staged=mutableListOf<PreparedPdf>()
            try {
                uris.forEach {uri->
                    check(model.backend.session.value?.userId==owner) {
                        "The account changed during document preparation."
                    }
                    staged+=PdfDocuments.prepare(context,uri,owner)
                }
                if(model.backend.session.value?.userId==owner) {
                    selectedPdfBatch.forEach {it.file.delete()}
                    selectedPdfBatch=staged.toList()
                } else throw java.io.IOException("The account changed.")
            } catch(e:Exception) {
                staged.forEach {it.file.delete()}
                throw e
            }
        }
    }
    val documentPicker=rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()) {uri->
        if(uri!=null)model.action {
            val account=model.backend.session.value?.userId ?: return@action
            val pdf=PdfDocuments.prepare(context,uri,account)
            if(model.backend.session.value?.userId==account) {
                preparedPdf?.file?.delete()
                preparedPdf=pdf
            } else pdf.file.delete()
        }
    }
    val galleryDocumentScanner=rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()) {pages->
        if(pages.isNotEmpty())model.action {
            val account=model.backend.session.value?.userId ?: return@action
            val selected=pages.take(10)
            require(pages.size<=10) {"Choose at most ten pages."}
            val pdf=PdfDocuments.scanGalleryPhotos(context,selected,account)
            if(model.backend.session.value?.userId==account) {
                preparedPdf?.file?.delete()
                preparedPdf=pdf
            } else pdf.file.delete()
        }
    }
    var stickerLibraryOpen by remember(cid,session?.userId) {mutableStateOf(false)}
    var savedStickers by remember(cid,session?.userId) {
        mutableStateOf(StickerLibrary.list(context,session?.userId))
    }
    var favoriteStickers by remember(cid,session?.userId) {
        mutableStateOf(StickerLibrary.favoriteNames(context,session?.userId))
    }
    var recentStickers by remember(cid,session?.userId) {
        mutableStateOf(StickerLibrary.recent(context,session?.userId))
    }
    var stickerShelfFilter by remember(cid,session?.userId) {mutableStateOf("All")}
    LaunchedEffect(stickerLibraryOpen,session?.userId) {
        if(stickerLibraryOpen) {
            savedStickers=StickerLibrary.list(context,session?.userId)
            favoriteStickers=StickerLibrary.favoriteNames(context,session?.userId)
            recentStickers=StickerLibrary.recent(context,session?.userId)
        }
    }
    var stickerNotice by remember(cid,session?.userId) {mutableStateOf<String?>(null)}
    var confirmSavedSticker by remember(cid,session?.userId) {mutableStateOf<java.io.File?>(null)}
    var sendingSavedSticker by remember(cid,session?.userId) {mutableStateOf(false)}
    fun sendSavedSticker(file:java.io.File) {
        val owner=session?.userId ?: return
        if(sendingSavedSticker)return
        sendingSavedSticker=true
        model.action {
            try {
                check(model.session.value?.userId==owner && model.repository.active.value==cid) {
                    "Conversation or account changed. Open the sticker shelf again."
                }
                val outgoing=StickerLibrary.prepareForSend(context,owner,file)
                try {
                    check(model.session.value?.userId==owner && model.repository.active.value==cid) {
                        "Conversation or account changed."
                    }
                    model.repository.sendSticker(cid,outgoing)
                    // The send is already queued. A metadata storage error must
                    // never report the actual message as unsent or retry it.
                    runCatching {StickerLibrary.markUsed(context,owner,file)}
                    if(model.session.value?.userId==owner)
                        recentStickers=StickerLibrary.recent(context,owner)
                    confirmSavedSticker=null
                    stickerLibraryOpen=false
                } catch(e:Exception) {
                    outgoing.file.delete()
                    throw e
                }
            } finally {sendingSavedSticker=false}
        }
    }
    var preparedSticker by remember(cid,session?.userId) {mutableStateOf<Images.Prepared?>(null)}
    var stickerSourceUri by remember(cid,session?.userId) {mutableStateOf<android.net.Uri?>(null)}
    var removeStickerWhite by remember(cid,session?.userId) {mutableStateOf(true)}
    var stickerLabel by remember(cid,session?.userId) {mutableStateOf("")}
    var appliedStickerLabel by remember(cid,session?.userId) {mutableStateOf("")}
    val stickerPicker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {uri->
        if(uri!=null)model.action {
            val account=model.backend.session.value?.userId ?: return@action
            val created=StickerFactory.prepare(context,uri,account,removeWhite=true)
            if(model.backend.session.value?.userId==account) {
                preparedSticker?.file?.delete()
                preparedSticker=created
                stickerSourceUri=uri
                removeStickerWhite=true
                stickerLabel=""
                appliedStickerLabel=""
            } else created.file.delete()
        }
    }
    var contactPickerAccount by remember(cid) {mutableStateOf<String?>(null)}
    val phonePicker=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {result->
        val selectedAccount=contactPickerAccount
        contactPickerAccount=null
        val uri=result.data?.data
        if(result.resultCode==android.app.Activity.RESULT_OK && uri!=null &&
            selectedAccount!=null && session?.userId==selectedAccount) {
            runCatching {
                val resolver=context.contentResolver
                val projection=arrayOf(
                    android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                resolver.query(uri,projection,null,null,null)?.use {cursor->
                    if(!cursor.moveToFirst())return@use
                    val name=cursor.getString(0).orEmpty().trim().take(100)
                    val number=cursor.getString(1).orEmpty().trim().take(64)
                    if(number.isNotBlank()) {
                        val detail=buildString {
                            append("Contact: ")
                            append(if(name.isBlank())"Unknown" else name)
                            append('\n')
                            append(number)
                        }
                        val next=if(body.isBlank())detail else "$body\n$detail"
                        require(next.length<=4000) {"Draft would exceed 4000 characters."}
                        updateInput(TextFieldValue(next,TextRange(next.length)))
                    } else error("That contact has no usable phone number.")
                } ?: error("Cannot read the selected contact.")
            }.onFailure {error->
                model.action {throw java.io.IOException(
                    error.message ?: "Could not read that contact.")}
            }
        }
    }
    var textToolsOpen by remember(cid,session?.userId) {mutableStateOf(false)}
    var textToolChoice by remember(cid,session?.userId) {
        mutableStateOf<OfflineTextAction?>(null)
    }
    var textToolSource by remember(cid,session?.userId) {mutableStateOf("")}
    var attachments by remember(cid) {mutableStateOf(false)}
    var cameraPath by rememberSaveable(cid) {mutableStateOf("")}
    var cameraAccount by rememberSaveable(cid) {mutableStateOf("")}
    val camera=rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {ok->
        val file=java.io.File(cameraPath)
        if(ok)model.action {
            try {
                if(model.backend.session.value?.userId==cameraAccount) {
                    val uri=androidx.core.content.FileProvider.getUriForFile(context,context.packageName+".files",file)
                    val image=Images.prepare(context,uri,cameraAccount,hd=model.display.value.photoQuality=="HD")
                    if(model.backend.session.value?.userId==cameraAccount)replacePhotoDraft(image) else image.file.delete()
                }
            } finally {file.delete();cameraPath=""}
        } else {file.delete();cameraPath=""}
    }
    var scannerPath by rememberSaveable(cid) {mutableStateOf("")}
    var scannerAccount by rememberSaveable(cid) {mutableStateOf("")}
    val scanner=rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {success->
        val photo=java.io.File(scannerPath)
        if(success)model.action {
            try {
                val owner=scannerAccount
                if(owner.isNotBlank() && model.backend.session.value?.userId==owner) {
                    val pdf=PdfDocuments.scanPhoto(context,photo,owner)
                    if(model.backend.session.value?.userId==owner) {
                        preparedPdf?.file?.delete()
                        preparedPdf=pdf
                    } else pdf.file.delete()
                }
            } finally {
                photo.delete()
                scannerPath=""
                scannerAccount=""
            }
        } else {
            photo.delete()
            scannerPath=""
            scannerAccount=""
        }
    }
    val display=LocalDisplayOptions.current
    var quickRepliesOpen by remember(cid,session?.userId) {mutableStateOf(false)}
    var quickReplyShortcut by remember(cid,session?.userId) {mutableStateOf("")}
    var quickReplyText by remember(cid,session?.userId) {mutableStateOf("")}
    var quickReplyFeedback by remember(cid,session?.userId) {mutableStateOf<String?>(null)}
    var quickReplies by remember(cid,session?.userId) {
        mutableStateOf(MbQuickReplies.list(context,session?.userId))
    }
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
    // Back dismisses the active chat subview before leaving the conversation.
    // The keyboard and native bottom sheets keep their normal system behavior.
    BackHandler {
        when(conversationBackAction(searchOpen,selectedIds.isNotEmpty())) {
            ConversationBackAction.CloseSearch->{
                searchOpen=false
                chatQuery=""
                searchIndex=0
                model.focusMessage.value=null
            }
            ConversationBackAction.ClearSelection->selectedIds=emptySet()
            ConversationBackAction.CloseChat->model.closeChat()
        }
    }
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
        // Keep every existing authenticated action; reconstruct only visual chrome.
        // The reference home toolbar has a compact 47dp content band, while
        // the original Liquid alternative retains its capsule and margins.
        val referenceChrome=referenceListUsesFlatRows(display)
        GlassChromeSurface(
            Modifier.fillMaxWidth().padding(
                horizontal=if(referenceChrome)0.dp else 8.dp,
                vertical=if(referenceChrome)0.dp else 3.dp),
            if(referenceChrome)Glass.compact else Glass.capsule
        ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min=if(referenceChrome)47.dp else 0.dp)
                .padding(horizontal=if(referenceChrome)0.dp else 4.dp,
                    vertical=if(referenceChrome)0.dp else 2.dp),
            verticalAlignment=Alignment.CenterVertically) {
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
                        GlassActionRow("Quick replies",Icons.Outlined.ChatBubbleOutline,
                            onClick={menu=false;quickRepliesOpen=true})
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
                        GlassActionRow("Export chat text",Icons.Outlined.FileUpload,
                            onClick={
                                menu=false
                                exportOwner=session?.userId
                                if(exportOwner!=null)exportChatText.launch("LiquidChat-Chat.txt")
                            })
                        GlassActionRow("Starred messages",Icons.Outlined.StarOutline,
                            onClick={menu=false;starsOpen=true})
                        GlassActionRow("Custom notifications",Icons.Outlined.Notifications,
                            onClick={menu=false;notificationsOpen=true})
                        HorizontalDivider(Modifier.padding(vertical=8.dp))
                        GlassActionRow(if(c.pinnedAt>0)"Unpin" else "Pin",Icons.Outlined.PushPin,
                            onClick={menu=false;model.action {model.repository.chatAction(c.id,"pin",c.pinnedAt==0L)}})
                        GlassActionRow("Chat appearance",Icons.Outlined.Wallpaper,onClick={menu=false;appearance=true})
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
        if(searchOpen) GlassSurface(
            Modifier.fillMaxWidth().padding(horizontal=if(referenceListUsesFlatRows(display))0.dp else 8.dp),
            if(referenceListUsesFlatRows(display))Glass.compact else Glass.card
        ) {
            Column(Modifier.padding(8.dp)) {
                // The reference APK has a 35dp/24dp search row. Retain the
                // real downloaded-message filters, query and explicit dismissal.
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    GlassSearch(chatQuery,{chatQuery=it;searchIndex=0},
                        "Search this chat",Modifier.weight(1f))
                    IconButton(onClick={searchOpen=false;chatQuery=""}) {
                        Icon(Icons.Outlined.Close,"Close search")
                    }
                }
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
                onDoubleTap={
                    if(canDoubleTapReact(m,display.doubleTapReactionEnabled,
                            selectedIds.isNotEmpty(),session?.userId))model.action {
                        val current=model.messages.value.firstOrNull {
                            it.id==m.id && it.conversation==cid && it.account==m.account
                        }
                        if(current!=null && canDoubleTapReact(current,
                                model.display.value.doubleTapReactionEnabled,
                                selectedIds.isNotEmpty(),model.session.value?.userId)) {
                            val emoji=normalizedDoubleTapReaction(model.display.value.doubleTapReactionEmoji)
                            val previous=runCatching {org.json.JSONObject(current.reactions)
                                .optString(current.account)}.getOrDefault("")
                            model.repository.messageAction(current.id,"react",if(previous==emoji)"" else emoji)
                        }
                    }
                },
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
                    clipboard.setText(AnnotatedString(selectedText.joinToString("\n") {copiedText(it,display.copySenderAndTime)}))
                    selectedIds=emptySet()
                },enabled=selectedText.isNotEmpty()) {Text("Copy")}
                TextButton(onClick={
                    forwardMessages=selectedText
                    forwardSearch=""
                    selectedIds=emptySet()
                },enabled=selectedText.isNotEmpty()) {Text("Forward")}
                val starCandidates=selectedText.filter {it.status=="SENT"}
                val shouldStar=starCandidates.any {!it.starred}
                TextButton(onClick={
                    val toChange=starCandidates.filter {it.starred==!shouldStar}
                    selectedIds=emptySet()
                    model.action {model.repository.starMessages(toChange.map {it.id},shouldStar)}
                },enabled=starCandidates.isNotEmpty()) {
                    Text(if(shouldStar)"Star" else "Unstar")
                }
                IconButton(onClick={selectedIds=emptySet()}) {Icon(Icons.Outlined.Close,"Cancel selection")}
            }
        }
        val matchingQuickReplies=MbQuickReplies.matches(quickReplies,body)
        if(matchingQuickReplies.isNotEmpty() && draftReady && !display.hideQuickReplySuggestions) {
            GlassSurface(Modifier.fillMaxWidth().padding(horizontal=10.dp),Glass.compact) {
                Column(Modifier.fillMaxWidth().padding(8.dp),
                    verticalArrangement=Arrangement.spacedBy(2.dp)) {
                    Text("Quick replies",style=MaterialTheme.typography.labelMedium,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                    matchingQuickReplies.forEach {reply->
                        TextButton(onClick={
                            updateInput(TextFieldValue(reply.body,TextRange(reply.body.length)))
                        },modifier=Modifier.fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth()) {
                                Text("/"+reply.shortcut,style=MaterialTheme.typography.labelLarge)
                                Text(reply.body,maxLines=1,overflow=TextOverflow.Ellipsis,
                                    style=MaterialTheme.typography.bodySmall,
                                    color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
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
        // Reference mode keeps the composer flush with the conversation edge.
        // No changes to draft revisions, keyboard focus, voice lock or send state.
        val referenceComposer=referenceListUsesFlatRows(display)
        GlassChromeSurface(
            Modifier.fillMaxWidth().padding(
                horizontal=if(referenceComposer)0.dp else 8.dp,
                vertical=if(referenceComposer)2.dp else 8.dp),
            if(referenceComposer)Glass.compact else Glass.capsule
        ) {
            Row(Modifier.fillMaxWidth().padding(
                horizontal=if(referenceComposer)8.dp else 4.dp,
                vertical=if(referenceComposer)2.dp else 4.dp),
                verticalAlignment=Alignment.Bottom,
                horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                IconButton(onClick={emojiPicker=true},enabled=draftReady) {Icon(Icons.Outlined.EmojiEmotions,"Open emoji picker")}
                androidx.compose.foundation.text.BasicTextField(input,{if(it.text.length<=4000)updateInput(it)},enabled=draftReady,keyboardOptions=KeyboardOptions(capitalization=KeyboardCapitalization.Sentences,keyboardType=KeyboardType.Text),modifier=Modifier.weight(1f).heightIn(min=48.dp,max=140.dp).padding(vertical=14.dp).onFocusChanged {inputFocused=it.isFocused}.testTag("message-input").semantics {contentDescription="Message"},textStyle=MaterialTheme.typography.bodyLarge.copy(color=MaterialTheme.colorScheme.onSurface),maxLines=5,decorationBox={inner->Box {if(body.isEmpty())Text("Message",color=MaterialTheme.colorScheme.onSurfaceVariant);inner()}})
                IconButton(onClick={attachments=true},enabled=!busy&&replyTo.isEmpty()) {Icon(Icons.Outlined.AttachFile,"Attach image")}
                if(body.isNotBlank()) IconButton(
                    onClick={textToolsOpen=true},enabled=draftReady&&!queueing,
                    modifier=Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.AutoFixHigh,"Offline text tools")
                }
                if(body.isBlank()&&replyTo.isEmpty())VoiceCapture(model,cid,busy) else FilledIconButton(onClick={if(body.isNotBlank()&&!queueing){
                    val submittedBody=body
                    val submittedReply=replyTo
                    val submittedVersion=draftVersion
                    queueing=true
                    model.send(cid,submittedBody,submittedReply,submittedVersion){
                        playOutgoingConversationCue()
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
    if(textToolsOpen) LiquidActionSheet(
        title="Text tools · offline",onDismiss={textToolsOpen=false}) {
        Text("Only this unsent draft is transformed. This is not an AI service and your text stays on this phone.",
            modifier=Modifier.padding(horizontal=16.dp,vertical=8.dp),
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        OfflineTextAction.entries.forEach {option->
            GlassActionRow(option.label,Icons.Outlined.TextFields,onClick={
                textToolSource=body
                textToolChoice=option
                textToolsOpen=false
            })
        }
    }
    textToolChoice?.let {choice->
        val result=remember(textToolSource,choice) {
            transformDraftOffline(textToolSource,choice)
        }
        LiquidAlertDialog(
            onDismissRequest={textToolChoice=null},
            title={Text(choice.label)},
            text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("Preview",style=MaterialTheme.typography.labelMedium)
                Text(result,modifier=Modifier.fillMaxWidth().heightIn(max=240.dp)
                    .verticalScroll(rememberScrollState()))
                if(body!=textToolSource)Text("Your draft has changed. Close and open Text tools again.",
                    color=MaterialTheme.colorScheme.error,
                    style=MaterialTheme.typography.bodySmall)
            }},
            confirmButton={TextButton(onClick={
                if(body==textToolSource) {
                    updateInput(TextFieldValue(result,TextRange(result.length)))
                }
                textToolChoice=null
            },enabled=draftReady&&!queueing&&body==textToolSource&&
                result!=textToolSource) {Text("Apply to draft")}},
            dismissButton={TextButton(onClick={textToolChoice=null}) {Text("Cancel")}}
        )
    }
    if(attachments)LiquidActionSheet(title="Attach",onDismiss={attachments=false}) {
        if(display.navBarStyle=="Reference") {
            MbReferenceSwitchRow(
                title="Send image in HD quality",
                subtitle="Higher-quality private JPEG messages",
                checked=display.photoQuality=="HD",
                icon=Icons.Outlined.PhotoLibrary,
                onCheckedChange={model.setDisplay(display.copy(
                    photoQuality=if(it)"HD" else "Standard"))})
            HorizontalDivider()
            MbReferenceSwitchRow(
                title="Send video in HD quality",
                subtitle="720p on · 480p off · maximum 30 seconds",
                checked=display.videoQuality=="HD",
                icon=Icons.Outlined.Videocam,
                onCheckedChange={model.setDisplay(display.copy(
                    videoQuality=if(it)"HD" else "Standard"))})
            HorizontalDivider()
        } else {
        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=6.dp),
            verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("HD photo upload",style=MaterialTheme.typography.titleSmall)
                Text("Larger JPEG photos; same private media delivery.",
                    style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked=display.photoQuality=="HD",
                onCheckedChange={model.setDisplay(display.copy(
                    photoQuality=if(it)"HD" else "Standard"))})
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=6.dp),
            verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("HD video upload",style=MaterialTheme.typography.titleSmall)
                Text("HD encodes 720p; off uses 480p. Both remain private and capped at 30 seconds / 8 MB.",
                    style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked=display.videoQuality=="HD",
                onCheckedChange={model.setDisplay(display.copy(
                    videoQuality=if(it)"HD" else "Standard"))})
        }
        }
        MbReferenceAttachmentGrid(listOf(
            MbAttachmentChoice("Gallery",Icons.Outlined.PhotoLibrary) {
                attachments=false;picker.launch("image/*")
            },
            MbAttachmentChoice("Select multiple photos",Icons.Outlined.PhotoLibrary) {
                attachments=false;albumPicker.launch("image/*")
            },
            MbAttachmentChoice("Video (30 seconds)",Icons.Outlined.Videocam) {
                attachments=false
                videoPickerOwner=session?.userId
                if(videoPickerOwner!=null)videoPicker.launch("video/*")
            },
            MbAttachmentChoice("Documents (PDF)",Icons.Outlined.Description) {
                attachments=false;documentPicker.launch(arrayOf("application/pdf"))
            },
            MbAttachmentChoice("Select multiple PDFs",Icons.Outlined.Description) {
                attachments=false;pdfBatchPicker.launch("application/pdf")
            },
            MbAttachmentChoice("Combine photos into PDF",Icons.Outlined.Description) {
                attachments=false;galleryDocumentScanner.launch("image/*")
            },
            MbAttachmentChoice("Scan document",Icons.Outlined.PhotoCamera) {
                attachments=false
                val owner=model.backend.session.value?.userId
                if(owner!=null) {
                    scannerAccount=owner
                    val file=java.io.File(context.cacheDir,
                        "camera/${java.util.UUID.randomUUID()}.jpg")
                    file.parentFile?.mkdirs()
                    scannerPath=file.absolutePath
                    runCatching {
                        scanner.launch(androidx.core.content.FileProvider.getUriForFile(
                            context,context.packageName+".files",file))
                    }.onFailure {
                        file.delete()
                        scannerPath=""
                        scannerAccount=""
                        model.action {throw java.io.IOException("Camera unavailable for document scanning.")}
                    }
                }
            },
            MbAttachmentChoice("Create sticker",Icons.Outlined.AutoAwesome) {
                attachments=false;stickerPicker.launch("image/*")
            },
            MbAttachmentChoice("My stickers",Icons.Outlined.Collections) {
                attachments=false
                savedStickers=StickerLibrary.list(context,session?.userId)
                stickerNotice=null
                stickerLibraryOpen=true
            },
            MbAttachmentChoice("Share contact as text",Icons.Outlined.PersonOutline) {
                attachments=false
                contactPickerAccount=session?.userId
                val intent=android.content.Intent(android.content.Intent.ACTION_PICK,
                    android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                runCatching {phonePicker.launch(intent)}.onFailure {
                    contactPickerAccount=null
                    model.action {throw java.io.IOException("No contact picker available.")}
                }
            },
            MbAttachmentChoice("Camera",Icons.Outlined.PhotoCamera) {
                attachments=false
                val account=model.backend.session.value?.userId
                if(account!=null) {
                    cameraAccount=account
                    val file=java.io.File(context.cacheDir,
                        "camera/${java.util.UUID.randomUUID()}.jpg")
                    file.parentFile?.mkdirs()
                    cameraPath=file.absolutePath
                    runCatching {
                        camera.launch(androidx.core.content.FileProvider.getUriForFile(
                            context,context.packageName+".files",file))
                    }.onFailure {
                        file.delete()
                        cameraPath=""
                        model.action {throw java.io.IOException("No camera app is available. Choose Gallery.")}
                    }
                }
            }
        ))

    }
    if(emojiPicker)EmojiPicker(onEmoji={emoji->
        val start=minOf(input.selection.start,input.selection.end);val end=maxOf(input.selection.start,input.selection.end)
        val text=body.replaceRange(start,end,emoji)
        if(text.length<=4000)updateInput(TextFieldValue(text,TextRange(start+emoji.length)))
    },onDismiss={emojiPicker=false},onSavedStickers={
        emojiPicker=false
        savedStickers=StickerLibrary.list(context,session?.userId)
        stickerNotice=null
        stickerLibraryOpen=true
    })
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
    if(quickRepliesOpen) LiquidAlertDialog(
        onDismissRequest={quickRepliesOpen=false},
        title={Text("Quick replies")},
        text={Column(Modifier.fillMaxWidth().heightIn(max=460.dp)
            .verticalScroll(rememberScrollState()),
            verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Type / in the message box to insert a saved reply. Selecting a reply never sends it automatically.",
                style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(value=quickReplyShortcut,
                onValueChange={quickReplyShortcut=it.take(25)},
                label={Text("Shortcut (e.g. /hello)")},
                modifier=Modifier.fillMaxWidth(),singleLine=true)
            OutlinedTextField(value=quickReplyText,
                onValueChange={if(it.length<=4000)quickReplyText=it},
                label={Text("Reply text")},
                modifier=Modifier.fillMaxWidth(),maxLines=4)
            FilledTonalButton(onClick={
                val account=session?.userId
                if(account!=null) {
                    runCatching {
                        quickReplies=MbQuickReplies.save(context,account,
                            quickReplyShortcut,quickReplyText)
                    }.onSuccess {
                        quickReplyShortcut=""
                        quickReplyText=""
                        quickReplyFeedback="Reply saved"
                    }.onFailure {quickReplyFeedback=it.message ?: "Unable to save reply"}
                }
            },modifier=Modifier.fillMaxWidth(),enabled=session?.userId!=null) {
                Text("Save quick reply")
            }
            quickReplyFeedback?.let {
                Text(it,style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if(quickReplies.isEmpty()) {
                Text("No quick replies saved on this phone.",
                    style=MaterialTheme.typography.bodySmall)
            } else quickReplies.forEach {reply->
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("/"+reply.shortcut,style=MaterialTheme.typography.titleSmall)
                        Text(reply.body,maxLines=2,overflow=TextOverflow.Ellipsis,
                            style=MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick={
                        quickReplyShortcut="/"+reply.shortcut
                        quickReplyText=reply.body
                    }) {Text("Edit")}
                    IconButton(onClick={
                        session?.userId?.let {account->
                            quickReplies=MbQuickReplies.delete(context,account,reply.shortcut)
                        }
                    }) {Icon(Icons.Outlined.DeleteOutline,"Delete quick reply")}
                }
                HorizontalDivider()
            }
        }},
        confirmButton={TextButton(onClick={quickRepliesOpen=false}) {Text("Done")}}
    )
    if(appearance)LiquidAlertDialog(
        onDismissRequest={appearance=false},
        title={Text("Chat appearance")},
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
    speechCandidate?.let {candidate->
        LiquidAlertDialog(
            onDismissRequest={speechCandidate=null},
            title={Text("Read this message aloud?")},
            text={Text("Android's installed speech engine will receive up to 2,000 " +
                "characters of this message. Some speech engines use online services. " +
                "Only continue if you consent to that processing.")},
            confirmButton={TextButton(onClick={
                val fresh=messages.firstOrNull {
                    it.id==candidate.id && it.account==session?.userId &&
                        it.deletedAt.isEmpty() && it.body.isNotBlank()
                }
                speechCandidate=null
                if(fresh!=null) {
                    messageSpeaker.speak(fresh.body)
                    spokenMessageId=fresh.id
                }
            }) {Text("Read aloud")}},
            dismissButton={TextButton(onClick={speechCandidate=null}) {Text("Cancel")}}
        )
    }
    actionMessage?.let {m->
        val categories=messageActionCategories(m)
        // One native set of actions, two presentations. The reference APK
        // uses a bottom action menu; Liquid style retains the old alert.
        val actionContents: @Composable ColumnScope.() -> Unit = {
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
                        if(m.status=="SENT" && m.editedAt.isNotEmpty() &&
                            m.mediaId.isEmpty() && m.deletedAt.isEmpty()) {
                            TextButton(onClick={
                                if(!busy) {
                                    actionMessage=null
                                    historyMessage=m
                                    historyRevisions=emptyList()
                                    historyError=null
                                    historyLoading=true
                                    model.action {
                                        try {
                                            val result=model.backend.rpc("message_edit_history",
                                                org.json.JSONObject().put("p_id",m.id))
                                            if(model.session.value?.userId==m.account &&
                                                historyMessage?.id==m.id)
                                                historyRevisions=parseEditVersions(result)
                                        } catch(e:kotlinx.coroutines.CancellationException) {
                                            throw e
                                        } catch(e:Exception) {
                                            historyError=e.message ?: "Edit history unavailable."
                                        } finally {historyLoading=false}
                                    }
                                }
                            },enabled=!busy,
                                modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {
                                Text("View edit history")
                            }
                        }
                        if(m.status=="SENT") {
                            GlassActionRow("Reply",Icons.Outlined.Reply,
                                onClick={replyTo=m.id;draftVersion=model.repository.saveDraft(cid,body,replyTo);actionMessage=null})
                            GlassActionRow(if(m.starred)"Unstar" else "Star",Icons.Outlined.StarOutline,
                                onClick={
                                    actionMessage=null
                                    model.action {model.repository.messageAction(m.id,"star",(!m.starred).toString())}
                                })
                        }
                        if(canForwardTextMessage(m,session?.userId)) {
                            GlassActionRow("Forward to chat",Icons.Outlined.Forward,
                                onClick={actionMessage=null;forwardMessages=listOf(m);forwardSearch=""})
                            GlassActionRow("Select multiple",Icons.Outlined.CheckBox,
                                onClick={actionMessage=null;selectedIds=setOf(m.id)})
                        }
                        if(m.mediaKind=="sticker" && m.status=="SENT" &&
                            m.mediaId.isNotEmpty() && m.deletedAt.isEmpty()) {
                            TextButton(onClick={
                                actionMessage=null
                                model.action {
                                    StickerLibrary.saveReceived(context,model.repository,m)
                                    savedStickers=StickerLibrary.list(context,session?.userId)
                                    stickerNotice="Sticker saved"
                                    stickerLibraryOpen=true
                                }
                            },modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {
                                Icon(Icons.Outlined.AutoAwesome,null)
                                Spacer(Modifier.width(8.dp))
                                Text("Save to My stickers")
                            }
                        }
                        if(m.body.isNotBlank() && m.deletedAt.isEmpty()) {
                            GlassActionRow("Read text aloud",Icons.Outlined.RecordVoiceOver,
                                onClick={speechCandidate=m;actionMessage=null})
                            if(spokenMessageId!=null) GlassActionRow(
                                "Stop reading",Icons.Outlined.VolumeOff,
                                onClick={
                                    messageSpeaker.stop()
                                    spokenMessageId=null
                                    actionMessage=null
                                })
                        }
                        if(m.mediaKind=="image" && m.body.isNotBlank())GlassActionRow(
                            "Copy text",Icons.Outlined.ContentCopy,onClick={
                                clipboard.setText(AnnotatedString(copiedText(m,display.copySenderAndTime)));actionMessage=null
                            })
                        if(m.mediaKind=="image" && m.body.isNotBlank())TextButton(onClick={
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
                            GlassActionRow("Edit",Icons.Outlined.Edit,
                                onClick={editMessage=m;editBody=m.body;actionMessage=null})
                    }
                    if("Safety" in categories) {
                        HorizontalDivider()
                        Text("Safety",style=MaterialTheme.typography.labelMedium,
                            color=MaterialTheme.colorScheme.onSurfaceVariant)
                        if(m.deletedAt.isEmpty()&&m.status=="SENT"&&m.sender!=m.account)
                            TextButton(onClick={reportMessage=m;actionMessage=null;report=true},
                                modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text("Report message")}
                        if(m.status in listOf("SENT","QUEUED","FAILED"))
                            GlassActionRow("Delete",Icons.Outlined.DeleteOutline,
                                destructive=true,onClick={deleteMessage=m;actionMessage=null})
                    }
                    Text("${if(m.sender==m.account)"You" else chat?.name.orEmpty()} · ${
                        java.time.Instant.ofEpochMilli(m.localTime)
                            .atZone(java.time.ZoneId.systemDefault())
                            .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
                    }",style=MaterialTheme.typography.bodySmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)

        }
        if(display.navBarStyle=="Reference") {
            LiquidActionSheet(title="Message actions",onDismiss={actionMessage=null}) {
                actionContents()
            }
        } else LiquidAlertDialog(
            onDismissRequest={actionMessage=null},
            title={Text("Message actions")},
            text={Column(Modifier.fillMaxWidth().heightIn(max=470.dp)
                .verticalScroll(rememberScrollState()),
                verticalArrangement=Arrangement.spacedBy(4.dp)) {
                actionContents()
            }},
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
    historyMessage?.let {message->
        MessageEditHistoryDialog(historyRevisions,historyLoading,historyError,
            onDismiss={historyMessage=null;historyRevisions=emptyList()})
    }
    editMessage?.let {m->LiquidAlertDialog(onDismissRequest={editMessage=null},title={Text("Edit message")},text={OutlinedTextField(editBody,{if(it.length<=4000)editBody=it},keyboardOptions=KeyboardOptions(capitalization=KeyboardCapitalization.Sentences),modifier=Modifier.fillMaxWidth())},confirmButton={TextButton(onClick={model.action {model.repository.messageAction(m.id,"edit",editBody);editMessage=null}},enabled=!busy&&editBody.isNotBlank()) {Text("Save")}},dismissButton={TextButton(onClick={editMessage=null}) {Text("Cancel")}})}
    deleteMessage?.let {m->LiquidAlertDialog(onDismissRequest={deleteMessage=null},title={Text("Delete message?")},text={Column {
        Text(if(m.status=="SENT")"Delete for me hides this message on your devices. The other person keeps their copy. Saved copies and screenshots cannot be erased." else "Remove this unsent message and its local attachment from this device.")
        val age=runCatching {java.time.Duration.between(java.time.Instant.parse(m.serverTime),java.time.Instant.now()).seconds}.getOrDefault(Long.MAX_VALUE)
        if(m.sender==m.account&&age<=172800&&m.deletedAt.isEmpty())TextButton(onClick={model.action {model.repository.messageAction(m.id,"delete");deleteMessage=null}},enabled=!busy) {Text("Delete for everyone")}
    }},confirmButton={TextButton(onClick={model.action {if(m.status=="SENT")model.repository.messageAction(m.id,"hide") else model.repository.discardUnsent(m.id);deleteMessage=null}},enabled=!busy) {Text(if(m.status=="SENT")"Delete for me" else "Remove unsent")}},dismissButton={TextButton(onClick={deleteMessage=null}) {Text("Cancel")}})}
    exportFeedback?.let {notice->
        LiquidAlertDialog(onDismissRequest={exportFeedback=null},
            text={Text(notice)},
            confirmButton={TextButton(onClick={exportFeedback=null}) {Text("OK")}})
    }
    if(starsOpen&&chat!=null)ContactMediaDialog(model,chat,messages,initialKind="star") {starsOpen=false}
    if(details&&chat!=null) ContactMediaDialog(model,chat,messages) {details=false}
    if(report&&chat!=null)ReportContentDialog(if(reportMessage==null)"Report contact" else "Report message",if(reportMessage==null)"Your explanation will be sent privately for review." else "Your explanation and this message's reference will be sent privately for review.",onSubmit={reason->model.report(chat.peer,reason,reportMessage?.let {listOf(it.id)} ?: emptyList());report=false;reportMessage=null},onDismiss={report=false;reportMessage=null})
    var photoEditBusy by remember(cid,session?.userId) {mutableStateOf(false)}
    selectedVideo?.let {uri->
        LiquidAlertDialog(
            onDismissRequest={if(!preparingVideo)selectedVideo=null},
            title={Text("Trim & prepare video")},
            text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("The video is compressed on this phone to a private H.264 MP4 (maximum 30 seconds, 720p, 8 MB).")
                Text("Source: ${selectedVideoLength/1000}s. Clip starts at ${(videoStartMs/1000f).toInt()}s.")
                if(selectedVideoLength>30_000L) Slider(
                    value=videoStartMs,
                    onValueChange={videoStartMs=it},
                    valueRange=0f..(selectedVideoLength-500L).coerceAtLeast(0L).toFloat(),
                    enabled=!preparingVideo)
                if(preparingVideo) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Preparing video locally…")
                }
            }},
            confirmButton={TextButton(onClick={
                val account=session?.userId ?: return@TextButton
                val start=videoStartMs.toLong()
                preparingVideo=true
                model.action {
                    try {
                        val staged=ChatVideo.prepare(context,uri,account,start,
                            quality=model.display.value.videoQuality)
                        if(model.backend.session.value?.userId==account && selectedVideo==uri) {
                            preparedChatVideo?.file?.delete()
                            preparedChatVideo=staged
                            selectedVideo=null
                            videoCaption=""
                        } else staged.file.delete()
                    } finally {preparingVideo=false}
                }
            },enabled=!preparingVideo&&session!=null) {Text("Prepare clip")}},
            dismissButton={TextButton(onClick={selectedVideo=null},
                enabled=!preparingVideo) {Text("Cancel")}})
    }
    preparedChatVideo?.let {clip->
        LiquidAlertDialog(
            onDismissRequest={if(!busy&&!videoSending) {
                clip.file.delete()
                preparedChatVideo=null
            }},
            title={Text("Send private video")},
            text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.Videocam,null,Modifier.size(40.dp))
                Text("${clip.durationMs/1000f}s · ${clip.width}×${clip.height} · ${clip.file.length()/1024} KB")
                OutlinedTextField(videoCaption,{if(it.length<=1000)videoCaption=it},
                    label={Text("Caption (optional)")},modifier=Modifier.fillMaxWidth(),
                    maxLines=3,enabled=!busy&&!videoSending)
                Text("The video is available only to this chat's participants through short-lived access URLs.",
                    style=MaterialTheme.typography.bodySmall)
            }},
            confirmButton={TextButton(onClick={
                val text=videoCaption
                val owner=session?.userId ?: return@TextButton
                if(!videoSending) {
                    videoSending=true
                    model.action {
                        try {
                            check(model.backend.session.value?.userId==owner)
                            model.repository.sendChatVideo(cid,clip,text)
                            if(model.backend.session.value?.userId==owner &&
                                preparedChatVideo===clip) {
                                preparedChatVideo=null
                                videoCaption=""
                            }
                        } finally {videoSending=false}
                    }
                }
            },enabled=!busy&&!videoSending&&session!=null) {Text("Send video")}},
            dismissButton={TextButton(onClick={
                clip.file.delete()
                preparedChatVideo=null
                videoCaption=""
            },enabled=!busy&&!videoSending) {Text("Cancel")}})
    }
    if(selectedPdfBatch.isNotEmpty()) LiquidAlertDialog(
        onDismissRequest={
            if(!busy&&!sendingPdfBatch) {
                selectedPdfBatch.forEach {it.file.delete()}
                selectedPdfBatch=emptyList()
            }
        },
        title={Text("Share ${selectedPdfBatch.size} documents")},
        text={Column(Modifier.fillMaxWidth().heightIn(max=380.dp)
            .verticalScroll(rememberScrollState()),
            verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("Each PDF is shared privately as an independent, retryable attachment.",
                style=MaterialTheme.typography.bodySmall)
            selectedPdfBatch.forEachIndexed {index,document->
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Description,null,Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(document.name,maxLines=2,
                            overflow=TextOverflow.Ellipsis,
                            style=MaterialTheme.typography.bodyMedium)
                        Text("${document.file.length()/1024} KB",
                            style=MaterialTheme.typography.labelSmall)
                    }
                    IconButton(enabled=!busy&&!sendingPdfBatch,onClick={
                        selectedPdfBatch=selectedPdfBatch.filterIndexed {i,_->i!=index}
                        document.file.delete()
                    }) {Icon(Icons.Outlined.Close,"Remove document ${index+1}")}
                }
            }
            if(sendingPdfBatch)LinearProgressIndicator(Modifier.fillMaxWidth())
        }},
        confirmButton={TextButton(enabled=!busy&&!sendingPdfBatch && session!=null,
            onClick={
                val owner=session?.userId ?: return@TextButton
                val chosen=selectedPdfBatch.toList()
                sendingPdfBatch=true
                model.action {
                    try {
                        check(model.backend.session.value?.userId==owner) {
                            "Account changed before documents were queued."
                        }
                        model.repository.sendDocuments(cid,chosen)
                        if(model.backend.session.value?.userId==owner) {
                            selectedPdfBatch=emptyList()
                        }
                    } finally {sendingPdfBatch=false}
                }
            }) {Text("Send PDFs")}},
        dismissButton={TextButton(enabled=!busy&&!sendingPdfBatch,onClick={
            selectedPdfBatch.forEach {it.file.delete()}
            selectedPdfBatch=emptyList()
        }) {Text("Cancel")}}
    )
    preparedPdf?.let {pdf->
        LiquidAlertDialog(
            onDismissRequest={if(!busy){pdf.file.delete();preparedPdf=null}},
            title={Text("Send PDF document")},
            text={Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Description,null,Modifier.size(36.dp),
                    tint=MaterialTheme.colorScheme.primary)
                Text(pdf.name,style=MaterialTheme.typography.titleMedium)
                Text("Private PDF attachment · ${pdf.file.length()/1024} KB · 8 MB maximum",
                    style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Only the participants of this Liquid Chat conversation can access the document.",
                    style=MaterialTheme.typography.bodySmall)
            }},
            confirmButton={TextButton(onClick={
                model.action {
                    model.repository.sendDocument(cid,pdf)
                    preparedPdf=null
                }
            },enabled=!busy && session!=null) {Text("Send PDF")}},
            dismissButton={TextButton(onClick={
                pdf.file.delete()
                preparedPdf=null
            },enabled=!busy) {Text("Cancel")}}
        )
    }
    if(stickerLibraryOpen) LiquidAlertDialog(
        onDismissRequest={stickerLibraryOpen=false},
        title={Text("My stickers")},
        text={Column(Modifier.fillMaxWidth(),
            verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Saved stickers are private to this account on this phone. Select Send to share one.",
                style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                listOf("All","Recent","Favorites").forEach {tab->
                    FilterChip(
                        selected=stickerShelfFilter==tab,
                        onClick={stickerShelfFilter=tab},
                        label={Text(tab)},
                        modifier=Modifier.weight(1f)
                    )
                }
            }
            val visibleStickers=when(stickerShelfFilter) {
                "Recent"->recentStickers
                "Favorites"->savedStickers.filter {it.name in favoriteStickers}
                else->savedStickers
            }
            if(visibleStickers.isEmpty())
                Text(when(stickerShelfFilter) {
                    "Recent"->"No recently sent stickers on this account."
                    "Favorites"->"Tap the star on a saved sticker to add a favorite."
                    else->"No saved stickers. Choose Create sticker to make one."
                })
            MbSavedStickerShelf(
                stickers=visibleStickers,enabled=!busy&&!sendingSavedSticker,
                favorites=favoriteStickers,
                onToggleFavorite={file->
                    val owner=session?.userId
                    if(owner!=null)runCatching {
                        StickerLibrary.setFavorite(context,owner,file,
                            file.name !in favoriteStickers)
                        StickerLibrary.favoriteNames(context,owner)
                    }.onSuccess {favoriteStickers=it;stickerNotice=null}
                        .onFailure {stickerNotice=it.message ?: "Cannot update favorite."}
                },
                onSend={file->
                    if(display.confirmSavedStickerSend)confirmSavedSticker=file
                    else sendSavedSticker(file)
                },
                onDelete={file->
                    val owner=session?.userId
                    if(owner!=null)runCatching {
                        StickerLibrary.remove(context,owner,file)
                        savedStickers=StickerLibrary.list(context,owner)
                        favoriteStickers=StickerLibrary.favoriteNames(context,owner)
                        recentStickers=StickerLibrary.recent(context,owner)
                    }.onFailure {stickerNotice=it.message ?: "Could not remove sticker."}
                }
            )
            stickerNotice?.let {Text(it,style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)}
        }},
        confirmButton={TextButton(onClick={stickerLibraryOpen=false}) {Text("Done")}}
    )
    confirmSavedSticker?.let {file->
        LiquidAlertDialog(
            onDismissRequest={if(!busy&&!sendingSavedSticker)confirmSavedSticker=null},
            title={Text("Send saved sticker?")},
            text={Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                coil.compose.AsyncImage(model=file,contentDescription="Sticker to send",
                    modifier=Modifier.fillMaxWidth().height(160.dp))
                Text("Share this sticker with ${chat?.name ?: "this conversation"}?")
            }},
            confirmButton={TextButton(onClick={sendSavedSticker(file)},enabled=!busy&&!sendingSavedSticker) {Text("Send sticker")}},
            dismissButton={TextButton(onClick={confirmSavedSticker=null},enabled=!busy&&!sendingSavedSticker) {Text("Cancel")}}
        )
    }
    preparedSticker?.let {sticker->
        LiquidAlertDialog(
            onDismissRequest={
                if(!busy){sticker.file.delete();preparedSticker=null;stickerSourceUri=null}
            },
            title={Text("Create & send sticker")},
            text={Column(Modifier.fillMaxWidth(),
                verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("512 × 512 transparent PNG · private conversation",
                    style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
                coil.compose.AsyncImage(
                    model=coil.request.ImageRequest.Builder(context).data(sticker.file)
                        .memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                        .diskCachePolicy(coil.request.CachePolicy.DISABLED).build(),
                    contentDescription="Sticker preview",
                    modifier=Modifier.fillMaxWidth().height(240.dp))
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text("Remove connected white background",
                        modifier=Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium)
                    Switch(checked=removeStickerWhite,enabled=!busy && stickerSourceUri!=null,
                        onCheckedChange={checked->
                            val source=stickerSourceUri
                            val owner=session?.userId
                            if(source!=null && owner!=null)model.action {
                                val next=StickerFactory.prepare(context,source,owner,checked,
                                    label=appliedStickerLabel)
                                if(model.backend.session.value?.userId==owner &&
                                    preparedSticker===sticker) {
                                    preparedSticker=next
                                    removeStickerWhite=checked
                                    sticker.file.delete()
                                } else next.file.delete()
                            }
                        })
                }
                Text("Only connected background near the image edges is removed; enclosed white details remain. Review the preview before sending.",
                    style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(value=stickerLabel,
                    onValueChange={stickerLabel=it.take(32)},
                    modifier=Modifier.fillMaxWidth(),
                    label={Text("Sticker text (optional)")},maxLines=1,singleLine=true,
                    enabled=!busy&&stickerSourceUri!=null)
                if(stickerSourceUri!=null) {
                    OutlinedButton(
                        modifier=Modifier.fillMaxWidth(),
                        enabled=!busy && stickerLabel!=appliedStickerLabel,
                        onClick={
                            val owner=session?.userId
                            val source=stickerSourceUri
                            val desired=stickerLabel
                            if(owner!=null&&source!=null)model.action {
                                val next=StickerFactory.prepare(context,source,owner,
                                    removeWhite=removeStickerWhite,label=desired)
                                if(model.backend.session.value?.userId==owner &&
                                    preparedSticker===sticker) {
                                    preparedSticker=next
                                    appliedStickerLabel=com.pritam.liquidchat.data.normalizedStickerLabel(desired)
                                    sticker.file.delete()
                                } else next.file.delete()
                            }
                        }) {
                        Text(if(stickerLabel.isBlank())"Remove sticker text" else "Apply sticker text")
                    }
                }
                OutlinedButton(onClick={
                    val owner=session?.userId
                    if(owner!=null)model.action {
                        StickerLibrary.save(context,owner,sticker)
                        savedStickers=StickerLibrary.list(context,owner)
                        stickerNotice="Saved to My stickers"
                    }
                },enabled=!busy && session?.userId!=null,
                    modifier=Modifier.fillMaxWidth()) {
                    Text("Save to My stickers")
                }
                if(stickerNotice=="Saved to My stickers")
                    Text("Saved to My stickers",style=MaterialTheme.typography.bodySmall)
            }},
            confirmButton={TextButton(onClick={
                model.action {
                    model.repository.sendSticker(cid,sticker)
                    preparedSticker=null
                    stickerSourceUri=null
                }
            },enabled=!busy) {Text("Send sticker")}},
            dismissButton={TextButton(onClick={
                sticker.file.delete();preparedSticker=null;stickerSourceUri=null
            },enabled=!busy) {Text("Cancel")}}
        )
    }
    if(preparedAlbum.isNotEmpty()) LiquidAlertDialog(
        onDismissRequest={
            if(!albumSending&&!albumEditing&&!busy) {
                preparedAlbum.forEach {it.file.delete()}
                preparedAlbum=emptyList()
                albumCaption=""
                albumEditIndex=0
            }
        },
        title={Text("Send ${preparedAlbum.size} photos")},
        text={Column(Modifier.fillMaxWidth().heightIn(max=440.dp)
            .verticalScroll(rememberScrollState()),
            verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                preparedAlbum.forEachIndexed {index,image->
                    Column(Modifier.width(136.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                        Box(Modifier.size(120.dp).clip(RoundedCornerShape(12.dp))
                            .clickable(enabled=!albumSending&&!albumEditing&&!busy) {
                                albumEditIndex=index
                            }) {
                            coil.compose.AsyncImage(model=image.file,
                                contentDescription="Select photo ${index+1} of ${preparedAlbum.size}",
                                contentScale=ContentScale.Crop,
                                modifier=Modifier.fillMaxSize())
                            if(index==albumEditIndex)Icon(Icons.Outlined.CheckCircle,
                                "Selected for editing",
                                modifier=Modifier.align(Alignment.BottomStart).padding(6.dp),
                                tint=MaterialTheme.colorScheme.primary)
                            IconButton(onClick={
                                if(!albumSending&&!albumEditing&&!busy) {
                                    preparedAlbum=preparedAlbum.filterIndexed {i,_->i!=index}
                                    albumEditIndex=albumEditIndex.coerceAtMost(
                                        (preparedAlbum.size-1).coerceAtLeast(0))
                                    image.file.delete()
                                }
                            },enabled=!albumSending&&!albumEditing&&!busy,
                                modifier=Modifier.align(Alignment.TopEnd).size(32.dp)) {
                                Icon(Icons.Outlined.Close,"Remove photo ${index+1}",
                                    tint=MaterialTheme.colorScheme.error)
                            }
                        }
                        Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick={
                                preparedAlbum=preparedAlbum.toMutableList().apply {
                                    add(index-1,removeAt(index))
                                }
                                albumEditIndex=index-1
                            },enabled=!albumSending&&!albumEditing&&!busy&&index>0,
                                modifier=Modifier.size(30.dp)) {
                                Icon(Icons.Outlined.ArrowBack,"Move photo left",Modifier.size(18.dp))
                            }
                            IconButton(onClick={
                                preparedAlbum=preparedAlbum.toMutableList().apply {
                                    add(index+1,removeAt(index))
                                }
                                albumEditIndex=index+1
                            },enabled=!albumSending&&!albumEditing&&!busy&&
                                index<preparedAlbum.lastIndex,
                                modifier=Modifier.size(30.dp)) {
                                Icon(Icons.Outlined.ArrowForward,"Move photo right",Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
            Text("Edit selected photo ${albumEditIndex+1}",
                style=MaterialTheme.typography.labelMedium)
            val albumCropPhoto=preparedAlbum.getOrNull(albumEditIndex)
            var albumFreeCrop by remember(albumCropPhoto?.file?.absolutePath) {
                mutableStateOf(false)
            }
            var albumCropBounds by remember(albumCropPhoto?.file?.absolutePath) {
                mutableStateOf<Images.CropBounds?>(null)
            }
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                TextButton(enabled=!albumSending&&!albumEditing&&!busy&&albumCropPhoto!=null,
                    onClick={albumFreeCrop=!albumFreeCrop;albumCropBounds=null}) {
                    Icon(Icons.Outlined.Crop,null,Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(if(albumFreeCrop)"Close crop" else "Free crop")
                }
                if(albumFreeCrop) TextButton(
                    enabled=!albumSending&&!albumEditing&&!busy&&albumCropBounds?.valid()==true,
                    onClick={
                        val owner=session?.userId
                        val selectedIndex=albumEditIndex
                        val source=albumCropPhoto
                        val bounds=albumCropBounds
                        if(owner!=null&&source!=null&&bounds?.valid()==true) {
                            albumEditing=true
                            model.action {
                                try {
                                    val edited=Images.cropSelection(context,owner,source,bounds)
                                    val valid=model.backend.session.value?.userId==owner &&
                                        !albumSending && preparedAlbum.getOrNull(selectedIndex)===source
                                    if(valid) {
                                        preparedAlbum=preparedAlbum.toMutableList().apply {
                                            set(selectedIndex,edited)
                                        }
                                        source.file.delete()
                                    } else edited.file.delete()
                                } finally {albumEditing=false}
                            }
                        }
                    }) {Text("Apply Crop")}
            }
            if(albumFreeCrop&&albumCropPhoto!=null) {
                MbGestureCropEditor(albumCropPhoto,enabled=!albumSending&&!albumEditing&&!busy,
                    onSelection={albumCropBounds=it})
                Text("Drag over the selected album photo to crop only that photo.",
                    style=MaterialTheme.typography.bodySmall)
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                listOf(
                    Images.Edit.CROP_SQUARE to "1:1",
                    Images.Edit.CROP_PORTRAIT to "4:5",
                    Images.Edit.CROP_WIDE to "16:9",
                    Images.Edit.ROTATE_LEFT to "Rotate left",
                    Images.Edit.ROTATE_RIGHT to "Rotate right",
                    Images.Edit.FLIP_HORIZONTAL to "Flip",
                    Images.Edit.GRAYSCALE to "B&W"
                ).forEach {(operation,label)->
                    MbReferenceEditorTool(label=label,enabled=!albumSending&&!albumEditing&&!busy,
                        onClick={
                            val owner=session?.userId
                            val selectedIndex=albumEditIndex
                            val original=preparedAlbum.getOrNull(selectedIndex)
                            if(owner!=null&&original!=null) {
                                albumEditing=true
                                model.action {
                                    try {
                                        val edited=Images.edit(context,owner,original,operation)
                                        val valid=model.backend.session.value?.userId==owner &&
                                            !albumSending&&preparedAlbum.getOrNull(selectedIndex)===original
                                        if(valid) {
                                            preparedAlbum=preparedAlbum.toMutableList().apply {
                                                set(selectedIndex,edited)
                                            }
                                            original.file.delete()
                                        } else edited.file.delete()
                                    } finally {albumEditing=false}
                                }
                            }
                        })
                }
            }
            OutlinedTextField(value=albumCaption,
                onValueChange={if(it.length<=4000)albumCaption=it},
                enabled=!albumSending&&!busy,
                label={Text("Caption for first photo (optional)")},
                modifier=Modifier.fillMaxWidth(),maxLines=3)
            Text("Up to 10 private photos. Each is queued as an individual message, so delivery and retry are independent.",
                style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(albumSending)Text("Queuing photos…",
                style=MaterialTheme.typography.bodySmall)
        }},
        confirmButton={TextButton(enabled=!albumSending&&!albumEditing&&!busy&&session!=null,
            onClick={
                val owner=session?.userId ?: return@TextButton
                val selected=preparedAlbum.toList()
                val firstCaption=albumCaption
                albumSending=true
                model.action {
                    try {
                        check(model.backend.session.value?.userId==owner) {
                            "Account changed before selected photos were queued."
                        }
                        // All prepared photos enter the account-scoped Room outbox
                        // atomically before a single WorkManager schedule attempt.
                        model.repository.sendImages(cid,selected,firstCaption)
                        if(model.backend.session.value?.userId==owner) {
                            preparedAlbum=emptyList()
                            albumCaption=""
                        }
                    } finally {albumSending=false}
                }
            }) {Text("Send photos")}},
        dismissButton={TextButton(enabled=!albumSending&&!albumEditing&&!busy,onClick={
            preparedAlbum.forEach {it.file.delete()}
            preparedAlbum=emptyList()
            albumCaption=""
            albumEditIndex=0
        }) {Text("Cancel")}}
    )
    prepared?.let {image->
        val editingAllowed=!photoEditBusy&&!busy
        LiquidAlertDialog(
            onDismissRequest={if(editingAllowed)clearPhotoDraft()},
            title={Text("Edit & send photo")},
            text={
                Column(Modifier.fillMaxWidth().heightIn(max=500.dp)
                    .verticalScroll(rememberScrollState()),
                    verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    // Keep the crop viewport tied to the exact local edit revision.
                    var freeCropOpen by remember(image.file.absolutePath) {mutableStateOf(false)}
                    var freeCropBounds by remember(image.file.absolutePath) {
                        mutableStateOf<Images.CropBounds?>(null)
                    }
                    var drawingOpen by remember(image.file.absolutePath) {mutableStateOf(false)}
                    var drawingStrokes by remember(image.file.absolutePath) {
                        mutableStateOf<List<Images.DrawStroke>>(emptyList())
                    }
                    var drawingColor by remember(image.file.absolutePath) {
                        mutableIntStateOf(0xFFFF4B52.toInt())
                    }
                    if(drawingOpen) {
                        MbPhotoDoodleEditor(image,drawingStrokes,drawingColor,
                            enabled=editingAllowed&&drawingStrokes.size<25,
                            onStroke={stroke->
                                if(drawingStrokes.size<25&&stroke.valid())
                                    drawingStrokes=drawingStrokes+stroke
                            })
                        Text("Draw on the photo. Undo a stroke or apply the artwork to the private image.",
                            style=MaterialTheme.typography.bodySmall)
                    } else if(freeCropOpen) {
                        MbGestureCropEditor(image,enabled=editingAllowed,
                            onSelection={freeCropBounds=it})
                        Text("Drag diagonally to select the area. Apply Crop preserves Undo history.",
                            style=MaterialTheme.typography.bodySmall)
                    } else {
                        coil.compose.AsyncImage(
                            model=coil.request.ImageRequest.Builder(context)
                                .data(image.file).memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                                .diskCachePolicy(coil.request.CachePolicy.DISABLED).build(),
                            contentDescription="Edited photo preview",
                            modifier=Modifier.fillMaxWidth().heightIn(max=240.dp)
                                .aspectRatio(image.width.toFloat()/image.height.coerceAtLeast(1)))
                    }
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick={
                            freeCropOpen=!freeCropOpen
                            freeCropBounds=null
                            drawingOpen=false
                            drawingStrokes=emptyList()
                        },enabled=editingAllowed) {
                            Icon(Icons.Outlined.Crop,null,Modifier.size(18.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(if(freeCropOpen)"Close crop" else "Free crop")
                        }
                        if(freeCropOpen) TextButton(
                            enabled=editingAllowed&&freeCropBounds?.valid()==true,
                            onClick={
                                val owner=session?.userId
                                val bounds=freeCropBounds
                                if(owner!=null&&bounds?.valid()==true&&!photoEditBusy) {
                                    photoEditBusy=true
                                    model.action {
                                        try {
                                            val next=Images.cropSelection(context,owner,image,bounds)
                                            if(model.session.value?.userId==owner&&prepared===image) {
                                                photoRedo.forEach {it.file.delete()}
                                                photoRedo=emptyList()
                                                val revisions=photoUndo+image
                                                photoUndo=revisions.takeLast(5)
                                                revisions.dropLast(5).forEach {it.file.delete()}
                                                prepared=next
                                            } else next.file.delete()
                                        } finally {photoEditBusy=false}
                                    }
                                }
                            }) {Text("Apply Crop")}
                        TextButton(onClick={
                            drawingOpen=!drawingOpen
                            drawingStrokes=emptyList()
                            freeCropOpen=false
                            freeCropBounds=null
                        },enabled=editingAllowed) {
                            Icon(Icons.Outlined.Draw,null,Modifier.size(18.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(if(drawingOpen)"Close drawing" else "Draw")
                        }
                    }
                    if(drawingOpen) {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                            listOf(
                                0xFFFF4B52.toInt() to "Red",
                                0xFF1596EB.toInt() to "Blue",
                                0xFF35B867.toInt() to "Green",
                                0xFFFFFFFF.toInt() to "White",
                                0xFF161616.toInt() to "Black"
                            ).forEach {(argb,label)->
                                FilterChip(selected=drawingColor==argb,
                                    onClick={drawingColor=argb},
                                    enabled=editingAllowed,
                                    label={Text(label)})
                            }
                        }
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp),
                            verticalAlignment=Alignment.CenterVertically) {
                            TextButton(onClick={drawingStrokes=drawingStrokes.dropLast(1)},
                                enabled=editingAllowed&&drawingStrokes.isNotEmpty()) {
                                Text("Undo stroke")
                            }
                            TextButton(onClick={drawingStrokes=emptyList()},
                                enabled=editingAllowed&&drawingStrokes.isNotEmpty()) {
                                Text("Clear")
                            }
                            TextButton(
                                enabled=editingAllowed&&Images.validDrawing(drawingStrokes),
                                onClick={
                                    val owner=session?.userId
                                    val strokes=drawingStrokes.toList()
                                    if(owner!=null&&Images.validDrawing(strokes)&&!photoEditBusy) {
                                        photoEditBusy=true
                                        model.action {
                                            try {
                                                val next=Images.drawStrokes(
                                                    context,owner,image,strokes)
                                                if(model.session.value?.userId==owner&&prepared===image) {
                                                    photoRedo.forEach {it.file.delete()}
                                                    photoRedo=emptyList()
                                                    val revisions=photoUndo+image
                                                    photoUndo=revisions.takeLast(5)
                                                    revisions.dropLast(5).forEach {it.file.delete()}
                                                    prepared=next
                                                } else next.file.delete()
                                            } finally {photoEditBusy=false}
                                        }
                                    }
                                }) {Text("Apply drawing")}
                        }
                    }
                    Text("Photo editor",style=MaterialTheme.typography.titleSmall)
                    var overlayPhotoText by remember(image.file.absolutePath) {mutableStateOf("")}
                    OutlinedTextField(
                        value=overlayPhotoText,
                        onValueChange={overlayPhotoText=it.take(64)},
                        label={Text("Add text on photo (optional)")},
                        singleLine=true,maxLines=1,
                        modifier=Modifier.fillMaxWidth(),enabled=editingAllowed)
                    OutlinedButton(
                        enabled=editingAllowed&&overlayPhotoText.isNotBlank(),
                        modifier=Modifier.fillMaxWidth(),
                        onClick={
                            val owner=session?.userId
                            val words=overlayPhotoText
                            if(owner!=null&&!photoEditBusy) {
                                photoEditBusy=true
                                model.action {
                                    try {
                                        val next=Images.addTextOverlay(context,owner,image,words)
                                        if(model.session.value?.userId==owner&&prepared===image) {
                                            photoRedo.forEach {it.file.delete()}
                                            photoRedo=emptyList()
                                            val revisions=photoUndo+image
                                            photoUndo=revisions.takeLast(5)
                                            revisions.dropLast(5).forEach {it.file.delete()}
                                            prepared=next
                                        } else next.file.delete()
                                    } finally {photoEditBusy=false}
                                }
                            }
                        }) {
                        Icon(Icons.Outlined.TextFields,null,Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Apply text to photo")
                    }
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        listOf(
                            Images.Edit.CROP_SQUARE to "Crop 1:1",
                            Images.Edit.CROP_PORTRAIT to "Crop 4:5",
                            Images.Edit.CROP_WIDE to "Crop 16:9",
                            Images.Edit.ROTATE_LEFT to "Rotate left",
                            Images.Edit.ROTATE_RIGHT to "Rotate right",
                            Images.Edit.FLIP_HORIZONTAL to "Flip horizontal",
                            Images.Edit.FLIP_VERTICAL to "Flip vertical",
                            Images.Edit.GRAYSCALE to "B&W"
                        ).forEach {(operation,label)->
                            MbReferenceEditorTool(label=label,enabled=editingAllowed,
                                onClick={
                                    val owner=session?.userId
                                    if(owner!=null && !photoEditBusy) {
                                        photoEditBusy=true
                                        model.action {
                                            try {
                                                val next=Images.edit(context,owner,image,operation)
                                                if(model.session.value?.userId==owner && prepared===image) {
                                                    photoRedo.forEach {it.file.delete()}
                                                    photoRedo=emptyList()
                                                    val revisions=photoUndo+image
                                                    photoUndo=revisions.takeLast(5)
                                                    revisions.dropLast(5).forEach {it.file.delete()}
                                                    prepared=next
                                                } else next.file.delete()
                                            } finally {photoEditBusy=false}
                                        }
                                    }
                                })
                        }
                    }
                    Row(verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            enabled=editingAllowed&&photoUndo.isNotEmpty(),
                            onClick={
                                val previous=photoUndo.lastOrNull()
                                if(previous!=null&&prepared===image) {
                                    photoUndo=photoUndo.dropLast(1)
                                    val nextRedo=photoRedo+image
                                    photoRedo=nextRedo.takeLast(5)
                                    nextRedo.dropLast(5).forEach {it.file.delete()}
                                    prepared=previous
                                }
                            }) {
                            Icon(Icons.Outlined.Undo,null,Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Undo")
                        }
                        OutlinedButton(
                            enabled=editingAllowed&&photoRedo.isNotEmpty(),
                            onClick={
                                val next=photoRedo.lastOrNull()
                                if(next!=null&&prepared===image) {
                                    photoRedo=photoRedo.dropLast(1)
                                    val revisions=photoUndo+image
                                    photoUndo=revisions.takeLast(5)
                                    revisions.dropLast(5).forEach {it.file.delete()}
                                    prepared=next
                                }
                            }) {Text("Redo")}
                        Text("${photoUndo.size} / 5 saved steps",
                            style=MaterialTheme.typography.bodySmall,
                            color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("All edits are performed on this phone. The result is sent as a private photo through Liquid Chat.",
                        style=MaterialTheme.typography.bodySmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(input,{if(it.text.length<=4000)updateInput(it)},
                        keyboardOptions=KeyboardOptions(
                            capitalization=KeyboardCapitalization.Sentences,
                            keyboardType=KeyboardType.Text),
                        label={Text("Caption (optional)")},modifier=Modifier.fillMaxWidth())
                }
            },
            confirmButton={TextButton(onClick={
                val caption=body
                val version=draftVersion
                model.action {
                    model.repository.sendImage(cid,image,caption,version)
                    // Only the selected edit is sent; discard private revisions.
                    photoUndo.forEach {it.file.delete()}
                    photoUndo=emptyList()
                    photoRedo.forEach {it.file.delete()}
                    photoRedo=emptyList()
                    prepared=null
                    if(draftVersion==version){input=TextFieldValue("");replyTo=""}
                }
            },enabled=editingAllowed) {Text("Send")}},
            dismissButton={TextButton(
                onClick={clearPhotoDraft()},enabled=editingAllowed
            ) {Text("Cancel")}}
        )
    }
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
@Composable internal fun MessageBubble(message:Message,chat:Chat?,model:AppViewModel,onRetry:()->Unit,highlight:Boolean,onLongClick:()->Unit={},onReply:()->Unit={},quoted:Message?=null,onQuoteClick:()->Unit={},selectionMode:Boolean=false,onSelect:()->Unit={},onDoubleTap:()->Unit={}) {
    val uploadFlow=remember(model,message.account,message.id) {model.repository.uploadProgress(message.account,message.id)}
    val uploadPercent by uploadFlow.collectAsStateWithLifecycle(initialValue=null)
    val mine=message.sender==message.account
    val isSticker=message.mediaKind=="sticker"&&message.deletedAt.isEmpty()
    val bubbleDisplay=LocalDisplayOptions.current
    val referenceBubble=referenceMessageChrome(bubbleDisplay)
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
    val replySwipeOffset by animateFloatAsState(
        targetValue=swipe,
        animationSpec=if(bubbleDisplay.reducedMotion) snap()
            else spring(dampingRatio=Spring.DampingRatioMediumBouncy,
                stiffness=Spring.StiffnessHigh),
        label="conversation reply gesture")
    var expanded by remember(message.id) {mutableStateOf(false)}
    var truncated by remember(message.id) {mutableStateOf(false)}
    BoxWithConstraints(Modifier.fillMaxWidth()) {
    val bubbleWidth=minOf(maxWidth*(if(referenceBubble).84f else .86f),480.dp)
    if(message.status=="SENT" && message.deletedAt.isEmpty() &&
        replySwipeOffset>3f) {
        Icon(Icons.Outlined.Reply,contentDescription="Swipe to reply",
            modifier=Modifier.align(if(rtl)Alignment.CenterEnd else Alignment.CenterStart)
                .padding(horizontal=12.dp).size(26.dp)
                .graphicsLayer {
                    val strength=(replySwipeOffset/threshold).coerceIn(0f,1f)
                    alpha=strength
                    scaleX=.7f+.3f*strength
                    scaleY=.7f+.3f*strength
                },
            tint=MaterialTheme.colorScheme.primary)
    }
    Row(Modifier.fillMaxWidth().offset {
        androidx.compose.ui.unit.IntOffset((if(rtl)-replySwipeOffset else replySwipeOffset).toInt(),0)
    }.draggable(drag,Orientation.Horizontal,
        enabled=message.status=="SENT"&&message.deletedAt.isEmpty(),
        onDragStopped={
            if(swipe>=threshold)replyCallback()
            swipe=0f
            crossed=false
        }),
        horizontalArrangement=if(mine)Arrangement.End else Arrangement.Start) {
        Surface(
            shape=conversationBubbleShape(bubbleDisplay.bubbleStyle,mine),
            color=if(highlight)MaterialTheme.colorScheme.tertiaryContainer
                else if(isSticker)Color.Transparent else bubbleSurfaceColor(bubbleDisplay,mine,LocalGlassDark.current,
                    if(LocalGlassDark.current) {if(mine)Color(0xFF1F5268) else Color(0xFF293F53)}
                    else if(mine)Color(0xFFD0EDFB) else Color(0xFFFAFDFF)),
            border=if(isSticker||referenceBubble)null else androidx.compose.foundation.BorderStroke(
                1.dp,Color.White.copy(alpha=if(LocalGlassDark.current).19f else .8f)),
            shadowElevation=if(isSticker||referenceBubble)0.dp else 2.dp,
            modifier=Modifier.widthIn(max=bubbleWidth)
                .then(if(isSticker||referenceBubble)Modifier else Modifier.glassSource())
                .combinedClickable(onClick={if(selectionMode)onSelect()},
                    onDoubleClick=if(canDoubleTapReact(message,bubbleDisplay.doubleTapReactionEnabled,
                        selectionMode,model.session.value?.userId))onDoubleTap else null,
                    onLongClick={if(selectionMode)onSelect() else onLongClick()})
        ) {
            Column(Modifier.padding(
                horizontal=if(isSticker)0.dp else if(referenceBubble)9.dp else 11.dp,
                vertical=if(isSticker)2.dp else if(referenceBubble)6.dp else 9.dp)) {
                if(message.deletedAt.isNotEmpty()) {
                    Text("This message was deleted",
                        style=MaterialTheme.typography.bodyMedium,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                if(message.replyTo.isNotEmpty()) Surface(
                    shape=if(referenceBubble)androidx.compose.foundation.shape.RoundedCornerShape(8.dp) else Glass.compact,
                    color=MaterialTheme.colorScheme.primary.copy(alpha=.1f),
                    modifier=Modifier.fillMaxWidth().clickable(onClick=onQuoteClick)) {
                    Column(Modifier.padding(8.dp)) {Text("Reply",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary);Text(quoted?.takeIf {it.deletedAt.isEmpty()}?.body ?: "Original message unavailable",maxLines=2,overflow=TextOverflow.Ellipsis,
                        color=conversationInkColor(bubbleDisplay.quotedInk,LocalGlassDark.current,
                            resolvedBubbleTextColor(bubbleDisplay,mine,LocalGlassDark.current,MaterialTheme.colorScheme.onSurface),bubbleDisplay.quotedInkHex),
                        style=MaterialTheme.typography.bodySmall)}
                }
                if(message.mediaId.isNotEmpty()||message.localImage.isNotEmpty()) {
                    when(message.mediaKind) {
                        "voice"->VoicePlayer(model,message)
                        "document"->PdfMessageAttachment(model,message)
                        "chat_video"->ChatVideoAttachment(model,message)
                        else->SharedImagePreview(model,message)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if(message.mediaKind !in listOf("voice","document") &&
                    !(message.mediaKind=="chat_video" && message.body=="Video") &&
                    !(message.mediaKind=="sticker" && message.body=="Sticker"))MessageBodyText(message.body,bubbleDisplay.messageFontSize,expanded,
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
                    if(message.deletedAt.isEmpty())editedMessageMarker(
                        message.editedAt,bubbleDisplay.showEditedLabel,bubbleDisplay.showEditedTime
                    )?.let {label->Text(label,style=MaterialTheme.typography.labelSmall)}
                    Text(time,style=MaterialTheme.typography.labelSmall,
                        color=conversationInkColor(bubbleDisplay.timestampInk,LocalGlassDark.current,
                            resolvedBubbleTextColor(bubbleDisplay,mine,LocalGlassDark.current,MaterialTheme.colorScheme.onSurfaceVariant),bubbleDisplay.timestampInkHex))
                    if(mine)Receipt(message.status,message.seq,chat?.peerDelivered ?: 0,chat?.peerRead ?: 0)
                }
                if(message.reactions!="{}"&&message.deletedAt.isEmpty()) {
                    val reactions=remember(message.reactions) {runCatching {val j=org.json.JSONObject(message.reactions);j.keys().asSequence().map {j.getString(it)}.groupingBy {it}.eachCount()}.getOrDefault(emptyMap())}
                    val own=remember(message.reactions,message.account) {runCatching {org.json.JSONObject(message.reactions).optString(message.account)}.getOrDefault("")}
                    Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        reactions.forEach {(emoji,count)->
                            val allowed=message.status=="SENT" &&
                                model.session.value?.userId==message.account &&
                                !selectionMode && message.deletedAt.isEmpty()
                            Surface(
                                onClick={
                                    model.action {
                                        // A stale bubble cannot react after a deletion,
                                        // account change, or a superseding message update.
                                        val latest=model.messages.value.firstOrNull {
                                            it.id==message.id && it.account==message.account &&
                                                it.conversation==message.conversation
                                        }
                                        if(latest!=null && latest.status=="SENT" &&
                                            latest.deletedAt.isEmpty() &&
                                            model.session.value?.userId==message.account) {
                                            val previous=runCatching {
                                                org.json.JSONObject(latest.reactions)
                                                    .optString(message.account)
                                            }.getOrDefault("")
                                            model.repository.messageAction(
                                                latest.id,"react",
                                                if(previous==emoji)"" else emoji)
                                        }
                                    }
                                },
                                enabled=allowed,
                                shape=Glass.capsule,
                                border=if(emoji==own)androidx.compose.foundation.BorderStroke(
                                    1.dp,MaterialTheme.colorScheme.primary) else null,
                                color=MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text("$emoji $count",
                                    Modifier.padding(horizontal=8.dp,vertical=3.dp),
                                    style=MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                if(message.status=="FAILED") {Text(message.error,style=MaterialTheme.typography.bodySmall);TextButton(onClick=onRetry) {Text("Retry")}}
            }
        }
    }
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable fun SettingsScreen(model:AppViewModel,busy:Boolean) {
    // Open the reference Settings dashboard first; Preferences stays one tap away.
    // Never bury account, help, storage and chat-list routes behind a Back action.
    var section by rememberSaveable {mutableStateOf<String?>(null)}
    var fromPreferences by rememberSaveable {mutableStateOf(false)}
    var unavailable by remember {mutableStateOf<String?>(null)}
    if(section==null) {
        SettingsHome(model) {title->
            val destination=title
            if(destination in listOf("Devices","Language")) unavailable=destination else {
                fromPreferences=false
                section=destination
            }
        }
        unavailable?.let {title->LiquidAlertDialog(onDismissRequest={unavailable=null},title={Text(title)},text={Text(when(title){"Language"->"English is the current app language.";"Chat Folders"->"Use All, Favorite, Archive and Unread in the Chats menu.";"Data and Storage"->"Shared images are loaded on demand. Automatic downloads are not enabled.";else->"Device management is not available in this version yet."})},confirmButton={TextButton(onClick={unavailable=null}){Text("OK")}})}
        return
    }
    BackHandler {
        section=referenceSettingsBackTarget(section,fromPreferences)
        if(section==null)fromPreferences=false
    }

    if(section=="Liquid Chat Preferences") {
        MbPreferencesLanding(
            onBack={section=null;fromPreferences=false},
            onNavigate={destination->fromPreferences=true;section=destination}
        )
        return
    }
    if(section=="Starred Messages") {
        val activeSession by model.session.collectAsStateWithLifecycle()
        val availableChats by model.chats.collectAsStateWithLifecycle()
        val owner=activeSession?.userId
        if(owner!=null) {
            MbAllStarredMessages(model,availableChats,owner,onDismiss={
                section=null
                fromPreferences=false
            })
        } else {
            Text("Sign in to view starred messages.",
                modifier=Modifier.padding(24.dp),
                style=MaterialTheme.typography.bodyMedium)
        }
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
    val photoThemePicker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {uri->
        if(uri!=null)model.action {
            val accent=PhotoAccentSampler.extract(context,uri)
            model.setDisplay(model.display.value.copy(accentColor="Custom",
                customAccentHex=accent))
            themeFeedback="Photo theme color applied: $accent"
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
    val referenceSection=referenceListUsesFlatRows(display)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal=if(referenceSection)20.dp else 16.dp,
            vertical=if(referenceSection)8.dp else 12.dp)
        .padding(bottom=98.dp+WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        verticalArrangement=Arrangement.spacedBy(if(referenceSection)12.dp else 20.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min=if(referenceSection)47.dp else 0.dp),
            verticalAlignment=Alignment.CenterVertically) {IconButton(onClick={
            section=referenceSettingsBackTarget(section,fromPreferences)
            if(section==null)fromPreferences=false
        }){Icon(Icons.AutoMirrored.Outlined.ArrowBack,"Back to Settings")};
            Text(section.orEmpty().removePrefix("Liquid Chat "),
                style=if(referenceSection)MaterialTheme.typography.titleLarge
                    else MaterialTheme.typography.headlineSmall)}
        if(section=="Liquid Chat Help") MbTipsAndHelp {destination->section=destination}
        if(section=="Liquid Chat Service Status") MbServiceStatus(model)
        if(section=="Liquid Chat Updates") MbAppUpdates()
        if(section=="Liquid Chat Developer Information") MbDeveloperInformation()
        if(section=="Liquid Chat Multimedia")
            MbRecordedMultimediaSettings(display,model::setDisplay)
        if(section=="Liquid Chat Animations")
            MbRecordedAnimationSettings(display,model::setDisplay)
        if(section=="Liquid Chat VIP Features")
            MbRecordedExtraFeaturesSettings(onNavigate={section=it})
        if(section=="Liquid Chat General Settings") MbGeneralAppearanceSettings(display,model::setDisplay)
        if(section=="Liquid Chat Home Screen") MbStoryPlaybackSettingsCard()
        if(section=="Liquid Chat Home Screen") GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
            Text("Home interaction controls",style=MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("Show preview on chat long-press",Modifier.weight(1f))
                Switch(checked=display.showLongPressPreview,
                    onCheckedChange={model.setDisplay(display.copy(showLongPressPreview=it))})
            }
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("Hide floating new-chat buttons",Modifier.weight(1f))
                Switch(checked=display.hideHomeFloatingButtons,
                    onCheckedChange={model.setDisplay(display.copy(hideHomeFloatingButtons=it))})
            }
            Text("Contacts and New chat remain available through existing navigation.",
                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if(section=="Liquid Chat Home Screen") MbNavigationVisibilitySettingsCard()
        if(section=="Liquid Chat Notifications") MbConversationSoundSettings()
        if(section=="Liquid Chat Conversation Screen") MbConversationInteractionSettings(model,display)
        if(section=="Liquid Chat Conversation Screen") GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
            Text("Keyboard privacy and stickers",style=MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Incognito keyboard")
                    Text("Ask your keyboard not to learn the text you type in chats. Android keyboards may ignore this request.",
                        style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked=display.incognitoKeyboard,
                    onCheckedChange={model.setDisplay(display.copy(incognitoKeyboard=it))})
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Custom sticker display size")
                    Text("Resize stickers in this app only; the original file and recipient experience are unchanged.",
                        style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked=display.customStickerSize,
                    onCheckedChange={model.setDisplay(display.copy(customStickerSize=it))})
            }
            if(display.customStickerSize) {
                Text("Sticker size: ${display.stickerSize} dp",style=MaterialTheme.typography.labelLarge)
                Slider(value=display.stickerSize.toFloat(),
                    onValueChange={model.setDisplay(display.copy(stickerSize=it.toInt().coerceIn(96,256)))},
                    valueRange=96f..256f)
            }
        }
        if(section=="Liquid Chat General Settings" || section=="Appearance") LauncherIconSettings()
        if(section=="Liquid Chat General Settings") MbTouchEffectsSettingsCard()
        if(section=="Data and Storage") DataStorage(model)
        if(section=="Chat Folders") MbChatFoldersSettings(model)
        if(section=="Liquid Chat Themes") GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
            Text("Themes",style=MaterialTheme.typography.titleLarge)
            Text("Save your current Liquid Chat appearance to a JSON theme, or import a saved theme.",style=MaterialTheme.typography.bodyMedium)
            Text("Imported themes change only this device's colors, navigation and chat appearance. No messages, accounts or contacts are exported.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider()
            FilledTonalButton(onClick={photoThemePicker.launch("image/*")},
                enabled=!busy,modifier=Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.AddPhotoAlternate,null)
                Spacer(Modifier.width(8.dp))
                Text("Create accent from photo")
            }
            Text("Choose a photo on this device to pick its dominant color. No photo is uploaded or stored.",
                style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
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
            var previewThemeName by remember {mutableStateOf<String?>(null)}
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
                    Text(name,Modifier.weight(1f),maxLines=1,
                        overflow=TextOverflow.Ellipsis)
                    TextButton(onClick={previewThemeName=name}) {Text("Preview")}
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
            previewThemeName?.let {name->
                val candidate=runCatching {
                    LiquidThemeLibrary.restore(context,name,model.display.value)
                }.getOrNull()
                if(candidate!=null) {
                    LiquidAlertDialog(
                        onDismissRequest={previewThemeName=null},
                        title={Text(name,maxLines=1,
                            overflow=TextOverflow.Ellipsis)},
                        text={MbThemePreview(candidate)},
                        confirmButton={TextButton(onClick={
                            model.setDisplay(candidate)
                            previewThemeName=null
                            themeFeedback="Theme applied: $name"
                        }) {Text("Apply theme")}},
                        dismissButton={TextButton(onClick={previewThemeName=null}) {
                            Text("Cancel")
                        }})
                } else LaunchedEffect(name) {
                    themeFeedback="Saved theme cannot be read."
                    previewThemeName=null
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
            MbColorPickerButton("Main theme color",
                normalizedAccentHex(accentHexInput)?:display.customAccentHex) {hex->
                accentHexInput=hex
                model.setDisplay(display.copy(accentColor="Custom",customAccentHex=hex))
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
            Text("Preset choices use Android's built-in fonts. Imported fonts stay on this device.",
                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Bold title font family",style=MaterialTheme.typography.labelLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf("Follow","System","Serif","Mono","Cursive").forEach {value->
                    FilterChip(selected=display.titleFontPreset==value,
                        onClick={model.setDisplay(display.copy(titleFontPreset=value))},
                        label={Text(if(value=="Follow")"Follow app font" else value)})
                }
            }
            Text("Headings and titles can use a separate font without changing message text.",
                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            MbFontImportSettings(
                onChooseBody={model.setDisplay(display.copy(fontPreset="Imported"))},
                onChooseTitle={model.setDisplay(display.copy(titleFontPreset="Imported"))},
                onRemoveBody={
                    if(model.display.value.fontPreset=="Imported")
                        model.setDisplay(model.display.value.copy(fontPreset="System"))
                },
                onRemoveTitle={
                    if(model.display.value.titleFontPreset=="Imported")
                        model.setDisplay(model.display.value.copy(titleFontPreset="Follow"))
                }
            )
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
        if(section=="Liquid Chat General Settings") {
            val accountId by model.session.collectAsStateWithLifecycle()
            MbQuickReplySettings(accountId?.userId)
            DataStorage(model)
        }
        
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
            MbContactInfoStyleSettingsCard()
            MbExpandableSettingsCard("Conversation header") {
                Text("Contact name size · ${display.chatHeaderNameSize} sp",style=MaterialTheme.typography.titleSmall)
                Slider(value=display.chatHeaderNameSize.toFloat(),
                    onValueChange={model.setDisplay(display.copy(chatHeaderNameSize=it.toInt()))},
                    valueRange=12f..24f,steps=11,modifier=Modifier.fillMaxWidth())
                MbReferenceChoicePicker(
                    title="Call, video and menu icon colors",
                    choices=listOf("Theme","Accent","Text"),
                    selected=display.chatHeaderIconColor,
                    onSelect={mode->model.setDisplay(display.copy(chatHeaderIconColor=mode))})
                Text("Changing icon colors never changes contact actions or calling availability.",
                    style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            MbExpandableSettingsCard("Media upload quality") {
                MbReferenceChoicePicker(title="Photo upload quality",
                    choices=listOf("Standard","HD"),
                    selected=display.photoQuality,
                    onSelect={quality->
                        model.setDisplay(display.copy(photoQuality=quality))
                    })
                MbReferenceChoicePicker(title="Video upload quality",
                    choices=listOf("Standard","HD"),
                    selected=display.videoQuality,
                    onSelect={quality->
                        model.setDisplay(display.copy(videoQuality=quality))
                    })
                Text("Photos use device-private JPEG processing. Video quality selects 480p or 720p on-device H.264 encoding; clips stay within 30 seconds and 8 MB.",
                    style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            MbExpandableSettingsCard("Copy message preferences") {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Include sender and time when copying")
                        Text("Off copies message text only, including multi-selection.",
                            style=MaterialTheme.typography.bodySmall,
                            color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked=display.copySenderAndTime,
                        onCheckedChange={model.setDisplay(display.copy(copySenderAndTime=it))})
                }
            }
            MbExpandableSettingsCard("Wallpaper & contrast") {
                Text("Photo wallpaper dimming · ${display.chatWallpaperDim}%",style=MaterialTheme.typography.titleSmall)
            Slider(value=display.chatWallpaperDim.toFloat(),onValueChange={model.setDisplay(display.copy(chatWallpaperDim=it.toInt()))},
                valueRange=0f..100f,modifier=Modifier.fillMaxWidth())
            Text("50% preserves the original conversation wallpaper contrast. Only locally selected photos use this control.",
                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            MbExpandableSettingsCard("Message bubbles") {
            if(display.navBarStyle=="Reference")
                MbReferenceSeekbarRow("Bubble transparency",display.bubbleOpacity,
                    45..100,"%",onApply={opacity->
                        model.setDisplay(display.copy(bubbleOpacity=opacity))
                    })
            else {
            Text("Bubble opacity · ${display.bubbleOpacity}%",style=MaterialTheme.typography.titleSmall)
            Slider(value=display.bubbleOpacity.toFloat(),
                onValueChange={model.setDisplay(display.copy(bubbleOpacity=it.toInt()))},
                valueRange=45f..100f,steps=54,modifier=Modifier.fillMaxWidth())
            }
                MbReferenceBubbleStylePicker(
                    selected=display.bubbleStyle,
                    onSelect={style->
                        model.setDisplay(display.copy(bubbleStyle=style))
                    })
                MbReferenceChoicePicker(title="Message bubble color",
                    choices=listOf("Original","Green","Lavender","Rose","Night"),
                    selected=display.bubbleColor,
                    onSelect={palette->
                        model.setDisplay(display.copy(bubbleColor=palette))
                    })
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
                MbColorPickerButton(if(mine)"Outgoing bubble" else "Incoming bubble",current) {hex->
                    input=hex
                    model.setDisplay(if(mine)
                        display.copy(outgoingBubbleHex=hex,outgoingBubblePreset="Custom")
                    else display.copy(incomingBubbleHex=hex,incomingBubblePreset="Custom"))
                }
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
                MbColorPickerButton(target,currentHex) {hex->
                    hexInput=hex
                    model.setDisplay(when(target) {
                        "Outgoing text"->display.copy(outgoingInk="Custom",outgoingInkHex=hex)
                        "Incoming text"->display.copy(incomingInk="Custom",incomingInkHex=hex)
                        "Quoted text"->display.copy(quotedInk="Custom",quotedInkHex=hex)
                        else->display.copy(timestampInk="Custom",timestampInkHex=hex)
                    })
                }
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
            if(display.navBarStyle=="Reference") {
                MbReferenceSeekbarRow("Message font size",display.messageFontSize,
                    10..30,"sp",onApply={size->
                        model.setDisplay(display.copy(messageFontSize=size))
                    })
                // Reference's emoji scale controls this device's real picker.
                MbReferenceSeekbarRow("Emoji picker size",display.emojiPickerSize,
                    18..32,"sp",onApply={size->
                        model.setDisplay(display.copy(emojiPickerSize=size))
                    })
            } else {
            Text("Message font size",style=MaterialTheme.typography.titleSmall)
            Text("${display.messageFontSize} sp",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(value=display.messageFontSize.toFloat(),onValueChange={value->model.setDisplay(display.copy(messageFontSize=value.toInt()))},
                valueRange=10f..30f,steps=19,modifier=Modifier.fillMaxWidth())
            // MB 3.3.4 mb_emoji_sizer: changes the real emoji picker, not message content.
            Text("Emoji picker size · ${display.emojiPickerSize} sp",style=MaterialTheme.typography.titleSmall)
            Slider(value=display.emojiPickerSize.toFloat(),
                onValueChange={value->model.setDisplay(display.copy(emojiPickerSize=value.toInt()))},
                valueRange=18f..32f,steps=13,modifier=Modifier.fillMaxWidth())            }
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
        if(section=="Notifications" || section=="Liquid Chat Notifications") {
            val currentAccount by model.session.collectAsStateWithLifecycle()
            DeviceAlertSettings(currentAccount?.userId)
            NotificationSystemSettings()
        }
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
    ProtectedImage(model.backend,message.mediaId,message.localImage,if(message.mediaKind=="sticker")"Shared sticker" else "Shared photo",(if(message.mediaKind=="sticker")Modifier.width(conversationStickerSize(LocalDisplayOptions.current).dp) else Modifier.fillMaxWidth()).aspectRatio(if(thumbnail)1f else (if(message.imageHeight>0&&message.imageWidth>0)message.imageWidth.toFloat()/message.imageHeight else 1f).coerceIn(.5f,2f)).clip(Glass.compact).clickable {open=true},scale=if(thumbnail)androidx.compose.ui.layout.ContentScale.Crop else androidx.compose.ui.layout.ContentScale.Fit)
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
                    ProtectedImage(model.backend,message.mediaId,message.localImage,if(message.mediaKind=="sticker")"Full-size shared sticker" else "Full-size shared photo",Modifier.fillMaxSize().graphicsLayer {scaleX=zoom;scaleY=zoom;translationX=pan.x;translationY=pan.y})
                }
                val context=LocalContext.current
                val scope=rememberCoroutineScope()
                var saving by remember(message.id) {mutableStateOf(false)}
                var saveFeedback by remember(message.id) {mutableStateOf<String?>(null)}
                Row(Modifier.fillMaxWidth(),
                    verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.SpaceEvenly) {
                    TextButton(onClick={zoom=1f;pan=Offset.Zero}) {Text("Reset zoom")}
                    if(message.status=="SENT" && message.deletedAt.isEmpty() &&
                        message.mediaId.isNotEmpty() && message.mediaKind=="image") {
                        TextButton(onClick={
                            saving=true
                            saveFeedback=null
                            scope.launch {
                                try {
                                    SavedMedia.savePhoto(context,model.repository,message)
                                    saveFeedback="Photo saved to Pictures/Liquid Chat"
                                } catch(e:kotlinx.coroutines.CancellationException) {throw e}
                                  catch(e:Exception) {
                                    saveFeedback=e.message ?: "Could not save photo."
                                } finally {saving=false}
                            }
                        },enabled=!saving) {
                            Text(if(saving)"Saving…" else "Save photo")
                        }
                    }
                }
                saveFeedback?.let {Text(it,
                    modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp),
                    style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)}

            }
        }
    }
}
