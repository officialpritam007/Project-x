package com.pritam.liquidchat.ui

import com.pritam.liquidchat.data.Message
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId

class MbConversationInteractionsTest {
    private fun message()=Message("owner","id","chat","peer","Hello",seq=1,status="SENT")
    @Test fun doubleTapRequiresAuthorizedUndeletedSentItem() {
        val sent=message()
        assertTrue(canDoubleTapReact(sent,true,false,"owner"))
        assertFalse(canDoubleTapReact(sent,false,false,"owner"))
        assertFalse(canDoubleTapReact(sent,true,true,"owner"))
        assertFalse(canDoubleTapReact(sent,true,false,null))
        assertFalse(canDoubleTapReact(sent,true,false,"another"))
        assertFalse(canDoubleTapReact(sent.copy(status="QUEUED"),true,false,"owner"))
        assertFalse(canDoubleTapReact(sent.copy(status="FAILED"),true,false,"owner"))
        assertFalse(canDoubleTapReact(sent.copy(deletedAt="deleted"),true,false,"owner"))
    }
    @Test fun emojiMustBeBackendAccepted() {
        MbQuickReactions.available.forEach {assertEquals(it,normalizedDoubleTapReaction(it))}
        assertEquals("❤️",normalizedDoubleTapReaction(null))
        assertEquals("❤️",normalizedDoubleTapReaction("unsupported"))
    }
    @Test fun editLabelsNeverInventDate() {
        val edited="2026-10-21T12:34:56Z"
        assertNull(editedMessageMarker(edited,false,true))
        assertNull(editedMessageMarker("",true,true))
        assertEquals("edited",editedMessageMarker(edited,true,false))
        assertEquals("edited",editedMessageMarker("invalid",true,true))
        assertTrue(editedMessageMarker(edited,true,true,ZoneId.of("Asia/Kolkata"))!!.endsWith(", 18:04"))
    }
    @Test fun appearanceResetPreservesPrivacyAndSendBehavior() {
        val original=DisplayOptions(doubleTapReactionEnabled=true,
            doubleTapReactionEmoji="😂",hideQuickReplySuggestions=true,
            confirmSavedStickerSend=false,incognitoKeyboard=true,
            showEditedLabel=false,showEditedTime=true)
        val updated=MbThemeTransfer.resetAppearance(original)
        assertTrue(updated.doubleTapReactionEnabled)
        assertEquals("😂",updated.doubleTapReactionEmoji)
        assertTrue(updated.hideQuickReplySuggestions)
        assertFalse(updated.confirmSavedStickerSend)
        assertTrue(updated.incognitoKeyboard)
        assertTrue(updated.showEditedLabel)
        assertFalse(updated.showEditedTime)
    }
}
