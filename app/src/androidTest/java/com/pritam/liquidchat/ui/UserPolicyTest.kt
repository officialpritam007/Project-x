package com.pritam.liquidchat.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class UserPolicyTest {
    @get:Rule val compose=createComposeRule()
    @Test fun sharingPolicyRequiresExplicitConsent() {
        var accepted=false
        compose.setContent {LiquidTheme(false) {UserPolicyScreen(false,{accepted=true},{})}}
        compose.onNodeWithText("Agree and continue").assertIsNotEnabled()
        compose.runOnIdle {assertFalse(accepted)}
        compose.onNodeWithTag("policy-agree").performScrollTo().performClick()
        compose.onNodeWithText("Agree and continue").performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle {assertTrue(accepted)}
    }
    @Test fun reportRequiresAnExplanationAndSharesOnlySubmittedReason() {
        var submitted:String?=null
        compose.setContent {LiquidTheme(false) {ReportContentDialog("Report message","Private review",{submitted=it},{})}}
        compose.onNodeWithText("Submit report").assertIsNotEnabled()
        compose.onNodeWithText("Reason").performTextInput("  harassment  ")
        compose.onNodeWithText("Submit report").performClick()
        compose.runOnIdle {assertEquals("harassment",submitted)}
    }
}
