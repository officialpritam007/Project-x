package com.pritam.liquidchat.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** Device-owned sticker metadata never crosses the signed-in account boundary. */
@RunWith(AndroidJUnit4::class)
class StickerShelfMetadataTest {
    private val context:Context=ApplicationProvider.getApplicationContext()

    private fun sticker(account:String):File {
        val folder=File(context.filesDir,"saved_stickers/$account").apply {mkdirs()}
        return File(folder,"${UUID.randomUUID()}.png").apply {
            writeBytes(ByteArray(200) {0})
        }
    }

    @Test fun favoritesArePrivateAndDoNotLeakAfterRemovalOrSignOut() {
        val owner=UUID.randomUUID().toString()
        val other=UUID.randomUUID().toString()
        try {
            val first=sticker(owner)
            sticker(other)
            StickerLibrary.setFavorite(context,owner,first,true)
            assertEquals(setOf(first.name),StickerLibrary.favoriteNames(context,owner))
            assertTrue(StickerLibrary.favoriteNames(context,other).isEmpty())

            StickerLibrary.markUsed(context,owner,first)
            assertEquals(listOf(first.name),StickerLibrary.recent(context,owner).map {it.name})
            assertTrue(StickerLibrary.recent(context,other).isEmpty())

            StickerLibrary.remove(context,owner,first)
            assertTrue(StickerLibrary.favoriteNames(context,owner).isEmpty())
            assertTrue(StickerLibrary.recent(context,owner).isEmpty())
        } finally {
            StickerLibrary.clearAccount(context,owner)
            StickerLibrary.clearAccount(context,other)
        }
    }

    @Test fun recentStickerOrderReflectsActualSendAndFavoritesSurviveListing() {
        val account=UUID.randomUUID().toString()
        try {
            val first=sticker(account)
            val second=sticker(account)
            assertTrue(StickerLibrary.recent(context,account).isEmpty())
            StickerLibrary.setFavorite(context,account,first,true)
            StickerLibrary.markUsed(context,account,first)
            StickerLibrary.markUsed(context,account,second)
            StickerLibrary.markUsed(context,account,first)
            assertEquals(listOf(first.name,second.name),
                StickerLibrary.recent(context,account).map {it.name})
            assertEquals(setOf(first.name),StickerLibrary.favoriteNames(context,account))
            StickerLibrary.setFavorite(context,account,first,false)
            assertTrue(StickerLibrary.favoriteNames(context,account).isEmpty())
        } finally {
            StickerLibrary.clearAccount(context,account)
        }
    }
}
