package com.pritam.liquidchat.ui

import com.pritam.liquidchat.data.Message
import org.junit.Assert.*
import org.junit.Test

class SharedMediaPaginationTest {
    @Test fun deletionDuringPageLoadCannotBeUndoneByItsOlderSnapshot() {
        val photo=Message("owner","photo","chat","peer","caption",seq=4,mediaId="private")
        val other=photo.copy(id="other",seq=3)
        assertEquals(listOf(other),mergeSharedMediaRows(listOf(photo),listOf(photo,other),false,setOf("photo")))
        assertEquals(listOf(other),mergeSharedMediaRows(listOf(photo),listOf(photo,other),true,setOf("photo")))
        assertEquals(listOf(photo,other),mergeSharedMediaRows(listOf(photo),listOf(photo,other),false,emptySet()))
    }
    private fun page(deleted:Boolean)= (60L downTo 31L).map {seq->
        Message("owner","id-$seq","chat","peer","body",seq=seq,
            deletedAt=if(deleted||seq==31L)"2026-10-19T00:00:00Z" else "")
    }
    @Test fun cursorUsesRawPageEvenWhenLastRowOrEntirePageWasDeleted() {
        val mixed=sharedMediaPage(page(false),Long.MAX_VALUE)
        assertEquals(29,mixed.visible.size)
        assertEquals(31L,mixed.cursor)
        assertTrue(mixed.more)
        val deleted=sharedMediaPage(page(true),Long.MAX_VALUE)
        assertTrue(deleted.visible.isEmpty())
        assertEquals(31L,deleted.cursor)
        assertTrue(deleted.more)
    }
    @Test fun brokenOrEmptyPageCannotLoopForever() {
        assertFalse(sharedMediaPage(emptyList(),31).more)
        assertFalse(sharedMediaPage(page(false),31).more)
    }
    @Test fun doubleTapCannotAppendConcurrentlyAndRefreshDiscardsOldPage() {
        val gate=SharedMediaRequests()
        val first=gate.begin(true)!!
        assertNull(gate.begin(false))
        val refresh=gate.begin(true)!!
        assertFalse(gate.current(first))
        gate.finish(first)
        assertNull(gate.begin(false))
        assertTrue(gate.current(refresh))
        gate.finish(refresh)
        assertNotNull(gate.begin(false))
    }
}
