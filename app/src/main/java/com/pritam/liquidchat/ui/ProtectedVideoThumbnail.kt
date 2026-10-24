package com.pritam.liquidchat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.pritam.liquidchat.data.Backend
import com.pritam.liquidchat.data.imageUrl
import kotlinx.coroutines.CancellationException

/**
 * Authorized, memory-only preview of a private video. No permanent public URL,
 * persisted thumbnail, or cross-account bitmap reuse. Coil performs frame
 * extraction asynchronously. Failure leaves a playable video card intact.
 */
@Composable internal fun ProtectedVideoThumbnail(
    backend:Backend,media:String,description:String,modifier:Modifier=Modifier
) {
    val context=LocalContext.current
    val session by backend.session.collectAsStateWithLifecycle()
    val epoch by backend.uiReads.sessionEpoch.collectAsStateWithLifecycle()
    val revision by backend.uiReads.revision.collectAsStateWithLifecycle()
    val owner=session?.userId
    val version=remember(owner,media,revision) {
        backend.uiReads.version(owner,"signed-image:$media")
    }
    var signed by remember(owner,epoch,media,version) {mutableStateOf<String?>(null)}
    var failed by remember(owner,epoch,media,version) {mutableStateOf(false)}
    var decoded by remember(owner,epoch,media,version) {mutableStateOf(false)}
    LaunchedEffect(owner,epoch,media,version) {
        if(owner.isNullOrBlank()||media.isBlank()||media=="null" ||
            backend.uiReads.epoch(owner)!=epoch) return@LaunchedEffect
        try {
            val url=backend.imageUrl(owner,media)
            if(backend.session.value?.userId==owner&&
                backend.uiReads.epoch(owner)==epoch) signed=url
        } catch(e:CancellationException) {throw e}
          catch(_:Exception) {
            if(backend.uiReads.epoch(owner)==epoch)failed=true
        }
    }
    val request=remember(signed,owner,epoch,media,version) {
        ImageRequest.Builder(context)
            .data(signed)
            .decoderFactory(VideoFrameDecoder.Factory())
            .memoryCacheKey("private-video-poster:$owner:$epoch:$version:$media")
            .diskCachePolicy(CachePolicy.DISABLED)
            .crossfade(false)
            .build()
    }
    Box(modifier.background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment=Alignment.Center) {
        if(signed!=null && !failed) {
            AsyncImage(model=request,contentDescription=description,
                modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop,
                onSuccess={decoded=true},
                onError={failed=true})
        }
        if(!decoded&&!failed) CircularProgressIndicator(Modifier.size(24.dp),
            strokeWidth=2.dp,color=MaterialTheme.colorScheme.onPrimaryContainer)
        // Video is still user-playable if thumbnail extraction is unsupported.
        if(failed) Icon(Icons.Outlined.PlayCircleOutline,
            contentDescription="Play video",modifier=Modifier.size(36.dp),
            tint=MaterialTheme.colorScheme.onPrimaryContainer)
    }
}
