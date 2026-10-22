package com.pritam.liquidchat.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceAlertPreferencesTest {
    @Test fun disabledNotificationsAndAutoHideAreAccountScopedAndCleared() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val a="test-notify-owner-a"
        val b="test-notify-owner-b"
        DeviceAlertPreferences.clearAccount(context,a)
        DeviceAlertPreferences.clearAccount(context,b)
        try {
            assertFalse(DeviceAlertPreferences.disableAll(context,a))
            assertFalse(DeviceAlertPreferences.autoHideMessages(context,a))
            DeviceAlertPreferences.update(context,a,disabled=true,autoHide=true)
            assertTrue(DeviceAlertPreferences.disableAll(context,a))
            assertTrue(DeviceAlertPreferences.autoHideMessages(context,a))
            assertFalse(DeviceAlertPreferences.disableAll(context,b))
            assertFalse(DeviceAlertPreferences.autoHideMessages(context,b))
            DeviceAlertPreferences.update(context,a,disabled=false)
            assertFalse(DeviceAlertPreferences.disableAll(context,a))
            assertTrue(DeviceAlertPreferences.autoHideMessages(context,a))
        } finally {
            DeviceAlertPreferences.clearAccount(context,a)
            DeviceAlertPreferences.clearAccount(context,b)
        }
        assertFalse(DeviceAlertPreferences.autoHideMessages(context,a))
    }
}
