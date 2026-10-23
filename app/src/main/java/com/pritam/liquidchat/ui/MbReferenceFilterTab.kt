package com.pritam.liquidchat.ui

import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Client-only reference presentation; filtering and paging stay with each screen. */
@Composable internal fun MbReferenceFilterTab(
    label:String,selected:Boolean,onClick:()->Unit,modifier:Modifier=Modifier
) {
    if(!referenceListUsesFlatRows(LocalDisplayOptions.current)) {
        FilterChip(selected=selected,onClick=onClick,label={Text(label)},
            modifier=modifier.heightIn(min=48.dp),shape=Glass.capsule)
        return
    }
    Column(modifier.defaultMinSize(minHeight=42.dp)
        .selectable(selected=selected,role=Role.Tab,onClick=onClick)
    Column(modifier.heightIn(min=48.dp)
        .clickable(role=Role.Tab,onClick=onClick)
        .semantics { this.selected=selected }
        .padding(horizontal=12.dp,vertical=5.dp),
        horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=Arrangement.Bottom) {
        Text(label,fontSize=14.sp,
            fontWeight=if(selected)FontWeight.Bold else FontWeight.Medium,
            color=if(selected)MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(5.dp))
        if(selected)HorizontalDivider(thickness=2.dp,
            color=MaterialTheme.colorScheme.primary)
        else Spacer(Modifier.height(2.dp))
    }
}
