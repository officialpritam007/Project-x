package com.pritam.liquidchat.ui

import org.junit.Assert.*
import org.junit.Test

class MomentAudienceTest {
    private fun moment(id:String,owner:String)=Moment(
        id=id,owner=owner,media="signed-private-media",name=owner,caption="",
        created="2026-10-24T00:00:00Z",expires=null,width=720,height=1280
    )

    @Test fun ownStoriesStayOutOfContactFeed() {
        val items=listOf(moment("self-1","current"),moment("peer-1","contact"),
            moment("self-2","current"),moment("peer-2","another"))
        assertEquals(listOf("self-1","self-2"),
            momentsForAudience(items,mine=true,userId="current").map {it.id})
        assertEquals(listOf("peer-1","peer-2"),
            momentsForAudience(items,mine=false,userId="current").map {it.id})
    }

    @Test fun missingLoginShowsNoPrivateFeed() {
        assertTrue(momentsForAudience(listOf(moment("a","other")),
            mine=false,userId=null).isEmpty())
    }

    @Test fun filterDoesNotMutateServerPageOrOrder() {
        val rows=listOf(moment("1","me"),moment("2","peer"))
        momentsForAudience(rows,mine=false,userId="me")
        assertEquals(listOf("1","2"),rows.map {it.id})
    }
}
