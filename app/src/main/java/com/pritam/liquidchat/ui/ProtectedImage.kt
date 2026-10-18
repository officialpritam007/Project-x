package com.pritam.liquidchat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.pritam.liquidchat.data.Backend
import kotlinx.coroutines.CancellationException
import org.json.JSONObject

@Composable fun ProtectedImage(
    backend:Backend,id:String,local:String="",description:String,
    modifier:Modifier=Modifier,scale:ContentScale=ContentScale.Fit
) {
    val session by backend.session.collectAsStateWithLifecycle()
    val account=session?.userId
    val context=LocalContext.current
    val key="signed-image:$id"
    // Keep the last image on-screen while its short-lived signed URL is refreshed.
    // It is never written to disk, and the cache is cleared on account changes.
    var url by remember(account,id,local) {
        mutableStateOf<String?>(local.ifEmpty {backend.uiReads.peek(account,key,allowStale=true)})
    }
    var failed by remember(account,id,local) {mutableStateOf(false)}
    var retry by remember(account,id,local) {mutableIntStateOf(0)}
    LaunchedEffect(account,id,local,retry) {
        if(local.isNotEmpty()) {url=local;failed=false;return@LaunchedEffect}
        if(account.isNullOrBlank()||id.isBlank()||id=="null") {url=null;return@LaunchedEffect}
        try {
            val signed=backend.uiReads.load(account,key,40_000L,force=retry>0) {
                JSONObject(backend.request("/functions/v1/media","POST",
                    JSONObject().put("action","download").put("id",id))).getString("url")
            }
            if(backend.session.value?.userId==account) {url=signed;failed=false}
        } catch(e:CancellationException) {throw e}
        catch(_:Exception) {if(url==null)failed=true}
    }
    Box(modifier.glassSource()) {
        if(url!=null) AsyncImage(
            model=ImageRequest.Builder(context).data(url)
                .memoryCacheKey("$account:$id:$local")
                .diskCachePolicy(CachePolicy.DISABLED).build(),
            contentDescription=description,modifier=Modifier.fillMaxSize(),
            contentScale=scale,
            onSuccess={failed=false},
            onError={failed=true}
        )
        if(failed) TextButton(onClick={
            backend.uiReads.invalidate(account,key)
            url=null;failed=false;retry++
        }) {Text("Retry image")}
        else if(url==null&&account!=null&&(id.isNotBlank()||local.isNotBlank()))
            CircularProgressIndicator(Modifier.size(24.dp))
    }
}
