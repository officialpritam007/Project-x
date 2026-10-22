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

    /** Save only a sent, nonhidden, currently authorized video. Never expose
     *  the signed URL as a permanent share link or background download.
     */
    suspend fun saveReceived(
        context:Context,repository:ChatRepository,message:Message,destination:Uri
    )=withContext(Dispatchers.IO) {
        val backend=repository.backend
        val account=backend.session.value?.userId ?: throw IOException("Sign in required.")
        val epoch=backend.uiReads.epoch(account)
        require(message.account==account && message.mediaKind=="chat_video" &&
            message.status=="SENT" && message.mediaId.isNotBlank() &&
            message.deletedAt.isEmpty()) {"Video is not available."}
        suspend fun authorized():Boolean {
            if(backend.session.value?.userId!=account ||
                backend.uiReads.epoch(account)!=epoch)return false
            val current=repository.dao.message(account,message.id) ?: return false
            return current.mediaKind=="chat_video" &&
                current.mediaId==message.mediaId &&
                current.status=="SENT" && current.deletedAt.isEmpty() &&
                !repository.dao.hidden(account,message.id)
        }
        check(authorized()) {"This video is no longer visible."}
        val url=backend.imageUrl(account,message.mediaId,force=true)
        require(url.startsWith("https://",ignoreCase=true)) {"Invalid private video URL."}
        val conn=java.net.URL(url).openConnection() as java.net.HttpURLConnection
        val bytes=try {
            conn.connectTimeout=15_000
            conn.readTimeout=45_000
            conn.useCaches=false
            conn.instanceFollowRedirects=false
            conn.inputStream.use {input->
                val output=java.io.ByteArrayOutputStream()
                val buffer=ByteArray(8192)
                var count=0
                while(true) {
                    val n=input.read(buffer)
                    if(n<0)break
                    count+=n
                    if(count>MAX_BYTES)throw IOException("Chat video exceeds 8 MB.")
                    output.write(buffer,0,n)
                }
                output.toByteArray()
            }
        } finally {conn.disconnect()}
        require(bytes.size>=100 &&
            bytes[4]=='f'.code.toByte() && bytes[5]=='t'.code.toByte() &&
            bytes[6]=='y'.code.toByte() && bytes[7]=='p'.code.toByte()) {
            "Invalid received MP4 video."
        }
        check(authorized()) {"Video access changed."}
        // Fetch new authorization after transfer; a deleted or blocked video
        // cannot be saved from a previously valid signed URL.
        backend.imageUrl(account,message.mediaId,force=true)
        check(authorized()) {"Video access changed."}
        context.contentResolver.openOutputStream(destination,"w")?.use {output->
            output.write(bytes)
            output.flush()
        } ?: throw IOException("Could not write the chosen video.")
    }

    suspend fun prepare(
        context:Context,
        source:Uri,
        account:String,
        startMs:Long=0L,
        onProgress:(Int)->Unit={},
        quality:String="HD"
    ):PreparedChatVideo {
        require(account.matches(Regex("[a-fA-F0-9-]{36}")))
        require(quality=="Standard"||quality=="HD") {"Unknown chat video quality."}
        val exported=StoryVideo.export(context,source,startMs,
            onProgress=onProgress,targetShortSide=if(quality=="HD")720 else 480)
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
