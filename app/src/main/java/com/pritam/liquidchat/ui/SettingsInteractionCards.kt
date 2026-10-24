package com.pritam.liquidchat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pritam.liquidchat.AppViewModel

/** Reusable presentation-only settings panels; persistence stays in the model. */
@Composable internal fun HomeInteractionSettingsCard(
    model:AppViewModel,display:DisplayOptions
) {
    GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
            Text("Home interaction controls",style=MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("Show preview on chat long-press",Modifier.weight(1f))
                Switch(checked=display.showLongPressPreview,
                    onCheckedChange={model.setDisplay(display.copy(showLongPressPreview=it))})
            }
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("Hide floating new-chat buttons",Modifier.weight(1f))
                Switch(checked=display.hideHomeFloatingButtons,
                    onCheckedChange={model.setDisplay(display.copy(hideHomeFloatingButtons=it))})
            }
            Text("Contacts and New chat remain available through existing navigation.",
                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
}
@Composable internal fun ConversationKeyboardStickerSettingsCard(
    model:AppViewModel,display:DisplayOptions
) {
    GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
            Text("Keyboard privacy and stickers",style=MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Incognito keyboard")
                    Text("Ask your keyboard not to learn the text you type in chats. Android keyboards may ignore this request.",
                        style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked=display.incognitoKeyboard,
                    onCheckedChange={model.setDisplay(display.copy(incognitoKeyboard=it))})
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Custom sticker display size")
                    Text("Resize stickers in this app only; the original file and recipient experience are unchanged.",
                        style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked=display.customStickerSize,
                    onCheckedChange={model.setDisplay(display.copy(customStickerSize=it))})
            }
            if(display.customStickerSize) {
                Text("Sticker size: ${display.stickerSize} dp",style=MaterialTheme.typography.labelLarge)
                Slider(value=display.stickerSize.toFloat(),
                    onValueChange={model.setDisplay(display.copy(stickerSize=it.toInt().coerceIn(96,256)))},
                    valueRange=96f..256f)
            }
        }
}
