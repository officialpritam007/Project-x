package com.pritam.liquidchat.ui

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Live screen settings: a long-press drag must persist an actual route permutation. */
class MbNavigationReorderRowsTest {
    @get:Rule val compose=createComposeRule()

    @Test fun holdAndDragMovesCallsOnePositionButKeepsAllDestinations() {
        val order=mutableStateOf(MbNavigationOrder.DEFAULT)
        compose.setContent {
            LiquidTheme(false) {
                MbNavigationReorderRows(order.value) {order.value=it}
            }
        }
        val callsTab=compose.onNodeWithTag("reorder-tab-1")
        val rowHeightPx=callsTab.fetchSemanticsNode().boundsInRoot.height
        callsTab.performTouchInput {
            down(center)
            // Feed stationary MOVE events throughout the hold, rather than
            // jumping straight from DOWN to a displaced MOVE after 1 second.
            // This lets the long-press detector observe the held pointer on
            // every emulator before any drag displacement is introduced.
            repeat(8) {
                advanceEventTime(125)
                moveBy(Offset.Zero)
            }
            advanceEventTime(48)
            moveBy(Offset(0f,rowHeightPx*0.1f))
            repeat(12) {
                advanceEventTime(24)
                moveBy(Offset(0f,rowHeightPx*0.13f))
            }
            advanceEventTime(24)
            up()
        }
        compose.runOnIdle {
            assertEquals("0,2,1,3,4",order.value)
            assertEquals(setOf(0,1,2,3,4),MbNavigationOrder.parse(order.value).toSet())
        }
    }

    @Test fun accessibleArrowButtonsReorderWithoutDragging() {
        val order=mutableStateOf(MbNavigationOrder.DEFAULT)
        compose.setContent {
            LiquidTheme(false) {MbNavigationReorderRows(order.value) {order.value=it}}
        }
        compose.onNodeWithContentDescription("Move Calls right").performClick()
        compose.runOnIdle {assertEquals("0,2,1,3,4",order.value)}
        compose.onNodeWithContentDescription("Move Calls left").performClick()
        compose.runOnIdle {assertEquals(MbNavigationOrder.DEFAULT,order.value)}
    }
}
