package com.pritam.liquidchat.ui

import android.content.Context
import android.net.Uri
import android.graphics.Typeface
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal enum class MbFontSlot { BODY, TITLE }
internal data class MbImportedFontFamilies(val body:FontFamily?,val title:FontFamily?)

/** Verify a bounded, standalone SFNT directory before the Android font parser. */
internal fun isSupportedFont(bytes:ByteArray):Boolean {
    if(bytes.size<12 || bytes.size>MbImportedFonts.MAX_BYTES)return false
    val signature=bytes.take(4).toByteArray()
    val trueType=signature.contentEquals(byteArrayOf(0,1,0,0)) ||
        signature.contentEquals("true".toByteArray(Charsets.US_ASCII))
    val openType=signature.contentEquals("OTTO".toByteArray(Charsets.US_ASCII))
    if(!trueType && !openType)return false
    val buffer=ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
    val count=buffer.getShort(4).toInt() and 0xffff
    if(count !in 1..256 || 12L+count*16L>bytes.size)return false
    val tags=mutableSetOf<String>()
    repeat(count) {index->
        val base=12+index*16
        val tag=String(bytes,base,4,Charsets.US_ASCII)
        if(!tags.add(tag))return false
        val offset=buffer.getInt(base+8).toLong() and 0xffffffffL
        val length=buffer.getInt(base+12).toLong() and 0xffffffffL
        if(offset<12L+count*16L || offset>bytes.size || length>bytes.size-offset)return false
    }
    if(!tags.containsAll(listOf("cmap","head","maxp","name")))return false
    return if(openType)"CFF " in tags || "CFF2" in tags else "glyf" in tags && "loca" in tags
}

/** Private device-local font files; never upload to Supabase or transfer MIME permissions. */
internal object MbImportedFonts {
    const val MAX_BYTES=8*1024*1024
    val revision=MutableStateFlow(0)
    private fun file(context:Context,slot:MbFontSlot):File? {
        val name=context.getSharedPreferences("liquid_imported_fonts_v1",Context.MODE_PRIVATE)
            .getString(slot.name,null) ?: return null
        if(!Regex("${slot.name.lowercase()}-[0-9a-f]{64}\\.font").matches(name))return null
        return File(context.filesDir,"appearance_fonts/$name")
    }

    suspend fun importFont(context:Context,slot:MbFontSlot,uri:Uri)=withContext(Dispatchers.IO) {
        val bytes=context.contentResolver.openInputStream(uri)?.use {stream->
            val output=java.io.ByteArrayOutputStream()
            val buffer=ByteArray(8192)
            while(true) {
                val count=stream.read(buffer)
                if(count<0)break
                require(output.size()+count<=MAX_BYTES) {"Choose a font smaller than 8 MiB."}
                output.write(buffer,0,count)
            }
            output.toByteArray()
        } ?: error("This font could not be opened.")
        require(isSupportedFont(bytes)) {"Choose a supported standalone TTF or OTF font."}
        val digest=java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") {"%02x".format(it.toInt() and 0xff)}
        val target=File(context.filesDir,"appearance_fonts/${slot.name.lowercase()}-$digest.font")
        check(target.parentFile?.let {it.isDirectory || it.mkdirs()}==true) {"Font storage unavailable."}
        val temporary=File.createTempFile("import-",".font",target.parentFile)
        try {
            java.io.FileOutputStream(temporary).use {it.write(bytes);it.fd.sync()}
            Typeface.createFromFile(temporary)
            java.nio.file.Files.move(temporary.toPath(),target.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                java.nio.file.StandardCopyOption.ATOMIC_MOVE)
            check(context.getSharedPreferences("liquid_imported_fonts_v1",Context.MODE_PRIVATE)
                .edit().putString(slot.name,target.name).commit()) {"Font choice could not be saved."}
            revision.value=revision.value+1
            target.parentFile?.listFiles()?.filter {
                it.name.startsWith("${slot.name.lowercase()}-") && it.extension=="font" && it!=target
            }?.forEach {it.delete()}
        } finally {temporary.delete()}
    }

    internal fun family(context:Context,slot:MbFontSlot):FontFamily? {
        val file=file(context,slot) ?: return null
        if(!file.isFile)return null
        return runCatching {FontFamily(Font(file))}.getOrNull()
    }

    fun remove(context:Context,slot:MbFontSlot) {
        val path=file(context,slot) ?: return
        check(!path.exists() || path.delete()) {"Could not remove imported font."}
        context.getSharedPreferences("liquid_imported_fonts_v1",Context.MODE_PRIVATE)
            .edit().remove(slot.name).apply()
        revision.value=revision.value+1
    }
}

@Composable internal fun rememberImportedFonts():MbImportedFontFamilies {
    val context=LocalContext.current
    val revision by MbImportedFonts.revision.collectAsState()
    return remember(context,revision) {
        MbImportedFontFamilies(MbImportedFonts.family(context,MbFontSlot.BODY),
            MbImportedFonts.family(context,MbFontSlot.TITLE))
    }
}
