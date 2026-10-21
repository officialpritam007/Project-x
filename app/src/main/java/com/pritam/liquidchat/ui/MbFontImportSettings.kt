package com.pritam.liquidchat.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable internal fun MbFontImportSettings(onChooseBody:()->Unit,onChooseTitle:()->Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var selected by remember {mutableStateOf(MbFontSlot.BODY)}
    var busy by remember {mutableStateOf(false)}
    var notice by remember {mutableStateOf<String?>(null)}
    val bodyCallback by rememberUpdatedState(onChooseBody)
    val titleCallback by rememberUpdatedState(onChooseTitle)
    val families=rememberImportedFonts()
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {uri->
        if(uri!=null && !busy) {
            val slot=selected
            busy=true
            scope.launch {
                try {
                    MbImportedFonts.importFont(context,slot,uri)
                    if(slot==MbFontSlot.BODY)bodyCallback() else titleCallback()
                    notice=if(slot==MbFontSlot.BODY)"Imported app font applied." else "Imported title font applied."
                } catch(e:CancellationException) {throw e}
                catch(_:Exception) {notice="Font import failed. Choose a valid TTF or OTF under 8 MiB; the previous font is preserved."}
                finally {busy=false}
            }
        }
    }
    GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
        Text("Import your own fonts",style=MaterialTheme.typography.titleMedium)
        Text("Select fonts you have permission to use. Files stay on this device and are not exported with themes.",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick={selected=MbFontSlot.BODY;picker.launch(arrayOf("*/*"))},
            enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text("Import app font")}
        Button(onClick={selected=MbFontSlot.TITLE;picker.launch(arrayOf("*/*"))},
            enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text("Import title font")}
        if(families.body!=null)TextButton(onClick=bodyCallback,enabled=!busy) {Text("Use imported app font")}
        if(families.title!=null)TextButton(onClick=titleCallback,enabled=!busy) {Text("Use imported title font")}
        Text("Choose System or Follow to return to built-in fonts. If an imported font is unavailable, Liquid Chat uses its original typography.",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
        notice?.let {Text(it,style=MaterialTheme.typography.bodySmall)}
    }
}
