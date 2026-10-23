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
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
        val owner=session?.userId
        val backgroundBrush=if(display.showWallpaper)
            wallpaper(if(palette=="Photo" || palette=="Video") "Ice" else palette,LocalGlassDark.current)
        else androidx.compose.ui.graphics.Brush.linearGradient(
            listOf(MaterialTheme.colorScheme.background,MaterialTheme.colorScheme.background)
        )
        Box(Modifier.matchParentSize().glassSource(.5f).background(backgroundBrush)) {
            if(display.showWallpaper && palette=="Video" && owner!=null) {
                val videoFile=remember(owner,cid,wallpaperRevision) {
                    runCatching {com.pritam.liquidchat.data.PrivateVideoWallpaper.location(context,owner,cid)}
                        .getOrNull()
                }
                if(videoFile?.isFile==true) {
                    LocalVideoWallpaper(videoFile,foreground,wallpaperRevision,Modifier.matchParentSize())
                    Box(Modifier.matchParentSize().background(
                        if(LocalGlassDark.current) Color.Black.copy(alpha=wallpaperDimAlpha(.46f,display.chatWallpaperDim))
                        else Color.White.copy(alpha=wallpaperDimAlpha(.30f,display.chatWallpaperDim))
                    ))
                }
            }
            if(display.showWallpaper && palette=="Photo" && owner!=null) {
                val photoFile=remember(owner,cid,wallpaperRevision) {
                    runCatching {com.pritam.liquidchat.data.PersonalWallpaper.location(context,owner,cid)}
                        .getOrNull()
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
                        if(LocalGlassDark.current) Color.Black.copy(alpha=wallpaperDimAlpha(.46f,display.chatWallpaperDim))
                        else Color.White.copy(alpha=wallpaperDimAlpha(.30f,display.chatWallpaperDim))
                    ))
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
            Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=8.dp),
            Glass.capsule
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
            Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=8.dp),
            Glass.capsule
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
        val start=minOf(input.selection.start,input.selection.end)
        val end=maxOf(input.selection.start,input.selection.end)
        val text=body.replaceRange(start,end,emoji)
        if(text.length<=4000)updateInput(TextFieldValue(text,TextRange(start+emoji.length)))
    },onDismiss={emojiPicker=false},onSavedStickers={
        emojiPicker=false
        savedStickers=StickerLibrary.list(context,session?.userId)
        stickerNotice=null
        stickerLibraryOpen=true
    },accountKey=session?.userId.orEmpty())
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
        var expandedMessageActions by remember(m.id) {mutableStateOf(false)}
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
                        if(m.body.isNotBlank() && m.deletedAt.isEmpty()
                            && m.mediaKind!="image") GlassActionRow(
                            "Copy text",Icons.Outlined.ContentCopy,onClick={
                                clipboard.setText(AnnotatedString(copiedText(m,display.copySenderAndTime)))
                                actionMessage=null
                            })
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
        if(expandedMessageActions) {
            LiquidActionSheet(title="Message actions",onDismiss={actionMessage=null}) {
                actionContents()
            }
        } else CompactMessageActionMenu(
            preview=m.body.takeIf {m.deletedAt.isEmpty() && it.isNotBlank()},
            reactions=if("Quick reactions" in categories)
                MbQuickReactions.selected(display.quickReactions) else emptyList(),
            canReply=m.status=="SENT" && m.deletedAt.isEmpty(),
            canCopy=m.body.isNotBlank() && m.deletedAt.isEmpty(),
            canForward=canForwardTextMessage(m,session?.userId),
            canStar=m.status=="SENT" && m.deletedAt.isEmpty(),
            starred=m.starred,
            canDelete=m.status in listOf("SENT","QUEUED","FAILED"),
            onReact={emoji->
                actionMessage=null
                model.action {
                    val previous=runCatching {
                        org.json.JSONObject(m.reactions).optString(m.account)
                    }.getOrDefault("")
                    model.repository.messageAction(m.id,"react",
                        if(previous==emoji)"" else emoji)
                }
            },
            onReply={
                replyTo=m.id
                draftVersion=model.repository.saveDraft(cid,body,replyTo)
                actionMessage=null
            },
            onCopy={
                clipboard.setText(AnnotatedString(copiedText(m,display.copySenderAndTime)))
                actionMessage=null
            },
            onForward={actionMessage=null;forwardMessages=listOf(m);forwardSearch=""},
            onStar={
                actionMessage=null
                model.action {model.repository.messageAction(m.id,"star",(!m.starred).toString())}
            },
            onSelect={actionMessage=null;selectedIds=setOf(m.id)},
            onDelete={deleteMessage=m;actionMessage=null},
            onMore={expandedMessageActions=true},
            onDismiss={actionMessage=null}
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
            if(savedStickers.isEmpty())
                Text("No saved stickers. Choose Create sticker to make one.")
            MbSavedStickerShelf(
                stickers=savedStickers,enabled=!busy&&!sendingSavedSticker,
            OutlinedButton(
                onClick={
                    stickerLibraryOpen=false
                    stickerPicker.launch("image/*")
                },
                enabled=!busy&&!sendingSavedSticker,
                modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)
            ) {
                Icon(Icons.Outlined.AddPhotoAlternate,contentDescription=null)
                Spacer(Modifier.width(8.dp))
                Text("Create sticker from photo")
            }
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
