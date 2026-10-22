package com.pritam.liquidchat.ui

import androidx.compose.ui.platform.LocalContext
import com.pritam.liquidchat.data.SavedMedia
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.pritam.liquidchat.AppViewModel
import com.pritam.liquidchat.data.Chat
import com.pritam.liquidchat.data.Message
import kotlinx.coroutines.launch

@Composable internal fun SharedImagePreview(model:AppViewModel,message:Message,thumbnail:Boolean=false) {
    var open by remember(message.id) {mutableStateOf(false)}
    ProtectedImage(model.backend,message.mediaId,message.localImage,if(message.mediaKind=="sticker")"Shared sticker" else "Shared photo",(if(message.mediaKind=="sticker")Modifier.width(conversationStickerSize(LocalDisplayOptions.current).dp) else Modifier.fillMaxWidth()).aspectRatio(if(thumbnail)1f else (if(message.imageHeight>0&&message.imageWidth>0)message.imageWidth.toFloat()/message.imageHeight else 1f).coerceIn(.5f,2f)).clip(Glass.compact).clickable {open=true},scale=if(thumbnail)androidx.compose.ui.layout.ContentScale.Crop else androidx.compose.ui.layout.ContentScale.Fit)
    if(open) androidx.compose.ui.window.Dialog(onDismissRequest={open=false},properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
            Column(Modifier.statusBarsPadding().navigationBarsPadding()) {
                TextButton(onClick={open=false}) {Text("Close image")}
                var zoom by remember(message.id) {mutableFloatStateOf(1f)}
                var pan by remember(message.id) {mutableStateOf(Offset.Zero)}
                var size by remember {mutableStateOf(IntSize.Zero)}
                val transform=rememberTransformableState {scale,move,_->
                    zoom=(zoom*scale).coerceIn(1f,4f)
                    val limitX=size.width*(zoom-1)/2;val limitY=size.height*(zoom-1)/2
                    pan=Offset((pan.x+move.x).coerceIn(-limitX,limitX),(pan.y+move.y).coerceIn(-limitY,limitY))
                }
                Box(Modifier.weight(1f).fillMaxWidth().clip(androidx.compose.ui.graphics.RectangleShape).onSizeChanged {size=it}.transformable(transform)) {
                    ProtectedImage(model.backend,message.mediaId,message.localImage,if(message.mediaKind=="sticker")"Full-size shared sticker" else "Full-size shared photo",Modifier.fillMaxSize().graphicsLayer {scaleX=zoom;scaleY=zoom;translationX=pan.x;translationY=pan.y})
                }
                val context=LocalContext.current
                val scope=rememberCoroutineScope()
                var saving by remember(message.id) {mutableStateOf(false)}
                var saveFeedback by remember(message.id) {mutableStateOf<String?>(null)}
                Row(Modifier.fillMaxWidth(),
                    verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.SpaceEvenly) {
                    TextButton(onClick={zoom=1f;pan=Offset.Zero}) {Text("Reset zoom")}
                    if(message.status=="SENT" && message.deletedAt.isEmpty() &&
                        message.mediaId.isNotEmpty() && message.mediaKind=="image") {
                        TextButton(onClick={
                            saving=true
                            saveFeedback=null
                            scope.launch {
                                try {
                                    SavedMedia.savePhoto(context,model.repository,message)
                                    saveFeedback="Photo saved to Pictures/Liquid Chat"
                                } catch(e:kotlinx.coroutines.CancellationException) {throw e}
                                  catch(e:Exception) {
                                    saveFeedback=e.message ?: "Could not save photo."
                                } finally {saving=false}
                            }
                        },enabled=!saving) {
                            Text(if(saving)"Saving…" else "Save photo")
                        }
                    }
                }
                saveFeedback?.let {Text(it,
                    modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp),
                    style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)}

            }
        }
    }
}
