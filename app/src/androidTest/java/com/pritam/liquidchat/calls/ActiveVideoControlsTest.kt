package com.pritam.liquidchat.calls

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pritam.liquidchat.ui.LiquidTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActiveVideoControlsTest {
    @get:Rule val compose=createComposeRule()

    @Test fun compactVideoStripKeepsAllFiveUsableControls() {
        var muted by mutableStateOf(false)
        var camera by mutableStateOf(true)
        var outputs=0
        var switches=0
        var ended=0
        compose.setContent {
            LiquidTheme(false) {
                ActiveVideoControlStrip(muted,camera,"Earpiece",
                    onMute={muted=!muted},onAudio={outputs++},
                    onCamera={camera=!camera},onSwitch={switches++},onHangup={ended++})
            }
        }
        compose.onNodeWithContentDescription("Mute microphone").performClick()
        compose.onNodeWithContentDescription("Unmute microphone").assertExists()
        compose.onNodeWithContentDescription("Select audio output, current: Earpiece").performClick()
        compose.onNodeWithContentDescription("Turn camera off").performClick()
        compose.onNodeWithContentDescription("Turn camera on").assertExists()
        compose.onNodeWithContentDescription("Switch camera").performClick()
        compose.onNodeWithContentDescription("End call").performClick()
        compose.runOnIdle {
            assertEquals(1,outputs)
            assertEquals(1,switches)
            assertEquals(1,ended)
        }
    }
}
