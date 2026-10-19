package com.pritam.liquidchat.ui

import com.pritam.liquidchat.data.Message
import org.junit.Assert.*
import org.junit.Test

class VoicePlaybackDeletionTest {
    private val voice=Message("account","voice","chat","peer","Voice message",mediaKind="voice",mediaId="audio")

    @Test fun cachedDeletionOrHideStopsPlaybackAfterBubbleLeavesViewport() {
        assertTrue(invalidatesVoicePlayback(voice.copy(deletedAt="2026-10-19T01:00:00Z"),true))
        assertTrue(invalidatesVoicePlayback(null,true))
        assertTrue(invalidatesVoicePlayback(voice.copy(mediaKind="image"),true))
        assertFalse(invalidatesVoicePlayback(voice,true))
    }

    @Test fun uncachedGalleryVoiceCanPlayWithoutRoomRow() {
        assertFalse(invalidatesVoicePlayback(null,false))
        assertFalse(invalidatesVoicePlayback(voice,false))
    }
}
