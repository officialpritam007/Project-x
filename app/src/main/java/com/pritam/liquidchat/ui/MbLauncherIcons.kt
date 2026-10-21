package com.pritam.liquidchat.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/** Launcher-only aliases never disable the OAuth, push and call MainActivity. */
internal enum class MbLauncherIcon(val title:String,val alias:String) {
    DEFAULT("Blue","Default"), NIGHT_GLASS("Night Glass","NightGlass"),
    MINT("Mint","Mint"), MIDNIGHT("Midnight","Midnight")
}

internal interface LauncherIconComponents {
    fun enabled(icon:MbLauncherIcon):Boolean
    fun setEnabled(icon:MbLauncherIcon,enabled:Boolean)
}

/** Enable/verify the selected alias before hiding any old launcher icon. */
internal fun switchLauncherIcon(components:LauncherIconComponents,selected:MbLauncherIcon) {
    components.setEnabled(selected,true)
    check(components.enabled(selected)) {"The new launcher icon could not be enabled."}
    MbLauncherIcon.entries.filter {it!=selected}.forEach {components.setEnabled(it,false)}
    check(MbLauncherIcon.entries.filter(components::enabled)==listOf(selected)) {
        "Another icon could not be hidden. Select an icon to retry."
    }
}

internal object MbLauncherIcons {
    private fun components(context:Context):LauncherIconComponents {
        val manager=context.packageManager
        fun component(icon:MbLauncherIcon)=ComponentName(
            context.packageName,"${context.packageName}.launcher.${icon.alias}")
        return object:LauncherIconComponents {
            override fun enabled(icon:MbLauncherIcon):Boolean=
                when(manager.getComponentEnabledSetting(component(icon))) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED->true
                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT->icon==MbLauncherIcon.DEFAULT
                    else->false
                }
            override fun setEnabled(icon:MbLauncherIcon,enabled:Boolean) {
                manager.setComponentEnabledSetting(component(icon),
                    if(enabled)PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP)
            }
        }
    }
    fun active(context:Context):List<MbLauncherIcon> =
        components(context).let {store->MbLauncherIcon.entries.filter(store::enabled)}
    fun select(context:Context,icon:MbLauncherIcon):Result<Unit> =
        runCatching {switchLauncherIcon(components(context),icon)}
}
