package com.pritam.liquidchat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import coil.Coil
import coil.compose.AsyncImage
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.pritam.liquidchat.data.Backend
import com.pritam.liquidchat.data.imageUrl
import com.pritam.liquidchat.data.imageUrlRefreshDelay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable fun ProtectedImage(
    backend:Backend,id:String,local:String="",description:String,
    modifier:Modifier=Modifier,scale:ContentScale=ContentScale.Fit,
    onLoadState:(Boolean)->Unit={}
) {
    val currentLoadState by rememberUpdatedState(onLoadState)
    val session by backend.session.collectAsStateWithLifecycle()
    val revision by backend.uiReads.revision.collectAsStateWithLifecycle()
    val sessionEpoch by backend.uiReads.sessionEpoch.collectAsStateWithLifecycle()
    val account=session?.userId
    val context=LocalContext.current
    val lifecycle=LocalLifecycleOwner.current.lifecycle
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
    LaunchedEffect(memoryKey,retry,lifecycle) {
        // Returning to a tab with a decoded Coil bitmap must not pulse loading
        // or hide an already-visible avatar while renewing its signed URL.
        // Keep the explicit failure path below (and onError) fail-closed.
        if(!cached)currentLoadState(false)
        val expectedEpoch=sessionEpoch
        if(local.isNotEmpty()) {url=local;return@LaunchedEffect}
        if(account.isNullOrBlank()||version==null||id.isBlank()||id=="null")return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while(isActive) {
                try {
                    // Cache hits require no network. Revalidate access at expiry while
                    // keeping the decoded bitmap stable across successful renewals.
                    val signed=backend.imageUrl(account,id,force=retry>0)
                    if(backend.uiReads.epoch(account)==expectedEpoch) {url=signed;failed=false}
                } catch(e:CancellationException) {throw e}
                catch(_:Exception) {url=null;failed=true;currentLoadState(false);return@repeatOnLifecycle}
                delay(backend.imageUrlRefreshDelay(account,id))
            }
        }
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
            onSuccess={failed=false;currentLoadState(true)},
            onError={
                currentLoadState(false)
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
