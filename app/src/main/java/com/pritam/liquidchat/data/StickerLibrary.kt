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
    private const val METADATA_PREFS="liquid_saved_sticker_metadata_v1"
    private const val MAX_RECENTS=12
    private fun prefs(context:Context)=
        context.getSharedPreferences(METADATA_PREFS,Context.MODE_PRIVATE)
    private fun favoritesKey(account:String)="favorite:$account"
    private fun recentKey(account:String)="recent:$account"

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
        val sourceRoot=File(context.filesDir,"media/$account").canonicalFile
        require(prepared.file.canonicalFile.parentFile==sourceRoot) {
            "This sticker belongs to a different account."
        }
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
    private fun ownedSavedFile(context:Context,account:String,file:File):Boolean =
        file.name.matches(Regex("[0-9a-fA-F-]{36}\\.png")) &&
            file.canonicalFile.parentFile==root(context,account).canonicalFile &&
            file.isFile

    /** Only owner-local, existing sticker basenames are saved in metadata. */
    fun favoriteNames(context:Context,account:String?):Set<String> {
        if(account.isNullOrBlank())return emptySet()
        val existing=list(context,account).map {it.name}.toSet()
        return runCatching {
            prefs(context).getStringSet(favoritesKey(account),emptySet())
                .orEmpty().filterTo(mutableSetOf()) {it in existing}
        }.getOrDefault(emptySet())
    }

    fun setFavorite(context:Context,account:String,file:File,favorite:Boolean) {
        require(ownedSavedFile(context,account,file)) {"Sticker no longer exists."}
        val next=favoriteNames(context,account).toMutableSet()
        if(favorite)next+=file.name else next-=file.name
        check(prefs(context).edit().putStringSet(favoritesKey(account),next).commit()) {
            "Unable to save sticker favorite."
        }
    }

    fun recent(context:Context,account:String?):List<File> {
        if(account.isNullOrBlank())return emptyList()
        val available=list(context,account).associateBy {it.name}
        val raw=runCatching {
            prefs(context).getString(recentKey(account),"[]") ?: "[]"
        }.getOrDefault("[]")
        return runCatching {
            val ids=org.json.JSONArray(raw)
            (0 until minOf(ids.length(),MAX_RECENTS)).mapNotNull {index->
                available[ids.optString(index)]
            }.distinctBy {it.name}
        }.getOrDefault(emptyList())
    }

    /** Record a genuinely queued send, never an attempted or cancelled send. */
    fun markUsed(context:Context,account:String,file:File) {
        require(ownedSavedFile(context,account,file)) {"Sticker no longer exists."}
        val updated=(listOf(file.name)+recent(context,account).map {it.name }
            .filterNot {it==file.name}).take(MAX_RECENTS)
        val json=org.json.JSONArray()
        updated.forEach(json::put)
        check(prefs(context).edit().putString(recentKey(account),json.toString()).commit()) {
            "Unable to save recent sticker."
        }
    }

    fun remove(context:Context,account:String,file:File) {
        require(ownedSavedFile(context,account,file))
        require(file.delete()) {"Sticker could not be removed."}
        val updatedFavorites=favoriteNames(context,account)-file.name
        val updatedRecent=org.json.JSONArray()
        recent(context,account).map {it.name}.filterNot {it==file.name}
            .forEach(updatedRecent::put)
        check(prefs(context).edit()
            .putStringSet(favoritesKey(account),updatedFavorites)
            .putString(recentKey(account),updatedRecent.toString()).commit()) {
            "Sticker removed, but metadata could not be updated."
        }
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
    /** Save an authenticated sticker from an undeleted, locally visible message. */
    suspend fun saveReceived(context:Context,repository:ChatRepository,message:Message):File =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val backend=repository.backend
            val account=backend.session.value?.userId
                ?: throw IOException("Please sign in.")
            val epoch=backend.uiReads.epoch(account)
            require(message.account==account && message.status=="SENT" &&
                message.deletedAt.isEmpty() && message.mediaKind=="sticker" &&
                message.mediaId.isNotBlank()) {"Sticker unavailable."}
            suspend fun allowed():Boolean {
                if(backend.session.value?.userId!=account ||
                    backend.uiReads.epoch(account)!=epoch)return false
                val current=repository.dao.message(account,message.id) ?: return false
                return current.mediaId==message.mediaId && current.mediaKind=="sticker" &&
                    current.deletedAt.isEmpty() && current.status=="SENT" &&
                    !repository.dao.hidden(account,message.id)
            }
            check(allowed()) {"Sticker no longer visible."}
            val signed=backend.imageUrl(account,message.mediaId,force=true)
            require(signed.startsWith("https://",ignoreCase=true)) {"Invalid sticker URL."}
            val connection=java.net.URL(signed).openConnection() as java.net.HttpURLConnection
            val bytes=try {
                connection.connectTimeout=15_000
                connection.readTimeout=30_000
                connection.useCaches=false
                connection.inputStream.use {source->
                    val data=java.io.ByteArrayOutputStream()
                    val buffer=ByteArray(4096)
                    var count=0
                    while(true) {
                        val n=source.read(buffer)
                        if(n<0)break
                        count+=n
                        if(count>524288)throw IOException("Sticker exceeds 512 KB.")
                        data.write(buffer,0,n)
                    }
                    data.toByteArray()
                }
            } finally {connection.disconnect()}
            val magic=byteArrayOf(137.toByte(),80,78,71,13,10,26,10)
            require(bytes.size in 100..524288 && bytes.copyOfRange(0,8).contentEquals(magic)
                && java.nio.ByteBuffer.wrap(bytes,16,4).int==512
                && java.nio.ByteBuffer.wrap(bytes,20,4).int==512
                && bytes[24]==8.toByte() && bytes[25]==6.toByte()) {
                "Invalid sticker PNG."
            }
            check(allowed()) {"Sticker access changed."}
            backend.imageUrl(account,message.mediaId,force=true)
            check(allowed()) {"Sticker access changed."}
            val folder=root(context,account).apply {mkdirs()}
            require(list(context,account).size<LIMIT) {"Sticker shelf is full."}
            val destination=File(folder,"${UUID.randomUUID()}.png")
            try {
                destination.outputStream().use {it.write(bytes)}
                if(!allowed())throw IOException("Sticker access changed while saving.")
                destination
            } catch(e:Exception) {destination.delete();throw e}
        }

    fun clearAccount(context:Context,account:String) {
        if(account.isNotBlank()) {
            root(context,account).deleteRecursively()
            prefs(context).edit().remove(favoritesKey(account))
                .remove(recentKey(account)).apply()
        }
    }
}
