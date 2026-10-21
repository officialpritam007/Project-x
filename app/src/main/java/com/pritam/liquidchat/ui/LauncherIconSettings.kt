package com.pritam.liquidchat.ui

import android.content.Context
import androidx.compose.runtime.Composable

/** Preserve earlier two-icon API while extending to Codex's four icon presets. */
internal object LauncherIconChoice {
    const val CLASSIC="Classic"
    const val NIGHT="Night Glass"
    val choices=listOf(CLASSIC,NIGHT,"Mint","Midnight")
    fun selected(context:Context):String=when(MbLauncherIcons.active(context).firstOrNull()) {
        MbLauncherIcon.NIGHT_GLASS->NIGHT
        MbLauncherIcon.MINT->"Mint"
        MbLauncherIcon.MIDNIGHT->"Midnight"
        else->CLASSIC
    }
    fun set(context:Context,kind:String) {
        require(kind in choices)
        val icon=when(kind) {
            NIGHT->MbLauncherIcon.NIGHT_GLASS
            "Mint"->MbLauncherIcon.MINT
            "Midnight"->MbLauncherIcon.MIDNIGHT
            else->MbLauncherIcon.DEFAULT
        }
        MbLauncherIcons.select(context,icon).getOrThrow()
    }
}

@Composable internal fun LauncherIconSettings() {MbLauncherIconSettings()}
