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

    @Test fun allEightReferenceCategoriesAreReachableWithoutHorizontalClipping() {
        compose.setContent {LiquidTheme(false) {EmojiPicker(onEmoji={},onDismiss={})}}
        val categories=listOf(
            "Smileys & People","Animals & Nature","Food","Activities",
            "Travel & Places","Objects","Symbols","Flags"
        )
        categories.forEachIndexed {index,label->
            compose.onNodeWithTag("emoji-categories").performScrollToIndex(index)
            compose.onNodeWithText(label,substring=true).assertIsDisplayed()
        }
    }

    @Test fun explicitSkinToneSelectionIsVisibleAndNotAppliedWithoutTapping() {
        compose.setContent {LiquidTheme(false) {EmojiPicker(onEmoji={},onDismiss={})}}
        compose.onNodeWithTag("emoji-tone-0").assertIsSelected()
        compose.onNodeWithTag("emoji-tone-3").performClick()
        compose.onNodeWithTag("emoji-tone-3").assertIsSelected()
        compose.onNodeWithTag("emoji-tone-0").assertIsNotSelected()
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
