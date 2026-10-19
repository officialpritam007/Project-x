package com.pritam.liquidchat.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * MB reference conversation customization, kept independent of message delivery.
 * Legacy "Tail" + "Original" reproduces the pre-customization Liquid Chat bubble.
 */
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
