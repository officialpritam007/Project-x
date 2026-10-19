package com.pritam.liquidchat.ui

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.pritam.liquidchat.AppViewModel
import com.pritam.liquidchat.data.Chat
import com.pritam.liquidchat.data.Message
import org.junit.Rule
import org.junit.Test

/** Selected 200% text/reduced-transparency semantics checks; not physical TalkBack acceptance. */
class LargeTextAccessibilityTest {
    @get:Rule val compose=createComposeRule()

    @Test fun unreadRowAndDeletedBubbleRemainAccessibleInBothThemesAtTwoHundredPercent() {
        val model=AppViewModel(ApplicationProvider.getApplicationContext<Application>())
        var dark by mutableStateOf(false)
        compose.setContent {
            val density=LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density,2f),
                LocalDisplayOptions provides DisplayOptions(reducedMotion=true,reducedTransparency=true)
            ) {
                LiquidTheme(dark) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
                        ChatRow(Chat("a","chat","peer","Long contact display name","contact",unread=3),model.backend,{})
                        MessageBubble(Message("a","deleted","chat","peer","private old caption",deletedAt="2026-10-19T00:00:00Z"),null,model,{},false)
                    }
                }
            }
        }
        repeat(2) {
            compose.onNodeWithText("3").assertExists()
            compose.onNodeWithText("Long contact display name").assertIsDisplayed()
            compose.onNodeWithText("This message was deleted").assertIsDisplayed()
            compose.onNodeWithText("private old caption").assertDoesNotExist()
            compose.runOnIdle {dark=true}
        }
    }
}
