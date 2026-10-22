package com.pritam.liquidchat.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Only options backed by working device preferences and the authenticated
 * Liquid Chat media pipeline are exposed here; no cloned WhatsApp services.
 */
@Composable internal fun MbRecordedMultimediaSettings(
    display:DisplayOptions,onDisplay:(DisplayOptions)->Unit
) {
    Text("Image",style=MaterialTheme.typography.labelLarge,
        color=MaterialTheme.colorScheme.primary)
    MbRecordedSettingsGroup {
        MbReferenceSwitchRow(
            title="HD quality images",
            subtitle="Send higher-resolution private images",
            checked=display.photoQuality=="HD",
            icon=Icons.Outlined.PhotoLibrary,
            onCheckedChange={onDisplay(display.copy(
                photoQuality=if(it)"HD" else "Standard"))})
    }
    Spacer(Modifier.height(12.dp))
    Text("Video",style=MaterialTheme.typography.labelLarge,
        color=MaterialTheme.colorScheme.primary)
    MbRecordedSettingsGroup {
        MbReferenceSwitchRow(
            title="HD quality videos",
            subtitle="720p on · 480p off · up to 30 seconds",
            checked=display.videoQuality=="HD",
            icon=Icons.Outlined.Videocam,
            onCheckedChange={onDisplay(display.copy(
                videoQuality=if(it)"HD" else "Standard"))})
    }
    Text("These settings control the real authenticated Liquid Chat image and video " +
        "composer. They do not bypass recipients' privacy settings.",
        style=MaterialTheme.typography.bodySmall,
        color=MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable internal fun MbRecordedAnimationSettings(
    display:DisplayOptions,onDisplay:(DisplayOptions)->Unit
) {
    Text("Liquid Glass & motion",style=MaterialTheme.typography.labelLarge,
        color=MaterialTheme.colorScheme.primary)
    MbRecordedSettingsGroup {
        MbReferenceSwitchRow(
            title="Reduce motion",
            subtitle="Shorten or remove nonessential interface animations",
            icon=Icons.Outlined.MotionPhotosOff,
            checked=display.reducedMotion,
            onCheckedChange={onDisplay(display.copy(reducedMotion=it))})
        HorizontalDivider(Modifier.padding(horizontal=12.dp))
        MbReferenceSwitchRow(
            title="Reduce transparency",
            subtitle="Use opaque backgrounds in transparent UI areas",
            icon=Icons.Outlined.LayersClear,
            checked=display.reducedTransparency,
            onCheckedChange={onDisplay(display.copy(reducedTransparency=it))})
        HorizontalDivider(Modifier.padding(horizontal=12.dp))
        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Animation level",style=MaterialTheme.typography.titleSmall)
            MbReferenceChoicePicker(
                title="Motion style",
                choices=listOf("Normal","Balanced","Extra"),
                selected=display.animation,
                onSelect={onDisplay(display.copy(animation=it))})
        }
    }
    MbTouchEffectsSettingsCard()
}

/** Genuine local contact style and security preferences, not third-party VIP
 * purchase flags. Navigation opens the actual Liquid Chat settings screens.
 */
@Composable internal fun MbRecordedExtraFeaturesSettings(
    onNavigate:(String)->Unit
) {
    MbContactInfoStyleSettingsCard()
    MbRecordedSettingsGroup {
        ListItem(
            headlineContent={Text("Conversation customization")},
            supportingContent={Text("Bubbles, reactions and chat wallpaper")},
            leadingContent={Icon(Icons.Outlined.ChatBubbleOutline,null)})
        TextButton(onClick={onNavigate("Liquid Chat Conversation Screen")},
            modifier=Modifier.fillMaxWidth()) {Text("Open conversation options")}
        HorizontalDivider(Modifier.padding(horizontal=12.dp))
        ListItem(
            headlineContent={Text("Privacy options")},
            supportingContent={Text("Control online presence and read receipts")},
            leadingContent={Icon(Icons.Outlined.Lock,null)})
        TextButton(onClick={onNavigate("Liquid Chat Privacy")},
            modifier=Modifier.fillMaxWidth()) {Text("Open privacy controls")}
    }
}

@Composable private fun MbRecordedSettingsGroup(
    content:@Composable ColumnScope.()->Unit
) {
    Surface(modifier=Modifier.fillMaxWidth(),
        shape=RoundedCornerShape(22.dp),
        color=MaterialTheme.colorScheme.surface,
        tonalElevation=0.dp,
        border=BorderStroke(1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha=.30f))) {
        Column(content=content)
    }
}
