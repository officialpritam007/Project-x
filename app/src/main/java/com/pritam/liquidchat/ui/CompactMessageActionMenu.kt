package com.pritam.liquidchat.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Lightweight, theme-aware floating message palette. The full action list stays
 * accessible through More, while the everyday actions remain one tap away.
 */
@Composable internal fun CompactMessageActionMenu(
    preview:String?,
    reactions:List<String>,
    canReply:Boolean,
    canCopy:Boolean,
    canForward:Boolean,
    canStar:Boolean,
    starred:Boolean,
    canDelete:Boolean,
    onReact:(String)->Unit,
    onReply:()->Unit,
    onCopy:()->Unit,
    onForward:()->Unit,
    onStar:()->Unit,
    onSelect:()->Unit,
    onDelete:()->Unit,
    onMore:()->Unit,
    onDismiss:()->Unit
) {
    Dialog(onDismissRequest=onDismiss,
        properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Box(
            Modifier.fillMaxSize().padding(WindowInsets.safeDrawing.asPaddingValues()),
            contentAlignment=Alignment.Center
        ) {
            GlassSurface(Modifier.fillMaxWidth().widthIn(max=370.dp)
                .padding(horizontal=20.dp),Glass.dialog) {
                Column(Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("Message actions",style=MaterialTheme.typography.titleSmall,
                        modifier=Modifier.padding(start=5.dp))
                    if(!preview.isNullOrBlank()) Text(preview,maxLines=1,
                        overflow=TextOverflow.Ellipsis,
                        style=MaterialTheme.typography.bodySmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier=Modifier.padding(horizontal=5.dp))
                    if(reactions.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement=Arrangement.SpaceEvenly) {
                            reactions.forEach {emoji->
                                TextButton(onClick={onReact(emoji)},modifier=Modifier.size(48.dp),
                                    contentPadding=PaddingValues(0.dp)) {
                                    Text(emoji,style=MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        CompactMessageAction("Reply",Icons.Outlined.Reply,onReply,
                            modifier=Modifier.weight(1f),enabled=canReply)
                        CompactMessageAction("Copy",Icons.Outlined.ContentCopy,onCopy,
                            modifier=Modifier.weight(1f),enabled=canCopy)
                        CompactMessageAction("Forward",Icons.Outlined.Forward,onForward,
                            modifier=Modifier.weight(1f),enabled=canForward)
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        CompactMessageAction(if(starred)"Unstar" else "Star",
                            Icons.Outlined.StarOutline,onStar,modifier=Modifier.weight(1f),
                            enabled=canStar)
                        CompactMessageAction("Select",Icons.Outlined.CheckBox,onSelect,
                            modifier=Modifier.weight(1f),enabled=canForward)
                        CompactMessageAction("More",Icons.Outlined.MoreHoriz,onMore,
                            modifier=Modifier.weight(1f))
                    }
                    if(canDelete) {
                        HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
                        TextButton(onClick=onDelete,
                            modifier=Modifier.fillMaxWidth().heightIn(min=44.dp)) {
                            Icon(Icons.Outlined.DeleteOutline,null,
                                tint=MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text("Delete message",color=MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
