package com.pritam.liquidchat.ui

import org.junit.Assert.*
import org.junit.Test

class MbLauncherIconsTest {
    private class Components(initial:Set<MbLauncherIcon>):LauncherIconComponents {
        val active=initial.toMutableSet()
        var failEnable=false
        var failDisable:MbLauncherIcon?=null
        override fun enabled(icon:MbLauncherIcon)=icon in active
        override fun setEnabled(icon:MbLauncherIcon,enabled:Boolean) {
            if(enabled) {
                if(failEnable)throw IllegalStateException("Enable rejected")
                active+=icon
            } else {
                if(icon==failDisable)throw IllegalStateException("Disable rejected")
                active-=icon
            }
            check(active.isNotEmpty()) {"Launcher became unreachable"}
        }
    }
    @Test fun switchAndRestoreNeverLoseEntryPoint() {
        val c=Components(setOf(MbLauncherIcon.DEFAULT))
        switchLauncherIcon(c,MbLauncherIcon.MINT)
        assertEquals(setOf(MbLauncherIcon.MINT),c.active)
        switchLauncherIcon(c,MbLauncherIcon.DEFAULT)
        assertEquals(setOf(MbLauncherIcon.DEFAULT),c.active)
    }
    @Test fun unavailableNewIconCannotDisableOld() {
        val c=Components(setOf(MbLauncherIcon.DEFAULT)).apply {failEnable=true}
        assertTrue(runCatching {switchLauncherIcon(c,MbLauncherIcon.MIDNIGHT)}.isFailure)
        assertEquals(setOf(MbLauncherIcon.DEFAULT),c.active)
    }
    @Test fun partialCleanupCanBeRetried() {
        val c=Components(setOf(MbLauncherIcon.DEFAULT)).apply {failDisable=MbLauncherIcon.DEFAULT}
        assertTrue(runCatching {switchLauncherIcon(c,MbLauncherIcon.MIDNIGHT)}.isFailure)
        assertTrue(MbLauncherIcon.MIDNIGHT in c.active)
        c.failDisable=null
        switchLauncherIcon(c,MbLauncherIcon.MIDNIGHT)
        assertEquals(setOf(MbLauncherIcon.MIDNIGHT),c.active)
    }
}
