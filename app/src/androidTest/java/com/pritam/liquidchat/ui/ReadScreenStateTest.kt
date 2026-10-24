package com.pritam.liquidchat.ui

import android.app.Application
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.pritam.liquidchat.AppViewModel
import org.junit.Rule
import org.junit.Test

/** Signed-out rendering cannot issue authenticated reads or stay in a loading state. */
class ReadScreenStateTest {
    @get:Rule val compose=createComposeRule()

    @Test fun callsEmptyAndMissedStatesSurviveRecompositionWithoutLoading() {
        val model=AppViewModel(ApplicationProvider.getApplicationContext<Application>())
        val updates=mutableIntStateOf(0)
        compose.setContent {LiquidTheme(updates.intValue%2==1) {CallsScreen(model,emptyList())}}
        compose.onNodeWithText("No recent calls").assertIsDisplayed()
        compose.onNodeWithTag("call-history-loading").assertDoesNotExist()
        compose.onNodeWithText("Missed").performClick()
        compose.onNodeWithText("No missed calls").assertIsDisplayed()
        compose.runOnIdle {updates.intValue++}
        compose.onNodeWithText("No missed calls").assertIsDisplayed()
        compose.onNodeWithTag("call-history-loading").assertDoesNotExist()
    }

    @Test fun signedOutPostPanelShowsStableEmptyStateAndDisabledEditor() {
        val model=AppViewModel(ApplicationProvider.getApplicationContext<Application>())
        compose.setContent {LiquidTheme(false) {MomentsPanel(model,"post",mine=true)}}
        compose.onNodeWithTag("moments-list").performScrollToNode(hasText("No posts yet"))
        compose.onNodeWithText("No posts yet").assertIsDisplayed()
        compose.onNodeWithText("Add a post").assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithTag("moments-loading").assertDoesNotExist()
    }
}
