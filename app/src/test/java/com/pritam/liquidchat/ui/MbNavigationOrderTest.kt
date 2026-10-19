package com.pritam.liquidchat.ui

import org.junit.Assert.*
import org.junit.Test

class MbNavigationOrderTest {
    @Test fun defaultKeepsAllFiveDestinationsAndCurrentOrder() {
        assertEquals(listOf(0,1,2,3,4),MbNavigationOrder.parse(null))
        assertEquals(MbNavigationOrder.DEFAULT,MbNavigationOrder.normalize(null))
        assertEquals(MbNavigationOrder.DEFAULT,DisplayOptions().navigationOrder)
    }
    @Test fun movingTabsDoesNotChangeRouteIdsOrLoseCalls() {
        val first=MbNavigationOrder.move(MbNavigationOrder.DEFAULT,1,1)
        assertEquals(listOf(0,2,1,3,4),MbNavigationOrder.parse(first))
        val second=MbNavigationOrder.move(first,1,-1)
        assertEquals(MbNavigationOrder.DEFAULT,second)
        val end=MbNavigationOrder.move(MbNavigationOrder.DEFAULT,4,-1)
        assertEquals(listOf(0,1,2,4,3),MbNavigationOrder.parse(end))
        assertEquals(setOf(0,1,2,3,4),MbNavigationOrder.parse(end).toSet())
    }
    @Test fun corruptedOrDuplicatedOrderFallsBackToSafeDefault() {
        for(input in listOf("0,1,1,3,4","0,1,2,3","5,1,2,3,4","0,1,2,3,4,6","junk","",",,,")) {
            assertEquals("Invalid order: $input",MbNavigationOrder.DEFAULT,MbNavigationOrder.normalize(input))
        }
    }
    @Test fun firstAndLastItemsCannotMoveOutsideNavigation() {
        assertEquals(MbNavigationOrder.DEFAULT,MbNavigationOrder.move(MbNavigationOrder.DEFAULT,0,-1))
        assertEquals(MbNavigationOrder.DEFAULT,MbNavigationOrder.move(MbNavigationOrder.DEFAULT,4,1))
        assertTrue(runCatching {MbNavigationOrder.move(MbNavigationOrder.DEFAULT,7,1)}.isFailure)
        assertTrue(runCatching {MbNavigationOrder.move(MbNavigationOrder.DEFAULT,1,0)}.isFailure)
    }
}
