package com.pritam.liquidchat.ui

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pritam.liquidchat.AppViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenAuditTest {
    @get:Rule val compose=createComposeRule()

    @Test fun powerSavingHasItsOwnControlsRatherThanAppearanceClones() {
        val model=AppViewModel(ApplicationProvider.getApplicationContext<Application>())
        compose.setContent {LiquidTheme(false) {SettingsScreen(model,busy=false)}}
        compose.onNodeWithText("Power Saving").performScrollTo().performClick()
        compose.onNodeWithText("Background delivery").assertIsDisplayed()
        compose.onNodeWithText("Media downloads").assertIsDisplayed()
        compose.onNodeWithText("Reduced motion").assertIsDisplayed()
        compose.onNodeWithText("Animation level").assertDoesNotExist()
        compose.onNodeWithText("Appearance").assertDoesNotExist()
    }

    @Test fun chatSettingsKeepsTheActualAppearanceControls() {
        val model=AppViewModel(ApplicationProvider.getApplicationContext<Application>())
        compose.setContent {LiquidTheme(false) {SettingsScreen(model,busy=false)}}
        compose.onNodeWithText("Chat Settings").performScrollTo().performClick()
        compose.onNodeWithText("Appearance").assertIsDisplayed()
        compose.onNodeWithText("Animation level").assertIsDisplayed()
        compose.onNodeWithText("Background delivery").assertDoesNotExist()
    }
}
