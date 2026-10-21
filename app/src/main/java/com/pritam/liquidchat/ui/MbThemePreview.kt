package com.pritam.liquidchat.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Offline visual preview for one saved Liquid Chat theme.
 * No messages, names, remote images, or account identifiers are read.
 * Nothing is applied until the owner taps Apply.
 */
@Composable internal fun MbThemePreview(theme:DisplayOptions) {
    val dark=when(theme.appearance) {
        "Dark"->true
        "Light"->false
        else->LocalGlassDark.current
    }
    val colors=liquidAccentScheme(dark,theme.accentColor)
    val accent=if(theme.accentColor=="Custom")
        appOverrideColor(theme.customAccentHex) ?: colors.primary
        else colors.primary
    val appBackground=appOverrideColor(theme.appBackgroundHex)
        ?: if(dark)Color(0xFF14283C) else Color(0xFFEAF4FA)
    val ink=readableAppInk(if(dark)Color.White else Color(0xFF172D42),
        appBackground)
    val outgoing=resolvedBubbleColor(theme,true,dark)
        ?: if(dark)Color(0xFF1F5268) else Color(0xFFD0EDFB)
    val incoming=resolvedBubbleColor(theme,false,dark)
        ?: if(dark)Color(0xFF293F53) else Color(0xFFFAFDFF)

    Column(Modifier.fillMaxWidth().background(appBackground,RoundedCornerShape(18.dp))
        .padding(14.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(34.dp).background(accent,RoundedCornerShape(12.dp)))
            Column(Modifier.weight(1f)) {
                Text("Liquid Chat",color=ink,style=MaterialTheme.typography.titleMedium)
                Text("${theme.appearance} · ${theme.fontPreset} · ${theme.bubbleStyle}",
                    maxLines=1,overflow=TextOverflow.Ellipsis,
                    color=ink.copy(alpha=.75f),style=MaterialTheme.typography.labelSmall)
            }
        }
        Surface(shape=conversationBubbleShape(theme.bubbleStyle,false),
            color=incoming,border=BorderStroke(1.dp,
                if(dark)Color.White.copy(alpha=.1f) else Color.Black.copy(alpha=.06f))) {
            Text("Incoming message preview",
                Modifier.padding(horizontal=12.dp,vertical=9.dp),
                color=readableAppInk(ink,incoming),
                style=MaterialTheme.typography.bodyMedium)
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
            Surface(shape=conversationBubbleShape(theme.bubbleStyle,true),
                color=outgoing,border=BorderStroke(1.dp,
                    if(dark)Color.White.copy(alpha=.1f) else Color.Black.copy(alpha=.06f))) {
                Text("Outgoing message preview ✓✓",
                    Modifier.padding(horizontal=12.dp,vertical=9.dp),
                    color=readableAppInk(ink,outgoing),
                    style=MaterialTheme.typography.bodyMedium)
            }
        }
        HorizontalDivider(color=accent.copy(alpha=.22f))
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp),
            verticalAlignment=Alignment.CenterVertically) {
            listOf(accent,incoming,outgoing).forEach {color->
                Box(Modifier.size(22.dp).background(color,RoundedCornerShape(7.dp)))
            }
            Text("No real chat content included",color=ink.copy(alpha=.7f),
                style=MaterialTheme.typography.labelSmall)
        }
    }
}
