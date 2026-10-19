package com.pritam.liquidchat.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceCaptureFocusTest {
    @Test fun realRecordingHidesKeyboardOnce() {
        var started=false
        var dismissed=0
        assertTrue(startVoiceWithFocusHandoff(
            begin={started=true},recording={started},dismissKeyboard={dismissed++}))
        assertEquals(1,dismissed)
    }

    @Test fun recorderNotStartedMustKeepKeyboardAndDraftFocused() {
        var dismissed=0
        assertFalse(startVoiceWithFocusHandoff(
            begin={},recording={false},dismissKeyboard={dismissed++}))
        assertEquals(0,dismissed)
    }

    @Test fun failedRecordingNeverDismissesKeyboard() {
        var dismissed=0
        try {
            startVoiceWithFocusHandoff(begin={throw IllegalStateException("Microphone unavailable")},
                recording={false},dismissKeyboard={dismissed++})
            throw AssertionError("Expected recorder failure")
        } catch(_:IllegalStateException) {
            assertEquals(0,dismissed)
        }
    }
}
