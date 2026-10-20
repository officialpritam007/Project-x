package com.pritam.liquidchat.data

/**
 * Device-local per-chat redaction can only be more private than the server's
 * global notification preview setting. Never persist the raw body in a
 * notification's expanded text when redaction is active.
 */
internal fun notificationBodyForDisplay(body:String,serverPreviews:Boolean,hideForChat:Boolean):String =
    if(serverPreviews && !hideForChat)body else "New message"

internal fun notificationBodyAllowed(serverPreviews:Boolean,hideForChat:Boolean):Boolean =
    serverPreviews && !hideForChat
