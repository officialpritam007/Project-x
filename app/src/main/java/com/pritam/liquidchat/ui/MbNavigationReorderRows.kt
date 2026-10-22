package com.pritam.liquidchat.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.consume
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * The five destination IDs never change: only their displayed order is saved.
 * A long press on the handle can move across multiple rows; the arrow buttons
 * remain usable with screen readers and switch access.
 */
@Composable internal fun MbNavigationReorderRows(
    order:String?,
    onOrderChange:(String)->Unit
) {
    val callback by rememberUpdatedState(onOrderChange)
    MbNavigationOrder.parse(order).forEachIndexed {position,destination ->
        val title=MbNavigationOrder.titles[destination]
        var dragOffset by remember(destination) {mutableFloatStateOf(0f)}
        Row(
            Modifier.fillMaxWidth().heightIn(min=56.dp)
                .graphicsLayer { translationY=dragOffset },
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(4.dp)
        ) {
            Box(
                Modifier.size(48.dp)
                    .testTag("reorder-tab-$destination")
                    .pointerInput(destination,order) {
                        val rowHeight=56.dp.toPx()
                        detectDragGesturesAfterLongPress(
                            onDragStart={dragOffset=0f},
                            onDragEnd={
                                val slots=(dragOffset/rowHeight).roundToInt()
                                dragOffset=0f
                                if(slots!=0)callback(MbNavigationOrder.moveBy(
                                    order,destination,slots))
                            },
                            onDragCancel={dragOffset=0f},
                            onDrag={change,amount->
                                change.consume()
                                dragOffset+=amount.y
                            }
                        )
                    }
                    .semantics {contentDescription="Hold and drag $title to reorder"},
                contentAlignment=Alignment.Center
            ) {
                Icon(Icons.Outlined.DragHandle,contentDescription=null,
                    tint=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(title,Modifier.weight(1f),
                maxLines=1,overflow=TextOverflow.Ellipsis,
                style=MaterialTheme.typography.bodyMedium)
            IconButton(
                enabled=position>0,
                onClick={callback(MbNavigationOrder.move(order,destination,-1))}
            ) {
                Icon(Icons.Outlined.KeyboardArrowUp,
                    contentDescription="Move $title left")
            }
            IconButton(
                enabled=position<4,
                onClick={callback(MbNavigationOrder.move(order,destination,1))}
            ) {
                Icon(Icons.Outlined.KeyboardArrowDown,
                    contentDescription="Move $title right")
            }
        }
    }
}
