package com.pritam.liquidchat.ui

import androidx.compose.foundation.layout.Row
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
                    Row {
                        MbReferenceFilterTab("All", selected = true, onClick = { clicked = "All" })
                        MbReferenceFilterTab("Unread", selected = false, onClick = { clicked = "Unread" })
                    }
                }
            }
        }
        compose.onNodeWithText("All").assertIsSelected()
        val allBounds=compose.onNodeWithText("All").fetchSemanticsNode().boundsInRoot
        val unreadBounds=compose.onNodeWithText("Unread").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue("Filter hit targets must not overlap",allBounds.right<=unreadBounds.left)
        compose.onNodeWithText("Unread").assertIsNotSelected()
        compose.onNode(hasText("Unread") and hasClickAction()).performClick()
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
