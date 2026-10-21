package com.pritam.liquidchat.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Both bar styles preserve all five real Liquid Chat navigation destinations. */
class MbNavigationBarUiTest {
    @get:Rule val compose=createComposeRule()

    @Test fun referenceIosBarKeepsFiveActiveAccessibleDestinations() {
        val visited=mutableListOf<Int>()
        compose.setContent {
            CompositionLocalProvider(LocalDisplayOptions provides DisplayOptions(
                navBarStyle="Reference",showNavLabels=true
            )) { LiquidTheme(false) { GlassNavigation(0) {visited+=it} } }
        }
        MbNavigationOrder.titles.forEach {title->
            compose.onNodeWithContentDescription(title).assertIsDisplayed().performClick()
        }
        compose.runOnIdle {assertEquals(listOf(0,1,2,3,4),visited)}
    }

    @Test fun optionalLiquidCapsuleRetainsReorderedNavigation() {
        val visited=mutableListOf<Int>()
        compose.setContent {
            CompositionLocalProvider(LocalDisplayOptions provides DisplayOptions(
                navBarStyle="Liquid",navigationOrder="4,3,2,1,0",showNavLabels=false
            )) { LiquidTheme(true) { GlassNavigation(2) {visited+=it} } }
        }
        listOf("Profile","Settings","Contacts","Calls","Chats").forEach {title->
            compose.onNodeWithContentDescription(title).assertIsDisplayed().performClick()
        }
        compose.runOnIdle {assertEquals(listOf(4,3,2,1,0),visited)}
    }
}
