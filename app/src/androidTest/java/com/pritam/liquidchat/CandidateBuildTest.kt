package com.pritam.liquidchat

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeNotNull
import org.junit.Test

/** Assert the actual installed candidate's flag, rather than trusting workflow input. */
class CandidateBuildTest {
    @Test fun installedCandidateHasRequestedCallingConfiguration() {
        val expected = InstrumentationRegistry.getArguments().getString("expectedExperimentalCalls")
        assumeNotNull(expected)
        // Reflection reads the installed application's value; a Java const reference
        // would be inlined into this separately packaged test APK by the compiler.
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        val installedFlag = target.classLoader.loadClass("com.pritam.liquidchat.BuildConfig")
            .getField("ENABLE_EXPERIMENTAL_CALLS").getBoolean(null)
        assertEquals("Installed candidate calling configuration differs from CI", expected!!.toBoolean(), installedFlag)
    }
}
