package com.pritam.liquidchat

import android.view.WindowManager
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenCaptureWindowFlagTest {
    @Test fun securityFlagIsEnabledOnlyWhenExplicitlyRequested() {
        assertEquals(0,screenCaptureWindowFlag(false))
        assertEquals(WindowManager.LayoutParams.FLAG_SECURE,screenCaptureWindowFlag(true))
    }
}
