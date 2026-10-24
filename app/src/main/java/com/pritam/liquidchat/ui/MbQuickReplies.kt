package com.pritam.liquidchat.ui

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** Device-local slash shortcuts; expanded text is sent by the existing Supabase outbox. */
internal data class MbQuickReply(val shortcut:String,val body:String)

internal object MbQuickReplies {
    private const val PREFS="liquid_chat_quick_replies_v1"
    private const val MAX_REPLIES=24
    private const val MAX_TEXT=4000
    private val format=Regex("[a-z0-9_]{1,24}")

    private fun normalized(value:String):String =
        value.trim().removePrefix("/").lowercase(Locale.ROOT).also {
            require(format.matches(it)) {"Use 1–24 letters, digits or underscores."}
        }

    private fun accountKey(account:String):String {
        require(account.isNotBlank()) {"Sign in before managing quick replies."}
        return "account:$account"
    }

    fun list(context:Context,account:String?):List<MbQuickReply> {
        if(account.isNullOrBlank())return emptyList()
        val raw=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
            .getString(accountKey(account),"[]") ?: "[]"
        return runCatching {
            val array=JSONArray(raw)
            buildList<MbQuickReply> {
                for(i in 0 until minOf(array.length(),MAX_REPLIES)) {
                    val item=array.optJSONObject(i) ?: continue
                    val shortcut=runCatching {normalized(item.optString("shortcut"))}.getOrNull()
                    val body=item.optString("body")
                    if(shortcut!=null && body.isNotBlank() && body.length<=MAX_TEXT &&
                        none {it.shortcut==shortcut})add(MbQuickReply(shortcut,body))
                }
            }
        }.getOrDefault(emptyList()).sortedBy {it.shortcut}
    }

    private fun persist(context:Context,account:String,items:List<MbQuickReply>) {
        val array=JSONArray()
        items.forEach {array.put(JSONObject().put("shortcut",it.shortcut).put("body",it.body))}
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
            .putString(accountKey(account),array.toString()).apply()
    }

    fun save(context:Context,account:String,shortcut:String,message:String):List<MbQuickReply> {
        val cleaned=normalized(shortcut)
        val body=message.trim()
        require(body.isNotEmpty() && body.length<=MAX_TEXT) {"Reply must contain 1–4000 characters."}
        val existing=list(context,account)
        require(existing.any {it.shortcut==cleaned} || existing.size<MAX_REPLIES) {
            "Maximum of 24 quick replies reached."
        }
        val next=(existing.filterNot {it.shortcut==cleaned}+MbQuickReply(cleaned,body))
            .sortedBy {it.shortcut}
        persist(context,account,next)
        return next
    }

    fun delete(context:Context,account:String,shortcut:String):List<MbQuickReply> {
        val next=list(context,account).filterNot {it.shortcut==normalized(shortcut)}
        persist(context,account,next)
        return next
    }

    fun matches(items:List<MbQuickReply>,typed:String):List<MbQuickReply> {
        if(!typed.startsWith("/") || typed.length>25 ||
            typed.any {it.isWhitespace()})return emptyList()
        val query=typed.drop(1).lowercase(Locale.ROOT)
        return items.filter {it.shortcut.startsWith(query)}.take(5)
    }

    /** Wipe potentially private reply text on logout and account deletion. */
    fun clear(context:Context,account:String) {
        if(account.isNotBlank())context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
            .edit().remove(accountKey(account)).apply()
    }
}
