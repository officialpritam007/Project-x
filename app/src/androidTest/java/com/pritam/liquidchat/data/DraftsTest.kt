package com.pritam.liquidchat.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class DraftsTest {
    @Test fun draftsAndAppearanceSurviveReopenWithoutCrossAccountLeakage()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val name="draft-fixture.db"
        context.deleteDatabase(name)
        fun open()=Room.databaseBuilder(context,LocalDatabase::class.java,name).build()
        var db=open()
        try {
            db.dao().putDraft(Draft("a","chat","Hello বাংলা 👋",replyTo="original"))
            db.dao().putDraft(Draft("b","chat","Other account"))
            db.dao().putAppearance(ChatAppearance("a","chat","Lilac"))
            db.dao().hide(HiddenMessage("a","hidden-id"))
            db.close();db=open()
            assertEquals("Hello বাংলা 👋",db.dao().draft("a","chat")?.body)
            assertEquals("original",db.dao().draft("a","chat")?.replyTo)
            assertTrue(db.dao().hidden("a","hidden-id"));assertFalse(db.dao().hidden("b","hidden-id"))
            assertNull(db.dao().draft("a","other-chat"))
            assertEquals("Lilac",db.dao().appearance("a","chat")?.palette)
            db.dao().clearDrafts("a");db.dao().clearAppearance("a")
            assertNull(db.dao().draft("a","chat"));assertNull(db.dao().appearance("a","chat"))
            assertEquals("Other account",db.dao().draft("b","chat")?.body)
        } finally {db.close();context.deleteDatabase(name)}
    }

    @Test fun olderSendCannotConsumeANewerIdenticalDraftOrReply()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val db=Room.inMemoryDatabaseBuilder(context,LocalDatabase::class.java).build()
        try {
            val dao=db.dao()
            dao.putDraft(Draft("a","chat","Same text","reply",updatedAt=10))
            // User edits away, then restores exactly the same text/reply before send returns.
            dao.putDraft(Draft("a","chat","Same text","reply",updatedAt=12))
            assertEquals(0,dao.consumeDraft("a","chat","Same text","reply",10))
            assertEquals(12L,dao.draft("a","chat")!!.updatedAt)
            assertEquals(0,dao.consumeDraft("b","chat","Same text","reply",12))
            assertEquals(0,dao.consumeDraft("a","chat","Same text","other-reply",12))
            assertEquals(1,dao.consumeDraft("a","chat","Same text","reply",12))
            assertNull(dao.draft("a","chat"))
        } finally {db.close()}
    }
}
