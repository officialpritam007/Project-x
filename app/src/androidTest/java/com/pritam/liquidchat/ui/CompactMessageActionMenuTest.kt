package com.pritam.liquidchat.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Menu actions must remain usable over a frosted popup on compact devices. */
class CompactMessageActionMenuTest {
    @get:Rule val compose=createComposeRule()

    @Test fun compactMenuSupportsPrimaryActionsAndAccessibleMore() {
        var replies=0
        var opensMore=0
        compose.setContent {
            LiquidTheme(false) {
                CompactMessageActionMenu(
                    preview="A sample private message",
                    reactions=listOf("❤️","👍"),
                    canReply=true,canCopy=true,canForward=true,canStar=true,
                    starred=false,canDelete=true,
                    onReact={},onReply={replies++},onCopy={},onForward={},
                    onStar={},onSelect={},onDelete={},onMore={opensMore++},
                    onDismiss={}
                )
            }
        }
        compose.onNodeWithText("A sample private message").assertExists()
        compose.onNodeWithText("Reply").performClick()
        compose.runOnIdle {assertEquals(1,replies)}
        compose.onNodeWithText("More").performClick()
        compose.runOnIdle {assertEquals(1,opensMore)}
        compose.onNodeWithText("Delete message").assertExists()
    }

    @Test fun unavailableActionsRemainDisabled() {
        var forwards=0
        compose.setContent {
            LiquidTheme(true) {
                CompactMessageActionMenu(
                    preview="Deleted content",
                    reactions=emptyList(),
                    canReply=false,canCopy=false,canForward=false,canStar=false,
                    starred=false,canDelete=false,
                    onReact={},onReply={},onCopy={},onForward={forwards++},
                    onStar={},onSelect={},onDelete={},onMore={},onDismiss={}
                )
            }
        }
        compose.onNodeWithText("Forward").assertIsNotEnabled()
        compose.onNodeWithText("Delete message").assertDoesNotExist()
        compose.runOnIdle {assertEquals(0,forwards)}
    }
}
