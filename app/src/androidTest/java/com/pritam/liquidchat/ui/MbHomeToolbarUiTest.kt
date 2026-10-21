package com.pritam.liquidchat.ui

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.pritam.liquidchat.AppViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Actual Home toolbar callbacks must navigate instead of rendering dead icons. */
class MbHomeToolbarUiTest {
    @get:Rule val compose=createComposeRule()

    @Test fun referenceToolbarRoutesToContactsAndProfileAndKeepsActionsMenu() {
        val model=AppViewModel(ApplicationProvider.getApplicationContext<Application>())
        var contacts=0
        var profile=0
        compose.setContent {
            LiquidTheme(false) {ChatsScreen(model,emptyList(),
                onContacts={contacts++},onProfile={profile++})}
        }
        compose.onNodeWithContentDescription("Liquid Chat").assertExists()
        compose.onNodeWithContentDescription("Open contacts").performClick()
        compose.onNodeWithContentDescription("Open profile").performClick()
        compose.runOnIdle {
            assertEquals(1,contacts)
            assertEquals(1,profile)
        }
        compose.onNodeWithContentDescription("Chat options").performClick()
        compose.onNodeWithText("Refresh conversations").assertIsDisplayed()
    }
}
