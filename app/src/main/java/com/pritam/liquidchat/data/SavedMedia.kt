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
