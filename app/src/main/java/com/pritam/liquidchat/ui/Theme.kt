package com.pritam.liquidchat.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object Glass {
    val compact = RoundedCornerShape(16.dp)
    val card = RoundedCornerShape(24.dp)
    val hero = RoundedCornerShape(28.dp)
    val capsule = RoundedCornerShape(50)
}
private val Light = lightColorScheme(primary=Color(0xFF235FD6), background=Color(0xFFF2F5FC), surface=Color(0xFFF9FAFF), onSurface=Color(0xFF16243A), onSurfaceVariant=Color(0xFF465570))
private val Dark = darkColorScheme(primary=Color(0xFFA5C5FF), background=Color(0xFF111722), surface=Color(0xFF1E2838), onSurface=Color(0xFFF2F5FC), onSurfaceVariant=Color(0xFFBECBE0))
@Composable fun LiquidTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme=if(dark) Dark else Light, shapes=Shapes(small=Glass.compact, medium=Glass.card, large=Glass.hero), content=content)
}
/** Tinted fallback: avoids nested live blur and keeps text contrast predictable. */
@Composable fun GlassCard(modifier: Modifier=Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, shape=Glass.card, color=MaterialTheme.colorScheme.surface.copy(alpha=.96f), border=BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shadowElevation=2.dp) {
        Column(Modifier.padding(20.dp), content=content)
    }
}
