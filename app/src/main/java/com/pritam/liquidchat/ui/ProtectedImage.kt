package com.pritam.liquidchat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.pritam.liquidchat.data.Backend
import kotlinx.coroutines.CancellationException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.json.JSONObject

@Composable fun ProtectedImage(backend:Backend,id:String,local:String="",description:String,modifier:Modifier=Modifier,scale:ContentScale=ContentScale.Fit) {
    val session by backend.session.collectAsStateWithLifecycle()
    val context=LocalContext.current
    var url by remember(session?.userId,id,local) {mutableStateOf<String?>(local.ifEmpty {null})}
    var failed by remember(session?.userId,id,local) {mutableStateOf(false)}
    var retry by remember(id) {mutableIntStateOf(0)}
    LaunchedEffect(session?.userId,id,local,retry) {
        failed=false
        if(local.isNotEmpty()) {url=local;return@LaunchedEffect}
        try {url=JSONObject(backend.request("/functions/v1/media","POST",JSONObject().put("action","download").put("id",id))).getString("url")}
        catch(e:CancellationException){throw e}catch(_:Exception){failed=true;url=null}
    }
    Box(modifier.glassSource()) {
        if(url!=null) AsyncImage(model=ImageRequest.Builder(context).data(url).memoryCacheKey("${session?.userId}:$id:$local").diskCachePolicy(CachePolicy.DISABLED).build(),contentDescription=description,modifier=Modifier.fillMaxSize(),contentScale=scale,onError={failed=true})
        if(failed) TextButton(onClick={url=null;retry++}) {Text("Retry image")}
        else if(url==null) CircularProgressIndicator(Modifier.size(24.dp))
    }
}
