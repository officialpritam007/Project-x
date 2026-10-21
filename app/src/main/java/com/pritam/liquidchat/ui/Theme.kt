package com.pritam.liquidchat.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import dev.chrisbanes.haze.*

object Glass {
    val compact = RoundedCornerShape(16.dp)
    val card = RoundedCornerShape(24.dp)
    val hero = RoundedCornerShape(28.dp)
    val dialog = RoundedCornerShape(30.dp)
    val capsule = RoundedCornerShape(50)
    val cyan = Color(0xFF00789D)
}
val LocalGlassState=staticCompositionLocalOf<HazeState?> {null}
@Composable fun Modifier.glassSource(zIndex:Float=1f):Modifier {val state=LocalGlassState.current;return if(state!=null)this.hazeSource(state,zIndex=zIndex) else this}
val LocalGlassDark = staticCompositionLocalOf { false }
// L2 Aurora Light / D1 Midnight Titanium: evolve the original ice-blue Figma identity.
private val Light = lightColorScheme(
    primary=Glass.cyan, onPrimary=Color.White,
    primaryContainer=Color(0xFFD9F0FB),onPrimaryContainer=Color(0xFF075979),
    background=Color(0xFFEAF4FA),surface=Color(0xFFF8FCFF),
    surfaceVariant=Color(0xFFE6F2FA),onSurface=Color(0xFF172D42),
    onSurfaceVariant=Color(0xFF536B7D),outlineVariant=Color(0xFFC9DCE9)
)
private val Dark = darkColorScheme(
    primary=Color(0xFF8DDFFF),onPrimary=Color(0xFF07364C),
    primaryContainer=Color(0xFF1D445B),onPrimaryContainer=Color(0xFFCCEDFF),
    background=Color(0xFF0D1829),surface=Color(0xFF182C42),
    surfaceVariant=Color(0xFF243C53),onSurface=Color(0xFFF4F9FF),
    onSurfaceVariant=Color(0xFFB8CDE0),outlineVariant=Color(0xFF537084)
)
// iOS-inspired type hierarchy, while preserving Android font scaling and TalkBack.
private val PremiumTypography=Typography(
    headlineSmall=Typography().headlineSmall.copy(fontWeight=FontWeight.SemiBold,letterSpacing=(-0.45f).sp),
    titleLarge=Typography().titleLarge.copy(fontWeight=FontWeight.SemiBold,letterSpacing=(-0.30f).sp),
    titleMedium=Typography().titleMedium.copy(fontWeight=FontWeight.SemiBold),
    titleSmall=Typography().titleSmall.copy(fontWeight=FontWeight.SemiBold),
    bodyLarge=Typography().bodyLarge.copy(letterSpacing=0.05f.sp),
    labelLarge=Typography().labelLarge.copy(fontWeight=FontWeight.Medium)
)
/** Four built-in Android font families; no unlicensed/private MBWhatsApp fonts. */
private fun builtInTypography(preset:String,imported:FontFamily?=null):Typography {
    val family=when(preset) {
        "Serif" -> FontFamily.Serif
        "Mono" -> FontFamily.Monospace
        "Cursive" -> FontFamily.Cursive
        "Imported" -> imported ?: return PremiumTypography
        else -> return PremiumTypography
    }
    return Typography(
        displayLarge=PremiumTypography.displayLarge.copy(fontFamily=family),
        displayMedium=PremiumTypography.displayMedium.copy(fontFamily=family),
        displaySmall=PremiumTypography.displaySmall.copy(fontFamily=family),
        headlineLarge=PremiumTypography.headlineLarge.copy(fontFamily=family),
        headlineMedium=PremiumTypography.headlineMedium.copy(fontFamily=family),
        headlineSmall=PremiumTypography.headlineSmall.copy(fontFamily=family),
        titleLarge=PremiumTypography.titleLarge.copy(fontFamily=family),
        titleMedium=PremiumTypography.titleMedium.copy(fontFamily=family),
        titleSmall=PremiumTypography.titleSmall.copy(fontFamily=family),
        bodyLarge=PremiumTypography.bodyLarge.copy(fontFamily=family),
        bodyMedium=PremiumTypography.bodyMedium.copy(fontFamily=family),
        bodySmall=PremiumTypography.bodySmall.copy(fontFamily=family),
        labelLarge=PremiumTypography.labelLarge.copy(fontFamily=family),
        labelMedium=PremiumTypography.labelMedium.copy(fontFamily=family),
        labelSmall=PremiumTypography.labelSmall.copy(fontFamily=family)
    )
}
internal fun normalizedTitleFontPreset(value:String?):String =
    value?.takeIf {it in setOf("Follow","System","Serif","Mono","Cursive","Imported")} ?: "Follow"

/** Separate title font preserves body/chat text styles. */
internal fun liquidTypography(preset:String,titlePreset:String="Follow",
    importedBody:FontFamily?=null,importedTitle:FontFamily?=null):Typography {
    val body=builtInTypography(preset,importedBody)
    val selected=normalizedTitleFontPreset(titlePreset)
    if(selected=="Follow")return body
    val titles=builtInTypography(selected,importedTitle)
    return body.copy(
        headlineLarge=titles.headlineLarge,
        headlineMedium=titles.headlineMedium,
        headlineSmall=titles.headlineSmall,
        titleLarge=titles.titleLarge,
        titleMedium=titles.titleMedium,
        titleSmall=titles.titleSmall
    )
}
/** User-selected primary theme; Cyan deliberately retains the original palette exactly. */
internal fun liquidAccentScheme(dark:Boolean,accent:String):ColorScheme {
    val base=if(dark)Dark else Light
    val colors=when(accent) {
        "Blue" -> if(dark)Color(0xFFA5C9FF) to Color(0xFF183A65) else Color(0xFF0056B5) to Color(0xFFE5F0FF)
        "Green" -> if(dark)Color(0xFF8FE2B0) to Color(0xFF1D4533) else Color(0xFF176C45) to Color(0xFFE5F5E8)
        "Purple" -> if(dark)Color(0xFFD7B9FF) to Color(0xFF493263) else Color(0xFF6E49B6) to Color(0xFFF0E7FF)
        "Rose" -> if(dark)Color(0xFFFFB1D4) to Color(0xFF563046) else Color(0xFFA33C6F) to Color(0xFFFFE9F2)
        "Amber" -> if(dark)Color(0xFFFFCE80) to Color(0xFF524021) else Color(0xFF825400) to Color(0xFFFFF0D0)
        else -> return base
    }
    return base.copy(primary=colors.first,primaryContainer=colors.second,
        onPrimary=if(dark)Color(0xFF102033) else Color.White,
        onPrimaryContainer=if(dark)colors.first else colors.first)
}
@Composable fun LiquidTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val haze=remember {HazeState()}
    val imported=rememberImportedFonts()
    val options=LocalDisplayOptions.current
    val parentDensity=LocalDensity.current
    val density=remember(parentDensity,options.appUiScale,options.appTextScale) {
        scaledAppDensity(parentDensity,options.appUiScale,options.appTextScale)
    }
    val baseScheme=if(options.accentColor=="Custom")
        customAccentScheme(if(dark)Dark else Light,dark,options.customAccentHex)
        else liquidAccentScheme(dark,options.accentColor)
    CompositionLocalProvider(LocalGlassDark provides dark,LocalGlassState provides haze,
        LocalDensity provides density) {
        MaterialTheme(
            colorScheme=appAppearanceScheme(baseScheme,options),
            shapes=Shapes(small=Glass.compact,medium=Glass.card,large=Glass.hero),
            typography=liquidTypography(LocalDisplayOptions.current.fontPreset,
                LocalDisplayOptions.current.titleFontPreset,imported.body,imported.title),
            content=content
        )
    }
}
@Composable fun glassBackground(): Brush {
    appOverrideColor(LocalDisplayOptions.current.appBackgroundHex)?.let {
        return Brush.linearGradient(listOf(it,it))
    }
    return Brush.linearGradient(if(LocalGlassDark.current)
        listOf(Color(0xFF081729),Color(0xFF152B41),Color(0xFF18253A),Color(0xFF132E43),Color(0xFF101C2D))
    else
        listOf(Color(0xFFD9F0FB),Color(0xFFEAF7FF),Color(0xFFF2ECFA),Color(0xFFEAF5FA),Color(0xFFDDEFFB))
    )
}
/** Source-backed live blur with foreground text outside the captured effect. */
@Composable fun GlassSurface(modifier: Modifier=Modifier, shape: Shape=Glass.card, content: @Composable () -> Unit) {
    GlassSurfaceImpl(modifier,shape,null,content)
}
/** Apply custom chrome tints only to supported headers and composer surfaces. */
@Composable internal fun GlassChromeSurface(modifier:Modifier=Modifier,shape:Shape=Glass.card,
    content:@Composable ()->Unit) {
    val tint=appOverrideColor(LocalDisplayOptions.current.appChromeHex)
    if(tint==null)GlassSurface(modifier,shape,content)
    else {
        val scheme=MaterialTheme.colorScheme
        val ink=readableAppInk(scheme.onSurface,tint)
        CompositionLocalProvider(LocalAppChromeInk provides ink,LocalContentColor provides ink) {
            MaterialTheme(colorScheme=scheme.copy(
                onSurface=ink,
                onSurfaceVariant=readableAppInk(scheme.onSurfaceVariant,tint),
                primary=readableAppInk(scheme.primary,tint))) {
                GlassSurfaceImpl(modifier,shape,tint,content)
            }
        }
    }
}
@Composable private fun GlassSurfaceImpl(modifier:Modifier,shape:Shape,tintOverride:Color?,
    content:@Composable ()->Unit) {
    val dark=if(tintOverride!=null)tintOverride.luminance()<.5f else LocalGlassDark.current
    val options=LocalDisplayOptions.current
    val opaque=options.reducedTransparency
    // Keep 22dp as the original blur at 55%; update the surface dynamically.
    val blurRadius=(options.blurIntensity.coerceIn(0,100)*0.4f).dp
    val tint=tintOverride ?: if(dark) Color(0xFF263A50) else Color.White
    Surface(
        modifier.shadow(10.dp,shape,
            ambientColor=Color(0xFF254B65).copy(alpha=if(dark).22f else .12f),
            spotColor=Color(0xFF254B65).copy(alpha=if(dark).18f else .10f)),
        shape=shape,
        color=if(opaque)tint else Color.Transparent,
        border=BorderStroke(1.dp,if(dark)Color.White.copy(alpha=.24f) else Color.White.copy(alpha=.88f))
    ) {
        val state=LocalGlassState.current
        val effect=if(!opaque&&state!=null) Modifier.hazeEffect(
            state,
            style=HazeStyle(
                backgroundColor=MaterialTheme.colorScheme.background,
                tint=HazeTint(tint.copy(alpha=if(tintOverride!=null).82f else if(dark).56f else .57f)),
                blurRadius=blurRadius,
                noiseFactor=.018f,
                fallbackTint=HazeTint(tint.copy(alpha=.94f))
            )
        ) else Modifier
        // A restrained directional highlight provides depth without painting
        // over foreground text or reducing its contrast.
        Box(Modifier.clip(shape).then(effect).background(
            Brush.linearGradient(listOf(
                Color.White.copy(alpha=if(dark).13f else .43f),
                Color.White.copy(alpha=if(dark).055f else .12f),
                Color.Transparent,
                Color.White.copy(alpha=if(dark).07f else .17f)
            ))
        )) { content() }
    }
}
@Composable fun GlassCard(modifier: Modifier=Modifier, shape:Shape=Glass.card, content: @Composable ColumnScope.() -> Unit) {
    GlassSurface(modifier,shape) {Column(Modifier.padding(16.dp),content=content)}
}
