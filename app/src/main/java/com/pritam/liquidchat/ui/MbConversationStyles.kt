package com.pritam.liquidchat.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * MB reference conversation customization, kept independent of message delivery.
 * Legacy "Tail" + "Original" reproduces the pre-customization Liquid Chat bubble.
 */
/** Styling-only reference switch; conversation state never depends on presentation. */
internal fun referenceMessageChrome(display:DisplayOptions):Boolean =
    referenceListUsesFlatRows(display)

/** Each visual setting affects actual incoming and outgoing chat bubbles,
 * not just the sample picker. Negative/concave geometry from the foreign
 * app is deliberately approximated until matching rendered screenshots exist.
 */
internal val MB_RECORDED_BUBBLE_STYLES=listOf(
    "iOS","Style 1","Style 2","Style 3","Style 4","Style 5",
    "Style 6","Style 7","Style 8","Style 9","Style 10","Style 11"
)

internal val LIQUID_BUBBLE_STYLES = setOf("Tail", "Rounded", "Compact") + MB_RECORDED_BUBBLE_STYLES

internal fun normalizedBubbleStyle(value:String?):String =
    value?.takeIf {it in LIQUID_BUBBLE_STYLES} ?: "Tail"

internal fun conversationBubbleShape(style:String,mine:Boolean):RoundedCornerShape = when(style) {
    "Rounded" -> RoundedCornerShape(22.dp)
    "Compact" -> RoundedCornerShape(11.dp)
    "iOS" -> RoundedCornerShape(
        topStart=19.dp,topEnd=19.dp,
        bottomStart=if(mine)19.dp else 5.dp,
        bottomEnd=if(mine)5.dp else 19.dp)
    "Style 1" -> RoundedCornerShape(
        topStart=8.dp,topEnd=20.dp,
        bottomStart=if(mine)8.dp else 2.dp,
        bottomEnd=if(mine)2.dp else 20.dp)
    "Style 2" -> RoundedCornerShape(
        topStart=25.dp,topEnd=25.dp,
        bottomStart=if(mine)25.dp else 1.dp,
        bottomEnd=if(mine)1.dp else 25.dp)
    "Style 3" -> RoundedCornerShape(
        topStart=5.dp,topEnd=18.dp,
        bottomStart=if(mine)18.dp else 4.dp,
        bottomEnd=if(mine)4.dp else 18.dp)
    "Style 4" -> RoundedCornerShape(
        topStart=24.dp,topEnd=6.dp,
        bottomStart=if(mine)24.dp else 7.dp,
        bottomEnd=if(mine)7.dp else 24.dp)
    "Style 5" -> RoundedCornerShape(
        topStart=2.dp,topEnd=2.dp,bottomStart=2.dp,bottomEnd=2.dp)
    "Style 6" -> RoundedCornerShape(
        topStart=7.dp,topEnd=24.dp,
        bottomStart=if(mine)24.dp else 7.dp,
        bottomEnd=if(mine)7.dp else 24.dp)
    "Style 7" -> RoundedCornerShape(
        topStart=18.dp,topEnd=18.dp,bottomStart=8.dp,bottomEnd=8.dp)
    "Style 8" -> RoundedCornerShape(
        topStart=28.dp,topEnd=12.dp,
        bottomStart=if(mine)12.dp else 2.dp,
        bottomEnd=if(mine)2.dp else 28.dp)
    "Style 9" -> RoundedCornerShape(
        topStart=6.dp,topEnd=6.dp,
        bottomStart=if(mine)16.dp else 2.dp,
        bottomEnd=if(mine)2.dp else 16.dp)
    "Style 10" -> RoundedCornerShape(32.dp)
    "Style 11" -> RoundedCornerShape(
        topStart=28.dp,topEnd=28.dp,bottomStart=10.dp,bottomEnd=10.dp)
    else -> RoundedCornerShape(
        topStart=22.dp,topEnd=22.dp,
        bottomStart=if(mine)22.dp else 8.dp,
        bottomEnd=if(mine)8.dp else 22.dp
    )
}

internal fun conversationBubbleColor(palette:String,mine:Boolean,dark:Boolean):Color? = when(palette) {
    "Green" -> if(dark) {
        if(mine)Color(0xFF124E42) else Color(0xFF273B35)
    } else {
        if(mine)Color(0xFFD9FDD3) else Color(0xFFFFFFFF)
    }
    "Lavender" -> if(dark) {
        if(mine)Color(0xFF46345F) else Color(0xFF302B43)
    } else {
        if(mine)Color(0xFFEADDFC) else Color(0xFFF8F1FF)
    }
    "Rose" -> if(dark) {
        if(mine)Color(0xFF61384B) else Color(0xFF493342)
    } else {
        if(mine)Color(0xFFFFDCE8) else Color(0xFFFFF5F8)
    }
    "Night" -> if(dark) {
        if(mine)Color(0xFF244B69) else Color(0xFF273746)
    } else {
        if(mine)Color(0xFFD6E6F5) else Color(0xFFECF3F9)
    }
    else -> null
}

/** Reference conversation XML exposes separate left/right bubble palettes. */
internal fun resolvedBubblePalette(options:DisplayOptions,mine:Boolean):String {
    val local=if(mine)options.outgoingBubblePreset else options.incomingBubblePreset
    return if(local=="Follow")options.bubbleColor else local
}

/** Color overrides never alter serialized messages or their decrypted/plain text. */
internal fun conversationInkColor(preset:String,dark:Boolean,default:Color,customHex:String?=null):Color=when(preset) {
    "Custom" -> normalizedAccentHex(customHex.orEmpty())?.let {Color(android.graphics.Color.parseColor(it))} ?: default
    "Blue" -> if(dark)Color(0xFFB9DBFF) else Color(0xFF1456A0)
    "Green" -> if(dark)Color(0xFFB4F1CB) else Color(0xFF176044)
    "Purple" -> if(dark)Color(0xFFE0C7FF) else Color(0xFF6C3E9F)
    "Rose" -> if(dark)Color(0xFFFFCBDE) else Color(0xFF9C2D62)
    "Amber" -> if(dark)Color(0xFFFFE0A0) else Color(0xFF86500F)
    else -> default
}

/** Fully local, opaque left/right picker matching the reference's separate color controls. */
internal fun resolvedBubbleColor(options:DisplayOptions,mine:Boolean,dark:Boolean):Color? {
    val palette=resolvedBubblePalette(options,mine)
    if(palette=="Custom") {
        val valid=normalizedAccentHex(if(mine)options.outgoingBubbleHex else options.incomingBubbleHex)
        return valid?.let {Color(android.graphics.Color.parseColor(it))}
    }
    return conversationBubbleColor(palette,mine,dark)
}

internal fun resolvedBubbleTextColor(options:DisplayOptions,mine:Boolean,dark:Boolean,theme:Color):Color {
    if(resolvedBubblePalette(options,mine)!="Custom")return theme
    val color=resolvedBubbleColor(options,mine,dark) ?: return theme
    val light=color.red*.2126f + color.green*.7152f + color.blue*.0722f
    return if(light>.52f) Color(0xFF132439) else Color.White
}


/** Same opacity in real message content and Settings preview; deleted highlights stay solid. */
internal fun bubbleSurfaceColor(options:DisplayOptions,mine:Boolean,dark:Boolean,fallback:Color):Color =
    (resolvedBubbleColor(options,mine,dark) ?: fallback)
        .copy(alpha=options.bubbleOpacity.coerceIn(45,100)/100f)
