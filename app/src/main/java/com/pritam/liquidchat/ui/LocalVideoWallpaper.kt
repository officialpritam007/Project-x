package com.pritam.liquidchat.ui

import android.net.Uri
import android.view.LayoutInflater
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.pritam.liquidchat.R
import java.io.File

/** Muted local wallpaper; foreground/lifecycle/reduced-motion guards avoid background playback. */
@Composable
internal fun LocalVideoWallpaper(file:File,foreground:Boolean,revision:Int,modifier:Modifier=Modifier) {
    val context=LocalContext.current
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    val currentState by lifecycle.currentStateFlow.collectAsState()
    val reducedMotion=LocalDisplayOptions.current.reducedMotion
    val player=remember(context,file.absolutePath) {
        ExoPlayer.Builder(context).build().apply {
            volume=0f
            repeatMode=Player.REPEAT_MODE_ONE
            setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            prepare()
        }
    }
    DisposableEffect(player) {
        onDispose {
            player.playWhenReady=false
            player.stop()
            player.release()
        }
    }
    LaunchedEffect(player,foreground,currentState,reducedMotion) {
        player.playWhenReady=foreground && currentState.isAtLeast(Lifecycle.State.STARTED) && !reducedMotion
    }
    AndroidView(
        factory={ctx->
            (LayoutInflater.from(ctx).inflate(R.layout.liquid_video_wallpaper,null,false) as PlayerView).apply {
                useController=false
                this.player=player
            }
        },
        update={view->if(view.player!==player)view.player=player},
        modifier=modifier
    )
}
