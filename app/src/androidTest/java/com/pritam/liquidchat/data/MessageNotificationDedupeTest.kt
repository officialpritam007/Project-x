package com.pritam.liquidchat.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class MessageNotificationDedupeTest {
    @Test fun repeatedMessageIsClaimedOnlyOnceAcrossWorkerInstances() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val account = UUID.randomUUID().toString()
        val message = UUID.randomUUID().toString()
        try {
            assertFalse(MessageNotificationDedupe.wasPosted(context, account, message))
            assertTrue(MessageNotificationDedupe.claim(context, account, message))
            assertTrue(MessageNotificationDedupe.wasPosted(context, account, message))
            // No in-memory worker flag: the second call reads a persisted receipt.
            assertFalse(MessageNotificationDedupe.claim(context, account, message))
            assertTrue(MessageNotificationDedupe.claim(context, account, UUID.randomUUID().toString()))
        } finally {
            MessageNotificationDedupe.clearAccount(context, account)
        }
    }

    @Test fun receiptsAreAccountScopedAndSignOutErasesOnlyOwnHistory() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val first = UUID.randomUUID().toString()
        val second = UUID.randomUUID().toString()
        val message = UUID.randomUUID().toString()
        try {
            assertTrue(MessageNotificationDedupe.claim(context, first, message))
            assertTrue(MessageNotificationDedupe.claim(context, second, message))
            MessageNotificationDedupe.clearAccount(context, first)
            assertFalse(MessageNotificationDedupe.wasPosted(context, first, message))
            assertTrue(MessageNotificationDedupe.wasPosted(context, second, message))
            assertFalse(MessageNotificationDedupe.claim(context, second, message))
        } finally {
            MessageNotificationDedupe.clearAccount(context, first)
            MessageNotificationDedupe.clearAccount(context, second)
        }
    }

    @Test fun failedNotificationPostingMayReleaseClaimForRetry() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val account = UUID.randomUUID().toString()
        val message = UUID.randomUUID().toString()
        try {
            assertTrue(MessageNotificationDedupe.claim(context, account, message))
            MessageNotificationDedupe.forget(context, account, message)
            assertTrue(MessageNotificationDedupe.claim(context, account, message))
        } finally {
            MessageNotificationDedupe.clearAccount(context, account)
        }
    }
}
