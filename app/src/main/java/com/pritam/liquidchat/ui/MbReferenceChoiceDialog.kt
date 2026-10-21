package com.pritam.liquidchat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** Owner-supplied mb_dialog_alert_ios.xml counterpart:
 * title, divided vertically stacked options and explicit dismissal. Data and
 * actions are always native Liquid Chat rather than copied WhatsApp services.
 */
@Composable internal fun MbReferenceChoiceDialog(
    title:String,
    choices:List<String>,
    selected:String,
    onSelect:(String)->Unit,
    onDismiss:()->Unit
) {
    LiquidAlertDialog(
        onDismissRequest=onDismiss,
        title={Text(title,style=MaterialTheme.typography.titleMedium)},
        text={Column(Modifier.fillMaxWidth().heightIn(max=420.dp)
            .verticalScroll(rememberScrollState())) {
            choices.distinct().forEachIndexed {index,option->
                if(index>0) HorizontalDivider(
                    color=MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().heightIn(min=49.dp)
                    .clickable(role=Role.Button) {onSelect(option)}
                    .padding(horizontal=15.dp,vertical=9.dp),
                    verticalAlignment=Alignment.CenterVertically) {
                    Text(option,Modifier.weight(1f),
                        style=MaterialTheme.typography.bodyLarge,
                        color=if(option==selected)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface)
                    if(option==selected) Icon(Icons.Outlined.Check,
                        contentDescription="Selected",modifier=Modifier.size(21.dp),
                        tint=MaterialTheme.colorScheme.primary)
                }
            }
        }},
        confirmButton={TextButton(onClick=onDismiss) {Text("Cancel")}}
    )
}

@Composable internal fun MbReferenceChoicePicker(
    title:String,
    choices:List<String>,
    selected:String,
    modifier:Modifier=Modifier,
    onSelect:(String)->Unit
) {
    var expanded by remember(title) {mutableStateOf(false)}
    OutlinedButton(onClick={expanded=true},
        modifier=modifier.fillMaxWidth().heightIn(min=48.dp),
        contentPadding=PaddingValues(horizontal=15.dp,vertical=8.dp)) {
        Text(title,modifier=Modifier.weight(1f),
            style=MaterialTheme.typography.bodyLarge)
        Text(selected,style=MaterialTheme.typography.bodySmall)
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Outlined.KeyboardArrowDown,
            contentDescription="Choose $title",modifier=Modifier.size(21.dp))
    }
    if(expanded) MbReferenceChoiceDialog(title,choices,selected,
        onSelect={value->onSelect(value);expanded=false},
        onDismiss={expanded=false})
}
