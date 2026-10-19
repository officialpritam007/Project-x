package com.pritam.liquidchat.ui

import com.pritam.liquidchat.data.Message
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MessageActionCategoriesTest {
    private fun message()=Message("owner","id","chat","peer","Hi",status="SENT")

    @Test fun acceptedIncomingMessageHasReactionsActionsAndSafety() {
        assertEquals(listOf("Quick reactions","Message actions","Safety"),
            messageActionCategories(message()))
    }

    @Test fun acceptedOwnMessageRetainsDeleteButNotReport() {
        assertEquals(listOf("Quick reactions","Message actions","Safety"),
            messageActionCategories(message().copy(sender="owner")))
    }

    @Test fun unsentAndFailedMessagesHaveNoServerReactions() {
        assertEquals(listOf("Message actions","Safety"),
            messageActionCategories(message().copy(status="FAILED")))
        assertEquals(listOf("Message actions","Safety"),
            messageActionCategories(message().copy(status="QUEUED")))
    }

    @Test fun deletedMessageNeverShowsActionsThatExposeOldText() {
        val sections=messageActionCategories(message().copy(deletedAt="2026-10-19T00:00:00Z"))
        assertEquals(listOf("Safety"),sections)
        assertFalse(sections.contains("Message actions"))
        assertFalse(sections.contains("Quick reactions"))
    }
}
