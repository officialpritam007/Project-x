package com.pritam.liquidchat.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeletedMessageSearchTest {
    @Test fun delayedHistoryCannotOverwriteAcceptedTombstone() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        val db=Room.inMemoryDatabaseBuilder(context,LocalDatabase::class.java).build()
        try {
            val dao=db.dao()
            val old=Message("account","voice","chat","peer","secret-value",status="SENT",seq=4,
                mediaId="old-audio",mediaKind="voice")
            val tombstone=scrubDeletedMessage(old.copy(deletedAt="2026-10-19T05:00:00Z"))
            dao.putPreservingAcceptance(tombstone)
            dao.putPreservingAcceptance(old)
            assertEquals(tombstone,dao.message("account","voice"))
            assertTrue(dao.search("account","secret-value").isEmpty())
        } finally {db.close()}
    }

    @Test fun localSearchExcludesDeletedMessageEvenIfOldRoomBodyIsStillPresent() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        val db=Room.inMemoryDatabaseBuilder(context,LocalDatabase::class.java).build()
        try {
            val dao=db.dao()
            dao.put(Message("account","deleted","chat","peer","secret-value",
                status="SENT",deletedAt="2026-10-19T05:00:00Z"))
            dao.put(Message("account","valid","chat","peer","secret-value",
                status="SENT"))
            dao.put(Message("other","foreign","chat","peer","secret-value",
                status="SENT"))
            val matches=dao.search("account","secret-value")
            assertEquals(listOf("valid"),matches.map {it.id})
            assertTrue(dao.search("other","secret-value").all {it.account=="other"})
        } finally {db.close()}
    }
}
