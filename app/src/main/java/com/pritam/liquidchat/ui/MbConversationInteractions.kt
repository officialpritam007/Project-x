package com.pritam.liquidchat.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pritam.liquidchat.AppViewModel
import com.pritam.liquidchat.data.Message
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Match the existing authenticated reaction action's accepted emoji vocabulary. */
internal fun normalizedDoubleTapReaction(value:String?):String =
    value?.takeIf {it in MbQuickReactions.available} ?: "❤️"

internal fun canDoubleTapReact(message:Message,enabled:Boolean,selectionMode:Boolean,
                              account:String?):Boolean =
    enabled && !selectionMode && account!=null && message.account==account &&
        message.status=="SENT" && message.deletedAt.isEmpty()

/** Local formatting of server-provided metadata; never fabricate edit history. */
internal fun editedMessageMarker(editedAt:String,showLabel:Boolean,showTime:Boolean,
                                zone:ZoneId=ZoneId.systemDefault()):String? {
    if(!showLabel || editedAt.isBlank())return null
    if(!showTime)return "edited"
    val timestamp=runCatching {
        Instant.parse(editedAt).atZone(zone).format(DateTimeFormatter.ofPattern("dd MMM, HH:mm"))
    }.getOrNull() ?: return "edited"
    return "edited $timestamp"
}

@Composable
internal fun MbConversationInteractionSettings(model:AppViewModel,display:DisplayOptions) {
    GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
        Text("Message interactions",style=MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text("Double-tap to react",Modifier.weight(1f))
            Switch(checked=display.doubleTapReactionEnabled,
                onCheckedChange={model.setDisplay(display.copy(doubleTapReactionEnabled=it))})
        }
        Text("Double-tap reaction",style=MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            MbQuickReactions.available.forEach {emoji->
                FilterChip(selected=normalizedDoubleTapReaction(display.doubleTapReactionEmoji)==emoji,
                    enabled=display.doubleTapReactionEnabled,
                    onClick={model.setDisplay(display.copy(doubleTapReactionEmoji=emoji))},
                    label={Text(emoji)})
            }
        }
        Text("Double-tap a sent message to add or remove a reaction. Disabled in selection mode; authenticated server rules still apply.",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider()
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text("Hide quick-reply suggestions",Modifier.weight(1f))
            Switch(checked=display.hideQuickReplySuggestions,
                onCheckedChange={model.setDisplay(display.copy(hideQuickReplySuggestions=it))})
        }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text("Confirm saved sticker sends",Modifier.weight(1f))
            Switch(checked=display.confirmSavedStickerSend,
                onCheckedChange={model.setDisplay(display.copy(confirmSavedStickerSend=it))})
        }
    }
    GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
        Text("Edited messages",style=MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text("Show edited label",Modifier.weight(1f))
            Switch(checked=display.showEditedLabel,
                onCheckedChange={model.setDisplay(display.copy(showEditedLabel=it))})
        }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text("Show edit date & time",Modifier.weight(1f))
            Switch(checked=display.showEditedTime,enabled=display.showEditedLabel,
                onCheckedChange={model.setDisplay(display.copy(showEditedTime=it))})
        }
        Text("This displays the server's actual latest edit time in your time zone. It cannot restore old message bodies.",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
