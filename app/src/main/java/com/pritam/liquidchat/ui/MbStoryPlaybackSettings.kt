package com.pritam.liquidchat.ui

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Device-only story playback preferences; message privacy remains server-controlled. */
internal data class MbStoryPlaybackOptions(
    val autoAdvance:Boolean=true,
    val startWithSound:Boolean=false
)

internal object MbStoryPlaybackSettings {
    private const val PREFS="liquid_story_playback_v1"
    fun read(context:Context):MbStoryPlaybackOptions {
        val prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
        return MbStoryPlaybackOptions(prefs.getBoolean("autoAdvance",true),
            prefs.getBoolean("startWithSound",false))
    }
    fun save(context:Context,options:MbStoryPlaybackOptions) {
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
            .putBoolean("autoAdvance",options.autoAdvance)
            .putBoolean("startWithSound",options.startWithSound).apply()
    }
    fun initiallyMuted(context:Context,options:MbStoryPlaybackOptions):Boolean {
        if(!options.startWithSound)return true
        return runCatching {
            val audio=context.getSystemService(AudioManager::class.java)
            val alerts=context.getSystemService(NotificationManager::class.java)
            audio==null || audio.ringerMode!=AudioManager.RINGER_MODE_NORMAL ||
                alerts==null || alerts.currentInterruptionFilter!=NotificationManager.INTERRUPTION_FILTER_ALL
        }.getOrDefault(true)
    }
}

@Composable internal fun MbStoryPlaybackSettingsCard() {
    val context=LocalContext.current
    var options by remember {mutableStateOf(MbStoryPlaybackSettings.read(context))}
    fun update(next:MbStoryPlaybackOptions) {
        MbStoryPlaybackSettings.save(context,next)
        options=next
    }
    GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
        Text("Stories",style=MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text("Disable automatic story advance",Modifier.weight(1f))
            Switch(checked=!options.autoAdvance,
                onCheckedChange={update(options.copy(autoAdvance=!it))})
        }
        Text("Stay on the current story when playback ends. Tap to move between stories or replay it.",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text("Start video stories with sound",Modifier.weight(1f))
            Switch(checked=options.startWithSound,
                onCheckedChange={update(options.copy(startWithSound=it))})
        }
        Text("Off by default. Silent mode and Do Not Disturb keep initial playback muted. You can mute or unmute while viewing.",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
