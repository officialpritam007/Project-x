package com.pritam.liquidchat.ui

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PremiumGlassUiTest {
    @get:Rule val compose=createComposeRule()

    @Test fun premiumDialogRetainsSafeConfirmAndDismissActions() {
        var confirmed=false
        compose.setContent {
            LiquidTheme(false) {
                LiquidAlertDialog(
                    onDismissRequest={},
                    title={Text("Confirm action")},
                    text={Text("Existing content is preserved")},
                    confirmButton={TextButton(onClick={confirmed=true}) {Text("Continue")}},
                    dismissButton={TextButton(onClick={}) {Text("Cancel")}}
                )
            }
        }
        compose.onNodeWithText("Existing content is preserved").assertIsDisplayed()
        compose.onNodeWithText("Continue").performClick()
        compose.runOnIdle { assertTrue(confirmed) }
    }

    @Test fun iconOnlyPremiumNavigationRetainsAccessibleTabActions() {
        val selection=mutableIntStateOf(0)
        compose.setContent {
            LiquidTheme(false) {
                GlassNavigation(selection.intValue) {selection.intValue=it}
            }
        }
        compose.onNodeWithContentDescription("Calls").performClick()
        compose.runOnIdle {assertEquals(1,selection.intValue)}
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.runOnIdle {assertEquals(3,selection.intValue)}
    }
}
