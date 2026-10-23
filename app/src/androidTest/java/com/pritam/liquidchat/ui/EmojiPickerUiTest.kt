package com.pritam.liquidchat.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests the AndroidX picker integration, not the legacy hardcoded category bar. */
@RunWith(AndroidJUnit4::class)
class EmojiPickerUiTest {
    @get:Rule val compose=createComposeRule()

    @Test fun officialEmojiPickerIsEmbeddedWithNavigationAndSearch() {
        compose.setContent {LiquidTheme(false) {EmojiPicker(onEmoji={},onDismiss={})}}
        // AndroidView owns the category navigation and native long-press variants.
        // Compose must still expose the host, without relying on internal View ids.
        compose.onNodeWithTag("emoji-categories").assertExists()
        compose.onNodeWithTag("emoji-search").assertIsDisplayed()
    }

    @Test fun stickerTabOpensExistingLibraryWithoutSendingAutomatically() {
        var opens=0
        var inserted=0
        compose.setContent {
            LiquidTheme(false) {
                EmojiPicker(onEmoji={inserted++},onDismiss={},
                    onSavedStickers={opens++})
            }
        }
        compose.onNodeWithTag("stickers-tab").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(1,opens)
            assertEquals(0,inserted)
        }
    }

    @Test fun localSearchFindsOtherCategoryEmojiAndReportsNoResults() {
        compose.setContent {LiquidTheme(false) {EmojiPicker(onEmoji={},onDismiss={})}}
        compose.onNodeWithTag("emoji-search").performTextInput("football")
        compose.onNodeWithText("⚽").assertExists()
        compose.onNodeWithText("😀").assertDoesNotExist()
        compose.onNodeWithTag("emoji-search").performTextClearance()
        compose.onNodeWithTag("emoji-search").performTextInput("not-a-real-emoji-term")
        compose.onNodeWithText("No matching emoji").assertExists()
    }

    @Test fun tappingSearchResultInsertsExactlyOneEmoji() {
        val selected=mutableListOf<String>()
        compose.setContent {LiquidTheme(false) {
            EmojiPicker(onEmoji={selected.add(it)},onDismiss={},accountKey="test-fixture")
        }}
        compose.onNodeWithTag("emoji-search").performTextInput("football")
        compose.onNodeWithText("⚽").performClick()
        compose.runOnIdle {assertEquals(listOf("⚽"),selected)}
    }
}
