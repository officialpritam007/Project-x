package com.pritam.liquidchat.ui

import java.net.URI

internal data class MessageWebLink(val start:Int,val end:Int,val url:String)

private val explicitWebUrl=Regex("https?://[^\\s<>\"\\p{Cntrl}]+",RegexOption.IGNORE_CASE)

/** Explicit web URLs only. Parsing never resolves DNS or downloads a preview. */
internal fun messageWebLinks(text:String):List<MessageWebLink> =
    explicitWebUrl.findAll(text).take(32).mapNotNull {match->
        var url=match.value.trimEnd('.',',',';',':','!','?','\u3002','\u2019','\u201d')
        for((open,close) in listOf('(' to ')','[' to ']','{' to '}')) {
            while(url.endsWith(close)&&url.count {it==close}>url.count {it==open})url=url.dropLast(1)
        }
        if(url.length>2048)return@mapNotNull null
        val parsed=runCatching {URI(url)}.getOrNull() ?: return@mapNotNull null
        if(parsed.scheme.lowercase() !in setOf("http","https") || parsed.host.isNullOrBlank() ||
            parsed.rawUserInfo!=null || parsed.port !in -1..65535)return@mapNotNull null
        MessageWebLink(match.range.first,match.range.first+url.length,url)
    }.toList()

/** Android IntentFilter scheme matching is case-sensitive; the message stays verbatim. */
internal fun androidBrowserUrl(link:MessageWebLink):String {
    val separator=link.url.indexOf(':')
    if(separator<=0)return link.url
    return link.url.substring(0,separator).lowercase(java.util.Locale.ROOT)+link.url.substring(separator)
}
