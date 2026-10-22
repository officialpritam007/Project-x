package com.pritam.liquidchat.ui

import org.junit.Assert.*
import org.junit.Test

class MbConversationChromeTest {
    @Test fun referenceModeOnlyChangesPresentationAndNeverMessageSettings() {
        val saved=DisplayOptions(
            navBarStyle="Reference",bubbleStyle="Rounded",bubbleColor="Rose",
            messageFontSize=19,outgoingInk="Custom",showEditedTime=true,
            doubleTapReactionEnabled=true)
        assertTrue(referenceMessageChrome(saved))
        val liquid=saved.copy(navBarStyle="Liquid")
        assertFalse(referenceMessageChrome(liquid))
        assertEquals(saved.bubbleStyle,liquid.bubbleStyle)
        assertEquals(saved.messageFontSize,liquid.messageFontSize)
        assertEquals(saved.outgoingInk,liquid.outgoingInk)
        assertEquals(saved.doubleTapReactionEnabled,liquid.doubleTapReactionEnabled)
    }
    @Test fun twelveVideoStylesAlterRealBubbleGeometry() {
        assertEquals(12,MB_RECORDED_BUBBLE_STYLES.size)
        assertEquals(12,MB_RECORDED_BUBBLE_STYLES.distinct().size)
        assertTrue(MB_RECORDED_BUBBLE_STYLES.all {it.isNotBlank()})
        assertNotEquals(conversationBubbleShape("iOS",true),
            conversationBubbleShape("Style 5",true))
        assertNotEquals(conversationBubbleShape("Style 2",true),
            conversationBubbleShape("Style 2",false))
    }
    @Test fun chatBubbleShapeRemainsSenderAware() {
        assertNotEquals(conversationBubbleShape("Tail",true),
            conversationBubbleShape("Tail",false))
        assertEquals(conversationBubbleShape("Rounded",true),
            conversationBubbleShape("Rounded",false))
    }
}
