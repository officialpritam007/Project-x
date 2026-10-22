package com.pritam.liquidchat.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.sp
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
        // Capture the old viewport during composition, before stable-key reanchoring
        // moves its index for a batch of newly received messages.
        val followOnAppend=remember(messages.lastOrNull()?.id,typing) {state.firstVisibleItemIndex<=1}
        LaunchedEffect(messages.lastOrNull()?.id,typing) {
            if(followOnAppend&&!state.isScrollInProgress)state.scrollToItem(0)
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
    val dotDuration=when(LocalDisplayOptions.current.animation) {"Normal"->650;"Extra"->330;else->500}
    Surface(shape=Glass.card,color=MaterialTheme.colorScheme.surface,modifier=Modifier.semantics(mergeDescendants=true) {contentDescription="Typing";liveRegion=LiveRegionMode.Polite}) {
        Row(Modifier.padding(horizontal=16.dp,vertical=14.dp),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            val transition=rememberInfiniteTransition(label="Typing")
            repeat(3) {i->
                val opacity=if(reduced)1f else transition.animateFloat(.3f,1f,infiniteRepeatable(tween(dotDuration,delayMillis=i*(dotDuration/4)),RepeatMode.Reverse),label="Dot $i").value
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
/** Keyword aliases are intentionally local: emoji lookup never sends text to a server. */
internal val emojiCategories:Map<String,List<String>> = linkedMapOf(
    "Faces" to listOf("😀","😃","😄","😁","😊","🥰","😍","😘","😎","🥹","😂","🤣","😭","😅","😉","🤔","😴","😢","😡","🤗"),
    "Hearts" to listOf("❤️","🩷","🧡","💛","💚","🩵","💙","💜","🤍","🖤","💖","💕","💗","💞","💘"),
    "Gestures" to listOf("👍","👎","👏","🙌","🙏","🤝","👋","👌","✌️","🤞","🫶","💪","🤙","👀","✨"),
    "More" to listOf("🎉","🎂","🎁","🌹","🌸","🌻","☀️","🌙","⭐","🔥","💯","✅","⚽","🏆","🎵","☕","🍰","🐱","🐶","🦋"),
    // The eight visible reference categories use device-rendered Unicode glyphs.
    // Legacy category keys above remain supported for existing saved/test callers.
    "Smileys & People" to listOf(
        "😀","😃","😄","😁","😆","😅","😂","🤣","😊","🙂","😉","🥲","🥹",
        "😍","🥰","😘","😗","😙","😚","😋","😜","🤪","😎","🥳","🤔",
        "😴","😢","😭","😮","😱","😡","🥺","🤗","😷","👍","👎","👏",
        "🙌","🙏","👋","👌","✌️","🤞","🫶","💪","👨","👩","👶","👵"
    ),
    "Animals & Nature" to listOf(
        "🐶","🐱","🐭","🐹","🐰","🦊","🐻","🐼","🐨","🐯","🦁","🐮",
        "🐷","🐸","🐵","🐔","🐧","🐦","🦆","🦅","🦉","🦋","🐝","🐢",
        "🐍","🐬","🐳","🐟","🌹","🌻","🌷","🌸","🌺","🌲","🌴","🍁",
        "🌈","☀️","🌙","⭐","🔥","❄️"
    ),
    "Food" to listOf(
        "🍎","🍌","🍇","🍉","🍓","🍒","🥭","🍍","🥥","🥝","🍋","🍑",
        "🥑","🥕","🌽","🥦","🍞","🥐","🧀","🥚","🍔","🍟","🍕","🌮",
        "🍣","🍜","🍝","🍛","🍚","🥘","🍰","🎂","🍪","🍩","🍫","🍿",
        "☕","🍵","🧃","🥛","🥤"
    ),
    "Activities" to listOf(
        "⚽","🏀","🏈","⚾","🎾","🏐","🏉","🥏","🎱","🏓","🏸","🥊",
        "🥋","🎯","🎮","🕹️","🎲","♟️","🎳","🏆","🥇","🎖️","🎉",
        "🎊","🎁","🎈","🎤","🎧","🎸","🎹","🥁","🎬","🎨","📷"
    ),
    "Travel & Places" to listOf(
        "🚗","🚕","🚌","🚎","🏎️","🚓","🚑","🚒","🚚","🚲","🏍️",
        "🚂","🚆","🚇","🚉","✈️","🚀","🚁","🚢","⛵","🚦","🚧",
        "⛽","🗺️","🧭","🏖️","🏝️","🏔️","🏕️","🏠","🏡","🏢",
        "🏥","🏫","🏛️","🗼","🌉","🌆","🌇","🌃"
    ),
    "Objects" to listOf(
        "📱","💻","⌨️","🖥️","🖨️","📷","📸","📺","📻","🎙️",
        "⏰","⌚","🔋","💡","🔦","🕯️","💰","💳","💎","🔑",
        "🔒","🔓","🔨","🪛","⚙️","🧰","🧲","📦","📚","📖",
        "✏️","🖊️","📝","📌","📎","✂️","🧹","🛒","🎒"
    ),
    "Symbols" to listOf(
        "❤️","🩷","🧡","💛","💚","🩵","💙","💜","🤍","🖤",
        "💖","💕","💗","💞","💘","✅","❌","❗","❓","⚠️",
        "💯","♻️","🔔","🔕","🔗","➕","➖","➡️","⬅️","⬆️",
        "⬇️","▶️","⏸️","⏹️","🔁","🔄","✔️"
    ),
    "Flags" to listOf(
        "🏳️","🏴","🏁","🚩","🏳️‍🌈","🇮🇳","🇧🇩","🇳🇵","🇧🇹",
        "🇱🇰","🇵🇰","🇺🇸","🇬🇧","🇨🇦","🇦🇺","🇯🇵","🇰🇷",
        "🇨🇳","🇩🇪","🇫🇷","🇮🇹","🇪🇸","🇧🇷","🇦🇷","🇿🇦"
    )
)

private val emojiSearchTerms=mapOf(
    "😀" to "smile happy face","😄" to "grin smile happy","😂" to "laugh tears joy",
    "🤣" to "rolling laughing","🥰" to "love hearts","😍" to "heart eyes love",
    "😭" to "cry tears sad","🤔" to "thinking question","😴" to "sleep tired",
    "❤️" to "heart red love","🩷" to "heart pink love","💙" to "heart blue",
    "👍" to "thumbs up yes like","👎" to "thumbs down dislike",
    "👏" to "clap applause","🙏" to "thanks pray hands",
    "👋" to "wave hello","🫶" to "heart hands love",
    "🎉" to "party celebration confetti","🎂" to "birthday cake",
    "🌹" to "rose flower","☀️" to "sun sunny","🌙" to "moon night",
    "🔥" to "fire hot","⚽" to "football soccer ball","🏆" to "trophy win",
    "🎵" to "music song","☕" to "coffee tea",
    "🐱" to "cat kitten","🐶" to "dog puppy","🦋" to "butterfly"
)

internal fun findEmojis(category:String,query:String):List<String> {
    val term=query.trim().lowercase()
    if(term.isEmpty())return emojiCategories[category].orEmpty()
    return emojiCategories.entries.flatMap {(group,emojis)->
        emojis.filter {emoji->
            emoji.contains(term) || group.lowercase().contains(term) ||
                emojiSearchTerms[emoji]?.contains(term)==true
        }
    }.distinct()
}
internal fun recentEmojiChoices(previous:List<String>,selected:String):List<String> =
    (listOf(selected)+previous.filterNot {it==selected}).take(5)

/** Unicode skin-tone modifiers apply only to glyphs known to support Fitzpatrick tones.
 * An unsupported emoji is returned verbatim, so no broken modifier sequence
 * is inserted into a recipient's message.
 */
internal fun tonedEmoji(base:String,tone:Int):String {
    if(tone !in 1..5 || base !in setOf(
        "👍","👎","👏","🙌","🙏","👋","👌","✌️","🤞","🫶","💪",
        "👨","👩","👶","👵"
    ))return base
    return base.replace("\uFE0F","") + String(Character.toChars(0x1F3FA+tone))
}

private val referenceEmojiCategoryNames=listOf(
    "Smileys & People","Animals & Nature","Food","Activities",
    "Travel & Places","Objects","Symbols","Flags"
)
private val referenceEmojiCategoryIcons=mapOf(
    "Smileys & People" to "😀","Animals & Nature" to "🐻","Food" to "🍎",
    "Activities" to "⚽","Travel & Places" to "🚗","Objects" to "💡",
    "Symbols" to "❤️","Flags" to "🏳️"
)

@Composable internal fun EmojiPicker(onEmoji:(String)->Unit,onDismiss:()->Unit) {
    var category by remember {mutableStateOf("Smileys & People")}
    var query by remember {mutableStateOf("")}
    var skinTone by remember {mutableIntStateOf(0)}
    // Session-only recents: do not persist message-writing habits across logouts.
    var recent by remember {mutableStateOf<List<String>>(emptyList())}
    fun choose(emoji:String) {
        val chosen=tonedEmoji(emoji,skinTone)
        recent=recentEmojiChoices(recent,chosen)
        onEmoji(chosen)
    }
    val emojiRows=remember(category,query) {findEmojis(category,query).chunked(5)}
    val emojiSize=LocalDisplayOptions.current.emojiPickerSize.coerceIn(18,32).sp
    ModalBottomSheet(
        onDismissRequest=onDismiss,
        sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),
        containerColor=MaterialTheme.colorScheme.surface,
        dragHandle={BottomSheetDefaults.DragHandle()}
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal=16.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Emoji",style=MaterialTheme.typography.titleMedium,
                modifier=Modifier.padding(horizontal=4.dp,vertical=4.dp))
            OutlinedTextField(
                value=query,onValueChange={query=it.take(32)},singleLine=true,
                label={Text("Search emoji")},modifier=Modifier.fillMaxWidth().testTag("emoji-search"),
                leadingIcon={Icon(Icons.Outlined.Search,null)},
                trailingIcon={
                    if(query.isNotBlank()) IconButton(onClick={query=""}) {
                        Icon(Icons.Outlined.Close,"Clear emoji search")
                    }
                }
            )
            // Categories remain reachable while a large emoji list is scrolled.
            if(query.isBlank()) LazyRow(
                horizontalArrangement=Arrangement.spacedBy(8.dp),
                contentPadding=PaddingValues(vertical=2.dp)
            ) {
                items(referenceEmojiCategoryNames,key={it}) {name->
                    FilterChip(
                        selected=category==name,
                        onClick={category=name},
                        label={Text("${referenceEmojiCategoryIcons[name].orEmpty()} $name")}
                    )
                }
            }
            if(query.isBlank() && category=="Smileys & People") {
                Column(verticalArrangement=Arrangement.spacedBy(2.dp)) {
                    Text("Skin tone for supported people and hand emojis",
                        style=MaterialTheme.typography.labelSmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement=Arrangement.spacedBy(1.dp)) {
                        (0..5).forEach {tone->
                            val sample=if(tone==0)"👍" else tonedEmoji("👍",tone)
                            FilterChip(
                                selected=skinTone==tone,
                                onClick={skinTone=tone},
                                label={Text(sample)},
                                modifier=Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            // Lazy rows avoid composing every emoji as the keyboard opens.
            LazyColumn(
                modifier=Modifier.fillMaxWidth().heightIn(max=430.dp),
                verticalArrangement=Arrangement.spacedBy(4.dp),
                contentPadding=PaddingValues(bottom=6.dp)
            ) {
                if(query.isBlank()) {
                    item(key="quick-reactions") {
                        Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                            Text("Quick reactions",style=MaterialTheme.typography.labelMedium)
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(1.dp)) {
                                MbQuickReactions.defaults.forEach {emoji->
                                    TextButton(onClick={choose(emoji)},
                                        modifier=Modifier.weight(1f).heightIn(min=48.dp),
                                        contentPadding=PaddingValues(0.dp)) {Text(emoji,fontSize=emojiSize)}
                                }
                            }
                        }
                    }
                    if(recent.isNotEmpty()) item(key="recent-emoji") {
                        Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                            Text("Recently picked",style=MaterialTheme.typography.labelMedium)
                            Row(Modifier.fillMaxWidth()) {
                                recent.forEach {emoji->
                                    TextButton(onClick={choose(emoji)},
                                        modifier=Modifier.weight(1f).heightIn(min=48.dp),
                                        contentPadding=PaddingValues(0.dp)) {Text(emoji,fontSize=emojiSize)}
                                }
                            }
                        }
                    }
                }
                if(emojiRows.isEmpty())item(key="no-emojis") {
                    Text("No matching emoji",style=MaterialTheme.typography.bodyMedium,
                        modifier=Modifier.padding(vertical=12.dp))
                }
                itemsIndexed(emojiRows,key={index,_->"emoji-row-$index"}) {_,row->
                    Row(Modifier.fillMaxWidth()) {
                        repeat(5) {index->
                            if(index<row.size) TextButton(
                                onClick={choose(row[index])},
                                modifier=Modifier.weight(1f).heightIn(min=48.dp),
                                contentPadding=PaddingValues(0.dp)
                            ) {Text(row[index],fontSize=emojiSize)}
                            else Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            TextButton(onClick=onDismiss,
                modifier=Modifier.align(Alignment.End)) {Text("Done")}
        }
        Spacer(Modifier.height(8.dp))
    }
}
