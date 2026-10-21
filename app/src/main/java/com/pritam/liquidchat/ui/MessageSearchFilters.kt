package com.pritam.liquidchat.ui

import com.pritam.liquidchat.data.Message
import java.time.Instant
import java.time.ZoneId

internal enum class SearchSender(val label:String) {ANY("Anyone"),YOU("You"),CONTACT("Contact")}
internal enum class SearchMedia(val label:String) {ANY("Any type"),TEXT("Text"),IMAGE("Photos"),VIDEO("Videos"),STICKER("Stickers"),VOICE("Voice"),DOCUMENT("Documents")}
internal enum class SearchPeriod(val label:String) {ANY("Any time"),TODAY("Today"),WEEK("Past 7 days"),MONTH("Past 30 days")}

/** Applies only to downloaded, account-scoped history; never imply a server-wide search. */
internal fun filterMessageHistory(
    messages:List<Message>,query:String,account:String?,sender:SearchSender=SearchSender.ANY,
    type:SearchMedia=SearchMedia.ANY,period:SearchPeriod=SearchPeriod.ANY,
    now:Long=System.currentTimeMillis(),zone:ZoneId=ZoneId.systemDefault()
):List<Message> {
    if(account.isNullOrBlank())return emptyList()
    if(query.isBlank()&&sender==SearchSender.ANY&&type==SearchMedia.ANY&&period==SearchPeriod.ANY)return emptyList()
    val since=when(period) {
        SearchPeriod.ANY->Long.MIN_VALUE
        SearchPeriod.TODAY->Instant.ofEpochMilli(now).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
        SearchPeriod.WEEK->now-7*86_400_000L
        SearchPeriod.MONTH->now-30*86_400_000L
    }
    return messages.filter {m->
        val media=m.mediaId.isNotEmpty()||m.localImage.isNotEmpty()
        m.account==account && m.deletedAt.isEmpty() && m.body.contains(query.trim(),true) &&
            when(sender) {SearchSender.ANY->true;SearchSender.YOU->m.sender==account;SearchSender.CONTACT->m.sender!=account} &&
            when(type) {
                SearchMedia.ANY->true
                SearchMedia.TEXT->!media&&m.mediaKind!="voice"
                SearchMedia.IMAGE->media&&m.mediaKind=="image"
                SearchMedia.VIDEO->media&&m.mediaKind=="chat_video"
                SearchMedia.STICKER->media&&m.mediaKind=="sticker"
                SearchMedia.DOCUMENT->media&&m.mediaKind=="document"
                SearchMedia.VOICE->m.mediaKind=="voice"
            } && m.localTime>=since
    }
}
