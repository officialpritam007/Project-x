package com.pritam.liquidchat.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/** Custom themes accept a single opaque RGB color, never arbitrary imported strings. */
internal fun normalizedAccentHex(input:String):String? {
    val text=input.trim()
    return text.takeIf {it.matches(Regex("^#[0-9A-Fa-f]{6}$"))}?.uppercase(java.util.Locale.ROOT)
}

internal fun customAccentScheme(base:ColorScheme,dark:Boolean,hex:String):ColorScheme {
    val rgb=normalizedAccentHex(hex) ?: return base
    val primary=Color(android.graphics.Color.parseColor(rgb))
    // A theme-controlled swatch may be nearly white or black. Maintain legible labels.
    val luminance=primary.red*.2126f+primary.green*.7152f+primary.blue*.0722f
    val foreground=if(luminance>.55f)Color(0xFF0D1829) else Color.White
    val container=if(dark) {
        Color(
            red=primary.red*.36f+.065f,
            green=primary.green*.36f+.08f,
            blue=primary.blue*.36f+.12f
        )
    } else {
        Color(
            red=primary.red*.14f+.83f,
            green=primary.green*.14f+.83f,
            blue=primary.blue*.14f+.83f
        )
    }
    return base.copy(primary=primary,onPrimary=foreground,primaryContainer=container,
        onPrimaryContainer=if(dark)Color.White else Color(0xFF172D42))
}
