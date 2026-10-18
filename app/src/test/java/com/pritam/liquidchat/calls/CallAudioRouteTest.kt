package com.pritam.liquidchat.calls

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CallAudioRouteTest {
    @Test fun chooseActualBuiltInSpeakerOrEarpiece() {
        val earpieceType=1
        val speakerType=2
        val bluetoothType=7
        val devices=listOf(103 to bluetoothType,101 to earpieceType,102 to speakerType)
        assertEquals(101,matchingCommunicationDeviceId(devices,earpieceType))
        assertEquals(102,matchingCommunicationDeviceId(devices,speakerType))
    }

    @Test fun noSilentFallbackWhenRequestedRouteMissing() {
        val bluetoothType=7
        val speakerType=2
        val devices=listOf(103 to bluetoothType)
        assertNull(matchingCommunicationDeviceId(devices,speakerType))
        assertNull(matchingCommunicationDeviceId(emptyList(),speakerType))
    }
}
