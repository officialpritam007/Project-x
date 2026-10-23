package com.pritam.liquidchat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pritam.liquidchat.R

@Composable internal fun MbLauncherIconSettings() {
    val context=LocalContext.current
    var active by remember {mutableStateOf(runCatching {MbLauncherIcons.active(context)}.getOrDefault(emptyList()))}
    var feedback by remember {mutableStateOf<String?>(null)}
    GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
        Text("App icon",style=MaterialTheme.typography.titleMedium)
        Text("Choose a Liquid Chat launcher icon. Your launcher may take a moment to refresh.",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.fillMaxWidth().selectableGroup()) {
            MbLauncherIcon.entries.forEach {icon->
                Row(Modifier.fillMaxWidth().heightIn(min=56.dp)
                    .selectable(selected=active==listOf(icon),role=Role.RadioButton,onClick={
                        val result=MbLauncherIcons.select(context,icon)
                        active=runCatching {MbLauncherIcons.active(context)}.getOrDefault(active)
                        feedback=if(result.isSuccess)"${icon.title} icon selected" else
                            "Could not finish changing the icon. The app remains available; choose an icon again."
                    }).padding(horizontal=8.dp,vertical=8.dp),
                    verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Icon(painterResource(when(icon) {
                        MbLauncherIcon.DEFAULT->R.drawable.ic_launcher_foreground
                        MbLauncherIcon.NIGHT_GLASS->R.drawable.ic_liquid_launcher_alt
                        MbLauncherIcon.MINT->R.drawable.ic_launcher_mint_foreground
                        MbLauncherIcon.MIDNIGHT->R.drawable.ic_launcher_midnight_foreground
                    }),contentDescription=null,tint=Color.Unspecified,modifier=Modifier.size(48.dp))
                    Text(icon.title,Modifier.weight(1f),style=MaterialTheme.typography.bodyLarge)
                    RadioButton(selected=active==listOf(icon),onClick=null)
                }
            }
        }
        feedback?.let {Text(it,style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)}
        Text("Icons are device-wide and do not change chats, calls or your account.",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
    MbAppShortcutsSettingsCard()
}
