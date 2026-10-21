package com.pritam.liquidchat.ui

/**
 * Local, deterministic draft transformations. This is deliberately NOT an
 * AI/LLM client: no chat text leaves the device and no network key is needed.
 * The user sees the result before opting to replace their unsent draft.
 */
internal enum class OfflineTextAction(val label:String) {
    SENTENCE("Capitalize first letter"),
    CLEAN_SPACES("Clean extra spaces"),
    UPPER("UPPERCASE"),
    LOWER("lowercase")
}

internal fun transformDraftOffline(text:String,action:OfflineTextAction):String = when(action) {
    OfflineTextAction.SENTENCE -> {
        val first=text.indexOfFirst {it.isLetter()}
        if(first<0) text else text.replaceRange(first,first+1,
            text.substring(first,first+1).uppercase())
    }
    OfflineTextAction.CLEAN_SPACES -> text.lineSequence()
        .map {line->line.trim().replace(Regex("[ \\t]{2,}")," ")}
        .joinToString("\n")
    OfflineTextAction.UPPER -> text.uppercase()
    OfflineTextAction.LOWER -> text.lowercase()
}.take(4000)
