package com.pritam.liquidchat.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pritam.liquidchat.data.Images
import kotlin.math.max
import kotlin.math.min

/** The selection is relative to the *rendered image*, never the letterbox viewport. */
internal fun normalizedCropBounds(
    start:Offset,end:Offset,width:Float,height:Float
):Images.CropBounds? {
    if(width<=0f||height<=0f)return null
    val rect=Images.CropBounds(
        (min(start.x,end.x)/width).coerceIn(0f,1f),
        (min(start.y,end.y)/height).coerceIn(0f,1f),
        (max(start.x,end.x)/width).coerceIn(0f,1f),
        (max(start.y,end.y)/height).coerceIn(0f,1f)
    )
    return rect.takeIf {it.valid()}
}

/**
 * Genuine local drag-to-crop editor. Each drag maps to intrinsic image fractions;
 * no image bytes or crop events are sent to a remote service.
 */
@Composable internal fun MbGestureCropEditor(
    image:Images.Prepared,enabled:Boolean,onSelection:(Images.CropBounds?)->Unit
) {
    var origin by remember(image.file.absolutePath) {mutableStateOf<Offset?>(null)}
    var endpoint by remember(image.file.absolutePath) {mutableStateOf<Offset?>(null)}
    val accent=MaterialTheme.colorScheme.primary
    BoxWithConstraints(Modifier.fillMaxWidth().height(240.dp),
        contentAlignment=Alignment.Center) {
        val aspect=image.width.toFloat()/image.height.coerceAtLeast(1)
        val frameWidth=minOf(maxWidth,maxHeight*aspect)
        val frameHeight=frameWidth/aspect
        Box(Modifier.size(frameWidth,frameHeight)
            .clip(RoundedCornerShape(10.dp))) {
            AsyncImage(model=image.file,
                contentDescription="Local photo for drag to crop",
                modifier=Modifier.matchParentSize(),contentScale=ContentScale.FillBounds)
            Canvas(Modifier.matchParentSize().testTag("photo-freeform-crop")
                .pointerInput(image.file.absolutePath,enabled) {
                    if(enabled)detectDragGestures(
                        onDragStart={point->
                            origin=point
                            endpoint=point
                            onSelection(null)
                        },
                        onDrag={change,_->
                            change.consume()
                            endpoint=change.position
                        },
                        onDragEnd={
                            val a=origin
                            val b=endpoint
                            onSelection(if(a!=null&&b!=null)
                                normalizedCropBounds(a,b,size.width.toFloat(),size.height.toFloat())
                                else null)
                        },
                        onDragCancel={
                            origin=null
                            endpoint=null
                            onSelection(null)
                        }
                    )
                }) {
                val a=origin
                val b=endpoint
                if(a!=null&&b!=null) {
                    val left=min(a.x,b.x).coerceIn(0f,size.width)
                    val top=min(a.y,b.y).coerceIn(0f,size.height)
                    val right=max(a.x,b.x).coerceIn(0f,size.width)
                    val bottom=max(a.y,b.y).coerceIn(0f,size.height)
                    if(right>left&&bottom>top) {
                        drawRect(Color.Black.copy(alpha=.15f))
                        drawRect(Color.White.copy(alpha=.12f),
                            topLeft=Offset(left,top),size=Size(right-left,bottom-top))
                        drawRect(accent,
                            topLeft=Offset(left,top),size=Size(right-left,bottom-top),
                            style=Stroke(width=2.dp.toPx()))
                    }
                }
            }
        }
    }
}
