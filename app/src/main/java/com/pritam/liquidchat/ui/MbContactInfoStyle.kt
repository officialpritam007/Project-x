package com.pritam.liquidchat.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

/**
 * Native, device-local counterpart to the reference's contact information
 * appearance setting. No third-party VIP entitlements or WhatsApp endpoints.
 */
internal object MbContactInfoStyle {
    private const val FILE="liquid_contact_info_style_v1"
    private const val KEY="ios_style"
    fun prefs(context:Context):SharedPreferences=
        context.applicationContext.getSharedPreferences(FILE,Context.MODE_PRIVATE)
    fun read(context:Context):Boolean=prefs(context).getBoolean(KEY,true)
    fun set(context:Context,value:Boolean) {
        prefs(context).edit().putBoolean(KEY,value).apply()
    }
}

@Composable internal fun rememberIosContactInfoStyle():Boolean {
    val context=androidx.compose.ui.platform.LocalContext.current.applicationContext
    val preferences=remember(context) {MbContactInfoStyle.prefs(context)}
    var enabled by remember(preferences) {
        mutableStateOf(preferences.getBoolean("ios_style",true))
    }
    DisposableEffect(preferences) {
        val callback=SharedPreferences.OnSharedPreferenceChangeListener {_,key->
            if(key=="ios_style")enabled=preferences.getBoolean(key,true)
        }
        preferences.registerOnSharedPreferenceChangeListener(callback)
        onDispose {preferences.unregisterOnSharedPreferenceChangeListener(callback)}
    }
    return enabled
}

@Composable internal fun MbContactInfoStyleSettingsCard() {
    val context=androidx.compose.ui.platform.LocalContext.current
    val enabled=rememberIosContactInfoStyle()
    GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
        Text("Contact information appearance",
            style=MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("iOS-style contact details")
                Text("Use the Liquid Chat glass profile layout in Contact Details and Shared Media. No changes to contacts or message privacy.",
                    style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked=enabled,onCheckedChange={
                MbContactInfoStyle.set(context,it)
            })
        }
    }
}
