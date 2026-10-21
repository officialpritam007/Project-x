package com.pritam.liquidchat.data

import android.content.Context
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Private device-only sticker shelf. Images are copied to private app storage,
 * never external storage or a public URL. A sticker is copied again to the
 * per-account outbox before sending, so saved originals are not deleted by
 * upload success or discard of pending messages.
 */
internal object StickerLibrary {
    const val LIMIT=48
    private fun root(context:Context,account:String):File {
        require(account.isNotBlank() && account.matches(Regex("[a-zA-Z0-9_-]{8,80}"))) {
            "Invalid sticker account."
        }
        return File(context.filesDir,"saved_stickers/$account")
    }
    fun list(context:Context,account:String?):List<File> {
        if(account.isNullOrBlank())return emptyList()
        return runCatching {
            root(context,account).listFiles()?.filter {
                it.isFile && it.extension=="png" && it.length() in 100..524288
            }?.sortedByDescending {it.lastModified()}?.take(LIMIT).orEmpty()
        }.getOrDefault(emptyList())
    }
    fun save(context:Context,account:String,prepared:Images.Prepared):File {
        require(prepared.width==512 && prepared.height==512 &&
            prepared.file.isFile && prepared.file.length() in 100..524288 &&
            prepared.file.extension=="png")
        val folder=root(context,account).apply {mkdirs()}
        require(list(context,account).size<LIMIT) {"Remove a sticker to save another."}
        val file=File(folder,"${UUID.randomUUID()}.png")
        try {
            prepared.file.inputStream().use {source->
                file.outputStream().use {source.copyTo(it)}
            }
            require(file.length() in 100..524288)
            return file
        } catch(e:Exception) {file.delete();throw e}
    }
    fun remove(context:Context,account:String,file:File) {
        require(file.canonicalFile.parentFile==root(context,account).canonicalFile)
        require(file.isFile && file.delete()) {"Sticker could not be removed."}
    }
    fun prepareForSend(context:Context,account:String,file:File):Images.Prepared {
        require(file.canonicalFile.parentFile==root(context,account).canonicalFile &&
            file.isFile && file.extension=="png" && file.length() in 100..524288)
        val destinationDir=File(context.filesDir,"media/$account").apply {mkdirs()}
        val copied=File(destinationDir,"${UUID.randomUUID()}.png")
        try {
            file.inputStream().use {src->
                copied.outputStream().use {src.copyTo(it)}
            }
            if(copied.length() !in 100..524288)throw IOException("Invalid saved sticker.")
            return Images.Prepared(copied,512,512)
        } catch(e:Exception) {copied.delete();throw e}
    }
    fun clearAccount(context:Context,account:String) {
        if(account.isNotBlank())root(context,account).deleteRecursively()
    }
}
