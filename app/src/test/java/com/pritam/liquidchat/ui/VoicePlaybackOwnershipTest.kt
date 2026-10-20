package com.pritam.liquidchat.ui

import com.pritam.liquidchat.data.Message
import org.junit.Assert.*
import org.junit.After
import org.junit.Test

/** Replays late callbacks/cancellation after the user starts a different clip. */
class VoicePlaybackOwnershipTest {
    private fun voice(id:String,account:String="account") =
        Message(account,id,"chat","peer","Voice message",mediaKind="voice",mediaId=id)

    @After fun cleanup() {VoicePlayback.stop()}

    @Test fun lateCompletionOrAudioFocusLossCannotStopTheNewClip() {
        val previous=VoicePlayback.begin(voice("first"))
        val current=VoicePlayback.begin(voice("second"))
        VoicePlayback.stopIfCurrent(previous)
        assertEquals("second",VoicePlayback.id)
        assertTrue(VoicePlayback.current(current))
        VoicePlayback.stopIfCurrent(current)
        assertNull(VoicePlayback.id)
        assertFalse(VoicePlayback.current(current))
    }

    @Test fun cancelledSourceFetchReleasesOnlyItsOwnPendingSelection() {
        val cancelled=VoicePlayback.begin(voice("first"))
        VoicePlayback.stopIfCurrent(cancelled)
        assertNull(VoicePlayback.id)
        assertFalse(VoicePlayback.ready)
        val nextAccount=VoicePlayback.begin(voice("second","other-account"))
        VoicePlayback.stopIfCurrent(cancelled)
        assertEquals("other-account",VoicePlayback.account)
        assertEquals("second",VoicePlayback.id)
        assertTrue(VoicePlayback.current(nextAccount))
    }
}
