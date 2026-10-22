package com.pritam.liquidchat.ui

/** Deterministic presentation-only back-stack behavior for conversation overlays. */
internal enum class ConversationBackAction {CloseSearch,ClearSelection,CloseChat}

internal fun conversationBackAction(searchOpen:Boolean,selectionActive:Boolean):ConversationBackAction =
    when {
        searchOpen -> ConversationBackAction.CloseSearch
        selectionActive -> ConversationBackAction.ClearSelection
        else -> ConversationBackAction.CloseChat
}
