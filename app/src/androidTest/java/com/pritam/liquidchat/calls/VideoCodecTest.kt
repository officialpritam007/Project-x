package com.pritam.liquidchat.calls

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.webrtc.EglBase
import org.webrtc.MediaStreamTrack

/** Runs against native WebRTC on each emulator; no camera, mic or user account needed. */
class VideoCodecTest {
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
