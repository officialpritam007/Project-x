package com.pritam.liquidchat.ui

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pritam.liquidchat.AppViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataStorageScreenTest {
    @get:Rule val compose=createComposeRule()

    @Test fun cacheClearRequiresConfirmationAndCanBeCanceled() {
        val model=AppViewModel(ApplicationProvider.getApplicationContext<Application>())
        compose.setContent { LiquidTheme(false) {
            Column(Modifier.verticalScroll(rememberScrollState())) {DataStorage(model)}
        }}
        compose.onNodeWithText("Storage overview").assertExists()
        compose.onNodeWithText("Messages waiting to send").assertExists()
        compose.onNodeWithText("Clear cached images").performClick()
        compose.onNodeWithText("Clear cached images?").assertExists()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Clear cached images?").assertDoesNotExist()
        compose.onNodeWithText("Clear cached images").assertExists()
    }

    @Test fun mediaLimitsStayVisibleWithoutDangerousRemoteDeleteActions() {
        val model=AppViewModel(ApplicationProvider.getApplicationContext<Application>())
        compose.setContent { LiquidTheme(false) {
            Column(Modifier.verticalScroll(rememberScrollState())) {DataStorage(model)}
        }}
        compose.onNodeWithText("Media limits").assertExists()
        compose.onNodeWithText("Voice messages: up to 120 seconds and 2 MB.").assertExists()
        compose.onNodeWithText("Delete all chats").assertDoesNotExist()
    }
}
