package com.pritam.liquidchat

import com.pritam.liquidchat.calls.CallView
import org.junit.Assert.*
import org.junit.Test

class CallWindowPrivacyTest {
    private val incoming=CallView("validated-call","chat","peer","video",true,"Incoming call")
    @Test fun onlyValidatedFullCallOverlayCanShowOverDeviceLock() {
        assertTrue(callWindowMayShowWhenLocked(incoming))
        assertTrue(callWindowMayShowWhenLocked(incoming.copy(status="Connected")))
        assertFalse(callWindowMayShowWhenLocked(incoming.copy(minimized=true)))
        assertFalse(callWindowMayShowWhenLocked(incoming.copy(id=null,status="Preparing")))
        assertFalse(callWindowMayShowWhenLocked(incoming.copy(id=null,status="Call ended")))
        assertFalse(callWindowMayShowWhenLocked(null))
    }
}
