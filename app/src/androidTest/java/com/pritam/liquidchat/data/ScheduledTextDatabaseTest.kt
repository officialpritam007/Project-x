package com.pritam.liquidchat.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Local-only scheduling remains isolated from immediate message delivery. */
@RunWith(AndroidJUnit4::class)
class ScheduledTextDatabaseTest {
    @Test fun schedulingDoesNotSendBeforeDueTimeAndIsIsolatedByAccount()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val db=Room.inMemoryDatabaseBuilder(context,LocalDatabase::class.java).build()
        try {
            val due=System.currentTimeMillis()+3_600_000L
            db.dao().putScheduledText(ScheduledText("alice","future-1","chat-1","Send later",sendAt=due))
            assertEquals(1,db.dao().scheduledTexts("alice","chat-1").first().size)
            assertTrue(db.dao().scheduledTexts("bob","chat-1").first().isEmpty())
            assertTrue(db.dao().pending("alice").isEmpty())
            assertEquals(0,db.dao().deleteScheduledText("bob","future-1"))
            assertEquals(1,db.dao().deleteScheduledText("alice","future-1"))
            assertTrue(db.dao().scheduledTexts("alice","chat-1").first().isEmpty())
        } finally {db.close()}
    }

    @Test fun clearingConversationSchedulesNeverDeletesRealMessages()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val db=Room.inMemoryDatabaseBuilder(context,LocalDatabase::class.java).build()
        try {
            val due=System.currentTimeMillis()+3_600_000L
            db.dao().put(Message("alice","sent-1","chat-1","alice","Already sent",status="SENT"))
            db.dao().putScheduledText(ScheduledText("alice","future-1","chat-1","Future message",sendAt=due))
            db.dao().clearScheduledConversation("alice","chat-1")
            assertNull(db.dao().scheduledText("alice","future-1"))
            assertEquals("Already sent",db.dao().message("alice","sent-1")?.body)
        } finally {db.close()}
    }
}
