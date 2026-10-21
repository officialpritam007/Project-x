package com.pritam.liquidchat.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** The new application is always Liquid Chat, even on Android's App Info page. */
@RunWith(AndroidJUnit4::class)
class LiquidChatBrandingTest {
    @Test fun launcherLabelPackageAndVersionNeverExposeOldProductName() {
        val context=ApplicationProvider.getApplicationContext<Application>()
        val label=context.applicationInfo.loadLabel(context.packageManager).toString()
        val pkg=context.packageManager.getPackageInfo(context.packageName,0)
        val version=pkg.versionName.orEmpty()
        assertEquals("Liquid Chat",label)
        assertEquals("com.pritam.liquidchat",context.packageName)
        assertTrue(version.startsWith("1.0.0-rc11-liquid"))
        assertFalse(version.contains("mbwhatsapp",ignoreCase=true))
        assertFalse(version.contains("-mb-",ignoreCase=true))
    }
}
