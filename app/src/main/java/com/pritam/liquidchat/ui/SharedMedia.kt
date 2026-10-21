package com.pritam.liquidchat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pritam.liquidchat.AppViewModel
import com.pritam.liquidchat.data.Chat
import com.pritam.liquidchat.data.Message
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

/** A refresh supersedes every older page, including a manually launched append. */
internal class SharedMediaRequests {
    private var generation=0L
    private var loading=false
    fun begin(reset:Boolean):Long? {
        if(!reset&&loading)return null
        loading=true
        return ++generation
    }
    fun current(request:Long)=request==generation
    fun finish(request:Long) {if(current(request))loading=false}
}

internal data class SharedMediaPage(val visible:List<Message>,val cursor:Long?,val more:Boolean)
internal fun sharedMediaPage(raw:List<Message>,before:Long):SharedMediaPage {
    val cursor=raw.lastOrNull()?.seq
    return SharedMediaPage(raw.filter {it.deletedAt.isEmpty()},cursor,
        raw.size==30&&cursor!=null&&cursor>0&&cursor<before)
}

internal fun mergeSharedMediaRows(existing:List<Message>,page:List<Message>,reset:Boolean,revoked:Set<String>):List<Message> =
    (if(reset)page else existing+page).filter {it.deletedAt.isEmpty()&&it.id !in revoked}.distinctBy {it.id}

/**
 * Only media/starred changes require reloading their RPC pages; ordinary text
 * activity should not blank the gallery or re-download signed thumbnails.
 */
internal fun sharedMediaRefreshKeys(messages:List<Message>,kind:String):List<String> =
    messages.asSequence().filter {message->
        message.deletedAt.isEmpty() && when(kind) {
            "star"->message.starred
            "voice"->message.mediaKind=="voice" && message.mediaId.isNotEmpty()
            "sticker"->message.mediaKind=="sticker" && message.mediaId.isNotEmpty()
            "document"->message.mediaKind=="document" && message.mediaId.isNotEmpty()
            "chat_video"->message.mediaKind=="chat_video" && message.mediaId.isNotEmpty()
            else->message.mediaKind=="image" && message.mediaId.isNotEmpty()
        }
    }.map {message->
        "${message.id}|${message.mediaId}|${message.editedAt}|${message.starred}"
    }.toList()

/**
 * Starred entries may be text, photos, or voice. Use the existing protected
 * media renderers rather than exposing only the caption or a media identifier.
 */
internal fun starredMediaPresentation(message:Message):String = when {
    message.deletedAt.isNotEmpty() -> "hidden"
    message.mediaKind=="voice" && message.mediaId.isNotEmpty() -> "voice"
    message.mediaKind=="image" && message.mediaId.isNotEmpty() -> "image"
    message.mediaKind=="sticker" && message.mediaId.isNotEmpty() -> "sticker"
    message.mediaKind=="document" && message.mediaId.isNotEmpty() -> "document"
    message.mediaKind=="chat_video" && message.mediaId.isNotEmpty() -> "chat_video"
    else -> "text"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun ContactMediaDialog(model:AppViewModel,chat:Chat,sourceVersion:List<Message>,initialKind:String="image",onDismiss:()->Unit) {
    val session by model.session.collectAsStateWithLifecycle()
    val iosStyle=rememberIosContactInfoStyle()
    val account=session?.userId
    // Observe auth-scope changes even if the user ID stays the same.
    val sessionEpoch by model.backend.uiReads.sessionEpoch.collectAsStateWithLifecycle()
    val epoch=model.backend.uiReads.epoch(account).takeIf {it==sessionEpoch}
    var kind by remember(chat.id,account) {mutableStateOf(initialKind)}
    var rows by remember(chat.id,account,epoch) {mutableStateOf(emptyList<Message>())}
    var more by remember(chat.id,account,epoch) {mutableStateOf(true)}
    var loading by remember(chat.id,account,epoch) {mutableStateOf(false)}
    var failed by remember(chat.id,account,epoch) {mutableStateOf(false)}
    var cursor by remember(chat.id,account,epoch) {mutableStateOf<Long?>(null)}
    val requests=remember(chat.id,account,epoch) {SharedMediaRequests()}
    val refreshKeys=remember(kind,sourceVersion) {sharedMediaRefreshKeys(sourceVersion,kind)}
    val scope=rememberCoroutineScope()
    var revokedInRequest by remember(chat.id,account,epoch) {mutableStateOf<Set<String>>(emptySet())}
    LaunchedEffect(chat.id,account,epoch) {
        model.repository.revocations.collect {event->
            if(event.account==account&&model.backend.uiReads.epoch(account)==epoch) {
                if(loading)revokedInRequest=revokedInRequest+event.id
                rows=rows.filterNot {it.id==event.id}
            }
        }
    }
    suspend fun page(reset:Boolean) {
        val request=requests.begin(reset) ?: return
        // Earlier revocations are already committed on the server. Retain only
        // those arriving during this fresh RPC so a delayed page cannot undo them.
        revokedInRequest=emptySet()
        val requestedKind=kind
        val requestedAccount=account
        val requestedEpoch=epoch
        if(requestedAccount==null || chat.account!=requestedAccount || requestedEpoch==null) {
            requests.finish(request);rows=emptyList();more=false;loading=false;return
        }
        fun current()=requests.current(request) && kind==requestedKind &&
            model.backend.session.value?.userId==requestedAccount &&
            model.backend.uiReads.epoch(requestedAccount)==requestedEpoch
        loading=true;failed=false
        try {
            val before=if(reset)Long.MAX_VALUE else cursor ?: Long.MAX_VALUE
            val fetched=if(requestedKind=="star")model.repository.starred(chat.id,before)
                else model.repository.sharedMedia(chat.id,before,requestedKind)
            if(!current())return
            val result=sharedMediaPage(fetched,before)
            rows=mergeSharedMediaRows(rows,result.visible,reset,revokedInRequest)
            cursor=result.cursor
            more=result.more
        } catch(e:kotlinx.coroutines.CancellationException){throw e}
          catch(_:Exception){if(current())failed=true}
        finally {if(current())loading=false;requests.finish(request)}
    }
    LaunchedEffect(chat.id,account,epoch,kind,refreshKeys) {
        rows=emptyList();cursor=null;more=true;page(true)
    }
    ModalBottomSheet(onDismissRequest=onDismiss,
        sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),
        containerColor=glassPopupColor(),tonalElevation=0.dp,
        shape=androidx.compose.foundation.shape.RoundedCornerShape(topStart=30.dp,topEnd=30.dp)) {
        ContactMediaContent(chat,kind,rows,loading,failed,more,
            iosStyle=iosStyle,
            onKind={kind=it},onDismiss=onDismiss,
            onSave={model.action {model.repository.saveContact(chat.peer,true)}},
            onPage={scope.launch {page(rows.isEmpty())}},
            avatar={Avatar(chat.name,chat.avatarId,model.backend,
                size=if(iosStyle)92.dp else 76.dp)},
            media={m->
                if(kind=="image" || kind=="sticker")SharedImagePreview(model,m,thumbnail=true)
                else if(kind=="document")PdfMessageAttachment(model,m)
                else if(kind=="chat_video")ChatVideoAttachment(model,m)
                else GlassCard(Modifier.fillMaxWidth()) {
                    if(kind=="voice")VoicePlayer(model,m)
                    else {
                        when(starredMediaPresentation(m)) {
                            "voice"->VoicePlayer(model,m)
                            "image"->SharedImagePreview(model,m)
                            "sticker"->SharedImagePreview(model,m)
                            "document"->PdfMessageAttachment(model,m)
                            "chat_video"->ChatVideoAttachment(model,m)
                            "text"->Text(m.body,maxLines=6,overflow=TextOverflow.Ellipsis)
                        }
                        TextButton(onClick={model.open(chat.id,m.id);onDismiss()}) {Text("Go to message")}
                    }
                }
            })
    }
}

/** One lazy scroll surface keeps the hero, tabs and results reachable in landscape/large text. */
@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun ContactMediaContent(
    chat:Chat,kind:String,rows:List<Message>,loading:Boolean,failed:Boolean,more:Boolean,
    iosStyle:Boolean=false,
    onKind:(String)->Unit,onDismiss:()->Unit,onSave:()->Unit,onPage:()->Unit,
    avatar:@Composable ()->Unit,media:@Composable (Message)->Unit
) {
    Column(Modifier.fillMaxWidth().fillMaxHeight(.9f).padding(horizontal=20.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text("Contact details",Modifier.weight(1f),style=MaterialTheme.typography.titleLarge)
            IconButton(onClick=onDismiss) {Icon(Icons.Outlined.Close,"Close contact details")}
        }
        // Each tab owns its viewport; media request/deletion guards stay unchanged.
        key(chat.id,kind) {
            LazyVerticalGrid(columns=GridCells.Adaptive(minSize=100.dp),
                modifier=Modifier.fillMaxWidth().weight(1f).testTag("contact-media-grid"),
                contentPadding=PaddingValues(top=12.dp,bottom=28.dp),
                horizontalArrangement=Arrangement.spacedBy(8.dp),
                verticalArrangement=Arrangement.spacedBy(12.dp)) {
                item(key="contact",span={GridItemSpan(maxLineSpan)}) {
                    GlassCard(Modifier.fillMaxWidth(),
                        if(iosStyle)Glass.hero else Glass.card) {
                        Column(Modifier.fillMaxWidth(),
                            horizontalAlignment=Alignment.CenterHorizontally,
                            verticalArrangement=Arrangement.spacedBy(if(iosStyle)12.dp else 8.dp)) {
                            if(iosStyle) Text("LIQUID CHAT CONTACT",
                                style=MaterialTheme.typography.labelSmall,
                                color=MaterialTheme.colorScheme.primary)
                            avatar()
                            Text(chat.name,style=if(iosStyle)
                                MaterialTheme.typography.headlineMedium
                                else MaterialTheme.typography.headlineSmall)
                            Text("@${chat.username}",
                                color=MaterialTheme.colorScheme.onSurfaceVariant)
                            if(iosStyle) HorizontalDivider(Modifier.fillMaxWidth(.8f),
                                color=MaterialTheme.colorScheme.outlineVariant)
                            OutlinedButton(onClick=onSave,modifier=Modifier.heightIn(min=48.dp)) {
                                Icon(Icons.Outlined.PersonAdd,null,Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp));Text("Save contact")
                            }
                            if(iosStyle) Text("Private Liquid Chat conversation",
                                style=MaterialTheme.typography.bodySmall,
                                color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item(key="tabs",span={GridItemSpan(maxLineSpan)}) {
                    FlowRow(Modifier.fillMaxWidth(),
                        horizontalArrangement=Arrangement.spacedBy(8.dp),
                        verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        listOf("image" to "Photos","chat_video" to "Videos",
                            "sticker" to "Stickers","voice" to "Voice",
                            "document" to "Documents","star" to "Starred").forEach {(value,label)->
                            FilterChip(selected=kind==value,onClick={onKind(value)},
                                label={Text(label)},shape=Glass.capsule)
                        }
                    }
                }
                item(key="scope",span={GridItemSpan(maxLineSpan)}) {
                    Text("Shared in this conversation",style=MaterialTheme.typography.labelMedium,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(rows,key={it.id},span={GridItemSpan(if(kind in listOf("image","sticker"))1 else maxLineSpan)}) {media(it)}
                if(rows.isEmpty()&&!loading&&!failed) item(key="empty",span={GridItemSpan(maxLineSpan)}) {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Icon(when(kind){"voice"->Icons.Outlined.MicNone;"sticker"->Icons.Outlined.AutoAwesome;"document"->Icons.Outlined.Description;"chat_video"->Icons.Outlined.Videocam;"star"->Icons.Outlined.StarOutline;else->Icons.Outlined.PhotoLibrary},
                            null,Modifier.size(32.dp),tint=MaterialTheme.colorScheme.primary)
                        Text("No shared ${when(kind){"voice"->"voice messages";"sticker"->"stickers";"document"->"documents";"chat_video"->"videos";"star"->"starred messages";else->"photos"}} yet",
                            style=MaterialTheme.typography.titleMedium)
                        Text(when(kind){"star"->"Star a message to find it here.";"voice"->"Voice messages you exchange appear here.";"sticker"->"Stickers you exchange appear here.";"document"->"PDF files you exchange appear here.";"chat_video"->"Private videos you exchange appear here.";else->"Photos you exchange appear here."},
                            style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item(key="page",span={GridItemSpan(maxLineSpan)}) {
                    Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center) {
                        when {
                            loading->CircularProgressIndicator(Modifier.size(28.dp))
                            failed->OutlinedButton(onClick=onPage) {Text("Retry shared media")}
                            more->OutlinedButton(onClick=onPage) {Text("Load more")}
                        }
                    }
                }
            }
        }
    }
}
