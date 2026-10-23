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
    GlassSurface(Modifier.fillMaxWidth()
        .combinedClickable(onClick=onClick,onLongClick=onLongClick),
        if(reference)Glass.compact else Glass.card) {
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
    if(inboxDisplay.showWallpaper && homeVideo?.isFile==true) {
        LocalVideoWallpaper(homeVideo,homeForeground,homePhotoRevision,Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(
            if(LocalGlassDark.current)Color.Black.copy(alpha=wallpaperDimAlpha(.56f,inboxDisplay.homeWallpaperDim))
            else Color.White.copy(alpha=wallpaperDimAlpha(.45f,inboxDisplay.homeWallpaperDim))))
    } else if(inboxDisplay.showWallpaper && homePhoto?.isFile==true) {
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
        if(inboxDisplay.navBarStyle!="Reference") GlassSurface(
            Modifier.size(52.dp),CircleShape
        ) {
            IconButton(onClick=onContacts,modifier=Modifier.fillMaxSize()) {
                Icon(Icons.Outlined.Add,"New chat",
                    tint=MaterialTheme.colorScheme.primary)
            }
        }
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
