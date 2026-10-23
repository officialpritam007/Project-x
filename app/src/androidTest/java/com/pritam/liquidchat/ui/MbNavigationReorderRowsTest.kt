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
        var currentOrder=""
        compose.setContent {
            var order by remember {mutableStateOf(MbNavigationOrder.DEFAULT)}
            currentOrder=order
            LiquidTheme(false) {
                MbNavigationReorderRows(order) {order=it}
            }
        }
        compose.onNodeWithTag("reorder-tab-1").performTouchInput {
            down(center)
            advanceEventTime(850)
            moveBy(Offset(0f,size.height*1.4f))
            up()
        }
        compose.runOnIdle {
            assertEquals("0,2,1,3,4",currentOrder)
            assertEquals(setOf(0,1,2,3,4),MbNavigationOrder.parse(currentOrder).toSet())
        }
    }

    @Test fun accessibleArrowButtonsReorderWithoutDragging() {
        var currentOrder=""
        compose.setContent {
            var order by remember {mutableStateOf(MbNavigationOrder.DEFAULT)}
            currentOrder=order
            LiquidTheme(false) {MbNavigationReorderRows(order) {order=it}}
        }
        compose.onNodeWithContentDescription("Move Calls right").performClick()
        compose.runOnIdle {assertEquals("0,2,1,3,4",currentOrder)}
        compose.onNodeWithContentDescription("Move Calls left").performClick()
        compose.runOnIdle {assertEquals(MbNavigationOrder.DEFAULT,currentOrder)}
    }
}
