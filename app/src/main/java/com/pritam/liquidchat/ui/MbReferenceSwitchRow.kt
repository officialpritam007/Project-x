package com.pritam.liquidchat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * mb_switch_layout.xml: a single 45dp reference row, 24dp leading icon,
 * 15sp title / 12sp subtitle and an actual accessible on/off switch.
 * The host owns persistence and (when required) server authorization.
 */
@Composable internal fun MbReferenceSwitchRow(
    title:String,subtitle:String,checked:Boolean,
    onCheckedChange:(Boolean)->Unit,
    modifier:Modifier=Modifier,
    enabled:Boolean=true,
    icon:ImageVector=Icons.Outlined.Tune
) {
    Row(modifier.fillMaxWidth().heightIn(min=45.dp).clip(Glass.compact)
        .toggleable(value=checked,enabled=enabled,role=Role.Switch,
            onValueChange=onCheckedChange)
        .semantics {contentDescription=title}
        .padding(start=16.dp,end=6.dp,top=6.dp,bottom=6.dp),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        Icon(icon,contentDescription=null,modifier=Modifier.size(24.dp),
            tint=MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f).padding(start=6.dp)) {
            Text(title,fontSize=15.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
            if(subtitle.isNotBlank())Text(subtitle,fontSize=12.sp,maxLines=2,
                overflow=TextOverflow.Ellipsis,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked=checked,onCheckedChange=null,enabled=enabled,
            modifier=Modifier.padding(end=6.dp))
    }
}
