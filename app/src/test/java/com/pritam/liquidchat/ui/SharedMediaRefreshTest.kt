package com.pritam.liquidchat.ui

import com.pritam.liquidchat.data.Message
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SharedMediaRefreshTest {
    private fun message(id:String,body:String="Hello",mediaId:String="",kind:String="image") =
        Message(account="owner",id=id,conversation="chat",sender="peer",
            body=body,status="SENT",mediaId=mediaId,mediaKind=kind)

    @Test fun unrelatedTextDoesNotReloadImageOrVoicePages() {
        val image=message("photo",mediaId="private-photo")
        val before=listOf(image)
        val after=before+message("text",body="New text")
        assertEquals(sharedMediaRefreshKeys(before,"image"),sharedMediaRefreshKeys(after,"image"))
        assertEquals(sharedMediaRefreshKeys(before,"voice"),sharedMediaRefreshKeys(after,"voice"))
        assertEquals(sharedMediaRefreshKeys(before,"star"),sharedMediaRefreshKeys(after,"star"))
    }

    @Test fun completedNewMediaTriggersCorrectTabOnly() {
        val photo=message("photo",mediaId="photo-id")
        val voice=message("voice",body="Voice message",mediaId="voice-id",kind="voice")
        assertNotEquals(sharedMediaRefreshKeys(emptyList(),"image"),sharedMediaRefreshKeys(listOf(photo),"image"))
        assertEquals(sharedMediaRefreshKeys(emptyList(),"voice"),sharedMediaRefreshKeys(listOf(photo),"voice"))
        assertNotEquals(sharedMediaRefreshKeys(emptyList(),"voice"),sharedMediaRefreshKeys(listOf(voice),"voice"))
    }

    @Test fun deletedMediaDisappearsFromVisibleRefreshKeys() {
        val photo=message("photo",mediaId="photo-id")
        val deleted=photo.copy(deletedAt="2026-10-19T05:00:00Z")
        assertEquals(emptyList<String>(),sharedMediaRefreshKeys(listOf(deleted),"image"))
        assertNotEquals(sharedMediaRefreshKeys(listOf(photo),"image"),sharedMediaRefreshKeys(listOf(deleted),"image"))
    }

    @Test fun starAndUnstarCauseRefreshOnlyForStarredTab() {
        val normal=message("plain")
        val starred=normal.copy(starred=true)
        assertEquals(emptyList<String>(),sharedMediaRefreshKeys(listOf(normal),"star"))
        assertNotEquals(sharedMediaRefreshKeys(listOf(normal),"star"),sharedMediaRefreshKeys(listOf(starred),"star"))
        assertEquals(sharedMediaRefreshKeys(listOf(normal),"image"),sharedMediaRefreshKeys(listOf(starred),"image"))
    }

    @Test fun editingAStarredMessageRefreshesItsDetail() {
        val starred=message("starred",body="Old body").copy(starred=true)
        val changed=starred.copy(body="New body",editedAt="2026-10-19T05:11:00Z")
        assertNotEquals(sharedMediaRefreshKeys(listOf(starred),"star"),sharedMediaRefreshKeys(listOf(changed),"star"))
    }
}
