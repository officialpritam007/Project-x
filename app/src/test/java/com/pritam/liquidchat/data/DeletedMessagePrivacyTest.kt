package com.pritam.liquidchat.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeletedMessagePrivacyTest {
    private val original=Message(account="account",id="id",conversation="chat",sender="peer",
        body="Private original message",status="SENT",
        mediaId="old-media",localImage="/tmp/old-media",imageWidth=300,imageHeight=400,
        mediaKind="voice",durationMs=3300,replyTo="original",reactions="{\"peer\":\"❤️\"}",
        starred=true)

    @Test fun deletedSnapshotCannotKeepTextOrMediaMetadata() {
        val deleted=scrubDeletedMessage(original.copy(deletedAt="2026-10-19T05:00:00Z"))
        assertEquals("This message was deleted",deleted.body)
        assertEquals("",deleted.mediaId)
        assertEquals("",deleted.localImage)
        assertEquals(0,deleted.imageWidth)
        assertEquals(0,deleted.imageHeight)
        assertEquals(0,deleted.durationMs)
        assertEquals("image",deleted.mediaKind)
        assertEquals("",deleted.replyTo)
        assertEquals("{}",deleted.reactions)
        assertEquals(false,deleted.starred)
        assertEquals(original.id,deleted.id)
        assertEquals(original.sender,deleted.sender)
        assertEquals(original.conversation,deleted.conversation)
        assertTrue(deleted.deletedAt.isNotEmpty())
    }

    @Test fun regularAcceptedMessagesRemainUnchanged() {
        assertEquals(original,scrubDeletedMessage(original))
    }

    @Test fun deletedContentScrubbingIsStableAcrossRepeatedSyncedUpdates() {
        val deleted=original.copy(deletedAt="2026-10-19T05:00:00Z")
        assertEquals(scrubDeletedMessage(deleted),scrubDeletedMessage(scrubDeletedMessage(deleted)))
    }
}
