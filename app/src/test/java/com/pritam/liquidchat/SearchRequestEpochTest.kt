package com.pritam.liquidchat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchRequestEpochTest {
    @Test fun resultsOnlyBelongToTheLatestKeystroke() {
        assertTrue(searchRequestCurrent(7,7,"account","account",2,2))
        assertFalse(searchRequestCurrent(6,7,"account","account",2,2))
        assertFalse(searchRequestCurrent(7,8,"account","account",2,2))
    }

    @Test fun crossAccountSearchResultsAreNeverReused() {
        assertFalse(searchRequestCurrent(7,7,"alice","bob",2,2))
        assertFalse(searchRequestCurrent(7,7,"alice",null,2,2))
    }

    @Test fun unownedOrInactiveBackendReadScopeDoesNotExposeSearch() {
        assertFalse(searchRequestCurrent(7,7,"account","account",null,null))
        assertFalse(searchRequestCurrent(7,7,"account","account",null,2))
    }

    @Test fun sameAccountDifferentAuthScopeMustNotRestoreOldResults() {
        assertFalse(searchRequestCurrent(7,7,"account","account",2,3))
    }
}
