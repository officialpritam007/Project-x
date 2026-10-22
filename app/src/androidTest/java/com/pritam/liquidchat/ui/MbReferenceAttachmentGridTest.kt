package com.pritam.liquidchat.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MbReferenceAttachmentGridTest {
    @get:Rule val compose=createComposeRule()

    @Test fun everyVisibleReferenceGridChoiceCallsItsOwnAction() {
        val selected=mutableListOf<Int>()
        val choices=(0 until 5).map { index ->
            MbAttachmentChoice("Media action ${index}",Icons.Outlined.PhotoLibrary) {
                selected+=index
            }
        }
        compose.setContent {
            LiquidTheme(false) {
                MbReferenceAttachmentGrid(choices)
            }
        }
        (0 until 5).forEach { index ->
            compose.onNodeWithText("Media action ${index}").performClick()
        }
        compose.runOnIdle {assertEquals((0 until 5).toList(),selected)}
    }
}
