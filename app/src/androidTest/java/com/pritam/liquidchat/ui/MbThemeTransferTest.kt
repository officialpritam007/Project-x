package com.pritam.liquidchat.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MbThemeTransferTest {
    @Test fun roundTripPreservesAppearanceWithoutPersonalData() {
        val expected=DisplayOptions(
            appearance="Dark",blurIntensity=81,bubbleStyle="Compact",
            bubbleColor="Rose",messageFontSize=20,navIconSize=27,
            showNavLabels=true,accentColor="Purple",showProfileAsTitle=true,fontPreset="Mono",homeAvatarSize=64,homeRowPadding=18,
            homeWallpaperDim=15,chatWallpaperDim=87,quickReactions="👏,👍,❤️",navigationOrder="0,2,1,4,3"
        )
        val encoded=MbThemeTransfer.export(expected)
        assertFalse(encoded.contains("user_id"))
        assertFalse(encoded.contains("token"))
        assertFalse(encoded.contains("hideHomePreviews"))
        assertEquals(expected,MbThemeTransfer.decode(encoded,DisplayOptions()))
    }
    @Test fun resetAppearanceKeepsPrivacyAndAccessibilityButRestoresVisualDefaults() {
        val custom=DisplayOptions(
            appearance="Dark",accentColor="Rose",bubbleStyle="Compact",
            bubbleColor="Night",messageFontSize=21,navIconSize=30,
            fontPreset="Cursive",homeWallpaperDim=3,chatWallpaperDim=88,
            quickReactions=MbQuickReactions.defaultSetting,
            preventScreenshots=true,hideHomePreviews=true,hideOutgoingTyping=true,
            reducedMotion=true,reducedTransparency=true
        )
        val restored=MbThemeTransfer.resetAppearance(custom)
        assertEquals("System",restored.appearance)
        assertEquals("Cyan",restored.accentColor)
        assertEquals("Tail",restored.bubbleStyle)
        assertEquals(16,restored.messageFontSize)
        assertEquals(50,restored.homeWallpaperDim)
        assertEquals(50,restored.chatWallpaperDim)
        assertTrue(restored.preventScreenshots)
        assertTrue(restored.hideHomePreviews)
        assertTrue(restored.hideOutgoingTyping)
        assertTrue(restored.reducedMotion)
        assertTrue(restored.reducedTransparency)
    }

    @Test fun rejectsForeignFormatsAndLargeFiles() {
        val invalid="{\"format\":\"mbwhatsapp\",\"version\":1}"
        val result=runCatching {MbThemeTransfer.decode(invalid,DisplayOptions())}
        assertTrue(result.isFailure)
        val oversized="x".repeat(MbThemeTransfer.MAX_IMPORT_BYTES+1)
        assertTrue(runCatching {MbThemeTransfer.decode(oversized,DisplayOptions())}.isFailure)
    }
    @Test fun ignoresUnknownKeysAndClampsUserSuppliedValues() {
        val json="""{"format":"liquid-chat-theme","version":1,"blurIntensity":999,"messageFontSize":-200,"accentColor":"bogus","unexpected":"ignored"}"""
        val updated=MbThemeTransfer.decode(json,DisplayOptions())
        assertEquals(100,updated.blurIntensity)
        assertEquals(14,updated.messageFontSize)
        assertEquals("Cyan",updated.accentColor)
        assertFalse(updated.hideArchiveAccess)
    }
}
