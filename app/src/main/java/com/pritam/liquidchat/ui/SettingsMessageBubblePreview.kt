package com.pritam.liquidchat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Live preview of independently configured incoming/outgoing chat bubbles. */
@Composable internal fun SettingsMessageBubblePreview(display:DisplayOptions) {
            GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
                Text("Live message preview",style=MaterialTheme.typography.titleMedium)
                Text("Preview",style=MaterialTheme.typography.titleSmall)
            listOf(false,true).forEach {mine ->
                Row(Modifier.fillMaxWidth(),horizontalArrangement=if(mine)Arrangement.End else Arrangement.Start) {
                    Surface(shape=conversationBubbleShape(display.bubbleStyle,mine),
                        color=bubbleSurfaceColor(display,mine,LocalGlassDark.current,
                            if(LocalGlassDark.current) {if(mine)Color(0xFF1F5268) else Color(0xFF293F53)}
                            else if(mine)Color(0xFFD0EDFB) else Color(0xFFFAFDFF))) {
                        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                            Text("↪ Original message",
                                style=MaterialTheme.typography.labelSmall,
                                color=conversationInkColor(display.quotedInk,LocalGlassDark.current,
                                    resolvedBubbleTextColor(display,mine,LocalGlassDark.current,MaterialTheme.colorScheme.onSurfaceVariant),display.quotedInkHex))
                            Text(if(mine)"Outgoing message" else "Incoming message",
                                style=MaterialTheme.typography.bodyLarge.copy(fontSize=display.messageFontSize.sp),
                                color=conversationInkColor(if(mine)display.outgoingInk else display.incomingInk,
                                    LocalGlassDark.current,
                                    resolvedBubbleTextColor(display,mine,LocalGlassDark.current,MaterialTheme.colorScheme.onSurface),
                                    if(mine)display.outgoingInkHex else display.incomingInkHex))
                            Text("12:34",modifier=Modifier.align(Alignment.End),
                                style=MaterialTheme.typography.labelSmall,
                                color=conversationInkColor(display.timestampInk,LocalGlassDark.current,
                                    resolvedBubbleTextColor(display,mine,LocalGlassDark.current,MaterialTheme.colorScheme.onSurfaceVariant),display.timestampInkHex))
                        }
                    }
                }
            }

            }
}
