package com.pritam.liquidchat.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

/** Regression: the visible chat filters remain tappable under accessibility scaling. */
class MbReferenceFilterAccessibilityTest {
    @get:Rule val compose=createComposeRule()

    @Test fun referenceFilterHas48DpTouchTargetAndAnnouncesSelection() {
        var selected by mutableStateOf(false)
        compose.setContent {
            CompositionLocalProvider(LocalDisplayOptions provides DisplayOptions(navBarStyle="Reference")) {
                LiquidTheme(false) {
                    MbReferenceFilterTab(label="Unread",selected=selected,onClick={selected=!selected})
                }
            }
        }
        val filter=compose.onNode(hasClickAction() and hasText("Unread"))
        filter.assertHeightIsAtLeast(48.dp).assertIsNotSelected()
        filter.performClick()
        filter.assertIsSelected()
    }

    @Test fun liquidFilterAlsoMaintains48DpTouchTarget() {
        compose.setContent {
            CompositionLocalProvider(LocalDisplayOptions provides DisplayOptions(navBarStyle="Liquid")) {
                LiquidTheme(false) {
                    MbReferenceFilterTab(label="Favorites",selected=true,onClick={})
                }
            }
        }
        compose.onNode(hasClickAction() and hasText("Favorites"))
            .assertHeightIsAtLeast(48.dp).assertIsSelected()
    }
}
