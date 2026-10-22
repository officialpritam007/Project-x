package com.pritam.liquidchat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * mb_seekbar_dialog.xml + mb_seekbar_layout.xml counterpart.
 * The 17sp dialog title, "Preview" and 18sp current value are grounded in the
 * owner's compiled APK. Moving the slider is local until Save is pressed.
 */
@Composable internal fun MbReferenceSeekbarRow(
    title:String,value:Int,range:IntRange,unit:String,
    onApply:(Int)->Unit,
    modifier:Modifier=Modifier
) {
    require(!range.isEmpty() && range.last-range.first<=200)
    var open by remember(title) {mutableStateOf(false)}
    var draft by remember(title) {mutableIntStateOf(value.coerceIn(range))}
    Row(modifier.fillMaxWidth().heightIn(min=45.dp)
        .clickable(role=Role.Button) {
            draft=value.coerceIn(range)
            open=true
        }.padding(horizontal=12.dp,vertical=8.dp),
        verticalAlignment=Alignment.CenterVertically) {
        Icon(Icons.Outlined.Tune,contentDescription=null,
            modifier=Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(title,Modifier.weight(1f),fontSize=15.sp)
        Text("$value $unit",fontSize=13.sp,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if(open) LiquidAlertDialog(
        onDismissRequest={open=false},
        title={Text(title,fontSize=17.sp)},
        text={Column(Modifier.fillMaxWidth(),
            verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("Preview",modifier=Modifier.fillMaxWidth(),
                fontSize=14.sp,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
            Slider(value=draft.toFloat(),
                onValueChange={draft=it.toInt().coerceIn(range)},
                valueRange=range.first.toFloat()..range.last.toFloat(),
                steps=(range.last-range.first-1).coerceAtLeast(0))
            Text("$draft $unit",modifier=Modifier.fillMaxWidth(),
                fontSize=18.sp,fontWeight=FontWeight.SemiBold,
                textAlign=androidx.compose.ui.text.style.TextAlign.Center)
        }},
        confirmButton={TextButton(onClick={onApply(draft);open=false}) {
            Text("Save")
        }},
        dismissButton={TextButton(onClick={open=false}) {
            Text("Cancel")
        }}
    )
}
