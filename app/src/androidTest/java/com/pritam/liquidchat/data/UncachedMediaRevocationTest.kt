package com.pritam.liquidchat.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/** Old paginated gallery items may be absent from Room; their deletion must still reach live viewers. */
class UncachedMediaRevocationTest {
    @Test fun uncachedDeletedSnapshotEmitsScopedRevocationWithoutCachingBody():Unit=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val db=Room.inMemoryDatabaseBuilder(context,LocalDatabase::class.java).build()
        try {
            val backend=Backend(SessionStore(context,"revocation-fixture"))
            val repository=ChatRepository(context,backend,db)
            val event=async(start=CoroutineStart.UNDISPATCHED) {withTimeout(3_000){repository.revocations.first()}}
            repository.applySnapshot(JSONObject().put("id","old-voice")
                .put("deleted_at","2026-10-19T07:00:00Z").put("body","old-private-body"),"owner",existingOnly=true)
            assertEquals(MessageRevocation("owner","old-voice"),event.await())
            assertNull(repository.dao.message("owner","old-voice"))
            assertTrue(repository.dao.search("owner","old-private-body").isEmpty())
        } finally {db.close()}
    }

    @Test fun uncachedHiddenSnapshotAlsoRevokesWithoutAHistoryRow():Unit=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val db=Room.inMemoryDatabaseBuilder(context,LocalDatabase::class.java).build()
        try {
            val repository=ChatRepository(context,Backend(SessionStore(context,"revocation-hide-fixture")),db)
            val event=async(start=CoroutineStart.UNDISPATCHED) {withTimeout(3_000){repository.revocations.first()}}
            repository.applySnapshot(JSONObject().put("id","old-photo").put("hidden",true),"owner",existingOnly=true)
            assertEquals(MessageRevocation("owner","old-photo"),event.await())
            assertTrue(repository.dao.hidden("owner","old-photo"))
            assertNull(repository.dao.message("owner","old-photo"))
        } finally {db.close()}
    }
}
