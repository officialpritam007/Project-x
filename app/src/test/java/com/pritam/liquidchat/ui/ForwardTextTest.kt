package com.pritam.liquidchat.ui

import com.pritam.liquidchat.data.Message
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class ForwardTextTest {
    private val account="account-1"
    private fun message()=Message(account,"id","source",account,"Hello",status="SENT")

    @Test fun acceptedTextMayBeForwarded() {
        assertTrue(canForwardTextMessage(message(),account))
        assertTrue(canForwardTextMessage(message().copy(sender="peer-1"),account))
    }

    @Test fun neverForwardAnotherAccountOrUnsentMessage() {
        assertFalse(canForwardTextMessage(message(),"account-2"))
        assertFalse(canForwardTextMessage(message(),null))
        assertFalse(canForwardTextMessage(message().copy(status="FAILED"),account))
        assertFalse(canForwardTextMessage(message().copy(status="QUEUED"),account))
    }

    @Test fun neverTurnMediaOrVoiceIntoMisleadingPlainText() {
        assertFalse(canForwardTextMessage(message().copy(mediaId="media-id"),account))
        assertFalse(canForwardTextMessage(message().copy(localImage="/local/photo.jpg"),account))
        assertFalse(canForwardTextMessage(message().copy(mediaKind="voice",body="Voice message"),account))
    }

    @Test fun textMultiSelectionTogglesWithoutDuplicateIds() {
        val one=toggleTextSelection(emptySet(),"a")
        assertEquals(setOf("a"),one)
        val two=toggleTextSelection(one,"b")
        assertEquals(setOf("a","b"),two)
        val three=toggleTextSelection(two,"a")
        assertEquals(setOf("b"),three)
        assertEquals(emptySet<String>(),toggleTextSelection(three,"b"))
        assertEquals(setOf("a"),one)
    }

    @Test fun deletedAndEmptyMessagesAreNotForwardable() {
        assertFalse(canForwardTextMessage(message().copy(deletedAt="2026-10-19T00:00:00Z"),account))
        assertFalse(canForwardTextMessage(message().copy(body=""),account))
        assertFalse(canForwardTextMessage(message().copy(body="    "),account))
    }
}
