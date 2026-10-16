package com.pritam.liquidchat.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalDatabaseTest {
    private lateinit var db:LocalDatabase
    @Before fun setup() {db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),LocalDatabase::class.java).build()}
    @After fun close() {db.close()}
    @Test fun retriesDeduplicateAndFailedMessagesNeedManualRequeue()=runBlocking {
        val dao=db.dao();val queued=Message("a","stable-id","chat","a","offline text",localTime=1)
        dao.put(queued);dao.put(queued.copy(status="SENDING",attempts=1))
        assertEquals(1,dao.pending("a").size)
        dao.put(queued.copy(status="FAILED",attempts=5));assertTrue(dao.pending("a").isEmpty())
        dao.put(queued.copy(status="QUEUED"));assertEquals("stable-id",dao.pending("a").single().id)
        dao.put(queued.copy(status="SENT",seq=10));dao.put(queued.copy(status="SENT",seq=10))
        assertTrue(dao.pending("a").isEmpty());assertEquals(1,dao.messages("a","chat").first().size)
    }
    @Test fun outgoingAcceptanceDoesNotAdvanceHistoryCatchupCursor()=runBlocking {
        val dao=db.dao();dao.putChats(listOf(Chat("a","chat","b","B","bee",syncSeq=3)))
        dao.put(Message("a","outgoing","chat","a","accepted before incoming catchup",seq=5,status="SENT"))
        assertEquals(3L,dao.chat("a","chat")!!.syncSeq)
        dao.put(Message("a","incoming","chat","b","arrived in the gap",seq=4,status="SENT"))
        dao.syncCursor("a","chat",5)
        assertEquals(listOf(4L,5L),dao.messages("a","chat").first().map {it.seq})
    }
    @Test fun clearingOneAccountDoesNotExposeOrRemoveAnotherAccount()=runBlocking {
        val dao=db.dao()
        dao.put(Message("a","same-id","chat","a","private A"));dao.put(Message("b","same-id","chat","b","private B"))
        dao.cacheProfile(CachedProfile("a","A"));dao.cacheProfile(CachedProfile("b","B"))
        dao.putContacts(listOf(Contact("a","contact","Contact A","alice"),Contact("b","contact","Contact B","bob")))
        dao.clearMessages("a");dao.clearChats("a");dao.clearContacts("a");dao.clearProfile("a")
        assertTrue(dao.messages("a","chat").first().isEmpty());assertNull(dao.profile("a"));assertTrue(dao.contacts("a").first().isEmpty())
        assertEquals("private B",dao.messages("b","chat").first().single().body);assertEquals("B",dao.profile("b")!!.json)
        assertEquals("Contact B",dao.contacts("b").first().single().name)
    }
    @Test fun deletionWatermarkRemovesOnlyAcceptedOldHistory()=runBlocking {
        val dao=db.dao();dao.putAll((1L..1000L).map {seq->Message("a","id-$seq","chat","b","message $seq",seq=seq,status="SENT")})
        dao.put(Message("a","queued","chat","a","still queued"))
        dao.prune("a","chat",999)
        assertEquals(setOf("id-1000","queued"),dao.messages("a","chat").first().map {it.id}.toSet())
    }
    @Test fun literalSearchKeepsOriginalSenderAndConversation()=runBlocking {
        val dao=db.dao()
        dao.put(Message("a","one","chat-one","salma","100% match"))
        dao.put(Message("a","two","chat-two","pritam","1000 match"))
        val result=dao.search("a","100\\%")
        assertEquals("salma",result.single().sender);assertEquals("chat-one",result.single().conversation)
    }
}
