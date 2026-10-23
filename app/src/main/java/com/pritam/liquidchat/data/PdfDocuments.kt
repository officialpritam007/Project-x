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

data class PreparedPdf(val file:File,val name:String)

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

    /** Convert a private camera capture into a single-page monochrome A4 PDF. */
    suspend fun scanPhoto(context:Context,captured:File,account:String):PreparedPdf =
        withContext(Dispatchers.IO) {
            require(account.matches(Regex("[a-zA-Z0-9_-]{8,80}")))
            require(captured.isFile && captured.canonicalFile.path.startsWith(
                File(context.cacheDir,"camera").canonicalPath+File.separator)) {
                "Scan source must be a local camera photo."
            }
            val image=android.graphics.ImageDecoder.decodeBitmap(
                android.graphics.ImageDecoder.createSource(captured)
            ) {decoder,info,_->
                val width=info.size.width
                val height=info.size.height
                require(width>0 && height>0 && width.toLong()*height<=25_000_000L) {
                    "Capture is too large."
                }
                val factor=minOf(1.0,1500.0/maxOf(width,height))
                decoder.setTargetSize(maxOf(1,(width*factor).toInt()),
                    maxOf(1,(height*factor).toInt()))
                decoder.allocator=android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
            }
            val directory=File(context.filesDir,"media/$account").apply {mkdirs()}
            val output=File(directory,"${UUID.randomUUID()}.pdf")
            try {
                val pdf=android.graphics.pdf.PdfDocument()
                try {
                    val pageInfo=android.graphics.pdf.PdfDocument.PageInfo.Builder(
                        595,842,1).create()
                    val page=pdf.startPage(pageInfo)
                    val canvas=page.canvas
                    canvas.drawColor(android.graphics.Color.WHITE)
                    val scale=minOf(555f/image.width,802f/image.height)
                    val width=image.width*scale
                    val height=image.height*scale
                    val target=android.graphics.RectF((595f-width)/2,(842f-height)/2,
                        (595f+width)/2,(842f+height)/2)
                    val filter=android.graphics.ColorMatrix().apply {setSaturation(0f)}
                    val paint=android.graphics.Paint(
                        android.graphics.Paint.ANTI_ALIAS_FLAG or
                            android.graphics.Paint.FILTER_BITMAP_FLAG
                    ).apply {
                        colorFilter=android.graphics.ColorMatrixColorFilter(filter)
                    }
                    canvas.drawBitmap(image,null,target,paint)
                    pdf.finishPage(page)
                    output.outputStream().use {pdf.writeTo(it)}
                } finally {pdf.close()}
                if(output.length() !in 100..MAX_BYTES.toLong())
                    throw IOException("Scanned PDF exceeds 8 MB. Try again with another photo.")
                PreparedPdf(output,"Scanned-document.pdf")
            } catch(e:Exception) {output.delete();throw e}
            finally {image.recycle()}
        }

    /** One A4 document from 1–10 explicitly selected gallery images.
     *  Source bytes stay on device; the shared PDF follows the same private upload
     *  and participant authorization path as a scanned camera document.
     */
    suspend fun scanGalleryPhotos(context:Context,photos:List<Uri>,account:String):PreparedPdf =
        withContext(Dispatchers.IO) {
            require(account.matches(Regex("[a-zA-Z0-9_-]{8,80}")))
            require(photos.isNotEmpty() && photos.size<=10 && photos.distinct().size==photos.size) {
                "Select between one and ten different pages."
            }
            val directory=File(context.filesDir,"media/$account").apply {mkdirs()}
            val result=File(directory,"${UUID.randomUUID()}.pdf")
            try {
                val pdf=android.graphics.pdf.PdfDocument()
                try {
                    photos.forEachIndexed {index,uri->
                        val mime=context.contentResolver.getType(uri)
                        require(mime==null || mime.startsWith("image/")) {
                            "Only gallery images can be scanned."
                        }
                        val bitmap=android.graphics.ImageDecoder.decodeBitmap(
                            android.graphics.ImageDecoder.createSource(context.contentResolver,uri)
                        ) {decoder,info,_->
                            val width=info.size.width
                            val height=info.size.height
                            require(width>0 && height>0 && width.toLong()*height<=25_000_000L) {
                                "Page ${index+1} is too large."
                            }
                            val factor=minOf(1.0,1500.0/maxOf(width,height))
                            decoder.setTargetSize(maxOf(1,(width*factor).toInt()),
                                maxOf(1,(height*factor).toInt()))
                            decoder.allocator=android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                        }
                        try {
                            val pageInfo=android.graphics.pdf.PdfDocument.PageInfo.Builder(
                                595,842,index+1).create()
                            val page=pdf.startPage(pageInfo)
                            val canvas=page.canvas
                            canvas.drawColor(android.graphics.Color.WHITE)
                            val scale=minOf(555f/bitmap.width,802f/bitmap.height)
                            val width=bitmap.width*scale
                            val height=bitmap.height*scale
                            val target=android.graphics.RectF((595f-width)/2,(842f-height)/2,
                                (595f+width)/2,(842f+height)/2)
                            val filter=android.graphics.ColorMatrix().apply {setSaturation(0f)}
                            val paint=android.graphics.Paint(
                                android.graphics.Paint.ANTI_ALIAS_FLAG or
                                    android.graphics.Paint.FILTER_BITMAP_FLAG
                            ).apply {colorFilter=android.graphics.ColorMatrixColorFilter(filter)}
                            canvas.drawBitmap(bitmap,null,target,paint)
                            pdf.finishPage(page)
                        } finally {bitmap.recycle()}
                    }
                    result.outputStream().use {pdf.writeTo(it)}
                } finally {pdf.close()}
                if(result.length() !in 100..MAX_BYTES.toLong()) {
                    throw IOException("Combined scan exceeds 8 MB. Choose fewer or smaller images.")
                }
                PreparedPdf(result,safeName("Scanned-${photos.size}-pages.pdf"))
            } catch(e:Exception) {result.delete();throw e}
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
