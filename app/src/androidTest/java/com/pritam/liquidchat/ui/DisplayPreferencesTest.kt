package com.pritam.liquidchat.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DisplayPreferencesTest {
    @Test fun everyPickerStyleAndCustomAccentSurviveSavingAndReopening() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val name="display-regression-${java.util.UUID.randomUUID()}"
        val prefs=context.getSharedPreferences(name,Context.MODE_PRIVATE)
        try {
            for(style in LIQUID_BUBBLE_STYLES) {
                val chosen=DisplayOptions(bubbleStyle=style,accentColor="Custom",
                    customAccentHex="#123456",showWallpaper=false)
                val saved=DisplayPreferences(prefs).write(chosen)
                assertEquals(chosen,saved)
                assertEquals(chosen,DisplayPreferences(prefs).read())
                assertEquals(chosen,MbThemeTransfer.decode(
                    MbThemeTransfer.export(saved),DisplayOptions()))
            }
        } finally {context.deleteSharedPreferences(name)}
    }

    @Test fun oldPreferencesKeepWallpapersAndInvalidStyleFallsBack() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val name="display-regression-${java.util.UUID.randomUUID()}"
        val prefs=context.getSharedPreferences(name,Context.MODE_PRIVATE)
        try {
            prefs.edit().putString("bubbleStyle","unknown-style").commit()
            val restored=DisplayPreferences(prefs).read()
            assertEquals("Tail",restored.bubbleStyle)
            assertTrue(restored.showWallpaper)
            val hidden=restored.copy(showWallpaper=false)
            assertFalse(MbThemeTransfer.decode(
                """{"format":"liquid-chat-theme","version":1}""",hidden).showWallpaper)
        } finally {context.deleteSharedPreferences(name)}
    }

    @Test fun appearanceResetPreservesBothMediaQualityChoices() {
        val original=DisplayOptions(photoQuality="HD",videoQuality="Standard")
        val reset=MbThemeTransfer.resetAppearance(original)
        assertEquals(original.photoQuality,reset.photoQuality)
        assertEquals(original.videoQuality,reset.videoQuality)
    }
}
