package com.pritam.liquidchat.calls

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallViewRenderTest {
    private val callId = "2a041f14-71b7-4c0d-8058-d159f32e1d0b"

    @Test fun incomingVideoOnlyRendersAfterAnswer() {
        val ringing = CallView(callId, "chat", "peer", "video", true, "Incoming call")
        assertFalse(ringing.renderVideo)
        assertTrue(ringing.copy(status = "Connecting").renderVideo)
        assertTrue(ringing.copy(status = "Connected").renderVideo)
    }

    @Test fun outgoingVideoRendersWhenActive() {
        val outgoing = CallView(callId, "chat", "peer", "video", false, "Ringing")
        assertTrue(outgoing.renderVideo)
    }

    @Test fun audioCallsAndTerminalStatesNeverRenderVideo() {
        assertFalse(CallView(callId, "chat", "peer", "audio", true, "Connected").renderVideo)
        assertFalse(CallView(null, "chat", "peer", "video", false, "Call unavailable").renderVideo)
    }
}
