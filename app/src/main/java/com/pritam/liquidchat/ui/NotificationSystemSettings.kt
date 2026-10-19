package com.pritam.liquidchat.ui

import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

internal fun notificationPermissionLabel(enabled:Boolean):String =
    if(enabled)"Allowed on this device" else "Blocked by Android settings"

/**
 * Android owns notification channels/sounds and POST_NOTIFICATIONS grants;
 * app previews and per-chat mute are separate server-managed preferences.
 */
@Composable internal fun NotificationSystemSettings() {
    val context=LocalContext.current
    val owner=LocalLifecycleOwner.current
    var allowed by remember(context) {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    DisposableEffect(context,owner) {
        val observer=LifecycleEventObserver {_,event->
            if(event==Lifecycle.Event.ON_RESUME)
                allowed=NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        owner.lifecycle.addObserver(observer)
        onDispose {owner.lifecycle.removeObserver(observer)}
    }
    GlassCard(Modifier.fillMaxWidth()) {
        Text("Android notification settings",style=MaterialTheme.typography.titleMedium)
        Text(notificationPermissionLabel(allowed),
            color=if(allowed)MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.error)
        Text("Android controls notification permissions, sounds, vibration and channels. Liquid Chat cannot override a blocked channel or device battery policy.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(
            modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),
            onClick={
                val notificationIntent=Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName)
                val fallback=Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}"))
                try {context.startActivity(notificationIntent)}
                catch(_:Exception) {runCatching {context.startActivity(fallback)}}
            }
        ) {Text("Open Android notification settings")}
    }
}
