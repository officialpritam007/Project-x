package com.pritam.liquidchat.data

import android.app.NotificationManager
import android.content.Context
import android.content.res.Configuration

/** Device- and account-scoped choices for silent message notifications. */
internal object DeviceAlertPreferences {
    private const val PREFS="liquid_message_alert_policies_v1"
    private fun key(account:String,suffix:String)="$account:$suffix"

    fun quietDuringDnd(context:Context,account:String):Boolean =
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
            .getBoolean(key(account,"dnd"),false)

    fun quietDuringLandscape(context:Context,account:String):Boolean =
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
            .getBoolean(key(account,"landscape"),false)

    /** These affect future account notifications on this phone, not Android's
     * app-wide permission or another signed-in account's stored preference.
     */
    fun disableAll(context:Context,account:String):Boolean =
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
            .getBoolean(key(account,"disabled"),false)

    fun autoHideMessages(context:Context,account:String):Boolean =
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
            .getBoolean(key(account,"autohide"),false)

    fun update(context:Context,account:String,
        dnd:Boolean?=null,landscape:Boolean?=null,
        disabled:Boolean?=null,autoHide:Boolean?=null
    ) {
        require(account.isNotBlank())
        val editor=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
        if(dnd!=null)editor.putBoolean(key(account,"dnd"),dnd)
        if(landscape!=null)editor.putBoolean(key(account,"landscape"),landscape)
        if(disabled!=null)editor.putBoolean(key(account,"disabled"),disabled)
        if(autoHide!=null)editor.putBoolean(key(account,"autohide"),autoHide)
        check(editor.commit()) {"Couldn't save notification alert preference."}
    }

    fun shouldSilence(context:Context,account:String):Boolean {
        val landscape=context.resources.configuration.orientation ==
            Configuration.ORIENTATION_LANDSCAPE
        val manager=context.getSystemService(NotificationManager::class.java)
        val dndActive=runCatching {
            manager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        }.getOrDefault(false)
        return (landscape && quietDuringLandscape(context,account)) ||
            (dndActive && quietDuringDnd(context,account))
    }

    fun clearAccount(context:Context,account:String) {
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
            .remove(key(account,"dnd"))
            .remove(key(account,"landscape"))
            .remove(key(account,"disabled"))
            .remove(key(account,"autohide"))
            .apply()
    }
}
