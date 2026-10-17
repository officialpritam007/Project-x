package com.pritam.liquidchat.data

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class LocalLogoutTest {
    @Test fun offlineRevocationStillErasesLocalStateAndReportsUnconfirmed()=runBlocking {
        var cleared=false
        val revoked=localLogout({throw IOException("offline")},{cleared=true})
        assertFalse(revoked);assertTrue(cleared)
    }
    @Test fun successfulRevocationAlsoClearsLocalState()=runBlocking {
        val events=mutableListOf<String>()
        assertTrue(localLogout({events+="remote"},{events+="local"}))
        assertEquals(listOf("remote","local"),events)
    }
    @Test fun cancellationWaitsForLocalCleanupAndPropagates()=runBlocking {
        var cleared=false
        val job=launch(start=CoroutineStart.UNDISPATCHED) {
            localLogout({awaitCancellation()},{delay(10);cleared=true})
        }
        job.cancelAndJoin()
        assertTrue(job.isCancelled);assertTrue(cleared)
    }
    @Test fun localErasureFailureIsNeverReportedAsSuccess()=runBlocking {
        try {localLogout({},{throw IOException("storage unavailable")});fail("Expected cleanup failure")}
        catch(e:IOException){assertEquals("storage unavailable",e.message)}
    }
}
