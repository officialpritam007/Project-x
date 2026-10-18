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
import dev.chrisbanes.haze.*

object Glass {
    val compact = RoundedCornerShape(16.dp)
    val card = RoundedCornerShape(24.dp)
    val hero = RoundedCornerShape(28.dp)
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
    background=Color(0xFFEAF3F9),surface=Color(0xFFFAFCFF),
    onSurface=Color(0xFF193247),onSurfaceVariant=Color(0xFF526B7B),
    outlineVariant=Color(0xFFC4D9E7)
)
private val Dark = darkColorScheme(
    primary=Color(0xFF8DDFFF),onPrimary=Color(0xFF07364C),
    primaryContainer=Color(0xFF1D445B),onPrimaryContainer=Color(0xFFCCEDFF),
    background=Color(0xFF101A29),surface=Color(0xFF263A50),
    onSurface=Color(0xFFF4F8FF),onSurfaceVariant=Color(0xFFB7CDDF),
    outlineVariant=Color(0xFF51677B)
)
@Composable fun LiquidTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val haze=remember {HazeState()}
    CompositionLocalProvider(LocalGlassDark provides dark,LocalGlassState provides haze) {
        MaterialTheme(colorScheme=if(dark) Dark else Light, shapes=Shapes(small=Glass.compact, medium=Glass.card, large=Glass.hero), content=content)
    }
}
@Composable fun glassBackground(): Brush = Brush.linearGradient(
    if(LocalGlassDark.current)
        listOf(Color(0xFF0B1929),Color(0xFF1C2A40),Color(0xFF192538),Color(0xFF102D3C))
    else
        listOf(Color(0xFFD4EDFB),Color(0xFFE8F3FA),Color(0xFFECE6F5),Color(0xFFEAF4F9),Color(0xFFD1E9F5))
)
/** Source-backed live blur with foreground text outside the captured effect. */
@Composable fun GlassSurface(modifier: Modifier=Modifier, shape: Shape=Glass.card, content: @Composable () -> Unit) {
    val dark=LocalGlassDark.current
    val opaque=LocalDisplayOptions.current.reducedTransparency
    val tint=if(dark) Color(0xFF263A50) else Color.White
    Surface(modifier.shadow(11.dp,shape,ambientColor=Color(0xFF244D65).copy(alpha=.11f),spotColor=Color(0xFF244D65).copy(alpha=.11f)),shape=shape,color=if(opaque)tint else Color.Transparent,border=BorderStroke(1.dp,if(dark)Color.White.copy(alpha=.18f) else Color.White.copy(alpha=.98f))) {
        val state=LocalGlassState.current
        val effect=if(!opaque&&state!=null)Modifier.hazeEffect(state,style=HazeStyle(backgroundColor=MaterialTheme.colorScheme.background,tint=HazeTint(tint.copy(alpha=.44f)),blurRadius=30.dp,noiseFactor=0f,fallbackTint=HazeTint(tint.copy(alpha=.9f)))) else Modifier
        Box(Modifier.clip(shape).then(effect).background(Brush.linearGradient(listOf(Color.White.copy(alpha=if(dark).08f else .25f),Color.White.copy(alpha=.04f),Color.Transparent,Color.White.copy(alpha=.16f))))) {content()}
    }
}
@Composable fun GlassCard(modifier: Modifier=Modifier, shape:Shape=Glass.card, content: @Composable ColumnScope.() -> Unit) {
    GlassSurface(modifier,shape) {Column(Modifier.padding(16.dp),content=content)}
}
