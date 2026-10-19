package com.pritam.liquidchat.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class StoryProgressTest {
    @Test fun slowOrFailedImageNeverConsumesViewingTime() {
        assertEquals(.4f,advancePhotoStory(.4f,250,false,false),0f)
        assertEquals(.4f,advancePhotoStory(.4f,250,true,true),0f)
    }
    @Test fun readyImageAdvancesByElapsedTimeWithoutSkippingAfterUiStall() {
        assertEquals(.41f,advancePhotoStory(.4f,50,false,true),.0001f)
        assertEquals(.45f,advancePhotoStory(.4f,10_000,false,true),.0001f)
        assertEquals(1f,advancePhotoStory(.99f,250,false,true),0f)
    }
}
