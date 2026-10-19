package com.pritam.liquidchat.ui

import org.junit.Assert.*
import org.junit.Test

class MbQuickReactionsTest {
    @Test fun defaultsKeepEveryPreviouslyAvailableReaction() {
        assertEquals(listOf("👍","❤️","😂","😮","😢","🙏"),
            MbQuickReactions.selected(null))
        assertEquals(MbQuickReactions.defaultSetting,
            MbQuickReactions.normalize(null))
    }

    @Test fun reactSelectionCanBeChangedButNeverEmptyOrAboveSix() {
        var setting=MbQuickReactions.defaultSetting
        setting=MbQuickReactions.toggle(setting,"🙏")
        assertEquals(5,MbQuickReactions.selected(setting).size)
        setting=MbQuickReactions.toggle(setting,"👏")
        assertEquals("👏",MbQuickReactions.selected(setting).last())
        assertEquals(6,MbQuickReactions.selected(setting).size)
        assertEquals(setting,MbQuickReactions.toggle(setting,"🎉"))
        for(emoji in MbQuickReactions.selected(setting).take(5)) {
            setting=MbQuickReactions.toggle(setting,emoji)
        }
        assertEquals(1,MbQuickReactions.selected(setting).size)
        assertEquals(setting,
            MbQuickReactions.toggle(setting,MbQuickReactions.selected(setting).single()))
    }

    @Test fun importsRejectBadValuesAndDuplicates() {
        assertEquals(MbQuickReactions.defaultSetting,
            MbQuickReactions.normalize("evil,invalid"))
        assertEquals("👍,😂",MbQuickReactions.normalize("👍,👍,😂"))
        assertEquals(MbQuickReactions.defaultSetting,
            MbQuickReactions.normalize("👍,".repeat(128)))
        assertEquals("👍",MbQuickReactions.toggle("👍","unknown"))
        assertEquals(MbQuickReactions.defaults,MbQuickReactions.available.take(6))
        assertEquals(listOf("👍","❤️","😂","😮","😢","🙏","👏"),MbQuickReactions.available)
        assertFalse("🔥" in MbQuickReactions.available)
    }

    @Test fun hidingInboxSnippetsNeverEchoesMessageText() {
        assertEquals("Message preview hidden",
            inboxSnippet("Personal secret message","fallback",true))
        assertEquals("Message preview hidden",
            inboxSnippet("","Say hello",true))
        assertEquals("Personal secret message",
            inboxSnippet("Personal secret message","fallback",false))
        assertEquals("Say hello",inboxSnippet("","Say hello",false))
    }

    @Test fun homePreviewPrivacyDefaultsOff() {
        assertFalse(DisplayOptions().hideHomePreviews)
        assertTrue(DisplayOptions(hideHomePreviews=true).hideHomePreviews)
    }
}
