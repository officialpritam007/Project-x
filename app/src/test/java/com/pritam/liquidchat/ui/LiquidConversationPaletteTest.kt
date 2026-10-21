package com.pritam.liquidchat.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Test

/** Decoded reference markup has separate outgoing/incoming bubble, ink and quote controls. */
class LiquidConversationPaletteTest {
    @Test fun oldThemeRemainsUnchangedByDefault() {
        val original=DisplayOptions(bubbleColor="Lavender")
        assertEquals("Lavender",resolvedBubblePalette(original,true))
        assertEquals("Lavender",resolvedBubblePalette(original,false))
        assertNull(conversationInkColor("Theme",false,Color.Unspecified).takeUnless {it==Color.Unspecified})
    }

    @Test fun twoSidesCanHaveIndependentBubblesAndInk() {
        val options=DisplayOptions(bubbleColor="Night",outgoingBubblePreset="Green",
            incomingBubblePreset="Rose",outgoingInk="Green",incomingInk="Blue",
            quotedInk="Purple",timestampInk="Amber")
        assertEquals("Green",resolvedBubblePalette(options,true))
        assertEquals("Rose",resolvedBubblePalette(options,false))
        assertNotEquals(conversationInkColor(options.outgoingInk,false,Color.Black),
            conversationInkColor(options.incomingInk,false,Color.Black))
        assertNotEquals(conversationInkColor(options.quotedInk,false,Color.Black),
            conversationInkColor(options.timestampInk,false,Color.Black))
        assertNotEquals(conversationInkColor("Rose",false,Color.Black),
            conversationInkColor("Rose",true,Color.Black))
    }

    @Test fun originalOnlyResetsOneSideWithoutChangingSharedPalette() {
        val options=DisplayOptions(bubbleColor="Lavender",outgoingBubblePreset="Original",
            incomingBubblePreset="Follow")
        assertEquals("Original",resolvedBubblePalette(options,true))
        assertEquals("Lavender",resolvedBubblePalette(options,false))
        assertNull(conversationBubbleColor(resolvedBubblePalette(options,true),true,false))
    }
}
