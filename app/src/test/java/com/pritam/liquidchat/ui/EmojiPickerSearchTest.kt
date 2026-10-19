package com.pritam.liquidchat.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmojiPickerSearchTest {
    @Test fun emptySearchRespectsActiveCategory() {
        assertEquals(emojiCategories.getValue("Faces"),findEmojis("Faces",""))
        assertEquals(emojiCategories.getValue("More"),findEmojis("More","  "))
    }
    @Test fun keywordSearchScansEveryCategoryWithoutNetwork() {
        assertTrue(findEmojis("Faces","football").contains("⚽"))
        assertTrue(findEmojis("More","heart").contains("❤️"))
        assertTrue(findEmojis("Faces","CAT").contains("🐱"))
        assertFalse(findEmojis("Faces","football").contains("😀"))
        assertEquals(emptyList<String>(),findEmojis("Faces","not-a-real-emoji-term"))
    }
    @Test fun recentEmojisAreUniqueAndBounded() {
        val starting=listOf("❤️","👍","⚽","🔥","🎉")
        assertEquals(listOf("⚽","❤️","👍","🔥","🎉"),recentEmojiChoices(starting,"⚽"))
        val added=recentEmojiChoices(starting,"😀")
        assertEquals(listOf("😀","❤️","👍","⚽","🔥"),added)
        assertEquals(5,added.distinct().size)
    }
}
