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
            db.close();db=open()
            assertEquals("Hello বাংলা 👋",db.dao().draft("a","chat")?.body)
            assertEquals("original",db.dao().draft("a","chat")?.replyTo)
            assertNull(db.dao().draft("a","other-chat"))
            assertEquals("Lilac",db.dao().appearance("a","chat")?.palette)
            db.dao().clearDrafts("a");db.dao().clearAppearance("a")
            assertNull(db.dao().draft("a","chat"));assertNull(db.dao().appearance("a","chat"))
            assertEquals("Other account",db.dao().draft("b","chat")?.body)
        } finally {db.close();context.deleteDatabase(name)}
    }
}
