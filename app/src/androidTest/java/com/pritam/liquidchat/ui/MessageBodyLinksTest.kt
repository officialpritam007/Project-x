package com.pritam.liquidchat.ui

import android.content.IntentFilter
import android.net.Uri
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** URL taps are intercepted; no browser, DNS or external account is involved. */
class MessageBodyLinksTest {
    @get:Rule val compose=createComposeRule()
    private val body="দেখো HtTpS://example.invalid/KeepCase?Token=AbC now"
    private var layout:TextLayoutResult?=null
    private fun tapLink() {
        compose.waitForIdle()
        val bounds=layout!!.getBoundingBox(body.indexOf("https",ignoreCase=true)+3)
        compose.onNodeWithText(body).performTouchInput {click(bounds.center)}
    }

    @Test fun onlyAnExplicitTapOpensAWebUrlAndSelectionModeDisablesIt() {
        val opened=mutableListOf<String>()
        val enabled=mutableStateOf(true)
        val handler=object:UriHandler {
            override fun openUri(uri:String) {
                val data=Uri.parse(uri)
                val filter=IntentFilter().apply {addDataScheme("https")}
                assertTrue("Android browser scheme must match",filter.matchData(null,data.scheme,data)>=0)
                opened.add(uri)
            }
        }
        compose.setContent {
            CompositionLocalProvider(LocalUriHandler provides handler) {
                LiquidTheme(false) {MessageBodyText(body,16,false,enabled.value) {layout=it}}
            }
        }
        compose.waitForIdle();assertTrue(opened.isEmpty())
        tapLink()
        compose.runOnIdle {assertEquals(listOf("https://example.invalid/KeepCase?Token=AbC"),opened);enabled.value=false}
        tapLink()
        compose.runOnIdle {assertEquals(1,opened.size)}
    }

    @Test fun noBrowserHandlerShowsRecoverableError() {
        val handler=object:UriHandler {override fun openUri(uri:String) {throw IllegalArgumentException("fixture no handler")}}
        compose.setContent {
            CompositionLocalProvider(LocalUriHandler provides handler) {
                LiquidTheme(false) {MessageBodyText(body,16,false,true) {layout=it}}
            }
        }
        tapLink()
        compose.onNodeWithText("No app could open this web link. You can copy the message instead.").assertIsDisplayed()
        compose.onNodeWithText("OK").performClick()
        compose.onNodeWithText("OK").assertDoesNotExist()
    }
}
