package com.pritam.liquidchat.data

import org.junit.Assert.*
import org.junit.Test

class NotificationTextPrivacyTest {
    private val secret="Private message with a personal URL and address"
    @Test fun hidingSenderAlsoMasksBodyAndNeverExposesTitle() {
        val sender="Private Contact"
        for(serverPreviews in listOf(false,true)) {
            for(hideBody in listOf(false,true)) {
                for(hideSender in listOf(false,true)) {
                    val effectiveBodyHide=hideBody || hideSender
                    if(hideSender) {
                        assertEquals("Liquid Chat",notificationTitleForDisplay(sender,true))
                        assertEquals("New message",notificationBodyForDisplay(secret,serverPreviews,effectiveBodyHide))
                        assertFalse(notificationBodyAllowed(serverPreviews,effectiveBodyHide))
                    } else {
                        assertEquals(sender,notificationTitleForDisplay(sender,false))
                    }
                }
            }
        }
    }

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
