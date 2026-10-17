package com.pritam.liquidchat.data

import org.junit.Assert.assertTrue
import org.junit.Test

class DraftVersionsTest {
    @Test fun revisionsAdvancePastRestoredDraftsAndRapidEdits() {
        val restored=System.currentTimeMillis()+100000
        DraftVersions.observe(restored)
        val versions=(1..1000).map {DraftVersions.next()}
        assertTrue(versions.first()>restored)
        assertTrue(versions.zipWithNext().all {(a,b)->b>a})
    }
}
