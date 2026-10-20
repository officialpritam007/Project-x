package com.pritam.liquidchat.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * A personal chat wallpaper is device-local and private. Never upload it to
 * Supabase/Cloudinary or copy the original source URI into shared settings.
 * UUID validation makes it impossible for account/chat parameters to form paths.
 */
internal object PersonalWallpaper {
    /** Reserved key for the owner-visible Chats home wallpaper, not a conversation. */
    const val HOME_CID="00000000-0000-0000-0000-000000000000"
    private fun checkedId(id:String):String =
        UUID.fromString(id).toString().also {
            require(it==id.lowercase()) {"Wallpaper identity must be a UUID."}
        }

    fun location(context:Context,account:String,cid:String):File =
        File(File(File(context.filesDir,"wallpapers"),checkedId(account)),checkedId(cid)+".jpg")

    fun has(context:Context,account:String,cid:String):Boolean =
        location(context,account,cid).isFile

    suspend fun save(context:Context,account:String,cid:String,uri:Uri):File {
        // Images.prepare enforces <=10 MB compressed input and <=25M pixels,
        // downscales to <=2048px and creates a safe private JPEG.
        val prepared=Images.prepare(context,uri,checkedId(account))
        return try {
            withContext(Dispatchers.IO) {
                val target=location(context,account,cid)
                check(target.parentFile?.mkdirs()==true || target.parentFile?.isDirectory==true)
                val staging=File(target.parentFile,".${target.name}.${UUID.randomUUID()}.tmp")
                try {
                    prepared.file.copyTo(staging,overwrite=false)
                    if(!staging.renameTo(target))throw IOException("Unable to save the local wallpaper.")
                    PrivateVideoWallpaper.clear(context,account,cid)
                } finally {staging.delete()}
                target
            }
        } finally {prepared.file.delete()}
    }

    /** Erase this conversation's wallpaper while preserving the account's Home/other chats. */
    fun clearConversation(context:Context,account:String,cid:String) {
        require(cid!=HOME_CID) {"Home wallpaper is not a conversation."}
        val file=location(context,account,cid)
        check(!file.exists() || file.delete()) {
            "Could not erase the deleted conversation's wallpaper."
        }
        PrivateVideoWallpaper.clear(context,account,cid)
    }

    fun clearAccount(context:Context,account:String) {
        val directory=File(File(context.filesDir,"wallpapers"),checkedId(account))
        check(!directory.exists() || directory.deleteRecursively()) {
            "Could not securely erase the account's local wallpapers."
        }
    }
}
