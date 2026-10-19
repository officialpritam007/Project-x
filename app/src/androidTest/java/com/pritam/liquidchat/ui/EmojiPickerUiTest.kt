package com.pritam.liquidchat.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EmojiPickerUiTest {
    @get:Rule val compose=createComposeRule()

    @Test fun categoryNamesAreAllReachableWithoutHorizontalClipping() {
        compose.setContent {LiquidTheme(false) {EmojiPicker(onEmoji={},onDismiss={})}}
        compose.onNodeWithText("Faces").assertExists()
        compose.onNodeWithText("Hearts").assertExists()
        compose.onNodeWithText("Gestures").assertExists()
        compose.onNodeWithText("More").assertExists()
    }

    @Test fun searchFindsOtherCategoryEmojiAndReportsNoResults() {
        compose.setContent {LiquidTheme(false) {EmojiPicker(onEmoji={},onDismiss={})}}
        compose.onNodeWithTag("emoji-search").performTextInput("football")
        compose.onNodeWithText("⚽").assertExists()
        compose.onNodeWithText("😀").assertDoesNotExist()
        compose.onNodeWithTag("emoji-search").performTextClearance()
        compose.onNodeWithTag("emoji-search").performTextInput("not-a-real-emoji-term")
        compose.onNodeWithText("No matching emoji").assertExists()
    }
}
