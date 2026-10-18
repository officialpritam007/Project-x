package com.pritam.liquidchat.ui

import android.app.Application
import android.content.ContentValues
import android.provider.MediaStore
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
        val resolver=context.contentResolver
        val values=ContentValues().apply {put(MediaStore.Images.Media.DISPLAY_NAME,"$name.png");put(MediaStore.Images.Media.MIME_TYPE,"image/png");put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/LiquidChatUiChecks");put(MediaStore.Images.Media.IS_PENDING,1)}
        val uri=resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values) ?: error("Screenshot output unavailable")
        val bitmap=compose.onRoot().captureToImage().asAndroidBitmap()
        resolver.openOutputStream(uri)!!.use {check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))}
        values.clear();values.put(MediaStore.Images.Media.IS_PENDING,0);resolver.update(uri,values,null,null)
    }
    @Test fun fiveScreensRenderAndNavigationIsIconOnly() {
        val context=ApplicationProvider.getApplicationContext<Application>();val model=AppViewModel(context)
        val chats=listOf(Chat("preview","preview-chat","peer","Salma 💗✨👀","salma",preview="Hello there",lastAt="2026-10-17T04:00:00Z"))
        var screen by mutableIntStateOf(0)
        compose.setContent {
            LiquidTheme(dark=false) {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.matchParentSize().glassSource(0f).background(glassBackground()))
                    Box(Modifier.fillMaxSize().padding(top=28.dp)) {
                            when(screen){0->ChatsScreen(model,chats);1->CallsScreen(model,chats);2->ContactsScreen(model,chats,false);3->SettingsHome(model){};4->MyProfileScreen(model,false){};5->ConversationScreen(model,"preview-chat",chats,false);else->Column {MessageBubble(Message("preview","incoming","preview-chat","peer","An incoming message",reactions="{\"peer\":\"❤️\",\"preview\":\"👍\"}"),chats[0],model,{},false);MessageBubble(Message("preview","outgoing","preview-chat","preview","An outgoing message",seq=5,status="SENT",replyTo="incoming",editedAt="2026-10-17T08:00:00Z",starred=true),chats[0],model,{},false)}}
                    }
                    if(screen<5)Box(Modifier.align(Alignment.BottomCenter)){GlassNavigation(screen){screen=it}}
                }
            }
        }
        compose.onNodeWithContentDescription("Calls").performClick();compose.onNodeWithText("No recent calls").assertExists();capture("02-calls")
        compose.onNodeWithContentDescription("Contacts").performClick();compose.onNodeWithText("Invite Friends").assertExists();capture("02-contacts")
        compose.onNodeWithContentDescription("Settings").performClick();compose.onNodeWithText("Liquid Chat Settings").assertExists();capture("03-settings")
        compose.onNodeWithContentDescription("Profile").performClick();compose.onNodeWithText("Set Photo").assertExists();capture("04-profile")
        compose.onNodeWithTag("moments-list").performScrollToNode(hasText("Add a post"));compose.onNodeWithText("Add a post").assertIsDisplayed();capture("04b-profile-posts")
        compose.onNodeWithContentDescription("Chats").performClick();compose.onNodeWithText("Salma 💗✨👀").assertExists();capture("01-chats")
        compose.onNodeWithText("Search Chats").performTextInput("notfound");compose.onNodeWithText("Salma 💗✨👀").assertDoesNotExist()
        compose.runOnIdle {screen=5};compose.onNodeWithContentDescription("Hold to record voice message").assertExists();capture("05-conversation")
        compose.runOnIdle {screen=6};compose.onNodeWithText("An incoming message").assertExists();compose.onNodeWithText("edited").assertExists();compose.onNodeWithContentDescription("Starred").assertExists();capture("06-message-bubbles")
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

    @Test fun failedLocalSendKeepsEditorFocusedAndDraftEditable() {
        val model=AppViewModel(ApplicationProvider.getApplicationContext<Application>())
        compose.setContent {LiquidTheme(false) {ConversationScreen(model,"offline-fixture",emptyList(),false)}}
        compose.onNodeWithTag("message-input").performClick().performTextInput("Hello")
        compose.onNodeWithTag("message-input").assertIsFocused()
        compose.onNodeWithContentDescription("Send message").performClick()
        // No account is configured in instrumentation: local queue fails, without network writes.
        compose.waitUntil(5000) {model.notice.value!=null}
        compose.onNodeWithTag("message-input").assertIsFocused().assertTextEquals("Hello")
        compose.onNodeWithTag("message-input").performTextInput(" again")
        compose.onNodeWithTag("message-input").assertTextEquals("Hello again")
    }
}
