package com.pritam.liquidchat.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

/** Account-scoped, on-device-only MP4 wallpaper: never upload or store a content URI. */
internal object PrivateVideoWallpaper {
    private const val MAX_BYTES=24L*1024*1024
    private const val MAX_DURATION_MS=30_000L

    fun location(context:Context,account:String,cid:String):File {
        // PersonalWallpaper.location checks both UUIDs and isolates the account directory.
        val photo=PersonalWallpaper.location(context,account,cid)
        return File(photo.parentFile,photo.nameWithoutExtension+".mp4")
    }

    fun has(context:Context,account:String,cid:String):Boolean=location(context,account,cid).isFile

    fun clear(context:Context,account:String,cid:String) {
        val file=location(context,account,cid)
        check(!file.exists() || file.delete()) {"Unable to erase the private video wallpaper."}
    }

    suspend fun save(context:Context,account:String,cid:String,uri:Uri):File {
        val type=context.contentResolver.getType(uri)?.lowercase()
        require(type=="video/mp4" || type=="application/mp4") {
            "Choose an MP4 video (up to 30 seconds and 24 MB)."
        }
        return withContext(Dispatchers.IO) {
            val target=location(context,account,cid)
            check(target.parentFile?.mkdirs()==true || target.parentFile?.isDirectory==true)
            val staging=File(target.parentFile,"."+target.name+"."+UUID.randomUUID()+".tmp")
            try {
                val input=context.contentResolver.openInputStream(uri)
                    ?: throw IOException("Unable to read the chosen video.")
                input.use {source->
                    FileOutputStream(staging).use {sink->
                        val buffer=ByteArray(64*1024)
                        var copied=0L
                        while(true) {
                            val n=source.read(buffer)
                            if(n<0)break
                            copied+=n
                            require(copied<=MAX_BYTES) {"Video exceeds 24 MB."}
                            sink.write(buffer,0,n)
                        }
                        sink.fd.sync()
                        require(copied>0L) {"The video is empty."}
                    }
                }
                val retriever=MediaMetadataRetriever()
                try {
                    retriever.setDataSource(staging.absolutePath)
                    val duration=retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull() ?: 0
                    val video=retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)=="yes"
                    val mime=retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                    val w=retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                        ?.toIntOrNull() ?: 0
                    val h=retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                        ?.toIntOrNull() ?: 0
                    require(video && mime=="video/mp4" && duration in 500L..MAX_DURATION_MS &&
                        w in 1..1920 && h in 1..1920 && w.toLong()*h<=1920L*1080L) {
                        "Choose a playable MP4 under 30 seconds, up to 1080p."
                    }
                } finally {retriever.release()}
                if(!staging.renameTo(target))throw IOException("Unable to save private video.")
                val old=PersonalWallpaper.location(context,account,cid)
                check(!old.exists() || old.delete()) {"Unable to replace previous photo wallpaper."}
                target
            } finally {staging.delete()}
        }
    }
}
