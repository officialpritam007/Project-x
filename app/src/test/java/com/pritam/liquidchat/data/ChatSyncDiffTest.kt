package com.pritam.liquidchat.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatSyncDiffTest {
    private val original=Chat(
        account="account-a",id="conversation",peer="peer",name="Contact",
        username="contact",preview="Hello",lastSeq=7,unread=1,
        avatarId="avatar-1",peerRead=2,pinnedAt=0
    )

    @Test fun unchangedServerSnapshotDoesNotRewriteRoomRows() {
        assertEquals(emptyList<Chat>(),
            changedChatRows(listOf(original.copy()),listOf(original)))
    }

    @Test fun changedPreviewReceiptAndAvatarAreStillPersisted() {
        val changed=original.copy(preview="Updated",peerRead=7,avatarId="avatar-2")
        assertEquals(listOf(changed),
            changedChatRows(listOf(changed),listOf(original)))
    }

    @Test fun unreadPinAndArchiveChangesArePersisted() {
        val changed=original.copy(unread=0,pinnedAt=1234,archived=true)
        assertEquals(listOf(changed),
            changedChatRows(listOf(changed),listOf(original)))
    }

    @Test fun newChatIsUpsertedWithoutRewritingUnaffectedChats() {
        val created=original.copy(id="second",peer="second-peer",name="Second")
        assertEquals(listOf(created),
            changedChatRows(listOf(original,created),listOf(original)))
    }

    @Test fun sameConversationIdInAnotherAccountNeverReusesOldSnapshot() {
        val otherAccount=original.copy(account="account-b")
        assertEquals(listOf(otherAccount),
            changedChatRows(listOf(otherAccount),listOf(original)))
    }
}
