package com.pritam.liquidchat.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.pritam.liquidchat.AppViewModel
import com.pritam.liquidchat.data.Chat
import com.pritam.liquidchat.data.Message
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Render real composables without signing in or contacting a live service. */
class LiquidScreensTest {
    @get:Rule val compose=createComposeRule()
    private fun capture(name:String) {
        compose.waitForIdle()
        val context=ApplicationProvider.getApplicationContext<Application>()
        val directory=File(context.getExternalFilesDir(null),"screenshots").apply {mkdirs()}
        compose.onRoot().captureToImage().asAndroidBitmap().let {bitmap->File(directory,"$name.png").outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}}
    }
    @Test fun fiveScreensRenderAndNavigationIsIconOnly() {
        val context=ApplicationProvider.getApplicationContext<Application>();val model=AppViewModel(context)
        val chats=listOf(Chat("preview","preview-chat","peer","Salma 💗✨👀","salma",preview="Hello there",lastAt="2026-10-17T04:00:00Z"))
        var screen by mutableIntStateOf(0)
        compose.setContent {
            LiquidTheme(dark=false) {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.matchParentSize().glassSource(0f).background(glassBackground()))
                    Column(Modifier.fillMaxSize().padding(top=28.dp)) {
                        Box(Modifier.weight(1f)) {
                            when(screen){0->ChatsScreen(model,chats);1->ContactsScreen(model,chats,false);2->SettingsHome(model){};3->MyProfileScreen(model,false){};4->ConversationScreen(model,"preview-chat",chats,false);else->Column {MessageBubble(Message("preview","incoming","preview-chat","peer","An incoming message"),chats[0],model,{},false);MessageBubble(Message("preview","outgoing","preview-chat","preview","An outgoing message",status="SENT"),chats[0],model,{},false)}}
                        }
                        if(screen<4)GlassNavigation(screen){screen=it}
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("Contacts").performClick();compose.onNodeWithText("Invite Friends").assertExists();capture("02-contacts")
        compose.onNodeWithContentDescription("Settings").performClick();compose.onNodeWithText("Liquid Chat Settings").assertExists();capture("03-settings")
        compose.onNodeWithContentDescription("Profile").performClick();compose.onNodeWithText("Set Photo").assertExists();compose.onNodeWithText("Add a post").assertExists();capture("04-profile")
        compose.onNodeWithContentDescription("Chats").performClick();compose.onNodeWithText("Salma 💗✨👀").assertExists();capture("01-chats")
        compose.onNodeWithText("Search Chats").performTextInput("notfound");compose.onNodeWithText("Salma 💗✨👀").assertDoesNotExist()
        compose.runOnIdle {screen=4};compose.onNodeWithContentDescription("Hold to record voice message").assertExists();capture("05-conversation")
        compose.runOnIdle {screen=5};compose.onNodeWithText("An incoming message").assertExists();capture("06-message-bubbles")
    }
    @Test fun glassCanRenderAnActualMovingBackdropWithoutBlurringItsLabel() {
        var offset by mutableIntStateOf(0)
        compose.setContent {LiquidTheme(false) {
            Box(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxSize().glassSource(0f)) {repeat(12){index->Box(Modifier.weight(1f).fillMaxHeight().background(if((index+offset)%2==0)Color.Red else Color.Blue))}}
                GlassCard(Modifier.padding(24.dp).fillMaxWidth().height(180.dp).testTag("glass")) {androidx.compose.material3.Text("Readable glass label")}
            }
        }}
        compose.onNodeWithText("Readable glass label").assertIsDisplayed();capture("07-live-glass-before")
        compose.runOnIdle {offset=1};compose.waitForIdle();capture("08-live-glass-after")
        compose.onNodeWithTag("glass").assertIsDisplayed()
    }
}
