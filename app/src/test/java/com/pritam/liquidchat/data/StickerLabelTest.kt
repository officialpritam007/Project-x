package com.pritam.liquidchat.data

import org.junit.Assert.*
import org.junit.Test

class StickerLabelTest {
    @Test fun trimsAndBoundsOverlayWithoutChangingSourceBytes() {
        assertEquals("Hi there",normalizedStickerLabel("  Hi   there  "))
        assertEquals("",normalizedStickerLabel("   "))
        assertEquals(32,normalizedStickerLabel("x".repeat(300)).length)
        assertEquals("Hello World",normalizedStickerLabel("Hello\nWorld"))
    }
}
