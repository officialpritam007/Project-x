package com.pritam.liquidchat.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.pritam.liquidchat.data.Delivery
import com.pritam.liquidchat.data.Message
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.launch

internal fun messageDate(message:Message):LocalDate =
    Instant.ofEpochMilli(message.localTime).atZone(ZoneId.systemDefault()).toLocalDate()
internal fun dateLabel(date:LocalDate,today:LocalDate=LocalDate.now()):String = when(date) {
    today->"Today";today.minusDays(1)->"Yesterday"
    else->date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
}
internal fun wallpaper(palette:String,dark:Boolean):Brush = Brush.linearGradient(when(palette) {
    "Lilac"->if(dark)listOf(Color(0xFF29223F),Color(0xFF192C40)) else listOf(Color(0xFFE9DFF8),Color(0xFFDCEBF8),Color(0xFFF2E7F9))
    "Mint"->if(dark)listOf(Color(0xFF173B36),Color(0xFF182C32)) else listOf(Color(0xFFD0EEE4),Color(0xFFE5F4EC),Color(0xFFCCE8ED))
    "Sand"->if(dark)listOf(Color(0xFF383024),Color(0xFF242D33)) else listOf(Color(0xFFF6EAD8),Color(0xFFF3EDE3),Color(0xFFE6E7E4))
    else->if(dark)listOf(Color(0xFF122D40),Color(0xFF233D48),Color(0xFF153846)) else listOf(Color(0xFFD3EBF7),Color(0xFFEEF6FC),Color(0xFFCFE9F0))
})

/** Reverse layout anchors newest at zero; stable message keys preserve older viewport offsets. */
@Composable internal fun MessageTimeline(
    cid:String,messages:List<Message>,typing:Boolean,focus:String?,onFocused:()->Unit,
    loadOlder:suspend ()->Boolean,modifier:Modifier=Modifier,
    bubble:@Composable (Message)->Unit
) {
    key(cid) {
        val state=rememberLazyListState()
        val scope=rememberCoroutineScope()
        var hasOlder by remember {mutableStateOf(true)}
        var loading by remember {mutableStateOf(false)}
        var failed by remember {mutableStateOf(false)}
        val latestLoader by rememberUpdatedState(loadOlder)
        val reversed=remember(messages) {messages.asReversed()}
        val nearBottom by remember {derivedStateOf {state.firstVisibleItemIndex<=1}}
        val atHistory by remember(messages.size) {derivedStateOf {
            state.layoutInfo.visibleItemsInfo.lastOrNull()?.index?.let {it>=messages.size-3}==true
        }}
        suspend fun older() {
            if(loading||!hasOlder)return
            loading=true;failed=false
            try {hasOlder=latestLoader()} catch(e:kotlinx.coroutines.CancellationException){throw e}
            catch(_:Exception){failed=true} finally {loading=false}
        }
        LaunchedEffect(atHistory,messages.firstOrNull()?.id) {
            if(atHistory&&messages.isNotEmpty()&&!failed)older()
        }
        LaunchedEffect(messages.lastOrNull()?.id,typing) {
            if(nearBottom&&!state.isScrollInProgress)state.scrollToItem(0)
        }
        LaunchedEffect(focus,messages) {
            if(focus!=null) {
                val index=reversed.indexOfFirst {it.id==focus}
                if(index>=0) {state.scrollToItem(index+if(typing)1 else 0);onFocused()}
            }
        }
        Box(modifier) {
            LazyColumn(state=state,reverseLayout=true,modifier=Modifier.fillMaxSize().testTag("message-timeline"),contentPadding=PaddingValues(horizontal=12.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                if(typing)item(key="typing") {TypingBubble()}
                itemsIndexed(reversed,key={_,m->m.id},contentType={_,_->"message"}) {index,m->
                    val preceding=reversed.getOrNull(index+1)
                    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        if(preceding==null||messageDate(preceding)!=messageDate(m)) Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center) {
                            Surface(shape=Glass.capsule,color=MaterialTheme.colorScheme.surface.copy(alpha=.85f)) {
                                Text(dateLabel(messageDate(m)),Modifier.padding(horizontal=14.dp,vertical=6.dp),style=MaterialTheme.typography.labelMedium)
                            }
                        }
                        bubble(m)
                    }
                }
                item(key="history") {
                    Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center) {
                        when {
                            messages.isEmpty()->Text("Say hello. Your messages will appear here.",style=MaterialTheme.typography.bodyMedium)
                            loading->CircularProgressIndicator(Modifier.size(24.dp))
                            failed->TextButton(onClick={scope.launch {older()}}) {Text("Retry older messages")}
                            !hasOlder->Text("Beginning of conversation",style=MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            if(!nearBottom) SmallFloatingActionButton(onClick={scope.launch {state.animateScrollToItem(0)}},modifier=Modifier.align(Alignment.BottomEnd).padding(8.dp),containerColor=MaterialTheme.colorScheme.surface) {
                Icon(Icons.Outlined.KeyboardArrowDown,"Jump to latest messages")
            }
        }
    }
}

@Composable internal fun TypingBubble() {
    val reduced=LocalDisplayOptions.current.reducedMotion
    Surface(shape=Glass.card,color=MaterialTheme.colorScheme.surface,modifier=Modifier.semantics(mergeDescendants=true) {contentDescription="Typing";liveRegion=LiveRegionMode.Polite}) {
        Row(Modifier.padding(horizontal=16.dp,vertical=14.dp),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            val transition=rememberInfiniteTransition(label="Typing")
            repeat(3) {i->
                val opacity=if(reduced)1f else transition.animateFloat(.3f,1f,infiniteRepeatable(tween(500,delayMillis=i*120),RepeatMode.Reverse),label="Dot $i").value
                Box(Modifier.size(7.dp).alpha(opacity).background(MaterialTheme.colorScheme.onSurfaceVariant,CircleShape))
            }
        }
    }
}
@Composable internal fun Receipt(status:String,seq:Long,delivered:Long,read:Long) {
    val label=Delivery.label(status,seq,delivered,read)
    val icon=when(label) {"Read","Delivered"->Icons.Outlined.DoneAll;"Sent"->Icons.Outlined.Done;"Failed"->Icons.Outlined.ErrorOutline;else->Icons.Outlined.Schedule}
    Icon(icon,contentDescription=label,modifier=Modifier.size(16.dp),tint=if(label=="Read")MaterialTheme.colorScheme.primary else if(label=="Failed")MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
}
@Composable internal fun EmojiPicker(onEmoji:(String)->Unit,onDismiss:()->Unit) {
    val categories=remember { linkedMapOf(
        "Faces" to listOf("😀","😃","😄","😁","😊","🥰","😍","😘","😎","🥹","😂","🤣","😭","😅","😉","🤔","😴","😢","😡","🤗"),
        "Hearts" to listOf("❤️","🩷","🧡","💛","💚","🩵","💙","💜","🤍","🖤","💖","💕","💗","💞","💘"),
        "Gestures" to listOf("👍","👎","👏","🙌","🙏","🤝","👋","👌","✌️","🤞","🫶","💪","🤙","👀","✨"),
        "More" to listOf("🎉","🎂","🎁","🌹","🌸","🌻","☀️","🌙","⭐","🔥","💯","✅","⚽","🏆","🎵","☕","🍰","🐱","🐶","🦋")) }
    var category by remember {mutableStateOf("Faces")}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Emoji")},text={Column {
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)) {categories.keys.forEach {name->FilterChip(category==name,{category=name},label={Text(name)})}}
        categories.getValue(category).chunked(5).forEach {row->Row {row.forEach {emoji->TextButton(onClick={onEmoji(emoji)},modifier=Modifier.weight(1f).heightIn(min=48.dp),contentPadding=PaddingValues(0.dp)) {Text(emoji,style=MaterialTheme.typography.titleLarge)}}}}
    }},confirmButton={TextButton(onClick=onDismiss) {Text("Done")}})
}
