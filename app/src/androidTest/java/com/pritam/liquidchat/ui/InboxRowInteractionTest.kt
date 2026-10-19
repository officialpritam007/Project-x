package com.pritam.liquidchat.ui

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pritam.liquidchat.AppViewModel
import com.pritam.liquidchat.data.Chat
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InboxRowInteractionTest {
    @get:Rule val compose=createComposeRule()

    @Test fun oneTapUsesSingleNavigationAction() {
        val model=AppViewModel(ApplicationProvider.getApplicationContext<Application>())
        val contact=Chat(account="account",id="chat",peer="peer",name="Contact",username="contact")
        var opens=0
        compose.setContent {LiquidTheme(false) {
            ChatRow(contact,model.backend,onClick={opens++})
        }}
        compose.onAllNodes(hasClickAction()).assertCountEquals(1)
        compose.onNode(hasClickAction()).performClick()
        compose.runOnIdle {assertEquals(1,opens)}
    }

    @Test fun longPressOpensMenuWithoutSendingNavigationClick() {
        val model=AppViewModel(ApplicationProvider.getApplicationContext<Application>())
        val contact=Chat(account="account",id="chat",peer="peer",name="Contact",username="contact")
        var opens=0
        var longPresses=0
        compose.setContent {LiquidTheme(false) {
            ChatRow(contact,model.backend,onClick={opens++},onLongClick={longPresses++})
        }}
        compose.onNode(hasClickAction()).performTouchInput {longClick()}
        compose.runOnIdle {
            assertEquals(0,opens)
            assertEquals(1,longPresses)
        }
    }
}
