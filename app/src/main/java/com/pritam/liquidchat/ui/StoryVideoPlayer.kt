package com.pritam.liquidchat.ui

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.pritam.liquidchat.data.Backend
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.json.JSONObject

/** Private signed URL fetched for each viewing session. No public or persistent URL cache. */
@OptIn(UnstableApi::class)
@Composable fun StoryVideoPlayer(
    backend:Backend,
    media:String,
    paused:Boolean,
    muted:Boolean,
    onProgress:(Float)->Unit,
    onFinished:()->Unit,
    modifier:Modifier=Modifier,
    poster:(@Composable ()->Unit)?=null
) {
    val context=LocalContext.current
    val account by backend.session.collectAsStateWithLifecycle()
    // User ID alone is insufficient: policy/authorization may rotate while
    // this account remains signed in. A new scope must discard player and URL.
    val sessionEpoch by backend.uiReads.sessionEpoch.collectAsStateWithLifecycle()
    val authorizedEpoch=backend.uiReads.epoch(account?.userId)
    var url by remember(media,account?.userId,sessionEpoch) {
        mutableStateOf<String?>(null)
    }
    var failure by remember(media,account?.userId,sessionEpoch) {
        mutableStateOf(false)
    }
    var retry by remember(media,account?.userId,sessionEpoch) {
        mutableIntStateOf(0)
    }
    LaunchedEffect(media,account?.userId,sessionEpoch,retry) {
        url=null
        failure=false
        val owner=account?.userId
        if(owner==null || authorizedEpoch==null || sessionEpoch!=authorizedEpoch) return@LaunchedEffect
        try {
            val signed=JSONObject(backend.request("/functions/v1/media","POST",
                JSONObject().put("action","download").put("id",media))).getString("url")
            if(backend.session.value?.userId==owner &&
                backend.uiReads.epoch(owner)==authorizedEpoch)url=signed
        } catch(e:CancellationException) {throw e}
        catch(_:Exception) {
            if(backend.uiReads.epoch(owner)==authorizedEpoch) {
                failure=true
                url=null
            }
        }
    }
    val currentOnProgress by rememberUpdatedState(onProgress)
    val currentOnFinished by rememberUpdatedState(onFinished)
    Box(modifier,contentAlignment=Alignment.Center) {
        val source=url
        if(source!=null) {
            key(media,source) {
                val player=remember(context,source) {
                    ExoPlayer.Builder(context).build().apply {
                        setMediaItem(MediaItem.fromUri(source))
                        volume=0f // New video stories start muted; user may unmute explicitly.
                        prepare()
                    }
                }
                var playbackError by remember(media,source) {mutableStateOf(false)}
                // A resolved signed URL does not mean ExoPlayer has decoded its
                // first frame. Keep the loading affordance through buffering.
                var videoReady by remember(media,source) {
                    mutableStateOf(player.playbackState==Player.STATE_READY)
                }
                var buffering by remember(media,source) {
                    mutableStateOf(player.playbackState!=Player.STATE_READY)
                }
                DisposableEffect(player) {
                    val observer=object:Player.Listener {
                        override fun onPlayerError(error:PlaybackException) {
                            playbackError=true
                            buffering=false
                        }
                        override fun onPlaybackStateChanged(state:Int) {
                            when(state) {
                                Player.STATE_READY -> {videoReady=true;buffering=false}
                                Player.STATE_BUFFERING,Player.STATE_IDLE -> buffering=true
                                Player.STATE_ENDED -> buffering=false
                            }
                        }
                    }
                    player.addListener(observer)
                    onDispose {
                        player.removeListener(observer)
                        player.stop()
                        player.release()
                    }
                }
                LaunchedEffect(player,paused,muted) {
                    player.volume=if(muted)0f else 1f
                    player.playWhenReady=!paused
                }
                LaunchedEffect(player) {
                    var reportedEnd=false
                    while(!playbackError) {
                        if(player.playbackState==Player.STATE_ENDED) {
                            if(!reportedEnd) {reportedEnd=true;currentOnProgress(1f);currentOnFinished()}
                            break
                        }
                        val duration=player.duration
                        if(duration>0L) {
                            currentOnProgress((player.currentPosition.toFloat()/duration).coerceIn(0f,1f))
                        }
                        delay(120)
                    }
                }
                AndroidView(
                    factory={ctx->PlayerView(ctx).apply {useController=false;this.player=player;setShutterBackgroundColor(android.graphics.Color.BLACK)}},
                    update={it.player=player},
                    modifier=Modifier.fillMaxSize()
                )
                // Show the cached first frame until playback has prepared.
                // Buffering after the first frame keeps the rendered frame.
                if(!videoReady&&!playbackError) poster?.invoke()
                if(playbackError) {
                    Column(horizontalAlignment=Alignment.CenterHorizontally) {
                        Text("Unable to play this private video",color=Color.White)
                        TextButton(onClick={url=null;retry++}) {Text("Retry playback")}
                    }
                } else if(buffering || !videoReady) {
                    Column(horizontalAlignment=Alignment.CenterHorizontally,
                        verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(color=Color.White)
                        Text("Loading video…",color=Color.White,
                            style=MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else if(failure) {
            Column(horizontalAlignment=Alignment.CenterHorizontally) {
                Text("Video unavailable",color=Color.White)
                TextButton(onClick={retry++}) {Text("Retry video")}
            }
        } else CircularProgressIndicator()
    }
}
