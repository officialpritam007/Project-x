package com.pritam.liquidchat.ui

import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class MomentPageTest {
    private val now=Instant.parse("2026-10-18T12:00:00Z")
    private fun moment(index:Int,expires:String?)=Moment(
        "moment-$index","owner","media-$index","Contact","",
        now.minusSeconds(index.toLong()).toString(),expires,720,1280
    )

    @Test fun expiredFullPageKeepsItsServerCursorAndOlderPageAvailable() {
        val raw=(0 until 30).map {moment(it,now.minusSeconds(1).toString())}
        val page=momentPage("story",raw,now)
        assertTrue(page.items.isEmpty())
        assertTrue(page.more)
        assertEquals(raw.last(),page.cursor)
    }

    @Test fun expiredTailDoesNotMovePaginationBackToLastVisibleStory() {
        val raw=(0 until 30).map {moment(it,if(it<4)now.plusSeconds(60).toString() else now.toString())}
        val page=momentPage("story",raw,now)
        assertEquals(raw.take(4),page.items)
        assertEquals(raw.last().id,page.cursor?.id)
        assertTrue(page.more)
    }

    @Test fun expiryBoundaryAndMalformedExpiryNeverDisplayAnExpiredStory() {
        val timeless=moment(0,null)
        val active=moment(1,now.plusNanos(1).toString())
        val expired=moment(2,now.toString())
        val malformed=moment(3,"invalid-time")
        assertEquals(listOf(timeless,active),unexpiredMoments("story",listOf(timeless,active,expired,malformed),now))
    }

    @Test fun shortPostPagePreservesPostsAndExhaustsPagination() {
        val raw=listOf(moment(0,now.minusSeconds(60).toString()))
        val page=momentPage("post",raw,now)
        assertEquals(raw,page.items)
        assertFalse(page.more)
        assertEquals(raw.last(),page.cursor)
        assertNull(momentPage("post",emptyList(),now).cursor)
    }
}
