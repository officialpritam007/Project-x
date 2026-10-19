package com.pritam.liquidchat.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageSnapshotStabilityTest {
    private val accepted=Message(
        account="owner",id="message-1",conversation="chat-1",sender="peer",
        body="Hello",seq=17,localTime=1234,serverTime="2026-10-19T01:00:00Z",
        status="SENT"
    )

    @Test fun identicalServerHistoryAndRealtimeSnapshotsDoNotRewriteRoom() {
        assertFalse(shouldPersistMessage(accepted,accepted.copy()))
        assertTrue(shouldPersistMessage(null,accepted))
    }

    @Test fun editsDeletesAndReactionsStillReachTimeline() {
        assertTrue(shouldPersistMessage(accepted,accepted.copy(body="Edited")))
        assertTrue(shouldPersistMessage(accepted,accepted.copy(deletedAt="2026-10-19T01:10:00Z")))
        assertTrue(shouldPersistMessage(accepted,accepted.copy(reactions="{\"heart\":[\"peer\"]}")))
    }

    @Test fun deliveredServerMessageNeverRegressesToSendingOrFailed() {
        assertFalse(shouldPersistMessage(accepted,accepted.copy(status="SENDING",seq=0)))
        assertFalse(shouldPersistMessage(accepted,accepted.copy(status="FAILED",error="Network")))
        assertFalse(shouldPersistMessage(accepted,accepted.copy(status="QUEUED")))
    }

    @Test fun legitimateQueuedToSendingToAcceptedTransitionsStillPersist() {
        val queued=accepted.copy(status="QUEUED",seq=0,serverTime="")
        val sending=queued.copy(status="SENDING",attempts=1)
        assertTrue(shouldPersistMessage(null,queued))
        assertTrue(shouldPersistMessage(queued,sending))
        assertTrue(shouldPersistMessage(sending,accepted))
        assertFalse(shouldPersistMessage(sending,sending.copy()))
    }

    @Test fun changedMessageFromAnotherAccountDoesNotReusePrivateSnapshot() {
        assertTrue(shouldPersistMessage(accepted,accepted.copy(account="another-account")))
    }
}
