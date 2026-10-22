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
        compose.onNodeWithText("Liquid Chat Preferences").assertIsDisplayed()
        compose.onNodeWithContentDescription("Liquid Chat Preferences icon").assertExists()
        for(route in MbReferencePreferenceSections.all) {
            compose.onNodeWithText(route.label).performScrollTo().performClick()
        }
        compose.onNodeWithContentDescription("Back to Settings").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(MbReferencePreferenceSections.all.map {it.target},opened)
            assertEquals(1,backs)
            assertEquals(MbReferencePreferenceSections.all.size,opened.distinct().size)
        }
    }

    @Test fun userFacingPreferencesContainOnlyLiquidChatBranding() {
        compose.setContent { LiquidTheme(false) {
            MbPreferencesLanding(onBack={},onNavigate={})
        } }
        compose.onNodeWithText("Liquid Chat Preferences").assertIsDisplayed()
        compose.onAllNodes(hasText("MB",substring=true)).assertCountEquals(0)
        compose.onAllNodes(hasText("WhatsApp",substring=true,ignoreCase=true)).assertCountEquals(0)
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
            assertEquals(listOf("Liquid Chat Privacy","Liquid Chat Notifications"),opened)
        }
    }

    @Test fun referenceInventoryNeverExposesUnimplementedWhatsAppRoutes() {
        val targets=MbReferencePreferenceSections.all.map {it.target}
        assertEquals(targets.size,targets.distinct().size)
        assertEquals(11,targets.size)
        assertFalse(targets.any {it.contains("WhatsApp",ignoreCase=true)})
        assertTrue(targets.all {it.startsWith("Liquid Chat ") || it in setOf("Account","Data and Storage","Power Saving")})
    }
}
