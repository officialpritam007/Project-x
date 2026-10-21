package com.pritam.liquidchat.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

internal data class PreparedPdf(val file:File,val name:String)

internal object PdfDocuments {
    const val MAX_BYTES=8_388_608

    fun safeName(text:String):String {
        val base=text.substringAfterLast('/').substringAfterLast('\\')
            .filter {it.isLetterOrDigit() || it in " _.()-"}
            .trim().take(155)
        val title=base.ifBlank {"document"}
        val filename=if(title.endsWith(".pdf",ignoreCase=true))title
            else "${title.removeSuffix(".")}.pdf"
        return filename.take(160)
    }

    suspend fun prepare(context:Context,uri:Uri,account:String):PreparedPdf =
        withContext(Dispatchers.IO) {
            require(account.matches(Regex("[a-zA-Z0-9_-]{8,80}")))
            val filename=runCatching {
                context.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,null,null)?.use {cursor->
                    if(cursor.moveToFirst())cursor.getString(0) else null
                }
            }.getOrNull() ?: "document.pdf"
            val directory=File(context.filesDir,"media/$account").apply {mkdirs()}
            val file=File(directory,"${UUID.randomUUID()}.pdf")
            try {
                context.contentResolver.openInputStream(uri)?.use {source->
                    file.outputStream().use {output->
                        val buffer=ByteArray(8192)
                        var count=0
                        while(true) {
                            val n=source.read(buffer)
                            if(n<0)break
                            count+=n
                            if(count>MAX_BYTES)throw IOException("PDF must be 8 MB or less.")
                            output.write(buffer,0,n)
                        }
                    }
                } ?: throw IOException("Unable to read the chosen PDF.")
                if(file.length() !in 100..MAX_BYTES.toLong())
                    throw IOException("Invalid PDF size.")
                file.inputStream().use {input->
                    val prefix=ByteArray(8)
                    if(input.read(prefix)!=8 ||
                        !String(prefix,Charsets.US_ASCII).startsWith("%PDF-"))
                        throw IOException("The selected file is not a PDF.")
                }
                // The Edge Function performs the authoritative PDF validation.
                PreparedPdf(file,safeName(filename))
            } catch(e:Exception) {file.delete();throw e}
        }

    /** User-directed copy of one authorized, undeleted PDF to a selected SAF URI. */
    suspend fun saveReceived(
        context:Context,repository:ChatRepository,message:Message,destination:Uri
    )=withContext(Dispatchers.IO) {
        val backend=repository.backend
        val account=backend.session.value?.userId ?: throw IOException("Sign in required.")
        val epoch=backend.uiReads.epoch(account)
        require(message.account==account && message.status=="SENT" &&
            message.deletedAt.isEmpty() && message.mediaKind=="document" &&
            message.mediaId.isNotBlank()) {"Document is no longer available."}
        suspend fun permitted():Boolean {
            if(backend.session.value?.userId!=account ||
                backend.uiReads.epoch(account)!=epoch)return false
            val current=repository.dao.message(account,message.id) ?: return false
            return current.mediaKind=="document" &&
                current.mediaId==message.mediaId && current.deletedAt.isEmpty() &&
                current.status=="SENT" && !repository.dao.hidden(account,message.id)
        }
        check(permitted()) {"Document is no longer visible."}
        val url=backend.imageUrl(account,message.mediaId,force=true)
        require(url.startsWith("https://",ignoreCase=true)) {"Invalid document link."}
        val connection=URL(url).openConnection() as HttpURLConnection
        val data=try {
            connection.connectTimeout=15_000
            connection.readTimeout=45_000
            connection.useCaches=false
            connection.inputStream.use {input->
                val output=java.io.ByteArrayOutputStream()
                val chunk=ByteArray(8192)
                var total=0
                while(true) {
                    val read=input.read(chunk)
                    if(read<0)break
                    total+=read
                    if(total>MAX_BYTES)throw IOException("PDF exceeds 8 MB.")
                    output.write(chunk,0,read)
                }
                output.toByteArray()
            }
        } finally {connection.disconnect()}
        if(data.size<100 || !String(data,0,minOf(data.size,8),Charsets.US_ASCII)
                .startsWith("%PDF-"))throw IOException("Invalid received PDF.")
        check(permitted()) {"Document access changed."}
        backend.imageUrl(account,message.mediaId,force=true)
        check(permitted()) {"Document access changed."}
        // SAF URI is deliberately supplied by the user. No silent downloads or
        // public document links, and only explicitly selected location is written.
        context.contentResolver.openOutputStream(destination,"w")?.use {out->
            out.write(data)
            out.flush()
        } ?: throw IOException("Unable to write the selected document.")
    }
}
