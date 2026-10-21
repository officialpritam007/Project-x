package com.pritam.liquidchat.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Source-only regression for access-rejection scope isolation. */
class BackendAuthorizationStatusTest {
    @Test fun unauthorizedAccessRotatesCachedVisibilityScope() {
        assertTrue(shouldRotateReadScopeForApiStatus(401))
    }

    @Test fun authenticatedForbiddenActionsNeverInvalidateTheSession() {
        // These cases may all be raised by Supabase policy/RLS or a blocked peer.
        listOf(400,403,404,409,422,425,429,500,503).forEach {status->
            assertFalse("HTTP $status must not invalidate a valid session",
                shouldRotateReadScopeForApiStatus(status))
        }
    }
}
