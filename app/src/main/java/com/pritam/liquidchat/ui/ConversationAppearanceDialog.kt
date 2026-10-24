package com.pritam.liquidchat.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Presentation only: the parent owns authenticated persistence and picker launch. */
@Composable internal fun ConversationAppearanceDialog(
    cid:String,account:String?,palette:String,chatAccent:String,wallpaperRevision:Int,
    onDismiss:()->Unit,onAccentSelected:(String)->Unit,onPaletteSelected:(String)->Unit,
    onPickPhoto:()->Unit,onPickVideo:()->Unit
) {
    val context=LocalContext.current
    LiquidAlertDialog(
        onDismissRequest=onDismiss,
        title={Text("Chat appearance")},
        text={Column(Modifier.fillMaxWidth().heightIn(max=430.dp).verticalScroll(rememberScrollState()),
            verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Choose a built-in palette, private photo or muted MP4 video from this phone.",
                style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Chat accent · header actions and send button",
                style=MaterialTheme.typography.titleSmall)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                MbConversationAccent.choices.forEach {choice->
                    FilterChip(selected=chatAccent==choice,
                        onClick={
                            onAccentSelected(choice)
                        },
                        label={Text(choice)})
                }
            }
            Text("Stored only on this device and isolated by account. Messages and calls are unchanged.",
                style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
            listOf("Ice","Lilac","Mint","Sand").forEach {choice->
                TextButton(onClick={onPaletteSelected(choice)}) {Text(if(choice==palette)"✓ $choice" else choice)}
            }
            FilledTonalButton(
                onClick=onPickPhoto,
                modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {
                Icon(Icons.Outlined.AddPhotoAlternate,null)
                Spacer(Modifier.width(8.dp))
                Text("Choose photo wallpaper")
            }
            FilledTonalButton(
                onClick=onPickVideo,
                modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {
                Icon(Icons.Outlined.Videocam,null)
                Spacer(Modifier.width(8.dp))
                Text("Choose MP4 video wallpaper")
            }
            val owner=account
            if(owner!=null && remember(owner,cid,wallpaperRevision) {
                runCatching {com.pritam.liquidchat.data.PersonalWallpaper.has(context,owner,cid)}
                    .getOrDefault(false)
            }) {
                TextButton(onClick={onPaletteSelected("Photo")}) {Text(if(palette=="Photo")"✓ My photo" else "My saved photo")}
            }
            if(owner!=null && remember(owner,cid,wallpaperRevision) {
                runCatching {com.pritam.liquidchat.data.PrivateVideoWallpaper.has(context,owner,cid)}
                    .getOrDefault(false)
            }) {
                TextButton(onClick={onPaletteSelected("Video")}) {Text(if(palette=="Video")"✓ My video" else "My saved video")}
            }
            Text("Photos and videos stay private on this phone. MP4 is limited to 30s / 24 MB, up to 1080p; playback loops silently and pauses in background or reduced motion. Files are erased on logout and chat deletion.",
                style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
        }},
        confirmButton={TextButton(onClick=onDismiss) {Text("Done")}}
    )

}
