package com.pritam.liquidchat.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pritam.liquidchat.AppViewModel
import com.pritam.liquidchat.data.Images
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

@Immutable data class Moment(val id:String,val owner:String,val media:String,val name:String,val caption:String,val created:String,val expires:String?,val width:Int,val height:Int)
internal fun parseMoments(array:JSONArray):List<Moment> =(0 until array.length()).map {index->val j=array.getJSONObject(index);Moment(j.getString("id"),j.getString("owner_id"),j.getString("media_id"),j.getString("display_name"),j.getString("caption"),j.getString("created_at"),if(j.isNull("expires_at"))null else j.getString("expires_at"),j.getInt("width"),j.getInt("height"))}

/** No public feed: audiences are the creator's saved contacts, rechecked on every media request. */
@Composable fun MomentsPanel(model:AppViewModel,kind:String,mine:Boolean,archived:Boolean=false,modifier:Modifier=Modifier) {
    val account by model.session.collectAsStateWithLifecycle()
    val foreground by model.repository.foreground.collectAsStateWithLifecycle()
    val scope=rememberCoroutineScope()
    var items by remember(kind,mine,archived,account?.userId){mutableStateOf(emptyList<Moment>())}
    var loading by remember {mutableStateOf(false)}
    var error by remember {mutableStateOf<String?>(null)}
    var more by remember(kind,mine,archived) {mutableStateOf(false)}
    var epoch by remember {mutableIntStateOf(0)}
    var editor by remember {mutableStateOf(false)}
    var focused by remember {mutableStateOf<Moment?>(null)}
    var deleting by remember {mutableStateOf<Moment?>(null)}
    suspend fun load(append:Boolean=false) {
        if(loading)return
        loading=true;error=null
        val expected=account?.userId
        try {
            val body=JSONObject().put("p_kind",kind).put("p_mine",mine).put("p_archived",archived)
            if(append)items.lastOrNull()?.let {body.put("p_before",it.created).put("p_before_id",it.id)}
            val page=parseMoments(JSONArray(model.backend.rpc("list_moments",body)))
            if(model.session.value?.userId==expected){items=if(append)(items+page).distinctBy {it.id} else page;more=page.size==30}
        } catch(e:CancellationException){throw e}catch(_:Exception){error="Could not load ${if(kind=="story")"stories" else "posts"}. Check your connection and retry."}finally {loading=false}
    }
    LaunchedEffect(kind,mine,archived,account?.userId,epoch) {if(account!=null){while(loading)kotlinx.coroutines.delay(50);load()}}
    // Expiry is enforced server-side. Remove an expired tile while this screen stays open too.
    LaunchedEffect(kind,foreground) {if(foreground&&kind=="story")while(true){kotlinx.coroutines.delay(1000);val now=java.time.Instant.now();items=items.filter {it.expires==null||runCatching {java.time.Instant.parse(it.expires).isAfter(now)}.getOrDefault(false)};focused?.let {if(items.none {m->m.id==it.id})focused=null}}}
    Column(modifier,verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(if(kind=="story")"Stories · 24 hours" else if(archived)"Archived posts" else "Posts",Modifier.weight(1f),fontWeight=FontWeight.Bold)
            IconButton(onClick={scope.launch {load()}},enabled=!loading){Icon(Icons.Outlined.Refresh,"Refresh ${if(kind=="story")"stories" else "posts"}")}
            if(mine&&!archived)IconButton(onClick={editor=true},enabled=account!=null){Icon(Icons.Outlined.Add,if(kind=="story")"Add a story" else "Add a post")}
        }
        if(loading&&items.isEmpty())LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let {Text(it,color=MaterialTheme.colorScheme.error);TextButton(onClick={scope.launch {load()}}){Text("Retry")}}
        LazyColumn(modifier=Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=16.dp)) {
            if(items.isEmpty()&&!loading&&error==null)item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(if(kind=="story")"No stories yet" else if(archived)"No archived posts" else "No posts yet...",style=MaterialTheme.typography.titleLarge)
                    Text(if(mine)"Share a photo with people in your saved contacts." else "Stories and posts appear when their creators save you as a contact.")
                    if(mine&&!archived)TextButton(onClick={editor=true}){Text(if(kind=="story")"Add a story" else "Add a post")}
                }
            }
            items(items,key={it.id}) {moment->
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(moment.name,fontWeight=FontWeight.Bold)
                    Text(remember(moment.created){runCatching{java.time.Instant.parse(moment.created).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("dd MMM · HH:mm"))}.getOrDefault("")},style=MaterialTheme.typography.labelSmall)
                    ProtectedImage(model.backend,moment.media,description="${moment.name}'s ${if(kind=="story")"story" else "post"}",modifier=Modifier.fillMaxWidth().heightIn(max=320.dp).aspectRatio((moment.width.toFloat()/moment.height.coerceAtLeast(1)).coerceIn(.6f,1.8f)).clickable {focused=moment},scale=androidx.compose.ui.layout.ContentScale.Crop)
                    if(moment.caption.isNotBlank())Text(moment.caption)
                    if(moment.owner==account?.userId)Row {
                        if(kind=="post")TextButton(enabled=!loading,onClick={scope.launch {
                            loading=true;try {model.backend.rpc("change_moment",JSONObject().put("p_id",moment.id).put("p_action",if(archived)"restore" else "archive"));epoch++}catch(e:CancellationException){throw e}catch(_:Exception){error="Could not update the post. Retry."}finally{loading=false}
                        }}){Text(if(archived)"Restore" else "Archive")}
                        TextButton(onClick={deleting=moment},enabled=!loading){Text("Delete")}
                    }
                }
            }
            if(more)item {TextButton(onClick={scope.launch {load(true)}},enabled=!loading){Text("Load older")}}
        }
    }
    if(editor)MomentEditor(model,kind,onDismiss={editor=false},onPublished={editor=false;epoch++})
    focused?.let {moment->androidx.compose.ui.window.Dialog(onDismissRequest={focused=null},properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize()) {Column(Modifier.systemBarsPadding()){TextButton(onClick={focused=null}){Text("Close")};ProtectedImage(model.backend,moment.media,description="Full-size moment",modifier=Modifier.weight(1f).fillMaxWidth());Text(moment.caption,Modifier.padding(16.dp))}}
    }}
    deleting?.let {moment->AlertDialog(onDismissRequest={deleting=null},title={Text("Delete this ${if(kind=="story")"story" else "post"}?")},text={Text("It will disappear for you and your contacts.")},confirmButton={TextButton(onClick={deleting=null;scope.launch {loading=true;try {model.backend.rpc("change_moment",JSONObject().put("p_id",moment.id).put("p_action","delete"));epoch++}catch(e:CancellationException){throw e}catch(_:Exception){error="Delete failed. Please retry."}finally{loading=false}}}){Text("Delete")}},dismissButton={TextButton(onClick={deleting=null}){Text("Cancel")}})}
}

@Composable fun MomentsDialog(model:AppViewModel,mine:Boolean,onDismiss:()->Unit) {
    var tab by rememberSaveable {mutableIntStateOf(0)}
    androidx.compose.ui.window.Dialog(onDismissRequest=onDismiss,properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().systemBarsPadding().padding(12.dp)) {
                Row {TextButton(onClick=onDismiss){Text("Back")};Text(if(mine)"My stories" else "Contacts",style=MaterialTheme.typography.titleLarge,modifier=Modifier.padding(12.dp))}
                Row {listOf("Stories","Posts").forEachIndexed {i,label->FilterChip(tab==i,{tab=i},label={Text(label)})}}
                MomentsPanel(model,if(tab==0)"story" else "post",mine,modifier=Modifier.weight(1f))
            }
        }
    }
}

@Composable private fun MomentEditor(model:AppViewModel,kind:String,onDismiss:()->Unit,onPublished:()->Unit) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    val session by model.session.collectAsStateWithLifecycle()
    val owner=session?.userId.orEmpty()
    val id=remember {UUID.randomUUID().toString()}
    var photo by remember {mutableStateOf<Images.Prepared?>(null)}
    var caption by rememberSaveable {mutableStateOf("")}
    var media by remember {mutableStateOf<String?>(null)}
    var busy by remember {mutableStateOf(false)}
    var error by remember {mutableStateOf<String?>(null)}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {uri->if(uri!=null){busy=true;scope.launch {try {val prepared=Images.prepare(context,uri,owner);if(model.session.value?.userId!=owner){prepared.file.delete();throw CancellationException("Account changed")};photo?.file?.delete();photo=prepared}catch(e:CancellationException){throw e}catch(_:Exception){error="Could not open that photo. Choose an image under 10 MB."}finally{busy=false}}}}
    DisposableEffect(Unit) {onDispose{photo?.file?.delete()}}
    AlertDialog(onDismissRequest={if(!busy)onDismiss()},title={Text(if(kind=="story")"New story" else "New post")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("Visible to people in your saved contacts. Blocked contacts cannot view it.")
        if(kind=="story")Text("Disappears after 24 hours.")
        photo?.let {coil.compose.AsyncImage(it.file,"Photo to publish",Modifier.fillMaxWidth().heightIn(max=220.dp).aspectRatio(it.width.toFloat()/it.height))}
        if(media==null)TextButton(onClick={picker.launch("image/*")},enabled=!busy){Text(if(photo==null)"Choose photo" else "Change photo")}
        OutlinedTextField(caption,{caption=it.take(1000)},label={Text("Caption")},enabled=!busy&&media==null,modifier=Modifier.fillMaxWidth(),maxLines=4)
        if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
    }},confirmButton={TextButton(enabled=photo!=null&&!busy,onClick={
        val image=photo ?: return@TextButton;busy=true;error=null
        scope.launch {
            try {
                if(model.session.value?.userId!=owner)throw CancellationException("Account changed")
                if(media!=null) {
                    try {model.backend.rpc("image_access",JSONObject().put("p_id",media))}
                    catch(e:com.pritam.liquidchat.data.ApiException){if(e.status==403||e.code=="42501")media=null else throw e}
                }
                if(media==null)media=model.backend.uploadImage(image.file,null).getString("id")
                model.backend.rpc("publish_moment",JSONObject().put("p_id",id).put("p_media",media).put("p_kind",kind).put("p_caption",caption.trim()))
                onPublished()
            }catch(e:CancellationException){throw e}catch(_:Exception){error="Could not publish. Your photo is still here; retry when connected."}finally{busy=false}
        }
    }){Text(if(error==null)"Publish" else "Retry")}},dismissButton={TextButton(onClick=onDismiss,enabled=!busy){Text("Cancel")}})
}
