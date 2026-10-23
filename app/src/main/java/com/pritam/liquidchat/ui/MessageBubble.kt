package com.pritam.liquidchat.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.ui.platform.LocalContext
import com.pritam.liquidchat.data.Images
import com.pritam.liquidchat.data.StickerFactory
import com.pritam.liquidchat.data.StickerLibrary
import com.pritam.liquidchat.data.PdfDocuments
import com.pritam.liquidchat.data.PreparedPdf
import com.pritam.liquidchat.data.PreparedChatVideo
import com.pritam.liquidchat.data.ChatVideo
import com.pritam.liquidchat.data.StoryVideo
import com.pritam.liquidchat.data.SavedMedia
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.pritam.liquidchat.AppViewModel
import com.pritam.liquidchat.data.Chat
import com.pritam.liquidchat.data.Message
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable internal fun MessageBubble(message:Message,chat:Chat?,model:AppViewModel,onRetry:()->Unit,highlight:Boolean,onLongClick:()->Unit={},onReply:()->Unit={},quoted:Message?=null,onQuoteClick:()->Unit={},selectionMode:Boolean=false,onSelect:()->Unit={},onDoubleTap:()->Unit={}) {
    val uploadFlow=remember(model,message.account,message.id) {model.repository.uploadProgress(message.account,message.id)}
    val uploadPercent by uploadFlow.collectAsStateWithLifecycle(initialValue=null)
    val mine=message.sender==message.account
    val isSticker=message.mediaKind=="sticker"&&message.deletedAt.isEmpty()
    val bubbleDisplay=LocalDisplayOptions.current
    val referenceBubble=referenceMessageChrome(bubbleDisplay)
    var swipe by remember(message.id) {mutableFloatStateOf(0f)}
    val threshold=with(LocalDensity.current) {72.dp.toPx()}
    val replyCallback by rememberUpdatedState(onReply)
    val rtl=LocalLayoutDirection.current==LayoutDirection.Rtl
    val haptic=LocalHapticFeedback.current
    var crossed by remember(message.id) {mutableStateOf(false)}
    val drag=rememberDraggableState {delta->
        swipe=(swipe+if(rtl)-delta else delta).coerceIn(0f,threshold*1.3f)
        if(swipe>=threshold&&!crossed) {crossed=true;haptic.performHapticFeedback(HapticFeedbackType.LongPress)}
    }
    val replySwipeOffset by animateFloatAsState(
        targetValue=swipe,
        animationSpec=if(bubbleDisplay.reducedMotion) snap()
            else spring(dampingRatio=Spring.DampingRatioMediumBouncy,
                stiffness=Spring.StiffnessHigh),
        label="conversation reply gesture")
    var expanded by remember(message.id) {mutableStateOf(false)}
    var truncated by remember(message.id) {mutableStateOf(false)}
    BoxWithConstraints(Modifier.fillMaxWidth()) {
    val bubbleWidth=minOf(maxWidth*(if(referenceBubble).84f else .86f),480.dp)
    if(message.status=="SENT" && message.deletedAt.isEmpty() &&
        replySwipeOffset>3f) {
        Icon(Icons.Outlined.Reply,contentDescription="Swipe to reply",
            modifier=Modifier.align(if(rtl)Alignment.CenterEnd else Alignment.CenterStart)
                .padding(horizontal=12.dp).size(26.dp)
                .graphicsLayer {
                    val strength=(replySwipeOffset/threshold).coerceIn(0f,1f)
                    alpha=strength
                    scaleX=.7f+.3f*strength
                    scaleY=.7f+.3f*strength
                },
            tint=MaterialTheme.colorScheme.primary)
    }
    Row(Modifier.fillMaxWidth().offset {
        androidx.compose.ui.unit.IntOffset((if(rtl)-replySwipeOffset else replySwipeOffset).toInt(),0)
    }.draggable(drag,Orientation.Horizontal,
        enabled=message.status=="SENT"&&message.deletedAt.isEmpty(),
        onDragStopped={
            if(swipe>=threshold)replyCallback()
            swipe=0f
            crossed=false
        }),
        horizontalArrangement=if(mine)Arrangement.End else Arrangement.Start) {
        Surface(
            shape=conversationBubbleShape(bubbleDisplay.bubbleStyle,mine),
            color=if(highlight)MaterialTheme.colorScheme.tertiaryContainer
                else if(isSticker)Color.Transparent else bubbleSurfaceColor(bubbleDisplay,mine,LocalGlassDark.current,
                    if(LocalGlassDark.current) {if(mine)Color(0xFF1F5268) else Color(0xFF293F53)}
                    else if(mine)Color(0xFFD0EDFB) else Color(0xFFFAFDFF)),
            border=if(isSticker||referenceBubble)null else androidx.compose.foundation.BorderStroke(
                1.dp,Color.White.copy(alpha=if(LocalGlassDark.current).19f else .8f)),
            shadowElevation=if(isSticker||referenceBubble)0.dp else 2.dp,
            modifier=Modifier.widthIn(max=bubbleWidth)
                .then(if(isSticker||referenceBubble)Modifier else Modifier.glassSource())
                .combinedClickable(onClick={if(selectionMode)onSelect()},
                    onDoubleClick=if(canDoubleTapReact(message,bubbleDisplay.doubleTapReactionEnabled,
                        selectionMode,model.session.value?.userId))onDoubleTap else null,
                    onLongClick={if(selectionMode)onSelect() else onLongClick()})
        ) {
            Column(Modifier.padding(
                horizontal=if(isSticker)0.dp else if(referenceBubble)9.dp else 11.dp,
                vertical=if(isSticker)2.dp else if(referenceBubble)6.dp else 9.dp)) {
                if(message.deletedAt.isNotEmpty()) {
                    Text("This message was deleted",
                        style=MaterialTheme.typography.bodyMedium,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                if(message.replyTo.isNotEmpty()) Surface(
                    shape=if(referenceBubble)androidx.compose.foundation.shape.RoundedCornerShape(8.dp) else Glass.compact,
                    color=MaterialTheme.colorScheme.primary.copy(alpha=.1f),
                    modifier=Modifier.fillMaxWidth().clickable(onClick=onQuoteClick)) {
                    Column(Modifier.padding(8.dp)) {Text("Reply",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary);Text(quoted?.takeIf {it.deletedAt.isEmpty()}?.body ?: "Original message unavailable",maxLines=2,overflow=TextOverflow.Ellipsis,
                        color=conversationInkColor(bubbleDisplay.quotedInk,LocalGlassDark.current,
                            resolvedBubbleTextColor(bubbleDisplay,mine,LocalGlassDark.current,MaterialTheme.colorScheme.onSurface),bubbleDisplay.quotedInkHex),
                        style=MaterialTheme.typography.bodySmall)}
                }
                if(message.mediaId.isNotEmpty()||message.localImage.isNotEmpty()) {
                    when(message.mediaKind) {
                        "voice"->VoicePlayer(model,message)
                        "document"->PdfMessageAttachment(model,message)
                        "chat_video"->ChatVideoAttachment(model,message)
                        else->SharedImagePreview(model,message)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if(message.mediaKind !in listOf("voice","document") &&
                    !(message.mediaKind=="chat_video" && message.body=="Video") &&
                    !(message.mediaKind=="sticker" && message.body=="Sticker"))MessageBodyText(message.body,bubbleDisplay.messageFontSize,expanded,
                    linksEnabled=!selectionMode,
                    textColor=conversationInkColor(if(mine)bubbleDisplay.outgoingInk else bubbleDisplay.incomingInk,
                        LocalGlassDark.current,resolvedBubbleTextColor(bubbleDisplay,mine,LocalGlassDark.current,MaterialTheme.colorScheme.onSurface),
                        if(mine)bubbleDisplay.outgoingInkHex else bubbleDisplay.incomingInkHex),
                    onTextLayout={truncated=it.hasVisualOverflow})
                if(message.status=="SENDING")uploadPercent?.let {percent->
                    LinearProgressIndicator(progress={percent/100f},modifier=Modifier.width(160.dp))
                    Text(if(percent<100)"Uploading · $percent%" else "Processing attachment…",style=MaterialTheme.typography.labelSmall)
                }
                if(truncated||expanded) TextButton(onClick={expanded=!expanded}) {Text(if(expanded)"Show less" else "Read more")}
                }
                val time=remember(message.localTime) {java.time.Instant.ofEpochMilli(message.localTime).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}
                Row(Modifier.align(Alignment.End),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    if(message.starred&&message.deletedAt.isEmpty())Icon(Icons.Outlined.Star,"Starred",Modifier.size(12.dp))
                    if(message.deletedAt.isEmpty())editedMessageMarker(
                        message.editedAt,bubbleDisplay.showEditedLabel,bubbleDisplay.showEditedTime
                    )?.let {label->Text(label,style=MaterialTheme.typography.labelSmall)}
                    Text(time,style=MaterialTheme.typography.labelSmall,
                        color=conversationInkColor(bubbleDisplay.timestampInk,LocalGlassDark.current,
                            resolvedBubbleTextColor(bubbleDisplay,mine,LocalGlassDark.current,MaterialTheme.colorScheme.onSurfaceVariant),bubbleDisplay.timestampInkHex))
                    if(mine)Receipt(message.status,message.seq,chat?.peerDelivered ?: 0,chat?.peerRead ?: 0)
                }
                if(message.reactions!="{}"&&message.deletedAt.isEmpty()) {
                    val reactions=remember(message.reactions) {runCatching {val j=org.json.JSONObject(message.reactions);j.keys().asSequence().map {j.getString(it)}.groupingBy {it}.eachCount()}.getOrDefault(emptyMap())}
                    val own=remember(message.reactions,message.account) {runCatching {org.json.JSONObject(message.reactions).optString(message.account)}.getOrDefault("")}
                    Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        reactions.forEach {(emoji,count)->
                            val allowed=message.status=="SENT" &&
                                model.session.value?.userId==message.account &&
                                !selectionMode && message.deletedAt.isEmpty()
                            Surface(
                                onClick={
                                    model.action {
                                        // A stale bubble cannot react after a deletion,
                                        // account change, or a superseding message update.
                                        val latest=model.messages.value.firstOrNull {
                                            it.id==message.id && it.account==message.account &&
                                                it.conversation==message.conversation
                                        }
                                        if(latest!=null && latest.status=="SENT" &&
                                            latest.deletedAt.isEmpty() &&
                                            model.session.value?.userId==message.account) {
                                            val previous=runCatching {
                                                org.json.JSONObject(latest.reactions)
                                                    .optString(message.account)
                                            }.getOrDefault("")
                                            model.repository.messageAction(
                                                latest.id,"react",
                                                if(previous==emoji)"" else emoji)
                                        }
                                    }
                                },
                                enabled=allowed,
                                shape=Glass.capsule,
                                border=if(emoji==own)androidx.compose.foundation.BorderStroke(
                                    1.dp,MaterialTheme.colorScheme.primary) else null,
                                color=MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text("$emoji $count",
                                    Modifier.padding(horizontal=8.dp,vertical=3.dp),
                                    style=MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                if(message.status=="FAILED") {Text(message.error,style=MaterialTheme.typography.bodySmall);TextButton(onClick=onRetry) {Text("Retry")}}
            }
        }
    }
    }
}
