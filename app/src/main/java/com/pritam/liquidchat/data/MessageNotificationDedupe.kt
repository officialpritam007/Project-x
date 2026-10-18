package com.pritam.liquidchat.data

import android.content.Context
import java.io.IOException

/**
 * Persistent, account-scoped at-most-once alert receipts for data-only message FCM.
 * Claim BEFORE NotificationManager.notify so dismissing a notification, restarting the
 * process, or receiving the same FCM again cannot trigger another sound/vibration.
 * WorkManager unique work alone is not enough after the first work has completed.
 */
internal object MessageNotificationDedupe {
    private const val FILE = "message_notification_receipts"
    private const val PREFIX = "posted:"
    private const val LAST_PRUNE = "last_prune"
    private const val DAY = 24L * 60 * 60 * 1000
    private const val RETAIN = 30 * DAY // Server FCM TTL is one day.
    private const val HIGH_WATERMARK = 5000
    private const val KEEP_AFTER_PRUNE = 4000
    private val lock = Any()

    private fun key(account: String, messageId: String) = "$"+"{PREFIX}$"+"account:$"+"messageId"

    fun wasPosted(context: Context, account: String, messageId: String): Boolean = synchronized(lock) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).contains(key(account, messageId))
    }

    fun claim(context: Context, account: String, messageId: String, now: Long = System.currentTimeMillis()): Boolean = synchronized(lock) {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        if (prefs.contains(key(account, messageId))) return@synchronized false
        val edit = prefs.edit()
        val last = prefs.getLong(LAST_PRUNE, 0L)
        if (now - last >= DAY || last > now || prefs.all.size > HIGH_WATERMARK) {
            val receipts = prefs.all.mapNotNull { (receiptKey, value) ->
                if (receiptKey.startsWith(PREFIX) && value is Long) receiptKey to value else null
            }.sortedBy { it.second }
            val expired = receipts.filter { it.second < now - RETAIN }
            expired.forEach { edit.remove(it.first) }
            val recent = receipts.drop(expired.size)
            if (recent.size >= HIGH_WATERMARK) {
                recent.take(recent.size - KEEP_AFTER_PRUNE + 1).forEach { edit.remove(it.first) }
            }
            edit.putLong(LAST_PRUNE, now)
        }
        edit.putLong(key(account, messageId), now)
        if (!edit.commit()) throw IOException("Unable to persist notification receipt")
        true
    }

    fun forget(context: Context, account: String, messageId: String) = synchronized(lock) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .remove(key(account, messageId)).commit()
    }

    fun clearAccount(context: Context, account: String) = synchronized(lock) {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val prefix = "$"+"{PREFIX}$"+"account:"
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith(prefix) }.forEach(editor::remove)
        if (!editor.commit()) throw IOException("Unable to erase notification receipts")
    }
}
