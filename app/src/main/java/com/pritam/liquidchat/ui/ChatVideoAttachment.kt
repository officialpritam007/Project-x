package com.pritam.liquidchat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pritam.liquidchat.AppViewModel
import com.pritam.liquidchat.data.Message

/**
 * A video is fetched only after a user taps Play. Every playback session must
 * obtain a short-lived authorized URL from the existing protected media API.
 * StoryVideoPlayer owns and releases Media3 resources when it leaves Compose.
 */
@Composable
fun ChatVideoAttachment(model:AppViewModel,message:Message) {
    val foreground by model.repository.foreground.collectAsStateWithLifecycle()
    val activeCall by model.calls.view.collectAsStateWithLifecycle()
    var playing by remember(message.account,message.id,message.mediaId) {mutableStateOf(false)}
    // Calling takes priority over media audio/video, and leaving the foreground
    // disposes the video player rather than holding an active signed stream.
    LaunchedEffect(foreground,activeCall) {
        if(!foreground || activeCall!=null)playing=false
    }
    var muted by remember(message.account,message.id,message.mediaId) {mutableStateOf(true)}
    val active=message.status=="SENT" && message.mediaId.isNotEmpty() &&
        message.deletedAt.isEmpty()
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        if(playing&&active&&foreground&&activeCall==null) {
            StoryVideoPlayer(
                backend=model.backend,media=message.mediaId,
                paused=!foreground,muted=muted,onProgress={},
                onFinished={playing=false},
                modifier=Modifier.fillMaxWidth().height(220.dp))
        } else {
            Surface(color=MaterialTheme.colorScheme.surfaceVariant,
                shape=Glass.compact,modifier=Modifier.fillMaxWidth().height(130.dp)) {
                Box(contentAlignment=Alignment.Center) {
                    Column(horizontalAlignment=Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Videocam,"Private video",Modifier.size(38.dp))
                        Text(if(active)"Private video · up to 30 seconds"
                            else "Video queued for private delivery",
                            style=MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        Row(verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            TextButton(enabled=active&&foreground&&activeCall==null,
                onClick={playing=!playing}) {
                Icon(if(playing)Icons.Outlined.Stop else Icons.Outlined.PlayArrow,null,
                    Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(if(playing)"Stop" else "Play")
            }
            if(playing)TextButton(onClick={muted=!muted}) {
                Icon(if(muted)Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp,null,
                    Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(if(muted)"Unmute" else "Mute")
            }
        }
    }
}
