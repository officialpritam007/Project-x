package com.pritam.liquidchat.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class MbDisplayOptionsTest {
    @Test fun oldPreferencesAndConstructorsKeepExistingBlur() {
        val options=DisplayOptions()
        assertEquals(55,options.blurIntensity)
        assertEquals(0,options.copy(blurIntensity=0).blurIntensity)
        assertEquals(100,options.copy(blurIntensity=100).blurIntensity)
        assertEquals("Dark",DisplayOptions(appearance="Dark").appearance)
        assertEquals("Tail",DisplayOptions().bubbleStyle)
        assertEquals("Original",DisplayOptions().bubbleColor)
        assertEquals(16,DisplayOptions().messageFontSize)
        assertEquals(true,DisplayOptions().showNavLabels)
        assertEquals(24,DisplayOptions().navIconSize)
        assertEquals(11,DisplayOptions().navLabelSize)
        assertEquals(25,DisplayOptions().headerTitleSize)
        assertEquals(16,DisplayOptions().homeChatNameSize)
        assertEquals(14,DisplayOptions().homeChatPreviewSize)
        assertEquals(52,DisplayOptions().homeAvatarSize)
        assertEquals(11,DisplayOptions().homeRowPadding)
        assertEquals(false,DisplayOptions().hideHomeSearch)
        assertEquals(false,DisplayOptions().showHomeDividers)
        assertEquals("Reference",DisplayOptions().navBarStyle)
        assertEquals(false,DisplayOptions().hidePhotoShortcut)
        assertEquals(true,DisplayOptions().showPinBadges)
        assertEquals(24,DisplayOptions().emojiPickerSize)
        assertEquals("Theme",DisplayOptions().linkColor)
        assertEquals("Green",DisplayOptions().copy(linkColor="Green").linkColor)
        assertEquals(32,DisplayOptions().copy(emojiPickerSize=32).emojiPickerSize)
        assertEquals("Liquid",DisplayOptions().copy(navBarStyle="Liquid").navBarStyle)
        assertEquals("Cyan",DisplayOptions().accentColor)
        assertEquals(false,DisplayOptions().showProfileAsTitle)
        assertEquals(false,DisplayOptions().hideArchiveAccess)
        assertEquals("System",DisplayOptions().fontPreset)
        assertEquals(false,DisplayOptions().hideOutgoingTyping)
        assertEquals(false,DisplayOptions().preventScreenshots)
        assertEquals(MbNavigationOrder.DEFAULT,DisplayOptions().navigationOrder)
        assertEquals(50,DisplayOptions().homeWallpaperDim)
        assertEquals(50,DisplayOptions().chatWallpaperDim)
        assertEquals(0f,wallpaperDimAlpha(.46f,0),0.0001f)
        assertEquals(.46f,wallpaperDimAlpha(.46f,50),0.0001f)
        assertEquals(.90f,wallpaperDimAlpha(.56f,100),0.0001f)
        assertEquals(.30f,wallpaperDimAlpha(.30f,50),0.0001f)
        assertEquals(.0f,wallpaperDimAlpha(.30f,-3),0.0001f)
        assertEquals(liquidTypography("System"),liquidTypography("unknown"))
        org.junit.Assert.assertNotEquals(liquidTypography("System").bodyLarge.fontFamily,liquidTypography("Serif").bodyLarge.fontFamily)
        assertEquals(liquidAccentScheme(false,"Cyan"),liquidAccentScheme(false,"unknown"))
        org.junit.Assert.assertNotEquals(liquidAccentScheme(false,"Cyan").primary,liquidAccentScheme(false,"Rose").primary)
    }
    @Test fun bubblePalettesAreAppliedWithoutChangingLegacyDefault() {
        assertEquals(null,conversationBubbleColor("Original",true,false))
        assertEquals(androidx.compose.ui.graphics.Color(0xFFD9FDD3),conversationBubbleColor("Green",true,false))
        assertEquals(androidx.compose.ui.graphics.Color(0xFFFFFFFF),conversationBubbleColor("Green",false,false))
        assertEquals(androidx.compose.ui.graphics.Color(0xFF124E42),conversationBubbleColor("Green",true,true))
    }
    @Test fun messageBubbleShapeOptionsAreReal() {
        val mine=conversationBubbleShape("Tail",true)
        val theirs=conversationBubbleShape("Tail",false)
        org.junit.Assert.assertNotEquals(mine,theirs)
        org.junit.Assert.assertNotEquals(conversationBubbleShape("Rounded",true),conversationBubbleShape("Compact",true))
    }
}
