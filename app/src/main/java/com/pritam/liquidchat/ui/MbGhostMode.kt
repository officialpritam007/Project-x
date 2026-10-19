package com.pritam.liquidchat.ui

/**
 * A convenience control for existing Supabase account privacy flags.
 * Receipts and presence are enforced by the server, not a client-only illusion.
 */
internal fun ghostModeActive(showPresence:Boolean,readReceipts:Boolean):Boolean =
    !showPresence && !readReceipts

/** The toggle controls both switches intentionally; notifications are unchanged. */
internal fun ghostModeSettings(enabled:Boolean):Pair<Boolean,Boolean> =
    if(enabled) false to false else true to true
