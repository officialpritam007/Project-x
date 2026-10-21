package com.pritam.liquidchat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pritam.liquidchat.data.Message
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * MB-style long-press peek: deliberately renders only downloaded metadata,
 * never signed URLs, media files, private thumbnails or deleted quoted text.
 */
@Composable internal fun MbConversationPeekRow(message:Message,contactName:String) {
    val mine=message.sender==message.account
    val display=LocalDisplayOptions.current
    val dark=LocalGlassDark.current
    val surface=bubbleSurfaceColor(display,mine,dark,
        if(dark) {
            if(mine)Color(0xFF1F5268) else Color(0xFF293F53)
        } else {
            if(mine)Color(0xFFD0EDFB) else Color(0xFFFAFDFF)
        })
    val safe=when(message.mediaKind) {
        "voice"->"Voice message"
        "sticker"->"Sticker"
        "document"->message.body.take(160)
        "chat_video"->if(message.body=="Video")"Private video"
            else "Video: ${message.body.take(160)}"
        else->if(message.mediaId.isNotEmpty()||message.localImage.isNotEmpty())
            "Photo message" else message.body.take(220)
    }
    val icon=when(message.mediaKind) {
        "voice"->Icons.Outlined.MicNone
        "sticker"->Icons.Outlined.AutoAwesome
        "document"->Icons.Outlined.Description
        "chat_video"->Icons.Outlined.Videocam
        else->Icons.Outlined.Image
    }
    val time=DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(message.localTime))
    Row(Modifier.fillMaxWidth(),
        horizontalArrangement=if(mine)Arrangement.End else Arrangement.Start) {
        Surface(shape=conversationBubbleShape(display.bubbleStyle,mine),
            color=surface,modifier=Modifier.widthIn(max=300.dp)) {
            Column(Modifier.padding(horizontal=11.dp,vertical=7.dp),
                verticalArrangement=Arrangement.spacedBy(3.dp)) {
                Text(if(mine)"You" else contactName.take(80),
                    style=MaterialTheme.typography.labelSmall,
                    color=MaterialTheme.colorScheme.primary,
                    maxLines=1,overflow=TextOverflow.Ellipsis)
                Row(verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                    if(message.mediaKind!="image" ||
                        message.mediaId.isNotEmpty() || message.localImage.isNotEmpty()) {
                        Icon(icon,contentDescription=null,modifier=Modifier.size(15.dp))
                    }
                    Text(safe,maxLines=2,overflow=TextOverflow.Ellipsis,
                        style=MaterialTheme.typography.bodyMedium)
                }
                Text(time,modifier=Modifier.align(Alignment.End),
                    style=MaterialTheme.typography.labelSmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
