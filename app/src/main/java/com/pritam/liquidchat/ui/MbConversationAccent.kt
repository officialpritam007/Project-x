package com.pritam.liquidchat.ui

import android.content.Context

/**
 * Per-conversation presentation color only. The backend owns all messaging,
 * conversation membership, notification, privacy and call state.
 *
 * Keys include the authenticated account so another login never inherits the
 * previous account's conversation appearance.
 */
internal object MbConversationAccent {
    val choices=listOf("Follow","Blue","Green","Purple","Rose","Amber")
    private const val PREFS="liquid_chat_conversation_accent_v1"
    private fun key(account:String,conversation:String)="$account:$conversation"

    fun read(context:Context,account:String,conversation:String):String {
        if(account.isBlank()||conversation.isBlank())return "Follow"
        return context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
            .getString(key(account,conversation),"Follow")
            ?.takeIf {it in choices} ?: "Follow"
    }

    fun save(context:Context,account:String,conversation:String,choice:String) {
        require(account.isNotBlank()&&conversation.isNotBlank())
        require(choice in choices)
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
            .putString(key(account,conversation),choice).apply()
    }
}
