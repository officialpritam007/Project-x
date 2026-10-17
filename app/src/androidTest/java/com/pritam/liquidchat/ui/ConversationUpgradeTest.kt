package com.pritam.liquidchat.ui

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.pritam.liquidchat.data.Message
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class ConversationUpgradeTest {
    @get:Rule val compose=createComposeRule()
    @Test fun readingOlderHistoryKeepsItsAnchorOnNewMessagesPrependAndReceiptChanges() {
        fun m(i:Int)=Message("a","id-$i","chat","b","Message $i",seq=i.toLong(),localTime=1700000000000L+i*1000,status="SENT")
        var messages by mutableStateOf((1..1000).map(::m))
        compose.setContent {LiquidTheme(false) {MessageTimeline("chat",messages,false,null,{},loadOlder={false},modifier=Modifier.fillMaxSize()) {Text(it.body,Modifier.height(androidx.compose.ui.unit.Dp(50f)))}}}
        compose.onNodeWithTag("message-timeline").performScrollToNode(hasText("Message 50"))
        compose.onNodeWithText("Message 50").assertIsDisplayed()
        val before=compose.onNodeWithText("Message 50").fetchSemanticsNode().boundsInRoot.top
        compose.runOnIdle {messages=messages+m(1001)}
        compose.onNodeWithText("Message 50").assertIsDisplayed()
        assertEquals(before,compose.onNodeWithText("Message 50").fetchSemanticsNode().boundsInRoot.top,.5f)
        compose.runOnIdle {messages=listOf(m(0))+messages.map {it.copy(error="receipt refresh")}}
        compose.onNodeWithText("Message 50").assertIsDisplayed()
        assertEquals(before,compose.onNodeWithText("Message 50").fetchSemanticsNode().boundsInRoot.top,.5f)
        compose.onNodeWithContentDescription("Jump to latest messages").performClick()
        compose.onNodeWithText("Message 1001").assertIsDisplayed()
        compose.runOnIdle {messages=messages+m(1002)}
        compose.onNodeWithText("Message 1002").assertIsDisplayed()
    }
    @Test fun emojiPickerOffersRealChoicesAndLocalizedDatesAreCorrect() {
        var picked=""
        compose.setContent {LiquidTheme(false) {EmojiPicker({picked=it},{})}}
        compose.onNodeWithText("😀").performClick();compose.runOnIdle {assertEquals("😀",picked)}
        compose.onNodeWithText("Hearts").performClick();compose.onNodeWithText("❤️").performClick()
        compose.runOnIdle {assertEquals("❤️",picked)}
        val today=LocalDate.of(2026,10,17)
        assertEquals("Today",dateLabel(today,today));assertEquals("Yesterday",dateLabel(today.minusDays(1),today))
    }
}
