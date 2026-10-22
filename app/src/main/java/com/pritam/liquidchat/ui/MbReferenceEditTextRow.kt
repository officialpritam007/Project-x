package com.pritam.liquidchat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Owner-supplied MB 3.3.4 mb_edittext_layout.xml / mb_edittext_box.xml:
 * 45dp settings row with 24dp icon, 15sp title, 13sp subtitle. Editing opens
 * a real 18sp title / 14sp explanation / two-action dialog. The callback owns
 * validation/persistence; no fake control or untrusted reference service.
 */
@Composable internal fun MbReferenceEditTextRow(
    title:String,
    description:String,
    value:String,
    maxLength:Int,
    onCommit:(String)->Unit,
    modifier:Modifier=Modifier,
    enabled:Boolean=true,
    icon:ImageVector=Icons.Outlined.Edit,
    valid:(String)->Boolean={it.isNotBlank()}
) {
    require(maxLength in 1..4000)
    var editing by remember(title) {mutableStateOf(false)}
    var draft by remember(title) {mutableStateOf("")}
    Row(modifier.fillMaxWidth().heightIn(min=45.dp).clip(Glass.compact)
        .clickable(enabled=enabled,role=Role.Button) {
            draft=value
            editing=true
        }
        .padding(start=16.dp,end=12.dp,top=6.dp,bottom=6.dp),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Icon(icon,contentDescription=null,modifier=Modifier.size(24.dp),
            tint=MaterialTheme.colorScheme.onSurface)
        Column(Modifier.weight(1f)) {
            Text(title,fontSize=15.sp,fontWeight=FontWeight.Medium,
                maxLines=1,overflow=TextOverflow.Ellipsis)
            Text(description.ifBlank {value},fontSize=13.sp,
                maxLines=2,overflow=TextOverflow.Ellipsis,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if(editing) LiquidAlertDialog(
        onDismissRequest={editing=false},
        title={Text(title,fontSize=18.sp,fontWeight=FontWeight.Bold)},
        text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            if(description.isNotBlank())Text(description,fontSize=14.sp,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(value=draft,onValueChange={
                if(it.length<=maxLength)draft=it
            },modifier=Modifier.fillMaxWidth(),singleLine=true,
                label={Text(title)})
        }},
        confirmButton={TextButton(enabled=enabled&&draft.length<=maxLength&&valid(draft),
            onClick={
                onCommit(draft.trim())
                editing=false
            }) {Text("Save",fontSize=17.sp)}},
        dismissButton={TextButton(onClick={editing=false}) {
            Text("Cancel",fontSize=17.sp)
        }}
    )
}
