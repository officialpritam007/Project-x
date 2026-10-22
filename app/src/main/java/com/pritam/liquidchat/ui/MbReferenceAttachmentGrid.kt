package com.pritam.liquidchat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A genuine action and handler, never a decorative/no-op picker tile. */
internal data class MbAttachmentChoice(
    val label:String,
    val icon:ImageVector,
    val onChoose:()->Unit
)

/**
 * The owner-provided APK mb_list_layout_item has an 8dp item inset,
 * 42dp image and 15sp caption; mb_list_layout_dialog is a grid.
 * These provide source-backed tile geometry, not a claim that a
 * proprietary attachment screen's exact behavior or art was copied.
 */
@Composable internal fun MbReferenceAttachmentGrid(choices:List<MbAttachmentChoice>) {
    if(!referenceListUsesFlatRows(LocalDisplayOptions.current)) {
        choices.forEach { choice ->
            GlassActionRow(choice.label,choice.icon,onClick=choice.onChoose)
        }
        return
    }
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(4.dp)) {
        choices.chunked(3).forEach { group ->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                group.forEach { choice ->
                    Column(
                        Modifier.weight(1f).heightIn(min=94.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(role=Role.Button,onClick=choice.onChoose)
                            .padding(8.dp),
                        horizontalAlignment=Alignment.CenterHorizontally,
                        verticalArrangement=Arrangement.Center
                    ) {
                        Icon(choice.icon,contentDescription=null,
                            tint=MaterialTheme.colorScheme.primary,
                            modifier=Modifier.size(42.dp))
                        Spacer(Modifier.height(6.dp))
                        Text(choice.label,
                            fontSize=15.sp,
                            textAlign=TextAlign.Center,
                            maxLines=2,
                            overflow=TextOverflow.Ellipsis,
                            color=MaterialTheme.colorScheme.onSurface)
                    }
                }
                repeat(3-group.size) {Spacer(Modifier.weight(1f))}
            }
        }
    }
}
