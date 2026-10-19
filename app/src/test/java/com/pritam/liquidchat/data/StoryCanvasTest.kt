package com.pritam.liquidchat.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryCanvasTest {
    @Test fun portraitLandscapeRotatedSquareAndUltraWideAlwaysFitUploadLimits() {
        assertEquals(1280 to 720,storyCanvasSize(3840,1080,0))
        assertEquals(720 to 1280,storyCanvasSize(1920,1080,90))
        assertEquals(1280 to 720,storyCanvasSize(1080,1920,270))
        assertEquals(720 to 720,storyCanvasSize(1080,1080,0))
        for(rotation in listOf(0,90,180,270,-90)) {
            val (width,height)=storyCanvasSize(3840,1080,rotation)
            assertTrue(minOf(width,height)<=720&&maxOf(width,height)<=1280)
            assertTrue(width%2==0&&height%2==0)
        }
    }
}
