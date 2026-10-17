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
import kotlinx.coroutines.launch

@Composable internal fun ContactMediaDialog(model:AppViewModel,chat:Chat,sourceVersion:List<Message>,initialKind:String="image",onDismiss:()->Unit) {
    var kind by remember {mutableStateOf(initialKind)}
    var rows by remember {mutableStateOf(emptyList<Message>())}
    var more by remember {mutableStateOf(true)}
    var loading by remember {mutableStateOf(false)}
    var failed by remember {mutableStateOf(false)}
    val scope=rememberCoroutineScope()
    suspend fun page(reset:Boolean) {
        val requestedKind=kind
        loading=true;failed=false
        try {
            val before=if(reset)Long.MAX_VALUE else rows.lastOrNull()?.seq ?: Long.MAX_VALUE
            val page=if(kind=="star")model.repository.starred(chat.id,before) else model.repository.sharedMedia(chat.id,before,kind)
            if(kind!=requestedKind)return
            rows=if(reset)page else (rows+page).distinctBy {it.id};more=page.size==30
        } catch(e:kotlinx.coroutines.CancellationException){throw e} catch(_:Exception){if(kind==requestedKind)failed=true}
        finally {if(kind==requestedKind)loading=false}
    }
    LaunchedEffect(kind,sourceVersion) {rows=emptyList();more=true;page(true)}
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
