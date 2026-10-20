package com.pritam.liquidchat.data

import org.junit.Assert.*
import org.junit.Test

class NotificationTextPrivacyTest {
    private val secret="Private message with a personal URL and address"
    @Test fun aPerChatSettingCanOnlyMakeGlobalPreviewPolicyStricter() {
        assertEquals(secret,notificationBodyForDisplay(secret,true,false))
        assertTrue(notificationBodyAllowed(true,false))
        for(server in listOf(true,false)) {
            for(hide in listOf(true,false)) {
                if(!server || hide) {
                    assertFalse(notificationBodyAllowed(server,hide))
                    assertEquals("New message",notificationBodyForDisplay(secret,server,hide))
                }
            }
        }
    }
}
