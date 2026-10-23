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
                            GlassSurface(shape=Glass.capsule) {
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
            if(!nearBottom) GlassSurface(Modifier.align(Alignment.BottomEnd).padding(8.dp),Glass.capsule) {
                IconButton(onClick={scope.launch {state.animateScrollToItem(0)}}) {
                    Icon(Icons.Outlined.KeyboardArrowDown,"Jump to latest messages")
                }
            }
        }
    }
}

@Composable internal fun TypingBubble() {
    val reduced=LocalDisplayOptions.current.reducedMotion
    val dotDuration=when(LocalDisplayOptions.current.animation) {"Normal"->650;"Extra"->330;else->500}
    GlassSurface(Modifier.semantics(mergeDescendants=true) {contentDescription="Typing";liveRegion=LiveRegionMode.Polite},Glass.card) {
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

/**
 * Recent emoji are deliberately in-memory and keyed by signed-in account.
 * The library's default provider persists recents app-wide, which would expose
 * one user's writing habits after a different account signs in.
 */
private object AccountEmojiRecents {
    private val recent=linkedMapOf<String,List<String>>()
    @Synchronized fun list(account:String):List<String> = recent[account].orEmpty()
    @Synchronized fun add(account:String,emoji:String) {
        recent[account]=(listOf(emoji)+recent[account].orEmpty().filterNot {it==emoji}).take(24)
        while(recent.size>4) recent.remove(recent.keys.first())
    }
}

@Composable internal fun EmojiPicker(
    onEmoji:(String)->Unit,
    onDismiss:()->Unit,
    onSavedStickers:(()->Unit)?=null,
    accountKey:String=""
) {
    var query by remember {mutableStateOf("")}
    val safeAccount=accountKey.ifBlank {"current-session"}
    val provider=remember(safeAccount) {
        object:androidx.emoji2.emojipicker.RecentEmojiProvider {
            override suspend fun getRecentEmojiList():List<String> =
                AccountEmojiRecents.list(safeAccount)
            override fun recordSelection(emoji:String) {
                AccountEmojiRecents.add(safeAccount,emoji)
            }
        }
    }
    ModalBottomSheet(
        onDismissRequest=onDismiss,
        sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),
        containerColor=glassPopupColor(),
        dragHandle={BottomSheetDefaults.DragHandle()}
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal=12.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("Emoji",style=MaterialTheme.typography.titleMedium,
                    modifier=Modifier.weight(1f).padding(start=8.dp))
                if(onSavedStickers!=null) TextButton(onClick=onSavedStickers,
                    modifier=Modifier.testTag("stickers-tab")) {
                    Icon(Icons.Outlined.AutoAwesome,null)
                    Spacer(Modifier.width(4.dp))
                    Text("My stickers")
                }
                IconButton(onClick=onDismiss) {Icon(Icons.Outlined.Close,"Close emoji picker")}
            }
            OutlinedTextField(query,{query=it.take(32)},singleLine=true,
                label={Text("Search emoji")},leadingIcon={Icon(Icons.Outlined.Search,null)},
                modifier=Modifier.fillMaxWidth().testTag("emoji-search"),
                trailingIcon={if(query.isNotBlank()) IconButton(onClick={query=""}) {
                    Icon(Icons.Outlined.Close,"Clear emoji search")
                }})
            if(query.isBlank()) {
                // Official AndroidX catalogue supplies current emoji categories,
                // recents and long-press tone / variant selectors.
                androidx.compose.ui.viewinterop.AndroidView(
                    factory={context->
                        androidx.emoji2.emojipicker.EmojiPickerView(context).apply {
                            emojiGridColumns=8
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            setRecentEmojiProvider(provider)
                            setOnEmojiPickedListener {item->onEmoji(item.emoji)}
                        }
                    },
                    update={view->view.setOnEmojiPickedListener {item->onEmoji(item.emoji)}},
                    modifier=Modifier.fillMaxWidth().height(390.dp)
                        .testTag("emoji-categories")
                )
            } else {
                // Search complements the native picker without transmitting typed
                // queries to a server. Existing keyword aliases remain supported.
                val matching=remember(query) {findEmojis("Smileys & People",query)}
                LazyColumn(Modifier.fillMaxWidth().heightIn(max=390.dp)) {
                    if(matching.isEmpty()) item {Text("No matching emoji",
                        modifier=Modifier.padding(16.dp))}
                    itemsIndexed(matching.chunked(6)) {_,line->
                        Row(Modifier.fillMaxWidth()) {
                            line.forEach {emoji->
                                TextButton(onClick={
                                    provider.recordSelection(emoji)
                                    onEmoji(emoji)
                                },modifier=Modifier.weight(1f).heightIn(min=48.dp)) {
                                    Text(emoji,fontSize=25.sp)
                                }
                            }
                            repeat(6-line.size) {Spacer(Modifier.weight(1f))}
                        }
                    }
                }
            }
        }
        Spacer(Modifier.navigationBarsPadding().height(8.dp))
    }
}
