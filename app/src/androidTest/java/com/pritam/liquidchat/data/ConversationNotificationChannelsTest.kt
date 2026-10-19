package com.pritam.liquidchat.data

import android.app.NotificationManager
import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConversationNotificationChannelsTest {
    @Test fun perChatChannelsAreOptInAndRemainAccountIsolated() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val manager=context.getSystemService(NotificationManager::class.java)
        val accountA="notification-test-account-a"
        val accountB="notification-test-account-b"
        val cid="test-chat-1"
        ConversationNotificationChannels.clearAccount(context,accountA)
        ConversationNotificationChannels.clearAccount(context,accountB)
        try {
            assertEquals("messages",ConversationNotificationChannels.channelForMessage(context,accountA,cid,"Ada"))
            assertFalse(ConversationNotificationChannels.enabled(context,accountA,cid))
            ConversationNotificationChannels.setEnabled(context,accountA,cid,"Ada",true)
            val id=ConversationNotificationChannels.channelId(accountA,cid)
            assertEquals(id,ConversationNotificationChannels.channelForMessage(context,accountA,cid,"Ada"))
            assertNotNull(manager.getNotificationChannel(id))
            assertNotEquals(id,ConversationNotificationChannels.channelId(accountB,cid))
            assertEquals("messages",ConversationNotificationChannels.channelForMessage(context,accountB,cid,"Ada"))
            val intent=ConversationNotificationChannels.settingsIntent(context,accountA,cid)
            assertEquals(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS,intent.action)
            assertEquals(id,intent.getStringExtra(Settings.EXTRA_CHANNEL_ID))
            assertEquals(context.packageName,intent.getStringExtra(Settings.EXTRA_APP_PACKAGE))
            ConversationNotificationChannels.setEnabled(context,accountA,cid,"Ada",false)
            assertEquals("messages",ConversationNotificationChannels.channelForMessage(context,accountA,cid,"Ada"))
        } finally {
            ConversationNotificationChannels.clearAccount(context,accountA)
            ConversationNotificationChannels.clearAccount(context,accountB)
        }
    }

    @Test fun cleanupDeletesOnlyTheSignedOutAccountsChannels() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val manager=context.getSystemService(NotificationManager::class.java)
        val a="notification-test-account-cleanup"
        val b="notification-test-account-other"
        val cid="test-chat-2"
        try {
            ConversationNotificationChannels.setEnabled(context,a,cid,"Alice",true)
            ConversationNotificationChannels.setEnabled(context,b,cid,"Bob",true)
            val first=ConversationNotificationChannels.channelId(a,cid)
            val other=ConversationNotificationChannels.channelId(b,cid)
            ConversationNotificationChannels.clearAccount(context,a)
            assertNull(manager.getNotificationChannel(first))
            assertNotNull(manager.getNotificationChannel(other))
            assertFalse(ConversationNotificationChannels.enabled(context,a,cid))
            assertTrue(ConversationNotificationChannels.enabled(context,b,cid))
        } finally {
            ConversationNotificationChannels.clearAccount(context,a)
            ConversationNotificationChannels.clearAccount(context,b)
        }
    }
}
