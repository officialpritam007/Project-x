package com.pritam.liquidchat.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class MbDisplayOptionsTest {
    @Test fun oldPreferencesAndConstructorsKeepExistingBlur() {
        val options=DisplayOptions()
        assertEquals(55,options.blurIntensity)
        assertEquals(0,options.copy(blurIntensity=0).blurIntensity)
        assertEquals(100,options.copy(blurIntensity=100).blurIntensity)
        assertEquals("Dark",DisplayOptions(appearance="Dark").appearance)
    }
}
