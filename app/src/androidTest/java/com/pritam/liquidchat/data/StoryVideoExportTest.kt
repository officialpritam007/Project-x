package com.pritam.liquidchat.data

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Real local AVC fixture and Transformer export; no live accounts, network or gallery data. */
class StoryVideoExportTest {
    private fun fixture(file:File) {
        val encoder=MediaCodec.createEncoderByType("video/avc")
        var muxer:MediaMuxer?=null
        var started=false
        try {
            encoder.configure(MediaFormat.createVideoFormat("video/avc",320,180).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                setInteger(MediaFormat.KEY_BIT_RATE,300_000)
                setInteger(MediaFormat.KEY_FRAME_RATE,30)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,1)
            },null,null,MediaCodec.CONFIGURE_FLAG_ENCODE)
            muxer=MediaMuxer(file.absolutePath,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            encoder.start();started=true
            val frame=ByteArray(320*180*3/2) {if(it<320*180)80.toByte() else 128.toByte()}
            val info=MediaCodec.BufferInfo()
            var input=0
            var track=-1
            var finished=false
            val deadline=android.os.SystemClock.elapsedRealtime()+20_000L
            while(!finished&&android.os.SystemClock.elapsedRealtime()<deadline) {
                if(input<=30) {
                    val index=encoder.dequeueInputBuffer(10_000)
                    if(index>=0) {
                        val eos=input==30
                        if(!eos)encoder.getInputBuffer(index)!!.apply {clear();put(frame)}
                        encoder.queueInputBuffer(index,0,if(eos)0 else frame.size,input*1_000_000L/30,
                            if(eos)MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0)
                        input++
                    }
                }
                val output=encoder.dequeueOutputBuffer(info,10_000)
                if(output==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    track=muxer.addTrack(encoder.outputFormat);muxer.start()
                } else if(output>=0) {
                    if(info.size>0&&info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG==0) {
                        check(track>=0)
                        muxer.writeSampleData(track,encoder.getOutputBuffer(output)!!,info)
                    }
                    finished=info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM!=0
                    encoder.releaseOutputBuffer(output,false)
                }
            }
            check(finished&&track>=0) {"Synthetic AVC encoder timed out"}
            muxer.stop()
        } finally {
            if(started)runCatching {encoder.stop()}
            encoder.release()
            muxer?.release()
        }
    }

    @Test fun exportCreatesPlayableBoundedMp4FromActualLocalVideo() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val input=File.createTempFile("story-fixture-",".mp4",context.cacheDir)
        var output:File?=null
        try {
            fixture(input)
            output=StoryVideo.export(context,Uri.fromFile(input),0)
            assertTrue(output.length() in 1..StoryVideo.MAX_BYTES)
            val metadata=MediaMetadataRetriever()
            try {
                metadata.setDataSource(output.absolutePath)
                val width=metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)!!.toInt()
                val height=metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)!!.toInt()
                val duration=metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong()
                assertTrue(minOf(width,height)<=720&&maxOf(width,height)<=1280)
                assertTrue(duration in 500..StoryVideo.MAX_DURATION_MS)
                val frame=metadata.getFrameAtTime(100_000)
                assertNotNull(frame)
                frame?.recycle()
            } finally {metadata.release()}
        } finally {input.delete();output?.delete()}
    }
}
