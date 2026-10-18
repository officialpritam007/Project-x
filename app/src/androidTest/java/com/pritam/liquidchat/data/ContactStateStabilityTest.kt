package com.pritam.liquidchat.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class ContactStateStabilityTest {
    @Test fun unchangedPresenceAndTypingDoNotReEmitEveryPoll() {
        val previous=JSONObject("""{"online":true,"typing":false,"last_seen":"2026-10-18T10:00:00Z"}""")
        val refreshed=JSONObject("""{"online":true,"typing":false,"last_seen":"2026-10-18T10:00:00Z"}""")
        assertSame(previous,stableContactSnapshot(previous,refreshed))
    }

    @Test fun genuinePresenceAndTypingChangesPublishImmediately() {
        val previous=JSONObject("""{"online":true,"typing":false}""")
        val next=JSONObject("""{"online":false,"typing":true}""")
        val result=stableContactSnapshot(previous,next)
        assertNotSame(previous,result)
        assertEquals(false,result.getBoolean("online"))
        assertEquals(true,result.getBoolean("typing"))
    }

    @Test fun errorSnapshotClearsVisiblePresenceWithoutRepeatFlicker() {
        val active=JSONObject("""{"online":true,"typing":true}""")
        val cleared=stableContactSnapshot(active,JSONObject())
        assertEquals("{}",cleared.toString())
        assertSame(cleared,stableContactSnapshot(cleared,JSONObject()))
    }
}
