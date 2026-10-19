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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    headlineSmall=Typography().headlineSmall.copy(fontWeight=FontWeight.SemiBold,letterSpacing=(-.45).sp),
    titleLarge=Typography().titleLarge.copy(fontWeight=FontWeight.SemiBold,letterSpacing=(-.3).sp),
    titleMedium=Typography().titleMedium.copy(fontWeight=FontWeight.SemiBold),
    titleSmall=Typography().titleSmall.copy(fontWeight=FontWeight.SemiBold),
    bodyLarge=Typography().bodyLarge.copy(letterSpacing=.05.sp),
    labelLarge=Typography().labelLarge.copy(fontWeight=FontWeight.Medium)
)
@Composable fun LiquidTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val haze=remember {HazeState()}
    CompositionLocalProvider(LocalGlassDark provides dark,LocalGlassState provides haze) {
        MaterialTheme(
            colorScheme=if(dark) Dark else Light,
            shapes=Shapes(small=Glass.compact,medium=Glass.card,large=Glass.hero),
            typography=PremiumTypography,
            content=content
        )
    }
}
@Composable fun glassBackground(): Brush = Brush.linearGradient(
    if(LocalGlassDark.current)
        listOf(Color(0xFF081729),Color(0xFF152B41),Color(0xFF18253A),Color(0xFF132E43),Color(0xFF101C2D))
    else
        listOf(Color(0xFFD9F0FB),Color(0xFFEAF7FF),Color(0xFFF2ECFA),Color(0xFFEAF5FA),Color(0xFFDDEFFB))
)
/** Source-backed live blur with foreground text outside the captured effect. */
@Composable fun GlassSurface(modifier: Modifier=Modifier, shape: Shape=Glass.card, content: @Composable () -> Unit) {
    val dark=LocalGlassDark.current
    val opaque=LocalDisplayOptions.current.reducedTransparency
    val tint=if(dark) Color(0xFF263A50) else Color.White
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
                tint=HazeTint(tint.copy(alpha=if(dark).56f else .57f)),
                blurRadius=22.dp,
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
