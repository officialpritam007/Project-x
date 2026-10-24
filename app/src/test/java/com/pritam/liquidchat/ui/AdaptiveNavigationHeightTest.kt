package com.pritam.liquidchat.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/** Implemented assertions only. Execute after the owner's verification go-ahead. */
class AdaptiveNavigationHeightTest {
    @Test fun referenceDefaultPreservesRecordedSize() {
        assertEquals(56,referenceNavigationHeight(DisplayOptions()))
    }
    @Test fun largeIconsAndLabelsHaveRoom() {
        assertEquals(84,referenceNavigationHeight(
            DisplayOptions(navIconSize=50,navLabelSize=20)))
    }
    @Test fun hiddenIconsRetainAccessibleLabels() {
        assertEquals(56,referenceNavigationHeight(
            DisplayOptions(hideNavIcons=true,showNavLabels=false,navLabelSize=20)))
    }
    @Test fun iconOnlyModeAllowsNoLabelSpace() {
        assertEquals(62,referenceNavigationHeight(
            DisplayOptions(showNavLabels=false,navIconSize=50)))
    }
}
