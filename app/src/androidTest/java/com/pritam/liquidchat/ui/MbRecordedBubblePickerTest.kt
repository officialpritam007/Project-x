package com.pritam.liquidchat.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MbRecordedBubblePickerTest {
    @get:Rule val compose=createComposeRule()

    @Test fun selectingReferenceShapeEmitsRealBubbleSetting() {
        var chosen=""
        compose.setContent {
            CompositionLocalProvider(
                LocalDisplayOptions provides DisplayOptions(navBarStyle="Reference")
            ) {
                LiquidTheme(false) {
                    MbReferenceBubbleStylePicker(
                        selected="iOS",onSelect={chosen=it})
                }
            }
        }
        compose.onNodeWithText("Bubble style").performClick()
        compose.onNodeWithContentDescription("Choose bubble style Style 2").performClick()
        compose.runOnIdle {assertEquals("Style 2",chosen)}
    }
}
