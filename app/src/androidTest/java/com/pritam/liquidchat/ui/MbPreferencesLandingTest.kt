package com.pritam.liquidchat.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The reference hierarchy is decorative only where it has real destinations. */
@RunWith(AndroidJUnit4::class)
class MbPreferencesLandingTest {
    @get:Rule val compose=createComposeRule()

    @Test fun referenceScreenOpensEverySupportedSettingAndBack() {
        val opened=mutableListOf<String>()
        var backs=0
        compose.setContent {
            LiquidTheme(false) {
                MbPreferencesLanding(onBack={backs++},onNavigate={opened+=it})
            }
        }
        compose.onNodeWithText("MB Preferences").assertIsDisplayed()
        compose.onNodeWithContentDescription("MB Preferences icon").assertExists()
        for(route in MbReferencePreferenceSections.all) {
            compose.onNodeWithText(route.label).performScrollTo().performClick()
        }
        compose.onNodeWithContentDescription("Back to Settings").performClick()
        compose.runOnIdle {
            assertEquals(MbReferencePreferenceSections.all.map {it.target},opened)
            assertEquals(1,backs)
            assertEquals(6,opened.distinct().size)
        }
    }

    @Test fun darkAndLargeFontKeepPrivacyAndNotificationsReachable() {
        val opened=mutableListOf<String>()
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f,2f)) {
                LiquidTheme(true) {
                    MbPreferencesLanding(onBack={},onNavigate={opened+=it})
                }
            }
        }
        compose.onNodeWithText("Privacy").performScrollTo().performClick()
        compose.onNodeWithText("Notifications").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(listOf("MB Privacy","MB Notifications"),opened)
        }
    }

    @Test fun referenceInventoryNeverExposesUnimplementedWhatsAppRoutes() {
        val targets=MbReferencePreferenceSections.all.map {it.target}
        assertEquals(targets.size,targets.distinct().size)
        assertEquals(6,targets.size)
        assertFalse(targets.any {it.contains("WhatsApp",ignoreCase=true)})
        assertTrue(targets.all {it.startsWith("MB ")})
    }
}
