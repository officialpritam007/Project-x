package com.pritam.liquidchat.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MessagePruneStabilityTest {
    @Test fun onlyExistingStaleRowsNeedADelete() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        val db=Room.inMemoryDatabaseBuilder(context,LocalDatabase::class.java).build()
        try {
            val dao=db.dao()
            dao.put(Message("a","old","chat","peer","Old",seq=2,status="SENT"))
            dao.put(Message("a","recent","chat","peer","Recent",seq=4,status="SENT"))
            dao.put(Message("a","queued","chat","a","Pending",seq=0))
            dao.put(Message("a","other-chat","another","peer","Other chat",seq=1,status="SENT"))
            dao.put(Message("b","same-id-other-account","chat","peer","Other account",seq=1,status="SENT"))

            assertFalse(dao.hasPrunableMessages("a","chat",0))
            assertFalse(dao.hasPrunableMessages("a","chat",1))
            assertTrue(dao.hasPrunableMessages("a","chat",2))
            dao.prune("a","chat",2)
            assertFalse(dao.hasPrunableMessages("a","chat",2))
            assertEquals(null,dao.message("a","old"))
            assertEquals("Recent",dao.message("a","recent")?.body)
            assertEquals("Pending",dao.message("a","queued")?.body)
            assertEquals("Other chat",dao.message("a","other-chat")?.body)
            assertEquals("Other account",dao.message("b","same-id-other-account")?.body)
        } finally {db.close()}
    }

    @Test fun laterWatermarkStillPrunesRowsIntroducedAfterEarlierSync() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        val db=Room.inMemoryDatabaseBuilder(context,LocalDatabase::class.java).build()
        try {
            val dao=db.dao()
            dao.put(Message("a","late","chat","peer","Late",seq=5,status="SENT"))
            assertFalse(dao.hasPrunableMessages("a","chat",3))
            assertTrue(dao.hasPrunableMessages("a","chat",5))
            dao.prune("a","chat",5)
            assertFalse(dao.hasPrunableMessages("a","chat",5))
        } finally {db.close()}
    }
}
