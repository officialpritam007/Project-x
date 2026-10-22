package com.pritam.liquidchat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Toolbar button for the existing private photo and album editor.
 * No operation is mocked: crop, rotate, flip, grayscale, Undo and Redo
 * remain implemented by the caller's Images.edit and draft history.
 */
@Composable internal fun MbReferenceEditorTool(
    label:String,enabled:Boolean,onClick:()->Unit
) {
    if(!referenceListUsesFlatRows(LocalDisplayOptions.current)) {
        FilterChip(selected=false,enabled=enabled,onClick=onClick,label={Text(label)})
        return
    }
    OutlinedButton(
        enabled=enabled,
        onClick=onClick,
        modifier=Modifier.heightIn(min=42.dp),
        shape=RoundedCornerShape(12.dp),
        contentPadding=PaddingValues(horizontal=10.dp,vertical=6.dp)
    ) {
        Icon(Icons.Outlined.Tune,contentDescription=null,modifier=Modifier.size(16.dp))
        Spacer(Modifier.width(5.dp))
        Text(label,style=MaterialTheme.typography.labelMedium)
    }
}
