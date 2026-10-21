package com.pritam.liquidchat.data

import android.content.Context
import com.pritam.liquidchat.R

/** Device-local artwork selection, never influenced by untrusted FCM payload fields. */
internal object LiquidNotificationIcon {
    fun resource(context:Context):Int =
        when(context.getSharedPreferences("display",Context.MODE_PRIVATE)
            .getString("notificationIconStyle","Classic")) {
            "Outline" -> R.drawable.ic_notification_outline
            else -> R.drawable.ic_notification
        }
}
