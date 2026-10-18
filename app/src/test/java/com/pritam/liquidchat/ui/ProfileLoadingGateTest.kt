package com.pritam.liquidchat.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileLoadingGateTest {
    @Test fun pendingInitialReadStillBlocksEnteringApp() {
        assertTrue(profileRequiresRetry(loaded=false,profileId=null,account="account-1",error=null))
    }

    @Test fun initialOfflineFailureShowsRetryInsteadOfOnboarding() {
        assertTrue(profileRequiresRetry(loaded=true,profileId=null,account="account-1",error="Offline"))
    }

    @Test fun cachedProfileRemainsUsableWhenRefreshFails() {
        assertFalse(profileRequiresRetry(loaded=true,profileId="account-1",account="account-1",error="Offline"))
    }

    @Test fun noServerProfileWithSuccessfulReadEntersSetup() {
        assertFalse(profileRequiresRetry(loaded=true,profileId=null,account="account-1",error=null))
    }

    @Test fun previousAccountProfileNeverShowsDuringAccountSwitch() {
        assertTrue(profileRequiresRetry(loaded=true,profileId="account-2",account="account-1",error=null))
    }

    @Test fun loadedMatchingProfileHasNoRecoveryGate() {
        assertFalse(profileRequiresRetry(loaded=true,profileId="account-1",account="account-1",error=null))
    }
}
