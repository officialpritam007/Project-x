package com.pritam.liquidchat.ui

import org.junit.Assert.*
import org.junit.Test

class MbNavigationVisibilityTest {
    @Test fun everyHiddenCombinationPreservesAllRoutesAndUserOrder() {
        val order="4,2,0,3,1"
        val expected=listOf(4,2,0,3,1)
        for(mask in 0 until 32) {
            val hidden=(0..4).filter {mask and (1 shl it)!=0}.toSet()
            val plan=navigationVisibilityPlan(order,hidden)
            assertEquals(expected.filterNot {it in hidden},plan.visible)
            assertEquals(expected.filter {it in hidden},plan.overflow)
            assertEquals(setOf(0,1,2,3,4),(plan.visible+plan.overflow).toSet())
            assertEquals(5,(plan.visible+plan.overflow).size)
            assertTrue(plan.visible.none {it in plan.overflow})
        }
    }
    @Test fun hidingAllStillProvidesMoreMenuAccessToCallsAndSettings() {
        val plan=navigationVisibilityPlan(null,setOf(0,1,2,3,4))
        assertTrue(plan.visible.isEmpty())
        assertEquals(listOf(0,1,2,3,4),plan.overflow)
    }
    @Test fun malformedSavedValuesFailOpenRatherThanHideNavigation() {
        listOf(null,"","1,wrong,3","-1","5","1,,2").forEach {
            assertEquals(emptySet<Int>(),MbNavigationVisibility.parse(it))
        }
        assertEquals(setOf(1,3),MbNavigationVisibility.parse(" 3,1,3 "))
        assertEquals(listOf(0,1,2,3,4),navigationVisibilityPlan("invalid",setOf(6)).visible)
    }
}
