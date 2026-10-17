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
private val Light = lightColorScheme(primary=Glass.cyan, onPrimary=Color.White, primaryContainer=Color(0xFFDAEFFB),onPrimaryContainer=Color(0xFF00749C),background=Color(0xFFEEF4F7), surface=Color(0xFFFAFCFD), onSurface=Color(0xFF16191E), onSurfaceVariant=Color(0xFF53616D),outlineVariant=Color.White.copy(alpha=.8f))
private val Dark = darkColorScheme(primary=Color(0xFF75DCF5), primaryContainer=Color(0xFF164B5A),onPrimaryContainer=Color(0xFFC1F2FC),background=Color(0xFF111D25), surface=Color(0xFF22323D), onSurface=Color(0xFFF2F5FC), onSurfaceVariant=Color(0xFFBECBD4))
@Composable fun LiquidTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val haze=remember {HazeState()}
    CompositionLocalProvider(LocalGlassDark provides dark,LocalGlassState provides haze) {
        MaterialTheme(colorScheme=if(dark) Dark else Light, shapes=Shapes(small=Glass.compact, medium=Glass.card, large=Glass.hero), content=content)
    }
}
@Composable fun glassBackground(): Brush = Brush.linearGradient(if(LocalGlassDark.current) listOf(Color(0xFF142C38),Color(0xFF1E2933),Color(0xFF153840)) else listOf(Color(0xFFD6EDF4),Color(0xFFEEF4F7),Color(0xFFE1EBF1),Color(0xFFE8F4F5),Color(0xFFCAE7EE)))
/** Source-backed live blur with foreground text outside the captured effect. */
@Composable fun GlassSurface(modifier: Modifier=Modifier, shape: Shape=Glass.card, content: @Composable () -> Unit) {
    val dark=LocalGlassDark.current
    val opaque=LocalDisplayOptions.current.reducedTransparency
    val tint=if(dark) Color(0xFF22323D) else Color.White
    Surface(modifier.shadow(11.dp,shape,ambientColor=Color(0xFF244D65).copy(alpha=.11f),spotColor=Color(0xFF244D65).copy(alpha=.11f)),shape=shape,color=if(opaque)tint else Color.Transparent,border=BorderStroke(1.dp,if(dark)Color.White.copy(alpha=.18f) else Color.White.copy(alpha=.98f))) {
        val state=LocalGlassState.current
        val effect=if(!opaque&&state!=null)Modifier.hazeEffect(state,style=HazeStyle(backgroundColor=MaterialTheme.colorScheme.background,tint=HazeTint(tint.copy(alpha=.44f)),blurRadius=30.dp,noiseFactor=0f,fallbackTint=HazeTint(tint.copy(alpha=.9f)))) else Modifier
        Box(Modifier.clip(shape).then(effect).background(Brush.linearGradient(listOf(Color.White.copy(alpha=if(dark).08f else .25f),Color.White.copy(alpha=.04f),Color.Transparent,Color.White.copy(alpha=.16f))))) {content()}
    }
}
@Composable fun GlassCard(modifier: Modifier=Modifier, shape:Shape=Glass.card, content: @Composable ColumnScope.() -> Unit) {
    GlassSurface(modifier,shape) {Column(Modifier.padding(16.dp),content=content)}
}
