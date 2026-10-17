package com.pritam.liquidchat.data

import android.Manifest
import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class VoiceRecorderTest {
    @get:Rule val foreground=androidx.test.ext.junit.rules.ActivityScenarioRule(androidx.activity.ComponentActivity::class.java)
    @get:Rule val microphone:GrantPermissionRule=GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)
    @Test fun recordsRealAacAndCancelledCaptureLeavesNoFile() {
        val context=ApplicationProvider.getApplicationContext<Context>();val owner="instrumentation-voice"
        val directory=File(context.filesDir,"media/$owner");directory.deleteRecursively()
        val capture=VoiceRecorder(context,owner)
        try {
            capture.start(true);Thread.sleep(900);val finished=capture.finish()
            assertNotNull(finished)
            val (file,duration)=finished!!
            assertTrue(duration>=500);assertTrue(file.length()>12)
            val reader=MediaMetadataRetriever()
            try {reader.setDataSource(file.absolutePath);assertTrue(reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toInt()>0)}finally {reader.release()}
            file.delete()
            capture.start();Thread.sleep(600);capture.cancel()
            assertFalse(capture.recording);assertTrue(directory.listFiles().orEmpty().isEmpty())
        } finally {capture.cancel();directory.deleteRecursively()}
    }
}
