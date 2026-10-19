package com.pritam.liquidchat.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class PushRecoveryTest {
    private val receivedAt=1_000_000L

    @Test fun transientCallFailureCanRecoverWithinWindow() {
        assertTrue(PushRecovery.retryCall(receivedAt,receivedAt+1_000,0))
        assertTrue(PushRecovery.retryCall(receivedAt,receivedAt+12_000,1))
        assertFalse(PushRecovery.retryCall(receivedAt,receivedAt+31_000,2))
    }

    @Test fun retryNeverStartsAtOrAfterDeadlineAndAttemptLimitIsFinite() {
        assertTrue(PushRecovery.retryCall(receivedAt,receivedAt+34_999,0))
        assertFalse(PushRecovery.retryCall(receivedAt,receivedAt+35_000,0))
        assertFalse(PushRecovery.retryCall(receivedAt,receivedAt,3))
        assertTrue(PushRecovery.callExpired(receivedAt,receivedAt+45_000))
        assertFalse(PushRecovery.retryCall(receivedAt,receivedAt+60_000,0))
        assertFalse(PushRecovery.retryCall(0,receivedAt,0))
        assertFalse(PushRecovery.retryCall(receivedAt,receivedAt-1,0))
    }

    @Test fun fcmTokenRegistrationRetriesOnlyRecoverableFailures() {
        assertTrue(PushRecovery.retryRegistration(IOException("offline"),0))
        assertTrue(PushRecovery.retryRegistration(ApiException(503,"","server unavailable"),0))
        assertTrue(PushRecovery.retryRegistration(ApiException(429,"","rate limited"),2))
        assertTrue(PushRecovery.retryRegistration(ApiException(408,"","timeout"),0))
        assertFalse(PushRecovery.retryRegistration(ApiException(401,"","revoked session"),0))
        assertFalse(PushRecovery.retryRegistration(ApiException(403,"","denied"),0))
        assertFalse(PushRecovery.retryRegistration(ApiException(400,"","invalid token"),0))
        assertFalse(PushRecovery.retryRegistration(IOException("offline"),6))
        assertFalse(PushRecovery.retryRegistration(IOException("offline"),-1))
    }

    @Test fun authorizedPushPostsEvenWhenSyncDidNotPopulateLocalCache()=runBlocking {
        val events=mutableListOf<String>()
        val localCache=mutableListOf<String>()
        deliverMessagePush(
            sync={events+="sync";localCache.clear()},
            preview={events+="authorize";"server-preview"},
            canPost={true},
            post={events+="post:$it"}
        )
        assertTrue(localCache.isEmpty())
        assertEquals(listOf("sync","authorize","post:server-preview"),events)
    }

    @Test fun authorizedForegroundChatSuppressionMarksMessageAsHandled()=runBlocking {
        var handled=0
        val trace=mutableListOf<String>()
        deliverMessagePush(sync={trace+="sync"},preview={trace+="preview";"authorized"},
            canPost={false},post={fail("Active conversation should not alert")},
            onSuppressed={handled++;trace+="handled"})
        assertEquals(1,handled)
        assertEquals(listOf("sync","preview","handled"),trace)
    }

    @Test fun unauthorizedPreviewNeverClaimsAReceipt()=runBlocking {
        deliverMessagePush<String>(sync={},preview={null},canPost={false},
            post={fail("No notification")},onSuppressed={fail("No receipt for denied push")})
    }

    @Test fun serverSuppressedPushNeverPosts()=runBlocking {
        deliverMessagePush<String>(sync={},preview={null},canPost={error("No authorized preview")},post={fail("Suppressed push posted")})
    }

    @Test fun accountOrActiveConversationChangeDuringPreviewSuppressesAlert()=runBlocking {
        var allowed=true
        deliverMessagePush(sync={},preview={allowed=false;"preview"},canPost={allowed},post={fail("Obsolete push posted")})
    }

    @Test fun syncFailureAndCancellationPropagateWithoutPosting()=runBlocking {
        for(failure in listOf(IOException("temporary"),CancellationException("stopped"))) {
            try {
                deliverMessagePush<String>(sync={throw failure},preview={fail("Sync failed");null},canPost={true},post={fail("Failed work posted")})
                fail("Failure swallowed")
            } catch(actual:Exception) {
                assertSame(failure,actual)
            }
        }
    }
}
