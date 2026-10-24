package com.pritam.liquidchat.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Intercept in-app browser opens: no WebView, browser, DNS or live account. */
class MessageBodyLinksTest {
    @get:Rule val compose=createComposeRule()
    private val body="দেখো HtTpS://example.invalid/KeepCase?Token=AbC now"
    private var layout:TextLayoutResult?=null

    private fun tapLink(message:String=body) {
        compose.waitForIdle()
        val bounds=layout!!.getBoundingBox(message.indexOf("https",ignoreCase=true)+3)
        compose.onNodeWithText(message).performTouchInput {click(bounds.center)}
    }

    @Test fun onlyAnExplicitTapOpensAWebUrlAndSelectionModeDisablesIt() {
        val opened=mutableListOf<String>()
        val enabled=mutableStateOf(true)
        compose.setContent {
            LiquidTheme(false) {
                MessageBodyText(body,16,false,enabled.value,
                    onBrowserOpen={opened.add(it)},onTextLayout={layout=it})
            }
        }
        compose.waitForIdle()
        assertTrue("Rendering must not open a link",opened.isEmpty())
        tapLink()
        compose.runOnIdle {
            assertEquals(listOf("https://example.invalid/KeepCase?Token=AbC"),opened)
            enabled.value=false
        }
        tapLink()
        compose.runOnIdle {assertEquals(1,opened.size)}
    }

    @Test fun privateNetworkLinkShowsRecoverableErrorWithoutOpeningBrowser() {
        val privateBody="See https://127.0.0.1/private now"
        val opened=mutableListOf<String>()
        compose.setContent {
            LiquidTheme(false) {
                MessageBodyText(privateBody,16,false,true,
                    onBrowserOpen={opened.add(it)},onTextLayout={layout=it})
            }
        }
        tapLink(privateBody)
        compose.onNodeWithText(
            "This web link could not be opened safely. You can copy the message instead."
        ).assertIsDisplayed()
        assertTrue("Private-network URLs must never reach the browser",opened.isEmpty())
        compose.onNodeWithText("OK").performClick()
        compose.onNodeWithText("OK").assertDoesNotExist()
    }
}
