package com.pritam.liquidchat.calls

import org.junit.Assert.*
import org.junit.Test

class IncomingCallRaceTest {
    @Test fun incomingNetworkResponseCannotReplaceNewerOutgoingCall() {
        val outgoing=CallView(null,"chat","peer","video",false,"Preparing")
        assertFalse(canPresentIncomingCall(outgoing,"owner","owner",true,7,7))
        assertTrue(canPresentIncomingCall(null,"owner","owner",true,7,7))
    }
    @Test fun logoutBackgroundAndSameAccountNewLoginRejectOldInvitations() {
        assertFalse(canPresentIncomingCall(null,"owner",null,true,7,null))
        assertFalse(canPresentIncomingCall(null,"owner","other",true,7,7))
        assertFalse(canPresentIncomingCall(null,"owner","owner",false,7,7))
        assertFalse(canPresentIncomingCall(null,"owner","owner",true,7,8))
        assertFalse(canPresentIncomingCall(null,"owner","owner",true,null,null))
    }
}
