package com.pritam.liquidchat.data

import org.junit.Assert.*
import org.junit.Test

class ImageCropBoundsTest {
    @Test fun selectionRejectsInvalidFractionsAndTinyRects() {
        assertFalse(Images.CropBounds(-.1f,0f,.5f,.5f).valid())
        assertFalse(Images.CropBounds(0f,0f,1.01f,1f).valid())
        assertFalse(Images.CropBounds(0f,0f,.01f,.5f).valid())
        assertFalse(Images.CropBounds(0f,0f,1f,.01f).valid())
        assertFalse(Images.CropBounds(Float.NaN,0f,.5f,.5f).valid())
        assertFalse(Images.CropBounds(.8f,.8f,.2f,.2f).valid())
    }
    @Test fun selectionAcceptsValidFourSidedRectangles() {
        assertTrue(Images.CropBounds(0f,0f,1f,1f).valid())
        assertTrue(Images.CropBounds(.2f,.1f,.95f,.75f).valid())
    }
}
