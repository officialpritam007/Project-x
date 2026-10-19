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
    val epoch=model.backend.uiReads.epoch(account)
    var kind by remember(chat.id,account) {mutableStateOf(initialKind)}
    var rows by remember(chat.id,account,epoch) {mutableStateOf(emptyList<Message>())}
    var more by remember(chat.id,account,epoch) {mutableStateOf(true)}
    var loading by remember(chat.id,account,epoch) {mutableStateOf(false)}
    var failed by remember(chat.id,account,epoch) {mutableStateOf(false)}
    val refreshKeys=remember(kind,sourceVersion) {sharedMediaRefreshKeys(sourceVersion,kind)}
    val scope=rememberCoroutineScope()
    suspend fun page(reset:Boolean) {
        val requestedKind=kind
        val requestedAccount=account
        val requestedEpoch=epoch
        if(requestedAccount==null || chat.account!=requestedAccount || requestedEpoch==null) {
            rows=emptyList();more=false;loading=false;return
        }
        fun current()=kind==requestedKind &&
            model.backend.session.value?.userId==requestedAccount &&
            model.backend.uiReads.epoch(requestedAccount)==requestedEpoch
        loading=true;failed=false
        try {
            val before=if(reset)Long.MAX_VALUE else rows.lastOrNull()?.seq ?: Long.MAX_VALUE
            val fetched=if(requestedKind=="star")model.repository.starred(chat.id,before)
                else model.repository.sharedMedia(chat.id,before,requestedKind)
            if(!current())return
            val visible=fetched.filter {it.deletedAt.isEmpty()}
            rows=if(reset)visible else (rows+visible).distinctBy {it.id}
            more=fetched.size==30
        } catch(e:kotlinx.coroutines.CancellationException){throw e}
          catch(_:Exception){if(current())failed=true}
        finally {if(current())loading=false}
    }
    LaunchedEffect(chat.id,account,epoch,kind,refreshKeys) {
        rows=emptyList();more=true;page(true)
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
