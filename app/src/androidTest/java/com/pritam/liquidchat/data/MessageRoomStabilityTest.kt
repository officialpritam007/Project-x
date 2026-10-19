package com.pritam.liquidchat.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MessageRoomStabilityTest {
    @Test fun duplicateAndStaleSnapshotsCannotOverwriteServerAcceptance() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        val db=Room.inMemoryDatabaseBuilder(context,LocalDatabase::class.java).build()
        try {
            val dao=db.dao()
            val accepted=Message("account","id","chat","peer","Hello",seq=4,
                localTime=1000,serverTime="2026-10-19T00:00:00Z",status="SENT")
            dao.putPreservingAcceptance(accepted)
            dao.putPreservingAcceptance(accepted.copy())
            dao.putPreservingAcceptance(accepted.copy(status="FAILED",seq=0,error="offline"))
            assertEquals(accepted,dao.message("account","id"))
            val edited=accepted.copy(body="Edited",editedAt="2026-10-19T00:10:00Z")
            dao.putPreservingAcceptance(edited)
            assertEquals(edited,dao.message("account","id"))
        } finally {db.close()}
    }

    @Test fun serverCursorsNeverGoBackwardsOrReapplySameVersion() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        val db=Room.inMemoryDatabaseBuilder(context,LocalDatabase::class.java).build()
        try {
            val dao=db.dao()
            dao.putChats(listOf(Chat("account","chat","peer","Peer","peer")))
            dao.syncCursor("account","chat",12)
            dao.syncCursor("account","chat",7)
            dao.syncCursor("account","chat",12)
            dao.changeCursor("account","chat",8)
            dao.changeCursor("account","chat",5)
            dao.changeCursor("account","chat",8)
            val chat=dao.chat("account","chat")
            assertNotNull(chat)
            assertEquals(12L,chat!!.syncSeq)
            assertEquals(8L,chat.changeCursor)
        } finally {db.close()}
    }
}
