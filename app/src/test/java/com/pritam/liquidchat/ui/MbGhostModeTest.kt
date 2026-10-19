package com.pritam.liquidchat.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MbGhostModeTest {
    @Test fun ghostModeOnlyEnabledWhenBothServerFlagsOff() {
        assertTrue(ghostModeActive(false,false))
        assertFalse(ghostModeActive(true,false))
        assertFalse(ghostModeActive(false,true))
        assertFalse(ghostModeActive(true,true))
    }

    @Test fun togglingChangesBothAccountFlagsTogether() {
        assertEquals(false to false,ghostModeSettings(true))
        assertEquals(true to true,ghostModeSettings(false))
    }
}
