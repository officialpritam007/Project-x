package com.pritam.liquidchat.ui

import org.junit.Assert.*
import org.junit.Test

class CallHistoryRowTest {
    private fun row(outgoing:Boolean=false,outcome:String="ended",answered:String?=null,ended:String?=null)=
        CallHistoryRow("call","conversation","peer","Contact","","video",outcome,
            "2026-10-18T12:00:00Z",answered,ended,outgoing)

    @Test fun missedFilterIncludesOnlyIncomingMissedCalls() {
        assertTrue(row(outcome="missed").missed)
        assertFalse(row(outgoing=true,outcome="missed").missed)
        assertEquals("No answer",row(outgoing=true,outcome="missed").direction)
        assertEquals("Declined",row(outgoing=true,outcome="declined").direction)
    }

    @Test fun callHistoryRedialRequestsSameMediaPermissionsAsChat() {
        assertArrayEquals(arrayOf(android.Manifest.permission.RECORD_AUDIO),
            callHistoryRequiredPermissions("audio"))
        assertArrayEquals(arrayOf(android.Manifest.permission.RECORD_AUDIO,android.Manifest.permission.CAMERA),
            callHistoryRequiredPermissions("video"))
    }

    @Test fun durationRequiresAConnectedCallAndNonNegativeTimes() {
        assertNull(row().durationSeconds)
        assertNull(row(answered="bad",ended="2026-10-18T12:00:30Z").durationSeconds)
        assertNull(row(answered="2026-10-18T12:00:30Z",ended="2026-10-18T12:00:00Z").durationSeconds)
        assertEquals(30L,row(answered="2026-10-18T12:00:00Z",ended="2026-10-18T12:00:30Z").durationSeconds)
    }
}
