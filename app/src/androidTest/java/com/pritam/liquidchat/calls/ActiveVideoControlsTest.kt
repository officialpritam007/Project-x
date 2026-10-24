package com.pritam.liquidchat.calls

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pritam.liquidchat.ui.LiquidTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActiveVideoControlsTest {
    @get:Rule val compose=createComposeRule()

    @Test fun videoControlsKeepCaptureActionsAndMoveAudioAndFlipIntoMore() {
        var call by mutableStateOf(CallView("active-call","chat","peer","video",false,"Connected"))
        var outputs=0
        var switches=0
        var ended=0
        compose.setContent {
            LiquidTheme(false) {
                ActiveVideoControlStrip(call.muted,call.cameraEnabled,"Earpiece",
                    onMute={call=call.copy(muted=!call.muted)},onAudio={outputs++},
                    onCamera={call=call.copy(cameraEnabled=!call.cameraEnabled)},
                    onSwitch={switches++},onHangup={ended++})
            }
        }
        compose.onNodeWithContentDescription("End call").assertIsDisplayed().assertHasClickAction()
        compose.onNodeWithContentDescription("Mute microphone").performClick()
        compose.onNodeWithContentDescription("Unmute microphone").assertIsDisplayed()
        compose.onNodeWithContentDescription("Turn camera off").performClick()
        compose.onNodeWithContentDescription("Turn camera on").assertIsDisplayed()
        compose.runOnIdle {
            assertFalse(call.cameraEnabled)
            assertTrue("Turning off the local camera must not remove remote video",call.renderVideo)
        }
        compose.onNodeWithContentDescription("Select audio output, current: Earpiece").assertDoesNotExist()
        compose.onNodeWithContentDescription("More call options").performClick()
        compose.onNodeWithContentDescription("Select audio output, current: Earpiece").performClick()
        compose.onNodeWithContentDescription("Switch camera").assertDoesNotExist()
        compose.onNodeWithContentDescription("More call options").performClick()
        compose.onNodeWithContentDescription("Switch camera").performClick()
        compose.onNodeWithContentDescription("End call").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(1,outputs)
            assertEquals(1,switches)
            assertEquals(1,ended)
        }
    }

    @Test fun videoControlsKeepFourCompleteTouchTargetsAtBothHeights() {
        var hostHeight by mutableStateOf(280.dp)
        var minimumSizePx=0f
        compose.setContent {
            minimumSizePx=with(LocalDensity.current) {48.dp.toPx()}
            LiquidTheme(false) {
                Box(Modifier.requiredSize(320.dp,hostHeight).clipToBounds().testTag("compact-call-controls")) {
                    ActiveVideoControlStrip(false,true,"Earpiece",
                        onMute={},onAudio={},onCamera={},onSwitch={},onHangup={})
                }
            }
        }
        listOf(280.dp,120.dp).forEach {height->
            compose.runOnIdle {hostHeight=height}
            val host=compose.onNodeWithTag("compact-call-controls").fetchSemanticsNode().boundsInRoot
            listOf("Turn camera off","Mute microphone","More call options","End call").forEach {description->
                val target=compose.onNodeWithContentDescription(description)
                    .assertIsDisplayed().assertHasClickAction()
                val bounds=target.fetchSemanticsNode().boundsInRoot
                assertTrue("$description must be at least 48dp wide at $height",bounds.width+.5f>=minimumSizePx)
                assertTrue("$description must be at least 48dp high at $height",bounds.height+.5f>=minimumSizePx)
                assertTrue("$description must fit horizontally at $height",bounds.left>=host.left-.5f && bounds.right<=host.right+.5f)
                assertTrue("$description must fit vertically at $height",bounds.top>=host.top-.5f && bounds.bottom<=host.bottom+.5f)
            }
        }
    }

    @Test fun voiceControlsKeepMuteAudioAndEndDirectlyAvailable() {
        var muted by mutableStateOf(false)
        var outputs=0
        var ended=0
        compose.setContent {
            LiquidTheme(false) {
                VoiceCallControls(muted,"Speaker",onMute={muted=!muted},
                    onAudio={outputs++},onHangup={ended++})
            }
        }
        compose.onNodeWithContentDescription("Mute microphone").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Unmute microphone").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Select audio output, current: Speaker").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("End call").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertFalse(muted)
            assertEquals(1,outputs)
            assertEquals(1,ended)
        }
    }

    @Test fun incomingControlsKeepAcceptAndDeclineAsSeparateActions() {
        var declined=0
        var accepted=0
        compose.setContent {
            LiquidTheme(false) {
                IncomingCallControls(onDecline={declined++},onAccept={accepted++})
            }
        }
        compose.onNodeWithContentDescription("Decline").assertIsDisplayed().assertHasClickAction().performClick()
        compose.runOnIdle {
            assertEquals(1,declined)
            assertEquals(0,accepted)
        }
        compose.onNodeWithContentDescription("Accept").assertIsDisplayed().assertHasClickAction().performClick()
        compose.runOnIdle {
            assertEquals(1,declined)
            assertEquals(1,accepted)
        }
    }
}
