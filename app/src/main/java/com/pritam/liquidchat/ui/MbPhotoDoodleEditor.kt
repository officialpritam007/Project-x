package com.pritam.liquidchat.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pritam.liquidchat.data.Images
import kotlin.math.min

internal fun normalizedDrawingPoint(offset:Offset,size:IntSize):Images.DrawPoint? =
    if(size.width<=0||size.height<=0||!offset.x.isFinite()||!offset.y.isFinite())null
    else Images.DrawPoint(
        (offset.x/size.width).coerceIn(0f,1f),
        (offset.y/size.height).coerceIn(0f,1f))

/** Freehand stroke preview maps to the image's exact intrinsic aspect ratio.
 * The caller owns all actual JPEG mutations and Undo/Redo history.
 */
@Composable internal fun MbPhotoDoodleEditor(
    image:Images.Prepared,strokes:List<Images.DrawStroke>,
    color:Int,enabled:Boolean,onStroke:(Images.DrawStroke)->Unit
) {
    var current by remember(image.file.absolutePath) {
        mutableStateOf<List<Images.DrawPoint>>(emptyList())
    }
    BoxWithConstraints(Modifier.fillMaxWidth().height(240.dp),
        contentAlignment=Alignment.Center) {
        val aspect=image.width.toFloat()/image.height.coerceAtLeast(1)
        val frameWidth=minOf(maxWidth,maxHeight*aspect)
        val frameHeight=frameWidth/aspect
        Box(Modifier.size(frameWidth,frameHeight).clip(RoundedCornerShape(10.dp))) {
            AsyncImage(image.file,contentDescription="Private photo drawing preview",
                modifier=Modifier.matchParentSize(),contentScale=ContentScale.FillBounds)
            Canvas(Modifier.matchParentSize().testTag("photo-doodle")
                .pointerInput(image.file.absolutePath,enabled,color) {
                    if(enabled)detectDragGestures(
                        onDragStart={start->
                            current=normalizedDrawingPoint(start,size)?.let {listOf(it)}
                                ?: emptyList()
                        },
                        onDrag={change,_->
                            change.consume()
                            if(current.isNotEmpty()&&current.size<500) {
                                normalizedDrawingPoint(change.position,size)?.let {
                                    current=current+it
                                }
                            }
                        },
                        onDragEnd={
                            if(current.isNotEmpty()) {
                                onStroke(Images.DrawStroke(current,color))
                            }
                            current=emptyList()
                        },
                        onDragCancel={current=emptyList()}
                    )
                }) {
                val drawn=strokes + if(current.isNotEmpty())
                    listOf(Images.DrawStroke(current,color)) else emptyList()
                drawn.forEach {stroke->
                    val paint=Color(stroke.argb)
                    val width=(stroke.widthFraction*min(size.width,size.height))
                        .coerceAtLeast(2.dp.toPx())
                    if(stroke.points.size==1) {
                        val point=stroke.points[0]
                        drawCircle(paint,radius=width/2f,
                            center=Offset(point.x*size.width,point.y*size.height))
                    } else if(stroke.points.isNotEmpty()) {
                        val path=Path().apply {
                            val first=stroke.points.first()
                            moveTo(first.x*size.width,first.y*size.height)
                            stroke.points.drop(1).forEach {point->
                                lineTo(point.x*size.width,point.y*size.height)
                            }
                        }
                        drawPath(path,paint,style=Stroke(width=width,
                            cap=androidx.compose.ui.graphics.StrokeCap.Round,
                            join=androidx.compose.ui.graphics.StrokeJoin.Round))
                    }
                }
            }
        }
    }
}
