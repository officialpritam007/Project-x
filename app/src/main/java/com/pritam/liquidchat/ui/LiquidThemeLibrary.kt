package com.pritam.liquidchat.ui

import android.content.Context

/**
 * Device-local theme bookmarks. Only theme JSON is stored; accounts, contacts,
 * auth material, wallpaper files and message data never enter this library.
 */
internal object LiquidThemeLibrary {
    private const val PREFS="liquid_chat_saved_themes_v1"
    private const val MAX_THEMES=24
    private fun prefs(context:Context)=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
    private fun name(raw:String):String {
        val value=raw.trim()
        require(value.isNotBlank()&&value.length<=36) {"Theme name must be 1–36 characters."}
        require(value.none {Character.isISOControl(it)}) {"Theme name contains invalid characters."}
        return value
    }
    fun names(context:Context):List<String> =
        prefs(context).getStringSet("names",emptySet()).orEmpty().toList().sortedBy {it.lowercase()}

    fun save(context:Context,title:String,options:DisplayOptions) {
        val clean=name(title)
        val saved=names(context)
        require(clean in saved || saved.size<MAX_THEMES) {"Maximum saved themes reached."}
        val json=MbThemeTransfer.export(options)
        require(json.toByteArray(Charsets.UTF_8).size<=MbThemeTransfer.MAX_IMPORT_BYTES)
        prefs(context).edit()
            .putString("theme:$clean",json)
            .putStringSet("names",(saved+clean).toSet())
            .apply()
    }

    fun restore(context:Context,title:String,current:DisplayOptions):DisplayOptions {
        val clean=name(title)
        require(clean in names(context)) {"Saved theme no longer exists."}
        val json=prefs(context).getString("theme:$clean",null)
            ?: error("Saved theme could not be read.")
        return MbThemeTransfer.decode(json,current)
    }

    fun delete(context:Context,title:String) {
        val clean=name(title)
        val saved=names(context)
        prefs(context).edit().remove("theme:$clean")
            .putStringSet("names",(saved-clean).toSet()).apply()
    }
}
