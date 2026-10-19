package com.pritam.liquidchat.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationPermissionLabelTest {
    @Test fun androidPermissionStatesAreExplicitAndNotConfusedWithPreviewToggle() {
        assertEquals("Allowed on this device",notificationPermissionLabel(true))
        assertEquals("Blocked by Android settings",notificationPermissionLabel(false))
    }
}
