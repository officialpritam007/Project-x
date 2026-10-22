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

internal fun conversationBubbleShape(style:String,mine:Boolean):RoundedCornerShape = when(style) {
    "Rounded" -> RoundedCornerShape(22.dp)
    "Compact" -> RoundedCornerShape(11.dp)
    else -> RoundedCornerShape(
        topStart=22.dp, topEnd=22.dp,
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
