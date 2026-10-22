package com.pritam.liquidchat.data

import org.junit.Assert.*
import org.junit.Test

class StickerBackgroundMaskTest {
    private val white=0xFFFFFFFF.toInt()
    private val black=0xFF000000.toInt()
    @Test fun removesOnlyWhiteConnectedToImageBorder() {
        val pixels=intArrayOf(
            white,white,white,white,white,
            white,black,black,black,white,
            white,black,white,black,white,
            white,black,black,black,white,
            white,white,white,white,white
        )
        val mask=borderConnectedNearWhiteMask(pixels,5,5)
        assertTrue(mask[0])
        assertTrue(mask[4])
        assertTrue(mask[24])
        assertFalse(mask[12]) // Bright eye/collar/text inside a dark subject.
        assertFalse(mask[6])
    }
    @Test fun neverRemovesIsolatedBrightPixels() {
        val pixels=intArrayOf(black,black,black,black,white,black,black,black,black)
        val mask=borderConnectedNearWhiteMask(pixels,3,3)
        assertEquals(0,mask.count {it})
    }
    @Test fun rejectsInvalidImageGeometry() {
        try {
            borderConnectedNearWhiteMask(intArrayOf(white),2,2)
            fail("expected invalid geometry rejection")
        } catch(_:IllegalArgumentException) {}
    }
}
