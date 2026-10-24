package com.pritam.liquidchat.ui

import org.junit.Assert.*
import org.junit.Test

/** Regression source for the pending owner-authorized test pass. */
class ChatAutoFollowTest {
    @Test fun currentBottomIsFollowed() {
        assertTrue(shouldAutoFollowLatest(index=0,offsetPx=0,thresholdPx=48))
    }
    @Test fun readingWithinTallMediaDoesNotJumpToNewestMessage() {
        assertFalse(shouldAutoFollowLatest(index=0,offsetPx=250,thresholdPx=48))
    }
    @Test fun typingItemStillCountsAsNearBottom() {
        assertTrue(shouldAutoFollowLatest(index=1,offsetPx=12,thresholdPx=48))
    }
    @Test fun olderMessagesAreNotAutoScrolled() {
        assertFalse(shouldAutoFollowLatest(index=3,offsetPx=0,thresholdPx=48))
    }
}
