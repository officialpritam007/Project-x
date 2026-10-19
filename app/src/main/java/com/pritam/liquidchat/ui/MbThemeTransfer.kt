package com.pritam.liquidchat.ui

import org.json.JSONObject

/**
 * Export only appearance; never serialize account ids, tokens, contacts,
 * messages, media links or other private application data.
 *
 * This is Liquid Chat theme JSON, NOT WhatsApp/MBWhatsApp's proprietary format.
 */
internal object MbThemeTransfer {
    const val MAX_IMPORT_BYTES = 32 * 1024
    const val MIME_TYPE = "application/json"

    fun export(options:DisplayOptions):String = JSONObject().apply {
        put("format","liquid-chat-theme")
        put("version",1)
        put("appearance",options.appearance)
        put("animation",options.animation)
        put("reducedMotion",options.reducedMotion)
        put("reducedTransparency",options.reducedTransparency)
        put("blurIntensity",options.blurIntensity)
        put("bubbleStyle",options.bubbleStyle)
        put("bubbleColor",options.bubbleColor)
        put("messageFontSize",options.messageFontSize)
        put("showNavLabels",options.showNavLabels)
        put("navIconSize",options.navIconSize)
        put("navLabelSize",options.navLabelSize)
        put("headerTitleSize",options.headerTitleSize)
        put("homeChatNameSize",options.homeChatNameSize)
        put("homeChatPreviewSize",options.homeChatPreviewSize)
        put("accentColor",options.accentColor)
        put("showProfileAsTitle",options.showProfileAsTitle)
        put("hideArchiveAccess",options.hideArchiveAccess)
        put("fontPreset",options.fontPreset)
        put("quickReactions",MbQuickReactions.normalize(options.quickReactions))
    }.toString(2)

    /**
     * Parse into immutable state first, only then persist all controls at once.
     * Unknown keys are ignored, invalid values revert to current settings.
     */
    fun decode(text:String,current:DisplayOptions):DisplayOptions {
        require(text.toByteArray(Charsets.UTF_8).size<=MAX_IMPORT_BYTES) {"Theme file is too large."}
        val j=JSONObject(text)
        require(j.optString("format")=="liquid-chat-theme" && j.optInt("version")==1) {
            "Choose a Liquid Chat theme JSON (v1). MBWhatsApp theme files are not directly compatible."
        }
        fun choice(key:String,allowed:Set<String>,previous:String):String =
            j.optString(key,previous).takeIf {it in allowed} ?: previous
        fun number(key:String,currentValue:Int,low:Int,high:Int):Int =
            if(j.has(key) && j.opt(key) is Number)
                (j.opt(key) as Number).toInt().coerceIn(low,high)
            else currentValue
        fun bool(key:String,previous:Boolean):Boolean =
            if(j.opt(key) is Boolean)j.getBoolean(key) else previous
        return current.copy(
            appearance=choice("appearance",setOf("System","Light","Dark"),current.appearance),
            animation=choice("animation",setOf("Normal","Balanced","Extra"),current.animation),
            reducedMotion=bool("reducedMotion",current.reducedMotion),
            reducedTransparency=bool("reducedTransparency",current.reducedTransparency),
            blurIntensity=number("blurIntensity",current.blurIntensity,0,100),
            bubbleStyle=choice("bubbleStyle",setOf("Tail","Rounded","Compact"),current.bubbleStyle),
            bubbleColor=choice("bubbleColor",setOf("Original","Green","Lavender","Rose","Night"),current.bubbleColor),
            messageFontSize=number("messageFontSize",current.messageFontSize,14,22),
            showNavLabels=bool("showNavLabels",current.showNavLabels),
            navIconSize=number("navIconSize",current.navIconSize,18,30),
            navLabelSize=number("navLabelSize",current.navLabelSize,9,15),
            headerTitleSize=number("headerTitleSize",current.headerTitleSize,20,30),
            homeChatNameSize=number("homeChatNameSize",current.homeChatNameSize,13,20),
            homeChatPreviewSize=number("homeChatPreviewSize",current.homeChatPreviewSize,12,18),
            accentColor=choice("accentColor",setOf("Cyan","Blue","Green","Purple","Rose","Amber"),current.accentColor),
            showProfileAsTitle=bool("showProfileAsTitle",current.showProfileAsTitle),
            hideArchiveAccess=bool("hideArchiveAccess",current.hideArchiveAccess),
            fontPreset=choice("fontPreset",setOf("System","Serif","Mono","Cursive"),current.fontPreset),
            quickReactions=if(j.has("quickReactions") && j.opt("quickReactions") is String)
                MbQuickReactions.normalize(j.optString("quickReactions")) else current.quickReactions
        )
    }
}
