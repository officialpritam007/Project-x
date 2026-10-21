package com.pritam.liquidchat.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** Link appearance is local UI state; URL normalization, privacy and tap handling stay intact. */
class LiquidLinkPaletteTest {
    @Test fun themeDefaultKeepsExistingPrimaryColor() {
        val theme=Color(0xFF0787B9)
        assertEquals(theme,linkedTextColor("Theme",theme,false))
        assertEquals(theme,linkedTextColor("Theme",theme,true))
        assertEquals(theme,linkedTextColor("unrecognized",theme,false))
    }

    @Test fun userPalettesAdaptToLightAndDarkBackgrounds() {
        val theme=Color(0xFF0787B9)
        listOf("Blue","Green","Purple","Rose","Amber").forEach {name->
            val light=linkedTextColor(name,theme,false)
            val dark=linkedTextColor(name,theme,true)
            assertNotEquals("Distinct light/dark palette: $name",light,dark)
            assertNotEquals("Override theme color: $name",theme,light)
        }
    }
}
