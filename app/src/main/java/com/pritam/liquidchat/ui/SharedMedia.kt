package com.pritam.liquidchat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
            else->message.mediaKind=="image" && message.mediaId.isNotEmpty()
        }
    }.map {message->
        "${message.id}|${message.mediaId}|${message.editedAt}|${message.starred}"
    }.toList()

@Composable internal fun ContactMediaDialog(model:AppViewModel,chat:Chat,sourceVersion:List<Message>,initialKind:String="image",onDismiss:()->Unit) {
    val session by model.session.collectAsStateWithLifecycle()
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
    AlertDialog(onDismissRequest=onDismiss,title={Text(chat.name)},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {Avatar(chat.name,chat.avatarId,model.backend);Column {Text("@${chat.username}");TextButton(onClick={model.action {model.repository.saveContact(chat.peer,true)}}) {Text("Save contact")}}}
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {listOf("image" to "Media","voice" to "Voice","star" to "Starred").forEach {(value,label)->FilterChip(selected=kind==value,onClick={kind=value},label={Text(label)})}}
        Text("Shared in this conversation",style=MaterialTheme.typography.labelSmall)
        LazyColumn(Modifier.fillMaxWidth().heightIn(max=350.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            items(rows,key={it.id}) {m->if(kind=="star")Column {Text(m.body,maxLines=3);TextButton(onClick={model.open(chat.id,m.id);onDismiss()}) {Text("Go to message")}} else if(kind=="voice")VoicePlayer(model,m) else SharedImagePreview(model,m)}
            if(rows.isEmpty()&&!loading&&!failed)item {Text("No shared ${if(kind=="voice")"voice messages" else if(kind=="star")"starred messages" else "photos"}.")}
            item {
                when {
                    loading->CircularProgressIndicator(Modifier.size(24.dp))
                    failed->TextButton(onClick={scope.launch {page(rows.isEmpty())}}) {Text("Retry shared media")}
                    more->TextButton(onClick={scope.launch {page(false)}}) {Text("Load more")}
                }
            }
        }
    }},confirmButton={TextButton(onClick=onDismiss) {Text("Done")}})
}
