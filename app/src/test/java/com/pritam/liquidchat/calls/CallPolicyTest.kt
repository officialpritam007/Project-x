package com.pritam.liquidchat.calls

import org.junit.Assert.*
import org.junit.Test

class CallPolicyTest {
    @Test fun automaticHeadsetManualSelectionAndDisconnectFallback() {
        val devices=listOf(10 to 1,20 to 2,30 to 7)
        assertEquals(30,preferredCallDevice(devices,null,true))
        assertEquals(20,preferredCallDevice(devices,20,false))
        assertEquals(10,preferredCallDevice(devices.filter {it.first!=30},30,false))
        assertEquals(20,preferredCallDevice(devices.filter {it.first!=30},30,true))
        assertNull(preferredCallDevice(emptyList(),30,true))
    }
    @Test fun bleAndWiredHeadsetsBeforeBuiltInFallback() {
        assertEquals(4,preferredCallDevice(listOf(1 to 2,4 to 26),null,true))
        assertEquals(5,preferredCallDevice(listOf(1 to 2,5 to 22),null,true))
    }
    @Test fun controlsStayAvailableForMenusPermissionsAndAccessibility() {
        assertTrue(canAutoHideCallControls("Connected",false,false,false))
        assertFalse(canAutoHideCallControls("Connected",true,false,false))
        assertFalse(canAutoHideCallControls("Connected",false,true,false))
        assertFalse(canAutoHideCallControls("Connected",false,false,true))
        listOf("Incoming call","Calling","Ringing","On hold","Reconnecting","Ended").forEach {
            assertFalse(canAutoHideCallControls(it,false,false,false))
        }
    }
    @Test fun ringingRequiresDeviceAcknowledgement() {
        assertEquals("Calling",invitationStatus(false))
        assertEquals("Ringing",invitationStatus(true))
    }
    @Test fun temporaryHoldCannotUndoCameraOffOrMute() {
        val intent=CaptureIntent(muted=true,camera=false)
        assertFalse(intent.copy(held=true).sendAudio)
        assertFalse(intent.copy(held=true).captureVideo)
        assertFalse(intent.copy(held=false).sendAudio)
        assertFalse(intent.copy(held=false).captureVideo)
        assertTrue(CaptureIntent().sendAudio)
        assertFalse(CaptureIntent(held=true).captureVideo)
    }
}
