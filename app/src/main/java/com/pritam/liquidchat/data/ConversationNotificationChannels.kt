package com.pritam.liquidchat.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import java.security.MessageDigest

/**
 * Dedicated Android notification channels are opt-in, account- and conversation-scoped.
 * Android controls channel sound, vibration and importance after creation.
 * Never delete/re-create an existing channel merely to reset a user's OS preferences.
 */
internal object ConversationNotificationChannels {
    private const val FILE="conversation_notification_channels"
    private const val PREFIX="enabled:"
    private const val HIDE_PREVIEW_PREFIX="hide-preview:"
    private const val CHANNEL_PREFIX="liquid-contact-"
    const val DEFAULT_CHANNEL="messages"

    internal fun channelId(account:String,cid:String):String {
        val digest=MessageDigest.getInstance("SHA-256")
            .digest("$account:$cid".toByteArray(Charsets.UTF_8))
        return CHANNEL_PREFIX+digest.take(12).joinToString("") {"%02x".format(it.toInt() and 0xff)}
    }

    private fun enabledKey(account:String,cid:String)="$PREFIX$account:$cid"
    private fun hidePreviewKey(account:String,cid:String)="$HIDE_PREVIEW_PREFIX$account:$cid"
    fun hiddenPreview(context:Context,account:String,cid:String):Boolean =
        context.getSharedPreferences(FILE,Context.MODE_PRIVATE)
            .getBoolean(hidePreviewKey(account,cid),false)

    fun setHiddenPreview(context:Context,account:String,cid:String,hidden:Boolean) {
        val saved=context.getSharedPreferences(FILE,Context.MODE_PRIVATE)
            .edit().putBoolean(hidePreviewKey(account,cid),hidden).commit()
        check(saved) {"Unable to save conversation notification preview privacy."}
    }

    fun enabled(context:Context,account:String,cid:String):Boolean =
        context.getSharedPreferences(FILE,Context.MODE_PRIVATE).getBoolean(enabledKey(account,cid),false)

    fun setEnabled(context:Context,account:String,cid:String,name:String,enabled:Boolean) {
        if(enabled)createChannel(context,account,cid,name)
        val ok=context.getSharedPreferences(FILE,Context.MODE_PRIVATE)
            .edit().putBoolean(enabledKey(account,cid),enabled).commit()
        check(ok) {"Unable to save notification preferences."}
    }

    /** For notifications received while the app is not running, use the same persisted choice. */
    fun channelForMessage(context:Context,account:String,cid:String,name:String):String {
        if(!enabled(context,account,cid))return DEFAULT_CHANNEL
        return createChannel(context,account,cid,name)
    }

    fun createChannel(context:Context,account:String,cid:String,name:String):String {
        val id=channelId(account,cid)
        val manager=context.getSystemService(NotificationManager::class.java)
        val existing=manager.getNotificationChannel(id)
        if(existing==null) manager.createNotificationChannel(
            NotificationChannel(id,name.ifBlank {"Conversation"},NotificationManager.IMPORTANCE_HIGH).apply {
                description="Notifications for one Liquid Chat conversation. Configure sounds and vibration in Android Settings."
            }
        ) else if(existing.name.toString()!=name && name.isNotBlank()) {
            // Android permits updating the display name but preserves the user's
            // customized sound/vibration/importance on the existing channel.
            manager.createNotificationChannel(NotificationChannel(id,name,existing.importance))
        }
        return id
    }

    fun settingsIntent(context:Context,account:String,cid:String):Intent =
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID,channelId(account,cid))

    /** Delete only a conversation's obsolete Android channel and its preference. */
    fun clearConversation(context:Context,account:String,cid:String) {
        val ok=context.getSharedPreferences(FILE,Context.MODE_PRIVATE)
            .edit().remove(enabledKey(account,cid))
            .remove(hidePreviewKey(account,cid)).commit()
        check(ok) {"Unable to remove the deleted conversation's notification preference."}
        context.getSystemService(NotificationManager::class.java)
            .deleteNotificationChannel(channelId(account,cid))
    }

    /** Do not expose a signed-out account's contacts in system notification settings. */
    fun clearAccount(context:Context,account:String) {
        val prefs=context.getSharedPreferences(FILE,Context.MODE_PRIVATE)
        val prefix="$PREFIX$account:"
        val channels=prefs.all.keys.filter {it.startsWith(prefix)}
        val privatePreviews=prefs.all.keys.filter {it.startsWith("$HIDE_PREVIEW_PREFIX$account:")}
        val manager=context.getSystemService(NotificationManager::class.java)
        channels.forEach {key->
            val cid=key.removePrefix(prefix)
            manager.deleteNotificationChannel(channelId(account,cid))
        }
        val edit=prefs.edit()
        channels.forEach {edit.remove(it)}
        privatePreviews.forEach {edit.remove(it)}
        check(edit.commit()) {"Unable to remove previous account notification settings."}
    }
}
