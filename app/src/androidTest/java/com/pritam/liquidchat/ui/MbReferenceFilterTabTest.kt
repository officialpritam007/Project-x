package com.pritam.liquidchat.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** P01-002: a selected chat filter must be announced by TalkBack, not color alone. */
@RunWith(AndroidJUnit4::class)
class MbReferenceFilterTabTest {
    @get:Rule val compose = createComposeRule()

    @Test fun referenceFilterHasSelectedSemanticsAndRetainsClickHandler() {
        var clicked = ""
        compose.setContent {
            CompositionLocalProvider(
                LocalDisplayOptions provides DisplayOptions(navBarStyle = "Reference")
            ) {
                LiquidTheme(false) {
                    Row(Modifier.fillMaxWidth()) {
                        MbReferenceFilterTab("All", selected = true, onClick = { clicked = "All" },modifier=Modifier.weight(1f))
                        MbReferenceFilterTab("Unread", selected = false, onClick = { clicked = "Unread" },modifier=Modifier.weight(1f))
                    }
                }
            }
        }
        compose.onNodeWithText("All").assertIsSelected()
        compose.onNodeWithText("Unread").assertIsNotSelected()
        compose.onNodeWithText("Unread").performClick()
        compose.runOnIdle { assertEquals("Unread", clicked) }
    }

    @Test fun liquidFilterPreservesSelectedSemantics() {
        compose.setContent {
            CompositionLocalProvider(
                LocalDisplayOptions provides DisplayOptions(navBarStyle = "Liquid")
            ) {
                LiquidTheme(false) {
                    Row {
                        MbReferenceFilterTab("All", selected = false, onClick = {})
                        MbReferenceFilterTab("Favorites", selected = true, onClick = {})
                    }
                }
            }
        }
        compose.onNodeWithText("All").assertIsNotSelected()
        compose.onNodeWithText("Favorites").assertIsSelected()
    }
}
