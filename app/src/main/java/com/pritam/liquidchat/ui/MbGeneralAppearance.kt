package com.pritam.liquidchat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import java.util.Locale

/** Empty means follow the user's current system/Liquid theme. */
internal fun normalizedAppColor(value:String?):String =
    normalizedAccentHex(value.orEmpty()) ?: ""

internal fun appOverrideColor(value:String):Color? =
    normalizedAppColor(value).takeIf {it.isNotEmpty()}?.let {
        Color(it.substring(1).toLong(16) or 0xFF000000L)
    }

internal fun appColorHex(color:Color):String =
    "#%06X".format(Locale.ROOT,color.toArgb() and 0xFFFFFF)

/** Preserve accessible contrast when the user supplies an arbitrary theme color. */
internal fun readableAppInk(preferred:Color,background:Color):Color {
    fun contrast(ink:Color):Float {
        val a=ink.luminance()
        val b=background.luminance()
        return (maxOf(a,b)+.05f)/(minOf(a,b)+.05f)
    }
    if(contrast(preferred)>=4.5f)return preferred
    return if(contrast(Color.Black)>=contrast(Color.White))Color.Black else Color.White
}

internal fun appAppearanceScheme(base:ColorScheme,options:DisplayOptions):ColorScheme {
    val background=appOverrideColor(options.appBackgroundHex) ?: base.background
    val ink=appOverrideColor(options.appTextHex)
    return base.copy(
        background=background,
        onBackground=if(ink!=null || options.appBackgroundHex.isNotEmpty())
            readableAppInk(ink ?: base.onBackground,background) else base.onBackground,
        onSurface=ink?.let {readableAppInk(it,base.surface)} ?: base.onSurface,
        onSurfaceVariant=ink?.let {readableAppInk(it,base.surfaceVariant)} ?: base.onSurfaceVariant
    )
}

internal val LocalAppChromeInk=staticCompositionLocalOf<Color?> {null}

/** Preserve Android's nonlinear accessibility text size behavior. */
internal fun scaledAppDensity(parent:Density,uiPercent:Int,textPercent:Int):Density {
    val ui=uiPercent.coerceIn(85,120)/100f
    val text=textPercent.coerceIn(90,130)/100f
    if(ui==1f && text==1f)return parent
    // Compose's Density factory supplies the correct FontScaling conversions
    // for both sp and dp; custom overrides can accidentally double-scale text.
    return Density(parent.density*ui,parent.fontScale*text)
}

@Composable private fun MbOptionalAppColor(
    label:String,
    value:String,
    fallback:Color,
    onChange:(String)->Unit
) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            Text(label,style=MaterialTheme.typography.titleSmall)
            Text(if(value.isEmpty())"Follow theme" else value,
                style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        MbColorPickerButton(label,value.ifEmpty {appColorHex(fallback)},onChange)
        TextButton(onClick={onChange("")},enabled=value.isNotEmpty()) {Text("Reset")}
    }
}

@Composable internal fun MbGeneralAppearanceSettings(
    display:DisplayOptions,
    onDisplay:(DisplayOptions)->Unit
) {
    GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
        Text("General colors & scale",style=MaterialTheme.typography.titleMedium)
        MbOptionalAppColor("App text",display.appTextHex,MaterialTheme.colorScheme.onSurface) {
            onDisplay(display.copy(appTextHex=it))
        }
        MbOptionalAppColor("App background",display.appBackgroundHex,MaterialTheme.colorScheme.background) {
            onDisplay(display.copy(appBackgroundHex=it))
        }
        MbOptionalAppColor("Header and footer",display.appChromeHex,MaterialTheme.colorScheme.surface) {
            onDisplay(display.copy(appChromeHex=it))
        }
        Text("Colors apply only to this device. Message colors and photo wallpapers remain independent.",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider()
        Text("Interface scale · ${display.appUiScale}%",style=MaterialTheme.typography.titleSmall)
        Slider(value=display.appUiScale.coerceIn(85,120).toFloat(),
            onValueChange={onDisplay(display.copy(appUiScale=it.toInt()))},
            valueRange=85f..120f,steps=34,modifier=Modifier.fillMaxWidth())
        Text("Text scale · ${display.appTextScale}%",style=MaterialTheme.typography.titleSmall)
        Slider(value=display.appTextScale.coerceIn(90,130).toFloat(),
            onValueChange={onDisplay(display.copy(appTextScale=it.toInt()))},
            valueRange=90f..130f,steps=39,modifier=Modifier.fillMaxWidth())
        Text("These app-only size adjustments retain Android accessibility font scaling; they do not change the device DPI or system bar.",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick={onDisplay(display.copy(appTextHex="",appBackgroundHex="",
            appChromeHex="",appUiScale=100,appTextScale=100))}) {
            Text("Reset general colors and scale")
        }
    }
}
