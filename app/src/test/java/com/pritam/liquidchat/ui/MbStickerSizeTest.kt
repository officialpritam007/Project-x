package com.pritam.liquidchat.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class MbStickerSizeTest {
    @Test fun defaultAndDisabledMaintainExistingSize() {
        assertEquals(192,conversationStickerSize(DisplayOptions()))
        assertEquals(192,conversationStickerSize(DisplayOptions(stickerSize=256)))
    }
    @Test fun sizeIsClampedWhenEnabled() {
        assertEquals(96,conversationStickerSize(DisplayOptions(customStickerSize=true,stickerSize=-1)))
        assertEquals(256,conversationStickerSize(DisplayOptions(customStickerSize=true,stickerSize=Int.MAX_VALUE)))
        assertEquals(160,conversationStickerSize(DisplayOptions(customStickerSize=true,stickerSize=160)))
    }
}
