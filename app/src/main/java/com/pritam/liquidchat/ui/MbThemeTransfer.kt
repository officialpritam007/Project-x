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

    /** Restore visuals only, without silently revoking privacy or accessibility choices. */
    fun resetAppearance(current:DisplayOptions):DisplayOptions =
        DisplayOptions().copy(
            reducedMotion=current.reducedMotion,
            reducedTransparency=current.reducedTransparency,
            hideOutgoingTyping=current.hideOutgoingTyping,
            hideHomePreviews=current.hideHomePreviews,
            preventScreenshots=current.preventScreenshots
        )

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
        put("navigationOrder",MbNavigationOrder.normalize(options.navigationOrder))
        put("quickReactions",MbQuickReactions.normalize(options.quickReactions))
        put("homeWallpaperDim",options.homeWallpaperDim)
        put("chatWallpaperDim",options.chatWallpaperDim)
        put("homeAvatarSize",options.homeAvatarSize)
        put("homeRowPadding",options.homeRowPadding)
        put("hideHomeSearch",options.hideHomeSearch)
        put("showHomeDividers",options.showHomeDividers)
        put("navBarStyle",options.navBarStyle)
        put("hidePhotoShortcut",options.hidePhotoShortcut)
        put("showPinBadges",options.showPinBadges)
        put("emojiPickerSize",options.emojiPickerSize)
        put("linkColor",options.linkColor)
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
            messageFontSize=number("messageFontSize",current.messageFontSize,10,30),
            showNavLabels=bool("showNavLabels",current.showNavLabels),
            navIconSize=number("navIconSize",current.navIconSize,20,50),
            navLabelSize=number("navLabelSize",current.navLabelSize,8,20),
            headerTitleSize=number("headerTitleSize",current.headerTitleSize,20,30),
            homeChatNameSize=number("homeChatNameSize",current.homeChatNameSize,10,30),
            homeChatPreviewSize=number("homeChatPreviewSize",current.homeChatPreviewSize,12,18),
            accentColor=choice("accentColor",setOf("Cyan","Blue","Green","Purple","Rose","Amber"),current.accentColor),
            showProfileAsTitle=bool("showProfileAsTitle",current.showProfileAsTitle),
            hideArchiveAccess=bool("hideArchiveAccess",current.hideArchiveAccess),
            fontPreset=choice("fontPreset",setOf("System","Serif","Mono","Cursive"),current.fontPreset),
            navigationOrder=if(j.has("navigationOrder") && j.opt("navigationOrder") is String)
                MbNavigationOrder.normalize(j.optString("navigationOrder")) else current.navigationOrder,
            quickReactions=if(j.has("quickReactions") && j.opt("quickReactions") is String)
                MbQuickReactions.normalize(j.optString("quickReactions")) else current.quickReactions,
            homeWallpaperDim=number("homeWallpaperDim",current.homeWallpaperDim,0,100),
            chatWallpaperDim=number("chatWallpaperDim",current.chatWallpaperDim,0,100),
            homeAvatarSize=number("homeAvatarSize",current.homeAvatarSize,40,68),
            homeRowPadding=number("homeRowPadding",current.homeRowPadding,6,20),
            hideHomeSearch=bool("hideHomeSearch",current.hideHomeSearch),
            showHomeDividers=bool("showHomeDividers",current.showHomeDividers),
            navBarStyle=choice("navBarStyle",setOf("Reference","Liquid"),current.navBarStyle),
            hidePhotoShortcut=bool("hidePhotoShortcut",current.hidePhotoShortcut),
            showPinBadges=bool("showPinBadges",current.showPinBadges),
            emojiPickerSize=number("emojiPickerSize",current.emojiPickerSize,18,32),
            linkColor=choice("linkColor",setOf("Theme","Blue","Green","Purple","Rose","Amber"),current.linkColor)
        )
    }
}
