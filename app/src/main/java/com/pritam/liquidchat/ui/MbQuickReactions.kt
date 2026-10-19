package com.pritam.liquidchat.ui

/**
 * Only emojis accepted by public.message_action/react in migration
 * 20261017075102_modern_message_actions.sql are selectable.
 * Local presentation shortcut only; reactions still use the existing
 * authenticated server action and are never faked in the message model.
 */
internal object MbQuickReactions {
    val available:List<String> = listOf(
        "👍", "❤️", "😂", "😮", "😢", "🙏", "👏"
    )
    val defaults:List<String> = available.take(6)
    val defaultSetting:String = defaults.joinToString(",")

    /** Reject unsupported values, duplicates and oversized imports. */
    fun normalize(value:String?):String {
        if(value==null || value.length>128)return defaultSetting
        val selected=value.split(",").filter {it in available}.distinct()
        return if(selected.size in 1..6)selected.joinToString(",") else defaultSetting
    }

    fun selected(setting:String?):List<String> = normalize(setting).split(",")

    /** Only changes selection; no stale placeholder is shown as a working toggle. */
    fun toggle(setting:String?,emoji:String):String {
        val previous=selected(setting)
        if(emoji !in available)return previous.joinToString(",")
        val next=if(emoji in previous) {
            if(previous.size==1)return previous.joinToString(",")
            previous-emoji
        } else {
            if(previous.size>=6)return previous.joinToString(",")
            previous+emoji
        }
        return next.joinToString(",")
    }
}
