package com.pritam.liquidchat.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

data class PreparedChatVideo(
    val file:File,
    val durationMs:Int,
    val width:Int,
    val height:Int
)

/**
 * Reuses the existing on-device StoryVideo transcoder, but moves the resulting
 * bounded MP4 into the signed-in account's private durable outbox directory.
 * No upload to third-party editors, no public URLs, no storage permission.
 */
object ChatVideo {
    const val MAX_BYTES=8_388_608L

    suspend fun prepare(
        context:Context,
        source:Uri,
        account:String,
        startMs:Long=0L,
        onProgress:(Int)->Unit={}
    ):PreparedChatVideo {
        require(account.matches(Regex("[a-fA-F0-9-]{36}")))
        val exported=StoryVideo.export(context,source,startMs,onProgress)
        try {
            return withContext(Dispatchers.IO) {
                val directory=File(context.filesDir,"media/$account").canonicalFile
                if(!directory.isDirectory&&!directory.mkdirs())throw IOException("Unable to save video.")
                val output=File(directory,"${UUID.randomUUID()}.mp4")
                try {
                    if(exported.length() !in 100..MAX_BYTES)
                        throw IOException("Video must be 8 MB or less.")
                    exported.inputStream().use {input->
                        output.outputStream().use {input.copyTo(it,64*1024)}
                    }
                    if(output.length() != exported.length())
                        throw IOException("Video copy was incomplete.")
                    val retriever=MediaMetadataRetriever()
                    val duration:Int
                    val width:Int
                    val height:Int
                    try {
                        retriever.setDataSource(output.absolutePath)
                        duration=retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_DURATION)?.toIntOrNull()
                            ?: throw IOException("Video duration missing.")
                        width=retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
                            ?: throw IOException("Video width missing.")
                        height=retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
                            ?: throw IOException("Video height missing.")
                    } finally {retriever.release()}
                    if(duration !in 500..30_000 || width<1 || height<1 ||
                        minOf(width,height)>720 || maxOf(width,height)>1280)
                        throw IOException("Please choose a 720p video up to 30 seconds.")
                    PreparedChatVideo(output,duration,width,height)
                } catch(error:Exception) {
                    output.delete()
                    throw error
                }
            }
        } finally {exported.delete()}
    }
}
