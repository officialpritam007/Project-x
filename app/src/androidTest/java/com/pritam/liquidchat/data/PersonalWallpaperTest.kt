package com.pritam.liquidchat.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PersonalWallpaperTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private val accountA=UUID.randomUUID().toString()
    private val accountB=UUID.randomUUID().toString()
    private val cid=UUID.randomUUID().toString()

    @Test fun privatePhotoIsCompressedStoredAndIsolated()=runBlocking {
        val image=File(context.cacheDir,"wallpaper-fixture-${UUID.randomUUID()}.png")
        Bitmap.createBitmap(24,24,Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.CYAN)
            image.outputStream().use {compress(Bitmap.CompressFormat.PNG,100,it)}
            recycle()
        }
        try {
            assertFalse(PersonalWallpaper.has(context,accountA,cid))
            val stored=PersonalWallpaper.save(context,accountA,cid,Uri.fromFile(image))
            assertTrue(stored.isFile)
            assertTrue(stored.length()>0)
            assertEquals("jpg",stored.extension)
            assertFalse(PersonalWallpaper.has(context,accountB,cid))
            assertEquals(stored,PersonalWallpaper.location(context,accountA,cid))
            val home=PersonalWallpaper.save(context,accountA,PersonalWallpaper.HOME_CID,Uri.fromFile(image))
            assertNotEquals(home,stored)
            assertTrue(PersonalWallpaper.has(context,accountA,PersonalWallpaper.HOME_CID))
            assertFalse(PersonalWallpaper.has(context,accountB,PersonalWallpaper.HOME_CID))
        } finally {
            PersonalWallpaper.clearAccount(context,accountA)
            PersonalWallpaper.clearAccount(context,accountB)
            image.delete()
        }
        assertFalse(PersonalWallpaper.has(context,accountA,cid))
    }

    @Test fun pathsCannotEscapeAccountDirectory() {
        assertTrue(runCatching {
            PersonalWallpaper.location(context,"../../escape",cid)
        }.isFailure)
        assertTrue(runCatching {
            PersonalWallpaper.location(context,accountA,"../outside")
        }.isFailure)
    }
}
