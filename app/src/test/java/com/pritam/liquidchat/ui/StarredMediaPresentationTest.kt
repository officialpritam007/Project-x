package com.pritam.liquidchat.ui

import com.pritam.liquidchat.data.Message
import org.junit.Assert.assertEquals
import org.junit.Test

class StarredMediaPresentationTest {
    private fun item(kind:String,media:String) = Message(
        account="owner",id="starred",conversation="chat",sender="peer",
        body="Caption",status="SENT",mediaKind=kind,mediaId=media,starred=true
    )

    @Test fun starredPhotoUsesProtectedImagePreview() {
        assertEquals("image",starredMediaPresentation(item("image","protected-photo")))
    }

    @Test fun starredVoiceUsesExistingVoicePlayer() {
        assertEquals("voice",starredMediaPresentation(item("voice","protected-voice")))
    }

    @Test fun mediaWithoutServerReferenceShowsTextInsteadOfBrokenPreview() {
        assertEquals("text",starredMediaPresentation(item("image","")))
        assertEquals("text",starredMediaPresentation(item("voice","")))
    }

    @Test fun deletedOrRevokedMessageNeverRendersRichMedia() {
        val deleted=item("voice","old-private-audio").copy(
            deletedAt="2026-10-19T00:00:00Z"
        )
        assertEquals("hidden",starredMediaPresentation(deleted))
    }

    @Test fun starredTextRemainsReadable() {
        assertEquals("text",starredMediaPresentation(item("image","")))
    }
}
