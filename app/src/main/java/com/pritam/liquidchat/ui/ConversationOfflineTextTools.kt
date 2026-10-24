package com.pritam.liquidchat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Offline-only draft transformations; stale input cannot be applied. */
@Composable internal fun ConversationOfflineTextTools(
    open:Boolean,selected:OfflineTextAction?,body:String,sourceText:String,
    draftReady:Boolean,queueing:Boolean,
    onCloseTools:()->Unit,onSelectTool:(OfflineTextAction)->Unit,
    onDismissPreview:()->Unit,onApply:(String)->Unit
) {
    if(open) LiquidActionSheet(
        title="Text tools · offline",onDismiss=onCloseTools) {
        Text("Only this unsent draft is transformed. This is not an AI service and your text stays on this phone.",
            modifier=Modifier.padding(horizontal=16.dp,vertical=8.dp),
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        OfflineTextAction.entries.forEach {option->
            GlassActionRow(option.label,Icons.Outlined.TextFields,onClick={
                onSelectTool(option)
            })
        }
    }
    selected?.let {choice->
        val result=remember(sourceText,choice) {
            transformDraftOffline(sourceText,choice)
        }
        LiquidAlertDialog(
            onDismissRequest=onDismissPreview,
            title={Text(choice.label)},
            text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("Preview",style=MaterialTheme.typography.labelMedium)
                Text(result,modifier=Modifier.fillMaxWidth().heightIn(max=240.dp)
                    .verticalScroll(rememberScrollState()))
                if(body!=sourceText)Text("Your draft has changed. Close and open Text tools again.",
                    color=MaterialTheme.colorScheme.error,
                    style=MaterialTheme.typography.bodySmall)
            }},
            confirmButton={TextButton(onClick={
                if(body==sourceText)onApply(result)
                onDismissPreview()
            },enabled=draftReady&&!queueing&&body==sourceText&&
                result!=sourceText) {Text("Apply to draft")}},
            dismissButton={TextButton(onClick=onDismissPreview) {Text("Cancel")}}
        )
    }

}
