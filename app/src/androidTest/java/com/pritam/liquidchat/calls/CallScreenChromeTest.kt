package com.pritam.liquidchat.calls

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pritam.liquidchat.ui.LiquidTheme
import com.pritam.liquidchat.ui.glassSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Exercise production call chrome without a controller, camera, microphone, or network. */
@OptIn(ExperimentalLayoutApi::class)
@RunWith(AndroidJUnit4::class)
class CallScreenChromeTest {
    @get:Rule val compose=createComposeRule()
    private val connected=CallView("fixture-call","conversation","peer","audio",false,"Connected",
        connectedAt=1L,audioRoute="Earpiece")

    @Test fun incomingVideoShowsCaptureControlsOnlyAfterAnswerWhileKeepingDirection() {
        var call by mutableStateOf(connected.copy(kind="video",incoming=true,
            status="Incoming call",connectedAt=0L))
        var accepted=0
        var declined=0
        compose.setContent {
            LiquidTheme(false) {
                CallScreenChrome(call,"Riya","02:34",call.renderVideo,true,
                    onMinimize={},
                    onAccept={accepted++;call=call.copy(status="Connecting")},
                    onDecline={declined++},onMute={},onAudio={},onCamera={},
                    onSwitch={},onHangup={},onClose={})
            }
        }
        compose.onNodeWithContentDescription("Accept").assertIsDisplayed()
        compose.onNodeWithContentDescription("Decline").assertIsDisplayed()
        listOf("Turn camera off","Mute microphone","More call options","End call").forEach {
            compose.onNodeWithContentDescription(it).assertDoesNotExist()
        }
        compose.onNodeWithContentDescription("Accept").performClick()
        compose.runOnIdle {
            assertEquals(1,accepted)
            assertEquals(0,declined)
            assertTrue("Answering must retain the incoming direction",call.incoming)
            assertEquals("Connecting",call.status)
        }
        compose.onNodeWithContentDescription("Accept").assertDoesNotExist()
        compose.onNodeWithContentDescription("Decline").assertDoesNotExist()
        listOf("Turn camera off","Mute microphone","More call options","End call").forEach {
            compose.onNodeWithContentDescription(it).assertIsDisplayed().assertHasClickAction()
        }
    }

    @Test fun terminalCallOnlyOffersCloseAndNeverDispatchesHangup() {
        var closed=0
        var otherActions=0
        val call=connected.copy(id=null,kind="video",status="Call unavailable")
        compose.setContent {
            LiquidTheme(false) {
                CallScreenChrome(call,"Riya","02:34",false,true,
                    onMinimize={otherActions++},onAccept={otherActions++},onDecline={otherActions++},
                    onMute={otherActions++},onAudio={otherActions++},onCamera={otherActions++},
                    onSwitch={otherActions++},onHangup={otherActions++},onClose={closed++})
            }
        }
        compose.onNodeWithText("Call unavailable").assertIsDisplayed()
        listOf("End call","Mute microphone","Turn camera off","More call options",
            "Accept","Decline","Minimize call").forEach {
            compose.onNodeWithContentDescription(it).assertDoesNotExist()
        }
        compose.onNodeWithContentDescription("Close").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(1,closed)
            assertEquals(0,otherActions)
        }
    }

    @Test fun landscapeLongCallerAtLargeTextKeepsIdentityAndVoiceActionsApart() {
        val name="Riya Chakraborty — a very long contact display name"
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f,1.6f)) {
                LiquidTheme(false) {
                    // Exercise the full 640x320 landscape layout but scale the
                    // fixture into a portrait emulator's physical viewport.
                    // Otherwise Compose clips the oversized test host, and
                    // assertIsDisplayed reports false for its timer.
                    Box(Modifier.requiredSize(640.dp,320.dp)
                        .graphicsLayer { scaleX=0.5f; scaleY=0.5f }
                        .consumeWindowInsets(WindowInsets.systemBars)
                        .clipToBounds().testTag("call-chrome-landscape")) {
                        CallScreenChrome(connected,name,"02:34",false,true,
                            onMinimize={},onAccept={},onDecline={},onMute={},onAudio={},
                            onCamera={},onSwitch={},onHangup={},onClose={})
                    }
                }
            }
        }
        val nameBounds=compose.onNodeWithText(name).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val identityBounds=listOf("Liquid Chat Audio",name,"02:34").map {
            compose.onNodeWithText(it).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        }
        val left=identityBounds.minOf {it.left}
        val right=identityBounds.maxOf {it.right}
        val top=identityBounds.minOf {it.top}
        val bottom=identityBounds.maxOf {it.bottom}
        val host=compose.onNodeWithTag("call-chrome-landscape").fetchSemanticsNode().boundsInRoot
        listOf("Select audio output, current: Earpiece","Mute microphone","End call").forEach {description->
            val action=compose.onNodeWithContentDescription(description)
                .assertIsDisplayed().assertHasClickAction()
            val bounds=action.fetchSemanticsNode().boundsInRoot
            assertTrue("$description must not overlap the caller identity",
                right<=bounds.left+.5f || left>=bounds.right-.5f ||
                    bottom<=bounds.top+.5f || top>=bounds.bottom-.5f)
            assertTrue("$description must stay inside the landscape viewport",
                bounds.left>=host.left-.5f && bounds.right<=host.right+.5f &&
                    bounds.top>=host.top-.5f && bounds.bottom<=host.bottom+.5f)
        }
        val audioBounds=compose.onNodeWithContentDescription("Select audio output, current: Earpiece")
            .fetchSemanticsNode().boundsInRoot
        assertTrue("Long caller name must stay beside the audio action",nameBounds.right<=audioBounds.left+.5f)
    }

    @Test fun voiceAndVideoChromeRenderOverSyntheticBackdrops() {
        var video by mutableStateOf(false)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f,1f)) {
                LiquidTheme(false) {
                    Box(Modifier.requiredSize(360.dp,640.dp)
                        .consumeWindowInsets(WindowInsets.systemBars)
                        .clipToBounds().testTag("call-chrome-fixture")) {
                        Box(Modifier.matchParentSize().glassSource(0f).background(
                            Brush.linearGradient(listOf(Color(0xFF263C43),Color(0xFF26272C),Color(0xFF493A34)))))
                        CallScreenChrome(connected.copy(kind=if(video)"video" else "audio"),
                            "Riya","02:34",video,true,
                            onMinimize={},onAccept={},onDecline={},onMute={},onAudio={},
                            onCamera={},onSwitch={},onHangup={},onClose={})
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("Select audio output, current: Earpiece").assertIsDisplayed()
        capture("26-ios-voice-call-chrome")
        compose.runOnIdle {video=true}
        compose.onNodeWithContentDescription("More call options").assertIsDisplayed()
        capture("27-ios-video-call-chrome")
    }

    private fun capture(name:String) {
        val resolver=ApplicationProvider.getApplicationContext<Application>().contentResolver
        val values=ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME,"$name.png")
            put(MediaStore.Images.Media.MIME_TYPE,"image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/LiquidChatUiChecks")
            put(MediaStore.Images.Media.IS_PENDING,1)
        }
        val uri=checkNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values))
        val bitmap=compose.onNodeWithTag("call-chrome-fixture").captureToImage().asAndroidBitmap()
        resolver.openOutputStream(uri)!!.use {check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))}
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING,0)
        resolver.update(uri,values,null,null)
    }
}
