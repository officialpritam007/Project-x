package com.pritam.liquidchat.ui

import org.junit.Assert.*
import org.junit.Test

class MessageWebLinksTest {
    @Test fun unicodeOffsetsMultipleLinksAndSentencePunctuationKeepExactText() {
        val text="দেখো 🙂 (https://example.org/a_(b)). More HTTP://example.net?q=1!"
        val links=messageWebLinks(text)
        assertEquals(listOf("https://example.org/a_(b)","HTTP://example.net?q=1"),links.map {it.url})
        links.forEach {assertEquals(it.url,text.substring(it.start,it.end))}
    }

    @Test fun customSchemesCredentialsMalformedAuthoritiesAndPortsStayPlain() {
        listOf("javascript:alert(1)","intent://example.org/#Intent;end", "file:///secret",
            "https://user:password@example.org/a", "https:///missing-host", "https://example.org:99999/a",
            "https://example.org/%xx").forEach {text->
            assertTrue(text,messageWebLinks(text).isEmpty())
        }
    }

    @Test fun ipv6QueryFragmentBalancedPathAndPercentEncodingArePreserved() {
        val url="https://[::1]:8080/a_(b)?q=hello%20world#part"
        assertEquals(url,messageWebLinks("[$url]").single().url)
    }

    @Test fun browserHandoffLowercasesOnlySchemeAndKeepsVisibleTextUnmodified() {
        val message="দেখো HtTpS://example.invalid/KeepCase?Token=AbC#Frag"
        val link=messageWebLinks(message).single()
        assertEquals("HtTpS://example.invalid/KeepCase?Token=AbC#Frag",link.url)
        assertEquals(link.url,message.substring(link.start,link.end))
        assertEquals("https://example.invalid/KeepCase?Token=AbC#Frag",androidBrowserUrl(link))
        val http=messageWebLinks("HTTP://example.org/MixedPath").single()
        assertEquals("http://example.org/MixedPath",androidBrowserUrl(http))
    }

    @Test fun multilingualSentenceTerminatorsAreNotIncludedInBrowserLinks() {
        val cases=listOf(
            "https://example.org/hello।" to "https://example.org/hello",
            "https://example.org/hello॥" to "https://example.org/hello",
            "https://example.org/hello…" to "https://example.org/hello",
            "https://example.org/hello！" to "https://example.org/hello",
            "https://example.org/hello？" to "https://example.org/hello"
        )
        cases.forEach { (text,expected) ->
            val link=messageWebLinks(text).single()
            assertEquals(expected,link.url)
            assertEquals(expected,text.substring(link.start,link.end))
        }
    }

    @Test fun oversizedAndExcessiveLinksAreBounded() {
        assertTrue(messageWebLinks("https://example.org/"+"a".repeat(2048)).isEmpty())
        assertEquals(32,messageWebLinks(List(100){"https://example.org/$it"}.joinToString(" ")).size)
    }
}
