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
