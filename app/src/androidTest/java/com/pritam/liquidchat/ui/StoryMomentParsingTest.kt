package com.pritam.liquidchat.ui

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Data returned by the authenticated RPC must support legacy photos and 30s videos. */
class StoryMomentParsingTest {
    @Test fun storyMediaMetadataParsesAndPreservesLegacyPhotoDefaults() {
        val photo=JSONObject()
            .put("id","photo-1").put("owner_id","owner")
            .put("media_id","media-p").put("display_name","Photo User")
            .put("caption","Hello").put("created_at","2026-10-18T12:00:00Z")
            .put("expires_at",JSONObject.NULL)
            .put("width",720).put("height",1280)
        val video=JSONObject()
            .put("id","video-1").put("owner_id","owner-2")
            .put("media_id","media-v").put("display_name","Video User")
            .put("caption","Evening").put("created_at","2026-10-18T13:00:00Z")
            .put("expires_at","2026-10-19T13:00:00Z")
            .put("width",405).put("height",720)
            .put("media_kind","story_video").put("duration_ms",30000)
        val moments=parseMoments(JSONArray().put(photo).put(video))
        assertEquals(2,moments.size)
        assertEquals("image",moments[0].mediaKind)
        assertEquals(0L,moments[0].durationMs)
        assertNull(moments[0].expires)
        assertEquals("story_video",moments[1].mediaKind)
        assertEquals(30000L,moments[1].durationMs)
        assertEquals("2026-10-19T13:00:00Z",moments[1].expires)
    }
}
