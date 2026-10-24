package com.pritam.liquidchat.calls

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.webrtc.EglBase
import org.webrtc.MediaStreamTrack

/** Runs against native WebRTC on each emulator; no camera, mic or user account needed. */
class VideoCodecTest {
    @org.junit.Test fun signalingAfterTeardownFailsPromptlyRatherThanSuspendingForever()=kotlinx.coroutines.runBlocking {
        val context=androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        // The production call engine is created from the UI thread. Android 29's
        // PhoneStateListener constructor requires a prepared Looper on that thread.
        lateinit var engine: CallEngine
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
            engine=CallEngine(context,false,{},{})
        }
        engine.close()
        engine.close()
        // Late ICE/control callbacks must be ignored before parsing or touching JNI.
        engine.remoteCandidate(org.json.JSONObject())
        engine.mute(true)
        engine.setCameraEnabled(false)
        engine.switchCamera()
        val actions:List<suspend ()->Unit> = listOf(
            {engine.createOffer();Unit},
            {engine.answerOffer("late-offer");Unit},
            {engine.acceptAnswer("late-answer")}
        )
        for(action in actions) {
            var rejected=false
            try {kotlinx.coroutines.withTimeout(1_000L) {action()}}
            catch(_:java.io.IOException) {rejected=true}
            org.junit.Assert.assertTrue("Closed peer must fail without waiting for a callback",rejected)
        }
    }
    @Test fun productionFactoryAdvertisesBidirectionalVideoAndAudio() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val egl=EglBase.create()
        val factory=CallEngine.createFactory(context,egl.eglBaseContext)
        try {
            val video=MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO
            val send=factory.getRtpSenderCapabilities(video).codecs.map {it.name.uppercase()}
            val receive=factory.getRtpReceiverCapabilities(video).codecs.map {it.name.uppercase()}
            // VP8 software fallback keeps video negotiable without a hardware codec.
            assertTrue("No VP8 video sender: $send", "VP8" in send)
            assertTrue("No VP8 video receiver: $receive", "VP8" in receive)
            val audio=MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO
            assertTrue(factory.getRtpSenderCapabilities(audio).codecs.isNotEmpty())
            assertTrue(factory.getRtpReceiverCapabilities(audio).codecs.isNotEmpty())
        } finally {
            factory.dispose()
            egl.release()
        }
    }
}
