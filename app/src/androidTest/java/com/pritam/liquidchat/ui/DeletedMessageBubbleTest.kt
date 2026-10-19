package com.pritam.liquidchat.ui

import android.app.Application
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pritam.liquidchat.AppViewModel
import com.pritam.liquidchat.data.Message
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeletedMessageBubbleTest {
    @get:Rule val compose=createComposeRule()

    @Test fun deletedMediaMessageRendersTombstoneWithoutOldCaption() {
        val model=AppViewModel(ApplicationProvider.getApplicationContext<Application>())
        val deleted=Message("account","deleted","chat","peer","old-private-caption",
            mediaId="cached-private-media",mediaKind="voice",durationMs=1200,
            status="SENT",deletedAt="2026-10-19T05:00:00Z")
        compose.setContent {LiquidTheme(false) {
            MessageBubble(deleted,null,model,onRetry={},highlight=false)
        }}
        compose.onNodeWithText("This message was deleted").assertIsDisplayed()
        compose.onNodeWithText("old-private-caption").assertDoesNotExist()
        compose.onNodeWithText("Voice message").assertDoesNotExist()
    }
}
