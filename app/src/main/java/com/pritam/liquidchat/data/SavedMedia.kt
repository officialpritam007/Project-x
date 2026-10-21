package com.pritam.liquidchat.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Save only an authenticated, still-visible photo message. Media access is
 * resolved through Liquid Chat's signed short-lived image endpoint, never by
 * exposing a public Cloudinary/Supabase storage URL.
 *
 * Android 10+ MediaStore writes do not need legacy storage permissions.
 */
internal object SavedMedia {
    private const val MAX_BYTES=10_485_760


    /**
     * Only an available, backend-authorized video story can be exported.
     * A fresh access check is required before making the MediaStore file visible.
     */
    suspend fun saveStoryVideo(
        context:Context,backend:Backend,storyId:String,mediaId:String,expires:String?
    ):Uri = withContext(Dispatchers.IO) {
        val account=backend.session.value?.userId ?: throw IOException("Please sign in.")
        val epoch=backend.uiReads.epoch(account)
        require(storyId.isNotBlank() && mediaId.isNotBlank())
        fun stillCurrent():Boolean =
            backend.session.value?.userId==account &&
            backend.uiReads.epoch(account)==epoch &&
            (expires==null || runCatching {
                java.time.Instant.parse(expires).isAfter(java.time.Instant.now())
            }.getOrDefault(false))
        check(stillCurrent()) {"The story has expired or your account changed."}
        val signed=backend.imageUrl(account,mediaId,force=true)
        require(signed.startsWith("https://",ignoreCase=true)) {"Invalid video link."}
        val connection=URL(signed).openConnection() as HttpURLConnection
        val bytes=try {
            connection.connectTimeout=15_000
            connection.readTimeout=45_000
            connection.useCaches=false
            connection.inputStream.use {input->
                val out=ByteArrayOutputStream()
                val buffer=ByteArray(8192)
                var count=0
                while(true) {
                    val n=input.read(buffer)
                    if(n<0)break
                    count+=n
                    if(count>8_388_608)throw IOException("Story video exceeds 8 MB.")
                    out.write(buffer,0,n)
                }
                out.toByteArray()
            }
        } finally {connection.disconnect()}
        if(bytes.size<16 || bytes[4]!='f'.code.toByte() ||
            bytes[5]!='t'.code.toByte() || bytes[6]!='y'.code.toByte() ||
            bytes[7]!='p'.code.toByte())
            throw IOException("The story is not a supported MP4 video.")
        check(stillCurrent()) {"The story has expired or your account changed."}
        backend.imageUrl(account,mediaId,force=true)
        check(stillCurrent()) {"Video access changed."}
        val values=ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME,"LiquidChat_Story_${storyId}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE,"video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH,"Movies/Liquid Chat")
            put(MediaStore.Video.Media.IS_PENDING,1)
        }
        val resolver=context.contentResolver
        val destination=resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,values)
            ?: throw IOException("Video library unavailable.")
        try {
            resolver.openOutputStream(destination,"w")?.use {
                it.write(bytes)
                it.flush()
            } ?: throw IOException("Could not write the story video.")
            check(stillCurrent()) {"Video access changed."}
            backend.imageUrl(account,mediaId,force=true)
            check(stillCurrent()) {"Video access changed."}
            val updated=resolver.update(destination,ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING,0)
            },null,null)
            if(updated<1)throw IOException("Could not finalize video.")
            destination
        } catch(e:Exception) {
            resolver.delete(destination,null,null)
            throw e
        }
    }

    /**
     * Story photos are authorized by the media endpoint. A fresh access request
     * after download guards against a story being removed or expiring mid-save.
     * Video stories are never accepted by this JPEG-only path.
     */
    suspend fun saveStoryPhoto(
        context:Context,backend:Backend,storyId:String,mediaId:String,expires:String?
    ):Uri = withContext(Dispatchers.IO) {
        val account=backend.session.value?.userId ?: throw IOException("Please sign in.")
        val epoch=backend.uiReads.epoch(account)
        require(storyId.isNotBlank() && mediaId.isNotBlank())
        fun stillCurrent():Boolean =
            backend.session.value?.userId==account &&
            backend.uiReads.epoch(account)==epoch &&
            (expires==null || runCatching {
                java.time.Instant.parse(expires).isAfter(java.time.Instant.now())
            }.getOrDefault(false))
        check(stillCurrent()) {"The story has expired or your account changed."}
        val signed=backend.imageUrl(account,mediaId,force=true)
        require(signed.startsWith("https://",ignoreCase=true)) {"Invalid story media link."}
        val connection=URL(signed).openConnection() as HttpURLConnection
        val bytes=try {
            connection.connectTimeout=15_000
            connection.readTimeout=30_000
            connection.useCaches=false
            connection.inputStream.use {input->
                val out=ByteArrayOutputStream()
                val buffer=ByteArray(8192)
                var count=0
                while(true) {
                    val n=input.read(buffer)
                    if(n<0)break
                    count+=n
                    if(count>MAX_BYTES)throw IOException("Story image is too large.")
                    out.write(buffer,0,n)
                }
                out.toByteArray()
            }
        } finally {connection.disconnect()}
        if(bytes.size<12 || bytes[0]!=0xff.toByte() ||
            bytes[1]!=0xd8.toByte() || bytes[2]!=0xff.toByte())
            throw IOException("The story is not a JPEG photo.")
        check(stillCurrent()) {"The story has expired or your account changed."}
        // Force the backend to authorize this media again after downloading.
        backend.imageUrl(account,mediaId,force=true)
        check(stillCurrent()) {"Story access changed."}
        val values=ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME,"LiquidChat_Story_${storyId}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/Liquid Chat")
            put(MediaStore.Images.Media.IS_PENDING,1)
        }
        val resolver=context.contentResolver
        val destination=resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)
            ?: throw IOException("Photo library unavailable.")
        try {
            resolver.openOutputStream(destination,"w")?.use {
                it.write(bytes)
                it.flush()
            } ?: throw IOException("Could not save the story.")
            check(stillCurrent()) {"Story access changed."}
            backend.imageUrl(account,mediaId,force=true)
            check(stillCurrent()) {"Story access changed."}
            val updated=resolver.update(destination,ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING,0)
            },null,null)
            if(updated<1)throw IOException("Could not finalize saved story.")
            destination
        } catch(e:Exception) {
            resolver.delete(destination,null,null)
            throw e
        }
    }

    suspend fun savePhoto(context:Context,repository:ChatRepository,message:Message):Uri =
        withContext(Dispatchers.IO) {
            val backend=repository.backend
            val account=backend.session.value?.userId
                ?: throw IOException("Please sign in.")
            val epoch=backend.uiReads.epoch(account)
            require(message.account==account && message.status=="SENT" &&
                message.deletedAt.isEmpty() && message.mediaKind=="image" &&
                message.mediaId.isNotBlank()) {"Only available photos can be saved."}

            suspend fun stillAllowed():Boolean {
                if(backend.session.value?.userId!=account ||
                    backend.uiReads.epoch(account)!=epoch)return false
                val current=repository.dao.message(account,message.id) ?: return false
                return current.status=="SENT" && current.deletedAt.isEmpty() &&
                    current.mediaId==message.mediaId &&
                    !repository.dao.hidden(account,message.id)
            }
            check(stillAllowed()) {"Photo is no longer available."}
            val signed=backend.imageUrl(account,message.mediaId,force=true)
            require(signed.startsWith("https://",ignoreCase=true)) {"Invalid media link."}
            val connection=URL(signed).openConnection() as HttpURLConnection
            val bytes=try {
                connection.connectTimeout=15_000
                connection.readTimeout=30_000
                connection.useCaches=false
                connection.inputStream.use {input->
                    val data=ByteArrayOutputStream()
                    val buffer=ByteArray(8192)
                    var count=0
                    while(true) {
                        val n=input.read(buffer)
                        if(n<0)break
                        count+=n
                        if(count>MAX_BYTES)throw IOException("Photo exceeds the download limit.")
                        data.write(buffer,0,n)
                    }
                    data.toByteArray()
                }
            } finally {connection.disconnect()}
            if(bytes.size<12 || bytes[0]!=0xff.toByte() ||
                bytes[1]!=0xd8.toByte() || bytes[2]!=0xff.toByte())
                throw IOException("The media provider did not return a JPEG photo.")
            check(stillAllowed()) {"Photo access changed during download."}
            val values=ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME,"LiquidChat_${message.id}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/Liquid Chat")
                put(MediaStore.Images.Media.IS_PENDING,1)
            }
            val resolver=context.contentResolver
            val destination=resolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)
                ?: throw IOException("Photo library is unavailable.")
            try {
                resolver.openOutputStream(destination,"w")?.use {out->
                    out.write(bytes)
                    out.flush()
                } ?: throw IOException("Unable to write the photo.")
                check(stillAllowed()) {"Photo access changed during save."}
                resolver.update(destination,ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING,0)
                },null,null)
                destination
            } catch(e:Exception) {
                resolver.delete(destination,null,null)
                throw e
            }
        }
}
