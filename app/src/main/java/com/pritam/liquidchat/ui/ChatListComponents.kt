package com.pritam.liquidchat.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pritam.liquidchat.data.Chat

@Composable fun Avatar(name:String,mediaId:String="",backend:com.pritam.liquidchat.data.Backend?=null,self:Boolean=false,size:androidx.compose.ui.unit.Dp=48.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(if(self)Color(0xFF07B4D8) else Color(0xFFB94799),CircleShape),contentAlignment=Alignment.Center) {
        if(mediaId.isNotEmpty()&&backend!=null) ProtectedImage(backend,mediaId,description="Profile photo for $name",modifier=Modifier.fillMaxSize(),scale=androidx.compose.ui.layout.ContentScale.Crop)
        else Text(if(self)name.split(" ").filter {it.firstOrNull()?.isLetter()==true}.take(2).joinToString(""){it.take(1)}.uppercase() else name.take(1).uppercase(),style=MaterialTheme.typography.titleLarge,color=Color.White)
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable fun ChatRow(chat:Chat,backend:com.pritam.liquidchat.data.Backend,onClick:()->Unit,onLongClick:()->Unit={}) {
    val homeDisplay=LocalDisplayOptions.current
    val reference=referenceListUsesFlatRows(homeDisplay)
    Surface(
        shape=if(reference)androidx.compose.ui.graphics.RectangleShape else Glass.card,
        color=if(reference)MaterialTheme.colorScheme.surface.copy(
            alpha=if(LocalGlassDark.current).72f else .86f)
            else if(LocalGlassDark.current)Color(0xFF20384D).copy(alpha=.62f)
            else Color.White.copy(alpha=.56f),
        border=if(reference)null else androidx.compose.foundation.BorderStroke(
            1.dp,Color.White.copy(alpha=if(LocalGlassDark.current).17f else .72f)),
        modifier=Modifier.fillMaxWidth()
            .then(if(reference)Modifier else Modifier.glassSource())
            .combinedClickable(onClick=onClick,onLongClick=onLongClick)
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical=homeDisplay.homeRowPadding.dp,horizontal=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Avatar(chat.name,chat.avatarId,backend,size=homeDisplay.homeAvatarSize.dp)
            Column(Modifier.weight(1f)) {
                Text(chat.name,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium.copy(fontSize=homeDisplay.homeChatNameSize.sp),maxLines=1,overflow=TextOverflow.Ellipsis)
                Text(inboxSnippet(chat.preview,"Say hello to @${chat.username}",homeDisplay.hideHomePreviews),style=MaterialTheme.typography.bodyMedium.copy(fontSize=homeDisplay.homeChatPreviewSize.sp),maxLines=1,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment=Alignment.End,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                if(chat.lastAt.isNotEmpty()) Text(remember(chat.lastAt) {runCatching {java.time.Instant.parse(chat.lastAt).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}.getOrDefault("")},style=MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    if(homeDisplay.showPinBadges && chat.pinnedAt>0)Icon(Icons.Outlined.PushPin,"Pinned",Modifier.size(14.dp))
                    if(chat.favorite)Icon(Icons.Outlined.Star,"Favorite",Modifier.size(14.dp))
                    if(chat.muted)Icon(Icons.Outlined.NotificationsOff,"Muted",Modifier.size(14.dp))
                }
                if(chat.unread>0) Badge {Text(if(chat.unread>99)"99+" else chat.unread.toString())}
                else if(chat.manualUnread)Badge(Modifier.semantics {contentDescription="Marked unread"})
            }
        }
    }
}
