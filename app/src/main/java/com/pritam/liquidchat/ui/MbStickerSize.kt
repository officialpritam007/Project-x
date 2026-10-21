package com.pritam.liquidchat.ui

/** Presentation only; the authenticated media bytes and privacy are unchanged. */
internal fun conversationStickerSize(options:DisplayOptions):Int =
    if(options.customStickerSize)options.stickerSize.coerceIn(96,256) else 192
