package com.pritam.liquidchat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Native, functional help links for the features supported by Liquid Chat. */
internal data class MbHelpTopic(
    val title:String,
    val description:String,
    val destination:String
)
internal object MbHelpTopics {
    val entries=listOf(
        MbHelpTopic(
            "Customize the conversation screen",
            "Change message bubbles, fonts, colors, icon tint and chat appearance.",
            "Liquid Chat Conversation Screen"
        ),
        MbHelpTopic(
            "Quick replies & storage",
            "Manage /shortcuts and choose optional automatic thumbnail cache cleanup.",
            "Liquid Chat General Settings"
        ),
        MbHelpTopic(
            "Arrange your home screen",
            "Change tab order, filters, row sizes, and header presentation.",
            "Liquid Chat Home Screen"
        ),
        MbHelpTopic(
            "Privacy & Ghost Mode",
            "Choose real account presence and read-receipt settings. Some protections are device-only.",
            "Liquid Chat Privacy"
        ),
        MbHelpTopic(
            "Personalize alerts",
            "Set Android notification options. Per-chat channels are available inside each conversation.",
            "Liquid Chat Notifications"
        ),
        MbHelpTopic(
            "Save your appearance",
            "Import/export supported Liquid Chat theme JSON or save a device-local preset.",
            "Liquid Chat Themes"
        )
    )
}

@Composable internal fun MbTipsAndHelp(onNavigate:(String)->Unit) {
    GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
        Text("Tips & Help",style=MaterialTheme.typography.titleLarge)
        Text("Use these working Liquid Chat controls. Your messages and account remain exclusively in Liquid Chat.",
            style=MaterialTheme.typography.bodyMedium,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
    MbHelpTopics.entries.forEach {topic->
        GlassCard(Modifier.fillMaxWidth(),Glass.compact) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(topic.title,style=MaterialTheme.typography.titleMedium)
                    Text(topic.description,style=MaterialTheme.typography.bodySmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick={onNavigate(topic.destination)}) {
                    Text("Open")
                    Icon(Icons.Outlined.ChevronRight,null)
                }
            }
        }
    }
    GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
        Text("About compatibility",style=MaterialTheme.typography.titleMedium)
        Text("External messaging-service protocols, account linking and unsupported third-party options cannot be enabled by importing another APK. Calls and messages use Liquid Chat's own authenticated services.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
