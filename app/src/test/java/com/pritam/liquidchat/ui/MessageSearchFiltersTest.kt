package com.pritam.liquidchat.ui

import com.pritam.liquidchat.data.Message
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class MessageSearchFiltersTest {
    private val now=Instant.parse("2026-10-19T12:00:00Z").toEpochMilli()
    private val text=Message("owner","text","chat","owner","Hello",localTime=now)
    private val photo=text.copy(id="photo",sender="peer",mediaId="photo",body="HELLO photo")
    private val voice=text.copy(id="voice",sender="peer",mediaId="voice",mediaKind="voice",body="Voice message")
    private val rows=listOf(text,photo,voice,photo.copy(id="deleted",deletedAt="2026-10-19T11:00:00Z"),photo.copy(id="foreign",account="other"))

    @Test fun typeAndSenderFiltersWorkWithoutTypingAndNeverLeakDeletedOrOtherAccounts() {
        assertEquals(listOf(photo),filterMessageHistory(rows,"","owner",SearchSender.CONTACT,SearchMedia.IMAGE))
        assertEquals(listOf(text),filterMessageHistory(rows,"hello","owner",SearchSender.YOU))
        assertEquals(listOf(voice),filterMessageHistory(rows,"","owner",type=SearchMedia.VOICE))
        assertTrue(filterMessageHistory(rows,"hello",null).isEmpty())
    }
    @Test fun linkSearchUsesOnlyDownloadedOwnedVisibleMessages() {
        val linked=text.copy(id="linked",
            body="See https://example.org/story",mediaKind="image")
        val other=linked.copy(id="foreign-link",account="someone-else")
        val deleted=linked.copy(id="deleted-link",deletedAt="2026-10-19T11:00:00Z")
        val media=linked.copy(id="photo-link",mediaId="media-id")
        val rows=listOf(linked,other,deleted,media,text)
        assertEquals(listOf(linked),
            filterMessageHistory(rows,"","owner",type=SearchMedia.LINK))
        assertEquals(listOf(linked),
            filterMessageHistory(rows,"EXAMPLE.ORG","owner",type=SearchMedia.LINK))
        assertTrue(filterMessageHistory(rows,"","owner",type=SearchMedia.DOCUMENT).isEmpty())
    }

    @Test fun datesRespectLocalMidnightAndRollingWindows() {
        val yesterday=text.copy(id="yesterday",localTime=now-86_400_000L)
        val old=text.copy(id="old",localTime=now-31*86_400_000L)
        val list=listOf(text,yesterday,old)
        val zone=ZoneId.of("Asia/Kolkata")
        assertEquals(listOf(text),filterMessageHistory(list,"hello","owner",period=SearchPeriod.TODAY,now=now,zone=zone))
        assertEquals(listOf(text,yesterday),filterMessageHistory(list,"hello","owner",period=SearchPeriod.WEEK,now=now,zone=zone))
        assertEquals(listOf(text,yesterday),filterMessageHistory(list,"hello","owner",period=SearchPeriod.MONTH,now=now,zone=zone))
    }
}
