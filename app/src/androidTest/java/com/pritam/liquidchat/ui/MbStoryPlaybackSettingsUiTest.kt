package com.pritam.liquidchat.ui

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** P08-008/011: the full labelled preference row must actually save its value. */
@RunWith(AndroidJUnit4::class)
class MbStoryPlaybackSettingsUiTest {
    @get:Rule val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var previous: MbStoryPlaybackOptions

    @Before fun savePrevious() {
        previous = MbStoryPlaybackSettings.read(context)
        MbStoryPlaybackSettings.save(context, MbStoryPlaybackOptions())
    }

    @After fun restorePrevious() {
        MbStoryPlaybackSettings.save(context, previous)
    }

    @Test fun tappingLabelsUpdatesPersistedStoryPlaybackSettings() {
        compose.setContent {
            LiquidTheme(false) {
                MbStoryPlaybackSettingsCard()
            }
        }
        compose.onNodeWithText("Disable automatic story advance").performClick()
        compose.runOnIdle {
            assertFalse(MbStoryPlaybackSettings.read(context).autoAdvance)
        }
        compose.onNodeWithText("Start video stories with sound").performClick()
        compose.runOnIdle {
            assertTrue(MbStoryPlaybackSettings.read(context).startWithSound)
        }
    }
}
