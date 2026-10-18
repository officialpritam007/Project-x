package com.pritam.liquidchat.ui

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/** No network: retain pagination even if one server row cannot be rendered. */
class ReadPageParsingTest {
    private fun call(index:Int)=JSONObject()
        .put("id","call-$index").put("conversation_id","chat").put("peer_id","peer")
        .put("caller_id","account").put("display_name","Contact").put("avatar_id",JSONObject.NULL)
        .put("kind","video").put("state","ended").put("created_at","2026-10-18T12:00:00Z")

    @Test fun filteredCallRowsDoNotHideOlderPageOrChangeServerCursor() {
        val raw=JSONArray()
        repeat(49) {raw.put(call(it))}
        val last=JSONObject().put("created_at","2026-10-18T11:00:00Z")
        raw.put(last)
        val page=callHistoryPage(raw,"account")
        assertEquals(49,page.rows.size)
        assertTrue(page.more)
        assertEquals("2026-10-18T11:00:00Z",page.before)
        assertEquals("",page.rows.first().avatar)
        assertTrue(page.rows.first().outgoing)
    }

    @Test fun emptyCallPageStopsPagination() {
        val page=callHistoryPage(JSONArray(),"account")
        assertTrue(page.rows.isEmpty())
        assertFalse(page.more)
        assertNull(page.before)
    }
}
