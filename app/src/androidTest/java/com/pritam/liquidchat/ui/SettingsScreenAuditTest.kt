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

    @Test fun mbGeneralGroupsHaveRealControlsAndBackToPreferences() {
        val model=AppViewModel(ApplicationProvider.getApplicationContext<Application>())
        compose.setContent {LiquidTheme(false) {SettingsScreen(model,busy=false)}}
        compose.onNodeWithText("Liquid Chat Preferences").performScrollTo().performClick()
        compose.onNodeWithText("General configuration").performScrollTo().performClick()
        compose.onNodeWithText("Theme & typography").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("App font family").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Motion & accessibility").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Reduced motion").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Liquid Glass effects").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Reduced transparency").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Liquid Glass blur intensity").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Back to Settings").performScrollTo().performClick()
        compose.onNodeWithText("Liquid Chat Preferences").assertIsDisplayed()
    }

    @Test fun homeReferenceGroupsKeepRealNavigationAndInboxControls() {
        val model=AppViewModel(ApplicationProvider.getApplicationContext<Application>())
        compose.setContent {LiquidTheme(false) {SettingsScreen(model,busy=false)}}
        compose.onNodeWithText("Liquid Chat Preferences").performScrollTo().performClick()
        compose.onNodeWithText("Home Screen").performScrollTo().performClick()
        compose.onNodeWithText("Bottom navigation & tabs").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("iOS navigation").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Liquid capsule").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Show navigation labels").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Hide navigation icons").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Inbox header & filters").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Hide Archive filter").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Hide home search").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Hide photo shortcut").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Home wallpaper").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Chat list & typography").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Show chat dividers").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Show pinned chat indicators").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Conversation preview size · ${model.display.value.homeChatPreviewSize} sp").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Back to Settings").performScrollTo().performClick()
        compose.onNodeWithText("Liquid Chat Preferences").assertIsDisplayed()
    }

    @Test fun conversationReferenceGroupsKeepActualMessageSettingsAndPreview() {
        val model=AppViewModel(ApplicationProvider.getApplicationContext<Application>())
        compose.setContent {LiquidTheme(false) {SettingsScreen(model,busy=false)}}
        compose.onNodeWithText("Liquid Chat Preferences").performScrollTo().performClick()
        compose.onNodeWithText("Conversation Screen").performScrollTo().performClick()
        compose.onNodeWithText("Wallpaper & contrast").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Message bubbles").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Bubble style").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Incoming and outgoing bubble colors").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Outgoing bubble").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Incoming bubble").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Message ink & timestamps").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Quoted text").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Message time").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Reactions & text").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Quick reaction emojis · choose 1–6").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Emoji picker size · 24 sp").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Web link color").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Live message preview").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Incoming message").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Back to Settings").performScrollTo().performClick()
        compose.onNodeWithText("Liquid Chat Preferences").assertIsDisplayed()
    }

    @Test fun mbPrivacyKeepsBothServerAndDeviceControlsAccessible() {
        val model=AppViewModel(ApplicationProvider.getApplicationContext<Application>())
        compose.setContent {LiquidTheme(false) {SettingsScreen(model,busy=false)}}
        compose.onNodeWithText("Liquid Chat Preferences").performScrollTo().performClick()
        compose.onNodeWithText("Privacy").performScrollTo().performClick()
        compose.onNodeWithText("Device privacy").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Block screenshots and app previews").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Hide outgoing typing").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Online presence & read receipts").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Read receipts").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Back to Settings").performScrollTo().performClick()
        compose.onNodeWithText("Liquid Chat Preferences").assertIsDisplayed()
    }

    @Test fun mbNotificationsUsesRealNotificationPreviewsAndSystemPanel() {
        val model=AppViewModel(ApplicationProvider.getApplicationContext<Application>())
        compose.setContent {LiquidTheme(false) {SettingsScreen(model,busy=false)}}
        compose.onNodeWithText("Liquid Chat Preferences").performScrollTo().performClick()
        compose.onNodeWithText("Notifications").performScrollTo().performClick()
        compose.onAllNodesWithText("Notification previews").onFirst().performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Device privacy").assertDoesNotExist()
    }

    @Test fun chatSettingsKeepsTheActualAppearanceControls() {
        val model=AppViewModel(ApplicationProvider.getApplicationContext<Application>())
        compose.setContent {LiquidTheme(false) {SettingsScreen(model,busy=false)}}
        compose.onNodeWithText("Chat Settings").performScrollTo().performClick()
        compose.onNodeWithText("Appearance").assertIsDisplayed()
        compose.onNodeWithText("Animation level").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Background delivery").assertDoesNotExist()
    }
}
