package com.pritam.liquidchat.ui

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        val callsTab=compose.onNodeWithTag("reorder-tab-1")
        val contactsBounds=compose.onNodeWithTag("reorder-tab-2").fetchSemanticsNode().boundsInRoot
        assertTrue("Reorder handles must occupy separate vertical rows",
            callsTab.fetchSemanticsNode().boundsInRoot.bottom<=contactsBounds.top)
        val rowHeightPx=callsTab.fetchSemanticsNode().boundsInRoot.height
        callsTab.performTouchInput {
            down(center)
            advanceEventTime(850)
            // The first move establishes the post-long-press drag; a subsequent
            // pointer move must carry the displacement before finger release.
            moveBy(Offset(0f,rowHeightPx*0.2f))
            advanceEventTime(16)
            moveBy(Offset(0f,rowHeightPx*1.2f))
            advanceEventTime(16)
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
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Move Calls left").assertIsEnabled().performClick()
        compose.waitForIdle()
        compose.runOnIdle {assertEquals(MbNavigationOrder.DEFAULT,currentOrder)}
    }
}
