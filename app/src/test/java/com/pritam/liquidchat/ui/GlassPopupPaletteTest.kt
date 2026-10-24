package com.pritam.liquidchat.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Test

/** Regression tests for themed popup surfaces used by action and media sheets. */
class GlassPopupPaletteTest {
    // Compose Color stores sRGB alpha at finite precision; allow one 8-bit step.

    @Test fun referencePalettePopupInheritsWhiteSurface() {
        val scheme=mbRecordedLightPalette(
            liquidAccentScheme(false,"Cyan"),DisplayOptions(),dark=false)
        val popup=popupSurfaceColor(scheme.surface,reducedTransparency=false)
        assertEquals(Color.White.red,popup.red,0.0001f)
        assertEquals(Color.White.green,popup.green,0.0001f)
        assertEquals(Color.White.blue,popup.blue,0.0001f)
        assertEquals(.985f,popup.alpha,0.001f)
    }

    @Test fun darkAndCustomSurfaceKeepTheirTint() {
        val tint=Color(0xFF233A50)
        val popup=popupSurfaceColor(tint,reducedTransparency=false)
        assertEquals(tint.red,popup.red,0.0001f)
        assertEquals(tint.green,popup.green,0.0001f)
        assertEquals(tint.blue,popup.blue,0.0001f)
        assertEquals(.985f,popup.alpha,0.001f)
    }

    @Test fun reducedTransparencyRemainsFullyOpaque() {
        val tint=Color(0xFF233A50)
        assertEquals(1f,popupSurfaceColor(tint,reducedTransparency=true).alpha,0.0001f)
    }
}
