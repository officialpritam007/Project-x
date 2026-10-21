package com.pritam.liquidchat.ui

import com.pritam.liquidchat.data.Message
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Explicit user-requested export of downloaded, visible *sent* chat history.
 * Does not serialize remote signed URLs, media bytes, account identifiers,
 * tombstones, hidden messages or unread local drafts.
 */
internal object ChatTextExport {
    const val MAX_BYTES=512*1024
    const val MAX_MESSAGES=1000

    fun render(
        messages:List<Message>,account:String,conversation:String,contactName:String
    ):String {
        require(account.isNotBlank() && conversation.isNotBlank())
        val selected=messages.asSequence()
            .filter {it.account==account && it.conversation==conversation &&
                it.deletedAt.isEmpty() && it.status=="SENT"}
            .sortedBy {it.seq}
            .takeLastBounded(MAX_MESSAGES)
        val formatter=DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
            .withZone(ZoneId.systemDefault())
        val title="Liquid Chat · Downloaded chat history\n" +
            "Contact: ${contactName.take(100)}\n" +
            "Only visible, downloaded messages on this phone are included.\n\n"
        val result=StringBuilder(title)
        selected.forEach {message->
            val safe=when(message.mediaKind) {
                "voice"->"[Voice message]"
                "sticker"->"[Sticker]"
                "image"->if(message.mediaId.isNotEmpty()||
                    message.localImage.isNotEmpty())"[Photo] ${message.body.take(1000)}"
                    else message.body
                else->"[Attachment]"
            }.take(4000)
            val author=if(message.sender==account)"You" else contactName.take(100)
            val line="[${formatter.format(Instant.ofEpochMilli(message.localTime))}] ${author}: ${safe}\n"
            if((result.length+line.length)*3>MAX_BYTES) {
                result.append("\n[Export size limit reached]\n")
                return@forEach
            }
            result.append(line)
        }
        val text=result.toString()
        require(text.toByteArray(Charsets.UTF_8).size<=MAX_BYTES)
        return text
    }

    private fun <T> Sequence<T>.takeLastBounded(limit:Int):List<T> {
        val queue=ArrayDeque<T>()
        for(item in this) {
            if(queue.size>=limit)queue.removeFirst()
            queue.addLast(item)
        }
        return queue.toList()
    }
}
