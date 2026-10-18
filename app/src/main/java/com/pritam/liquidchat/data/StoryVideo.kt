package com.pritam.liquidchat.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Bounded, local-only video story processing. No credentials or cloud encoding service. */
object StoryVideo {
    const val MAX_DURATION_MS=30_000L
    const val MAX_BYTES=8L*1024*1024

    fun durationMs(context:Context,uri:Uri):Long {
        val metadata=MediaMetadataRetriever()
        try {
            metadata.setDataSource(context,uri)
            return metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.takeIf {it>0L} ?: throw IOException("Video duration unavailable.")
        } finally {metadata.release()}
    }

    /**
     * Transcodes the selected <=30 s segment to 720p H264/AAC MP4 with a low requested
     * bitrate, then checks the exact output size. The Edge Function independently
     * checks real MP4 atoms, codec, resolution, duration, and limits before storage.
     */
    @OptIn(UnstableApi::class)
    suspend fun export(
        context:Context,source:Uri,startMs:Long,onProgress:(Int)->Unit={}
    ):File=withContext(Dispatchers.Main) {
        val duration=durationMs(context,source)
        val start=startMs.coerceIn(0L,(duration-500L).coerceAtLeast(0L))
        val end=(start+MAX_DURATION_MS).coerceAtMost(duration)
        if(end-start<500L)throw IOException("Video must be at least half a second.")
        val file=File(context.cacheDir,"story-"+UUID.randomUUID()+".mp4")
        try {
            suspendCancellableCoroutine { continuation->
                val clip=MediaItem.Builder().setUri(source)
                    .setClippingConfiguration(MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(start)
                        .setEndPositionMs(end)
                        .build())
                    .build()
                val edited=EditedMediaItem.Builder(clip)
                    .setEffects(Effects(emptyList(),listOf(Presentation.createForHeight(720))))
                    .build()
                val encoder=DefaultEncoderFactory.Builder(context)
                    .setRequestedVideoEncoderSettings(VideoEncoderSettings.Builder()
                        .setBitrate(1_450_000).build())
                    .build()
                val transformer=Transformer.Builder(context)
                    .setVideoMimeType(MimeTypes.VIDEO_H264)
                    .setAudioMimeType(MimeTypes.AUDIO_AAC)
                    .setEncoderFactory(encoder)
                    .addListener(object:Transformer.Listener {
                        override fun onCompleted(composition:Composition,exportResult:ExportResult) {
                            if(continuation.isActive)continuation.resume(file)
                        }
                        override fun onError(
                            composition:Composition,exportResult:ExportResult,exportException:ExportException
                        ) {
                            if(continuation.isActive)continuation.resumeWithException(
                                IOException("Video export failed on this device.",exportException))
                        }
                    }).build()
                val progress=androidx.media3.transformer.ProgressHolder()
                val polling=CoroutineScope(Dispatchers.Main).launch {
                    while(isActive&&continuation.isActive) {
                        if(transformer.getProgress(progress)==Transformer.PROGRESS_STATE_AVAILABLE)
                            onProgress(progress.progress.coerceIn(0,100))
                        delay(180)
                    }
                }
                continuation.invokeOnCancellation {
                    polling.cancel()
                    transformer.cancel()
                    file.delete()
                }
                try {transformer.start(edited,file.absolutePath)}
                catch(error:Exception) {
                    polling.cancel()
                    if(continuation.isActive)continuation.resumeWithException(error)
                }
            }
            if(file.length()<=0||file.length()>MAX_BYTES)
                throw IOException("Compressed video exceeds 8 MB. Select a shorter clip.")
            onProgress(100)
            file
        } catch(e:CancellationException) {
            file.delete()
            throw e
        } catch(e:Exception) {
            file.delete()
            throw e
        }
    }
}
