package com.pritam.liquidchat.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class MbConversationBackTest {
    @Test fun searchClosesBeforeMultiSelectOrChat() {
        assertEquals(ConversationBackAction.CloseSearch,
            conversationBackAction(searchOpen=true,selectionActive=true))
    }
    @Test fun selectionClosesBeforeLeavingTheConversation() {
        assertEquals(ConversationBackAction.ClearSelection,
            conversationBackAction(searchOpen=false,selectionActive=true))
    }
    @Test fun defaultBackReturnsToChatList() {
        assertEquals(ConversationBackAction.CloseChat,
            conversationBackAction(searchOpen=false,selectionActive=false))
    }
}
