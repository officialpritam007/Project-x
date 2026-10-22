package com.pritam.liquidchat.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Test

class MbRecordedPaletteTest {
    @Test fun referenceLightGetsWhiteWithGreenAccents() {
        val old=liquidAccentScheme(false,"Cyan")
        val new=mbRecordedLightPalette(old,DisplayOptions(),dark=false)
        assertEquals(Color.White,new.background)
        assertEquals(Color(0xFF19A66C),new.primary)
        assertNotEquals(old,new)
    }
    @Test fun liquidAndDarkAndCustomPreferencesAreNotOverwritten() {
        val initial=liquidAccentScheme(false,"Cyan")
        assertEquals(initial,mbRecordedLightPalette(
            initial,DisplayOptions(navBarStyle="Liquid"),dark=false))
        assertEquals(initial,mbRecordedLightPalette(
            initial,DisplayOptions(),dark=true))
        assertEquals(initial,mbRecordedLightPalette(
            initial,DisplayOptions(accentColor="Green"),dark=false))
        assertEquals(initial,mbRecordedLightPalette(
            initial,DisplayOptions(appBackgroundHex="#123456"),dark=false))
    }
}
