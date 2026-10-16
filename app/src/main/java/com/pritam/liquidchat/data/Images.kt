package com.pritam.liquidchat.data

import android.content.Context
import android.graphics.ImageDecoder
import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

object Images {
    data class Prepared(val file:File,val width:Int,val height:Int)
    suspend fun prepare(context:Context,uri:Uri,account:String):Prepared=withContext(Dispatchers.IO) {
        val directory=File(context.filesDir,"media/$account").apply {mkdirs()}
        val original=File(directory,"${UUID.randomUUID()}.source")
        val compressed=File(directory,"${UUID.randomUUID()}.jpg")
        try {
            context.contentResolver.openInputStream(uri)?.use {input->original.outputStream().use {out->
                val buffer=ByteArray(8192);var total=0
                while(true) {val n=input.read(buffer);if(n<0)break;total+=n;if(total>10485760)throw IOException("Choose an image under 10 MB.");out.write(buffer,0,n)}
            }} ?: throw IOException("Cannot open that image.")
            val bitmap=ImageDecoder.decodeBitmap(ImageDecoder.createSource(original)) {decoder,info,_->
                val w=info.size.width;val h=info.size.height
                if(w.toLong()*h>25000000)throw IOException("Choose a smaller image.")
                val scale=minOf(1.0,2048.0/maxOf(w,h))
                decoder.setTargetSize(maxOf(1,(w*scale).toInt()),maxOf(1,(h*scale).toInt()))
                decoder.allocator=ImageDecoder.ALLOCATOR_SOFTWARE
            }
            val w=bitmap.width;val h=bitmap.height
            try {compressed.outputStream().use {check(bitmap.compress(Bitmap.CompressFormat.JPEG,85,it))}} finally {bitmap.recycle()}
            Prepared(compressed,w,h)
        } catch(e:Exception) {compressed.delete();throw e} finally {original.delete()}
    }
}
