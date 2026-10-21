package com.pritam.liquidchat.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Android-supported alternate launcher aliases; no package renaming or app reset. */
internal object LauncherIconChoice {
    const val CLASSIC="Classic"
    const val NIGHT="Night Glass"
    val choices=listOf(CLASSIC,NIGHT)
    private fun component(context:Context,kind:String):ComponentName {
        val suffix=if(kind==NIGHT)"NightGlass" else "Default"
        return ComponentName(context.packageName,
            "${context.packageName}.launcher.$suffix")
    }
    fun selected(context:Context):String {
        val manager=context.packageManager
        return if(manager.getComponentEnabledSetting(component(context,NIGHT))==
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED)NIGHT else CLASSIC
    }
    fun set(context:Context,kind:String) {
        require(kind in choices)
        val manager=context.packageManager
        val other=if(kind==NIGHT)CLASSIC else NIGHT
        if(selected(context)==kind)return
        val flags=PackageManager.DONT_KILL_APP
        // Enable the chosen alias before disabling the old alias; MainActivity
        // stays enabled to continue handling push and OAuth callbacks.
        manager.setComponentEnabledSetting(component(context,kind),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,flags)
        manager.setComponentEnabledSetting(component(context,other),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,flags)
    }
}

@Composable internal fun LauncherIconSettings() {
    val context=LocalContext.current
    var choice by remember(context) {mutableStateOf(LauncherIconChoice.selected(context))}
    var failure by remember {mutableStateOf<String?>(null)}
    GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
        Text("Launcher icon",style=MaterialTheme.typography.titleMedium)
        Text("Choose how Liquid Chat appears on your Android home screen. Your chats, account and call settings are unchanged.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            LauncherIconChoice.choices.forEach {kind->
                FilterChip(selected=choice==kind,onClick={
                    runCatching {LauncherIconChoice.set(context,kind)}
                        .onSuccess {choice=LauncherIconChoice.selected(context);failure=null}
                        .onFailure {failure=it.message ?: "Could not switch icon"}
                },label={Text(kind)})
            }
        }
        failure?.let {Text(it,color=MaterialTheme.colorScheme.error,
            style=MaterialTheme.typography.bodySmall)}
        Text("Some Android launchers may take a moment to refresh the icon.",
            style=MaterialTheme.typography.labelSmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
