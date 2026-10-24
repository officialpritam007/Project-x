package com.pritam.liquidchat.ui

import com.pritam.liquidchat.data.Message

/** A refresh supersedes every older page, including a manually launched append. */
internal class SharedMediaRequests {
    private var generation=0L
    private var loading=false
    fun begin(reset:Boolean):Long? {
        if(!reset&&loading)return null
        loading=true
        return ++generation
    }
    fun current(request:Long)=request==generation
    fun finish(request:Long) {if(current(request))loading=false}
}

internal data class SharedMediaPage(val visible:List<Message>,val cursor:Long?,val more:Boolean)
internal fun sharedMediaPage(raw:List<Message>,before:Long):SharedMediaPage {
    val cursor=raw.lastOrNull()?.seq
    return SharedMediaPage(raw.filter {it.deletedAt.isEmpty()},cursor,
        raw.size==30&&cursor!=null&&cursor>0&&cursor<before)
}

internal fun mergeSharedMediaRows(existing:List<Message>,page:List<Message>,reset:Boolean,revoked:Set<String>):List<Message> =
    (if(reset)page else existing+page).filter {it.deletedAt.isEmpty()&&it.id !in revoked}.distinctBy {it.id}

/**
 * Only media/starred changes require reloading their RPC pages; ordinary text
 * activity should not blank the gallery or re-download signed thumbnails.
 */
internal fun sharedMediaRefreshKeys(messages:List<Message>,kind:String):List<String> =
    messages.asSequence().filter {message->
        message.deletedAt.isEmpty() && when(kind) {
            "star"->message.starred
            "voice"->message.mediaKind=="voice" && message.mediaId.isNotEmpty()
            "sticker"->message.mediaKind=="sticker" && message.mediaId.isNotEmpty()
            "document"->message.mediaKind=="document" && message.mediaId.isNotEmpty()
            "chat_video"->message.mediaKind=="chat_video" && message.mediaId.isNotEmpty()
            else->message.mediaKind=="image" && message.mediaId.isNotEmpty()
        }
    }.map {message->
        "${message.id}|${message.mediaId}|${message.editedAt}|${message.starred}"
    }.toList()

/**
 * Starred entries may be text, photos, or voice. Use the existing protected
 * media renderers rather than exposing only the caption or a media identifier.
 */
internal fun starredMediaPresentation(message:Message):String = when {
    message.deletedAt.isNotEmpty() -> "hidden"
    message.mediaKind=="voice" && message.mediaId.isNotEmpty() -> "voice"
    message.mediaKind=="image" && message.mediaId.isNotEmpty() -> "image"
    message.mediaKind=="sticker" && message.mediaId.isNotEmpty() -> "sticker"
    message.mediaKind=="document" && message.mediaId.isNotEmpty() -> "document"
    message.mediaKind=="chat_video" && message.mediaId.isNotEmpty() -> "chat_video"
    else -> "text"
}

