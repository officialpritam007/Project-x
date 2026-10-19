package com.pritam.liquidchat.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class VoicePlaybackSpeedTest {
    @Test fun playbackCyclesThroughThreeSupportedSpeeds() {
        assertEquals(1.5f,nextVoicePlaybackSpeed(1f))
        assertEquals(2f,nextVoicePlaybackSpeed(1.5f))
        assertEquals(1f,nextVoicePlaybackSpeed(2f))
    }

    @Test fun unexpectedSpeedFallsBackToNormal() {
        assertEquals(1f,nextVoicePlaybackSpeed(0.5f))
        assertEquals(1f,nextVoicePlaybackSpeed(10f))
    }
}
