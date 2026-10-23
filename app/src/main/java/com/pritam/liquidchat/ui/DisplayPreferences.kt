package com.pritam.liquidchat.ui

import android.content.SharedPreferences

/** One persistence boundary for live edits and process-restored appearance. */
internal class DisplayPreferences(private val displayPrefs:SharedPreferences) {
    fun read():DisplayOptions = DisplayOptions(displayPrefs.getString("appearance","System") ?: "System",displayPrefs.getString("animation","Balanced") ?: "Balanced",displayPrefs.getBoolean("reducedMotion",false),
        displayPrefs.getBoolean("reducedTransparency",false),
        displayPrefs.getInt("blurIntensity",55).coerceIn(0,100),normalizedBubbleStyle(displayPrefs.getString("bubbleStyle","Tail")),
        displayPrefs.getString("bubbleColor","Original")?.takeIf {it in setOf("Original","Green","Lavender","Rose","Night")} ?: "Original",displayPrefs.getInt("messageFontSize",16).coerceIn(10,30),
        displayPrefs.getBoolean("showNavLabels",true),
        displayPrefs.getInt("navIconSize",24).coerceIn(20,50),
        displayPrefs.getInt("navLabelSize",11).coerceIn(8,20),
        displayPrefs.getInt("headerTitleSize",25).coerceIn(20,30),
        displayPrefs.getInt("homeChatNameSize",16).coerceIn(10,30),
        displayPrefs.getInt("homeChatPreviewSize",14).coerceIn(12,18),
        displayPrefs.getString("accentColor","Cyan")?.takeIf {it in setOf("Cyan","Blue","Green","Purple","Rose","Amber","Custom")} ?: "Cyan",displayPrefs.getBoolean("showProfileAsTitle",false),
        displayPrefs.getBoolean("hideArchiveAccess",false),
        displayPrefs.getString("fontPreset","System")?.takeIf {it in setOf("System","Serif","Mono","Cursive","Imported")} ?: "System",displayPrefs.getBoolean("hideOutgoingTyping",false),
        displayPrefs.getBoolean("preventScreenshots",false),
        displayPrefs.getInt("homeWallpaperDim",50).coerceIn(0,100),
        displayPrefs.getInt("chatWallpaperDim",50).coerceIn(0,100),
        displayPrefs.getBoolean("hideHomePreviews",false),MbQuickReactions.normalize(displayPrefs.getString("quickReactions",null)),MbNavigationOrder.normalize(displayPrefs.getString("navigationOrder",null)),
        displayPrefs.getInt("homeAvatarSize",52).coerceIn(40,68),
        displayPrefs.getInt("homeRowPadding",11).coerceIn(6,20),
        displayPrefs.getBoolean("hideHomeSearch",false),
        displayPrefs.getBoolean("showHomeDividers",false),
        displayPrefs.getString("navBarStyle","Reference")?.takeIf {it in setOf("Reference","Liquid")} ?: "Reference",displayPrefs.getBoolean("hidePhotoShortcut",false),
        displayPrefs.getBoolean("showPinBadges",true),
        displayPrefs.getInt("emojiPickerSize",24).coerceIn(18,32),
        displayPrefs.getString("linkColor","Theme")?.takeIf {it in setOf("Theme","Blue","Green","Purple","Rose","Amber","Custom")} ?: "Theme",displayPrefs.getBoolean("hideNavIcons",false),
        displayPrefs.getString("outgoingBubblePreset","Follow")?.takeIf {it in setOf("Follow","Original","Green","Lavender","Rose","Night","Custom")} ?: "Follow",displayPrefs.getString("incomingBubblePreset","Follow")?.takeIf {it in setOf("Follow","Original","Green","Lavender","Rose","Night","Custom")} ?: "Follow",displayPrefs.getString("outgoingInk","Theme")?.takeIf {it in setOf("Theme","Blue","Green","Purple","Rose","Amber","Custom")} ?: "Theme",displayPrefs.getString("incomingInk","Theme")?.takeIf {it in setOf("Theme","Blue","Green","Purple","Rose","Amber","Custom")} ?: "Theme",displayPrefs.getString("quotedInk","Theme")?.takeIf {it in setOf("Theme","Blue","Green","Purple","Rose","Amber","Custom")} ?: "Theme",displayPrefs.getString("timestampInk","Theme")?.takeIf {it in setOf("Theme","Blue","Green","Purple","Rose","Amber","Custom")} ?: "Theme",normalizedAccentHex(displayPrefs.getString("customAccentHex","#00789D") ?: "") ?: "#00789D",normalizedAccentHex(displayPrefs.getString("outgoingBubbleHex","#D0EDFB") ?: "") ?: "#D0EDFB",normalizedAccentHex(displayPrefs.getString("incomingBubbleHex","#FAFDFF") ?: "") ?: "#FAFDFF",displayPrefs.getBoolean("hideHomeFilters",false),
        displayPrefs.getBoolean("hideStoriesCarousel",false),
        displayPrefs.getBoolean("randomAccentOnLaunch",false),
        displayPrefs.getString("notificationIconStyle","Classic")?.takeIf {it in setOf("Classic","Outline")} ?: "Classic",displayPrefs.getInt("bubbleOpacity",100).coerceIn(45,100),normalizedAccentHex(displayPrefs.getString("outgoingInkHex","#172D42") ?: "") ?: "#172D42",normalizedAccentHex(displayPrefs.getString("incomingInkHex","#172D42") ?: "") ?: "#172D42",normalizedAccentHex(displayPrefs.getString("quotedInkHex","#172D42") ?: "") ?: "#172D42",normalizedAccentHex(displayPrefs.getString("timestampInkHex","#536B7D") ?: "") ?: "#536B7D",chatHeaderIconColor=displayPrefs.getString("chatHeaderIconColor","Theme")?.takeIf {it in setOf("Theme","Accent","Text")} ?: "Theme",chatHeaderNameSize=displayPrefs.getInt("chatHeaderNameSize",16).coerceIn(12,24),copySenderAndTime=displayPrefs.getBoolean("copySenderAndTime",false),photoQuality=displayPrefs.getString("photoQuality","Standard")?.takeIf {it in setOf("Standard","HD")} ?: "Standard",videoQuality=displayPrefs.getString("videoQuality","HD")?.takeIf {it in setOf("Standard","HD")} ?: "HD",incognitoKeyboard=displayPrefs.getBoolean("incognitoKeyboard",false),titleFontPreset=normalizedTitleFontPreset(displayPrefs.getString("titleFontPreset","Follow")),customStickerSize=displayPrefs.getBoolean("customStickerSize",false),stickerSize=displayPrefs.getInt("stickerSize",192).coerceIn(96,256),doubleTapReactionEnabled=displayPrefs.getBoolean("doubleTapReactionEnabled",false),doubleTapReactionEmoji=displayPrefs.getString("doubleTapReactionEmoji","❤️") ?: "❤️",hideQuickReplySuggestions=displayPrefs.getBoolean("hideQuickReplySuggestions",false),confirmSavedStickerSend=displayPrefs.getBoolean("confirmSavedStickerSend",true),showEditedLabel=displayPrefs.getBoolean("showEditedLabel",true),showEditedTime=displayPrefs.getBoolean("showEditedTime",false),showLongPressPreview=displayPrefs.getBoolean("showLongPressPreview",true),hideHomeFloatingButtons=displayPrefs.getBoolean("hideHomeFloatingButtons",false),appTextHex=normalizedAppColor(displayPrefs.getString("appTextHex",null)),appBackgroundHex=normalizedAppColor(displayPrefs.getString("appBackgroundHex",null)),appChromeHex=normalizedAppColor(displayPrefs.getString("appChromeHex",null)),appUiScale=displayPrefs.getInt("appUiScale",100).coerceIn(85,120),appTextScale=displayPrefs.getInt("appTextScale",100).coerceIn(90,130),showWallpaper=displayPrefs.getBoolean("showWallpaper",true))
    fun write(options:DisplayOptions):DisplayOptions {
        val safe=options.copy(
            blurIntensity=options.blurIntensity.coerceIn(0,100),
            bubbleStyle=normalizedBubbleStyle(options.bubbleStyle),
            bubbleColor=options.bubbleColor.takeIf {it in setOf("Original","Green","Lavender","Rose","Night")} ?: "Original",
            messageFontSize=options.messageFontSize.coerceIn(10,30),
            navIconSize=options.navIconSize.coerceIn(20,50),
            navLabelSize=options.navLabelSize.coerceIn(8,20),
            headerTitleSize=options.headerTitleSize.coerceIn(20,30),
            homeChatNameSize=options.homeChatNameSize.coerceIn(10,30),
            homeChatPreviewSize=options.homeChatPreviewSize.coerceIn(12,18),
            accentColor=options.accentColor.takeIf {it in setOf("Cyan","Blue","Green","Purple","Rose","Amber","Custom")} ?: "Cyan",
            customAccentHex=normalizedAccentHex(options.customAccentHex) ?: "#00789D",
            notificationIconStyle=options.notificationIconStyle.takeIf {it in setOf("Classic","Outline")} ?: "Classic",
            bubbleOpacity=options.bubbleOpacity.coerceIn(45,100),
            outgoingInkHex=normalizedAccentHex(options.outgoingInkHex) ?: "#172D42",
            incomingInkHex=normalizedAccentHex(options.incomingInkHex) ?: "#172D42",
            quotedInkHex=normalizedAccentHex(options.quotedInkHex) ?: "#172D42",
            timestampInkHex=normalizedAccentHex(options.timestampInkHex) ?: "#536B7D",
            outgoingBubbleHex=normalizedAccentHex(options.outgoingBubbleHex) ?: "#D0EDFB",
            incomingBubbleHex=normalizedAccentHex(options.incomingBubbleHex) ?: "#FAFDFF",
            // Imported is a valid private on-device font family selected in Settings.
            // The renderer already falls back safely when no file is present.
            fontPreset=options.fontPreset.takeIf {it in setOf("System","Serif","Mono","Cursive","Imported")} ?: "System",
            homeWallpaperDim=options.homeWallpaperDim.coerceIn(0,100),
            chatWallpaperDim=options.chatWallpaperDim.coerceIn(0,100),
            quickReactions=MbQuickReactions.normalize(options.quickReactions),
            navigationOrder=MbNavigationOrder.normalize(options.navigationOrder),
            homeAvatarSize=options.homeAvatarSize.coerceIn(40,68),
            homeRowPadding=options.homeRowPadding.coerceIn(6,20),
            navBarStyle=options.navBarStyle.takeIf {it in setOf("Reference","Liquid")} ?: "Reference",
            emojiPickerSize=options.emojiPickerSize.coerceIn(18,32),
            linkColor=options.linkColor.takeIf {it in setOf("Theme","Blue","Green","Purple","Rose","Amber","Custom")} ?: "Theme",
            outgoingBubblePreset=options.outgoingBubblePreset.takeIf {it in setOf("Follow","Original","Green","Lavender","Rose","Night","Custom")} ?: "Follow",
            incomingBubblePreset=options.incomingBubblePreset.takeIf {it in setOf("Follow","Original","Green","Lavender","Rose","Night","Custom")} ?: "Follow",
            outgoingInk=options.outgoingInk.takeIf {it in setOf("Theme","Blue","Green","Purple","Rose","Amber","Custom")} ?: "Theme",
            incomingInk=options.incomingInk.takeIf {it in setOf("Theme","Blue","Green","Purple","Rose","Amber","Custom")} ?: "Theme",
            quotedInk=options.quotedInk.takeIf {it in setOf("Theme","Blue","Green","Purple","Rose","Amber","Custom")} ?: "Theme",
            timestampInk=options.timestampInk.takeIf {it in setOf("Theme","Blue","Green","Purple","Rose","Amber","Custom")} ?: "Theme",
            chatHeaderIconColor=options.chatHeaderIconColor.takeIf {it in setOf("Theme","Accent","Text")} ?: "Theme",
            chatHeaderNameSize=options.chatHeaderNameSize.coerceIn(12,24),
            photoQuality=options.photoQuality.takeIf {it in setOf("Standard","HD")} ?: "Standard",
            videoQuality=options.videoQuality.takeIf {it in setOf("Standard","HD")} ?: "HD",
            stickerSize=options.stickerSize.coerceIn(96,256),
            titleFontPreset=normalizedTitleFontPreset(options.titleFontPreset),
            appTextHex=normalizedAppColor(options.appTextHex),
            appBackgroundHex=normalizedAppColor(options.appBackgroundHex),
            appChromeHex=normalizedAppColor(options.appChromeHex),
            appUiScale=options.appUiScale.coerceIn(85,120),
            appTextScale=options.appTextScale.coerceIn(90,130),
            doubleTapReactionEmoji=normalizedDoubleTapReaction(options.doubleTapReactionEmoji)
        )
        displayPrefs.edit()
            .putString("appearance",safe.appearance)
            .putString("animation",safe.animation)
            .putBoolean("reducedMotion",safe.reducedMotion)
            .putBoolean("reducedTransparency",safe.reducedTransparency)
            .putInt("blurIntensity",safe.blurIntensity)
            .putString("bubbleStyle",safe.bubbleStyle)
            .putString("bubbleColor",safe.bubbleColor) 
            .putInt("messageFontSize",safe.messageFontSize)
            .putBoolean("showNavLabels",safe.showNavLabels)
            .putInt("navIconSize",safe.navIconSize)
            .putInt("navLabelSize",safe.navLabelSize)
            .putInt("headerTitleSize",safe.headerTitleSize)
            .putInt("homeChatNameSize",safe.homeChatNameSize)
            .putInt("homeChatPreviewSize",safe.homeChatPreviewSize)
            .putString("accentColor",safe.accentColor)
            .putBoolean("showProfileAsTitle",safe.showProfileAsTitle)
            .putBoolean("hideArchiveAccess",safe.hideArchiveAccess)
            .putString("fontPreset",safe.fontPreset)
            .putBoolean("hideOutgoingTyping",safe.hideOutgoingTyping)
            .putBoolean("preventScreenshots",safe.preventScreenshots)
            .putInt("homeWallpaperDim",safe.homeWallpaperDim)
            .putInt("chatWallpaperDim",safe.chatWallpaperDim)
            .putBoolean("hideHomePreviews",safe.hideHomePreviews)
            .putString("quickReactions",safe.quickReactions)
            .putString("navigationOrder",safe.navigationOrder)
            .putInt("homeAvatarSize",safe.homeAvatarSize)
            .putInt("homeRowPadding",safe.homeRowPadding)
            .putBoolean("hideHomeSearch",safe.hideHomeSearch)
            .putBoolean("showHomeDividers",safe.showHomeDividers)
            .putString("navBarStyle",safe.navBarStyle)
            .putBoolean("hidePhotoShortcut",safe.hidePhotoShortcut)
            .putBoolean("showPinBadges",safe.showPinBadges)
            .putInt("emojiPickerSize",safe.emojiPickerSize)
            .putString("linkColor",safe.linkColor)
            .putBoolean("hideNavIcons",safe.hideNavIcons)
            .putString("outgoingBubblePreset",safe.outgoingBubblePreset)
            .putString("incomingBubblePreset",safe.incomingBubblePreset)
            .putString("outgoingInk",safe.outgoingInk)
            .putString("incomingInk",safe.incomingInk)
            .putString("quotedInk",safe.quotedInk)
            .putString("timestampInk",safe.timestampInk)
            .putString("customAccentHex",safe.customAccentHex)
            .putString("outgoingBubbleHex",safe.outgoingBubbleHex)
            .putString("incomingBubbleHex",safe.incomingBubbleHex)
            .putBoolean("hideHomeFilters",safe.hideHomeFilters)
            .putBoolean("hideStoriesCarousel",safe.hideStoriesCarousel)
            .putBoolean("randomAccentOnLaunch",safe.randomAccentOnLaunch)
            .putString("notificationIconStyle",safe.notificationIconStyle)
            .putInt("bubbleOpacity",safe.bubbleOpacity)
            .putString("outgoingInkHex",safe.outgoingInkHex)
            .putString("incomingInkHex",safe.incomingInkHex)
            .putString("quotedInkHex",safe.quotedInkHex)
            .putString("timestampInkHex",safe.timestampInkHex)
            .putString("chatHeaderIconColor",safe.chatHeaderIconColor)
            .putInt("chatHeaderNameSize",safe.chatHeaderNameSize)
            .putBoolean("copySenderAndTime",safe.copySenderAndTime)
            .putString("photoQuality",safe.photoQuality)
            .putString("videoQuality",safe.videoQuality)
            .putBoolean("incognitoKeyboard",safe.incognitoKeyboard)
            .putString("titleFontPreset",safe.titleFontPreset)
            .putBoolean("customStickerSize",safe.customStickerSize)
            .putInt("stickerSize",safe.stickerSize)
            .putBoolean("doubleTapReactionEnabled",safe.doubleTapReactionEnabled)
            .putString("doubleTapReactionEmoji",safe.doubleTapReactionEmoji)
            .putBoolean("hideQuickReplySuggestions",safe.hideQuickReplySuggestions)
            .putBoolean("confirmSavedStickerSend",safe.confirmSavedStickerSend)
            .putBoolean("showEditedLabel",safe.showEditedLabel)
            .putBoolean("showEditedTime",safe.showEditedTime)
            .putBoolean("showLongPressPreview",safe.showLongPressPreview)
            .putBoolean("hideHomeFloatingButtons",safe.hideHomeFloatingButtons)
            .putString("appTextHex",safe.appTextHex)
            .putString("appBackgroundHex",safe.appBackgroundHex)
            .putString("appChromeHex",safe.appChromeHex)
            .putInt("appUiScale",safe.appUiScale)
            .putInt("appTextScale",safe.appTextScale)
            .putBoolean("showWallpaper",safe.showWallpaper).apply()
        return safe
    }
}
