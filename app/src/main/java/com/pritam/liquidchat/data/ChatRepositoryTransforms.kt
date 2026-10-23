package com.pritam.liquidchat.data

import org.json.JSONObject

/**
 * Reuse a render snapshot when an RPC returns identical presence/typing data.
 * JSONObject compares by identity, so replacing it every three seconds otherwise
 * causes avoidable Compose updates and visible header jitter.
 */
internal fun stableContactSnapshot(previous:JSONObject,incoming:JSONObject):JSONObject =
    if(previous.toString()==incoming.toString())previous else incoming

/**
 * Do not invalidate the entire inbox on every periodic or Realtime sync.
 * A changed chat is still upserted, including updated previews, receipts,
 * unread count, pinned/favourite/archive and synchronized cursors.
 */
internal fun changedChatRows(incoming:List<Chat>,existing:List<Chat>):List<Chat> {
    val previous=existing.associateBy {it.account to it.id}
    return incoming.filter {previous[it.account to it.id]!=it}
}

/** Never keep deleted text, attachments or reply metadata in newly cached tombstones. */
internal fun scrubDeletedMessage(message:Message):Message =
    if(message.deletedAt.isEmpty())message else message.copy(
        body="This message was deleted",mediaId="",localImage="",
        imageWidth=0,imageHeight=0,mediaKind="image",durationMs=0,
        replyTo="",reactions="{}",starred=false
    )

data class MessageRevocation(val account:String,val id:String)

internal fun changedContactRows(incoming:List<Contact>,existing:List<Contact>):List<Contact> {
    val previous=existing.associateBy {it.account to it.id}
    return incoming.filter {previous[it.account to it.id]!=it}
}

