package com.pritam.liquidchat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.Coil
import coil.compose.AsyncImage
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.pritam.liquidchat.data.Backend
import com.pritam.liquidchat.data.imageUrl
import kotlinx.coroutines.CancellationException

@Composable fun ProtectedImage(
    backend:Backend,id:String,local:String="",description:String,
    modifier:Modifier=Modifier,scale:ContentScale=ContentScale.Fit
) {
    val session by backend.session.collectAsStateWithLifecycle()
    val revision by backend.uiReads.revision.collectAsStateWithLifecycle()
    val sessionEpoch by backend.uiReads.sessionEpoch.collectAsStateWithLifecycle()
    val account=session?.userId
    val context=LocalContext.current
    val key="signed-image:$id"
    // Resource versions survive URL expiry, and change on auth or avatar invalidation.
    val version=remember(account,id,revision) {backend.uiReads.version(account,key)}
    val memoryKey="$account:$version:$id:$local"
    val imageLoader=Coil.imageLoader(context)
    val cached=remember(memoryKey) {imageLoader.memoryCache?.get(MemoryCache.Key(memoryKey))!=null}
    var url by remember(memoryKey) {mutableStateOf<String?>(local.takeIf {it.isNotEmpty()} ?: id.takeIf {cached})}
    var failed by remember(memoryKey) {mutableStateOf(false)}
    var retry by remember(memoryKey) {mutableIntStateOf(0)}
    var autoRetried by remember(memoryKey) {mutableStateOf(false)}
    LaunchedEffect(memoryKey,retry) {
        if(local.isNotEmpty()) {url=local;return@LaunchedEffect}
        if(account.isNullOrBlank()||version==null||id.isBlank()||id=="null")return@LaunchedEffect
        // A decoded bitmap needs no signing request on tab switches.
        if(cached&&retry==0) {url=id;return@LaunchedEffect}
        try {
            val signed=backend.imageUrl(account,id,force=retry>0)
            if(backend.uiReads.epoch(account)==sessionEpoch) {url=signed;failed=false}
        } catch(e:CancellationException) {throw e}
        catch(_:Exception) {failed=true}
    }
    val request=remember(url,memoryKey,retry) {
        ImageRequest.Builder(context).data(url).memoryCacheKey(memoryKey)
            .placeholderMemoryCacheKey(memoryKey).diskCachePolicy(CachePolicy.DISABLED)
            .crossfade(false).build()
    }
    Box(modifier.glassSource()) {
        if(url!=null) AsyncImage(
            model=request,imageLoader=imageLoader,contentDescription=description,
            modifier=Modifier.fillMaxSize(),contentScale=scale,
            onSuccess={failed=false},
            onError={
                // An evicted bitmap or expired link gets one automatic URL renewal.
                if(local.isEmpty()&&!autoRetried) {autoRetried=true;url=null;retry++}
                else failed=true
            }
        )
        if(failed) TextButton(onClick={url=null;failed=false;autoRetried=false;retry++}) {Text("Retry image")}
        else if(url==null&&account!=null&&version!=null&&id.isNotBlank()&&id!="null")
            CircularProgressIndicator(Modifier.size(24.dp))
    }
}
