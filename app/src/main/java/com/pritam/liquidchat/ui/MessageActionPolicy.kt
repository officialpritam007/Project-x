package com.pritam.liquidchat.ui

import com.pritam.liquidchat.data.Message

/** Never offer text forwarding for deleted, failed, or media-only messages. */
internal fun canForwardTextMessage(message:Message,account:String?):Boolean =
    account!=null && message.account==account && message.status=="SENT" &&
    message.deletedAt.isEmpty() && message.mediaId.isEmpty() &&
    message.localImage.isEmpty() && message.mediaKind=="image" && message.body.isNotBlank()

/** Unknown server state must never produce contradictory block/unblock actions. */
internal fun blockActionLabel(ownedBlocked:Boolean?):String =
    when(ownedBlocked) { true->"Unblock contact"; false->"Block contact"; null->"Checking block status…" }

internal enum class SettingsPanel { APPEARANCE, POWER_SAVING, OTHER }
internal fun settingsPanelFor(section:String?):SettingsPanel = when(section) {
    "Chat Settings","Liquid Chat General Settings"->SettingsPanel.APPEARANCE
    "Power Saving"->SettingsPanel.POWER_SAVING
    else->SettingsPanel.OTHER
}

/** Display category order for the one-to-one message action sheet. */
internal fun messageActionCategories(message:Message):List<String> {
    val result=mutableListOf<String>()
    if(message.deletedAt.isEmpty()) {
        if(message.status=="SENT")result+="Quick reactions"
        result+="Message actions"
    }
    if((message.status=="SENT" && message.sender!=message.account && message.deletedAt.isEmpty()) ||
        message.status in listOf("SENT","QUEUED","FAILED"))result+="Safety"
    return result
}

internal fun toggleTextSelection(ids:Set<String>,id:String):Set<String> =
    if(id in ids) ids-id else if(ids.size<30) ids+id else ids

