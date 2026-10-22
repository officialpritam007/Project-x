package com.pritam.liquidchat.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoTextOverlayTest {
    @Test fun annotationTextStaysBoundedAndWhitespaceNormalized() {
        assertEquals("Hello World",Images.normalizedOverlayText(" Hello   World "))
        assertEquals("",Images.normalizedOverlayText("\n \t"))
        assertEquals(64,Images.normalizedOverlayText("z".repeat(1000)).length)
    }
}
