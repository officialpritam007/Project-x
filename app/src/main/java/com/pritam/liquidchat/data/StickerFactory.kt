package com.pritam.liquidchat.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Local still-sticker creator. Output: 512x512 8-bit RGBA PNG with real alpha.
 * This keeps the sticker distinct from JPEG photo messages in the backend.
 * No remote cutout API, SDK account or paid image service is used.
 */
/** Remove only near-white pixels connected to the image edge.
 * Disconnected white details inside a character remain opaque. The flood-fill
 * is strictly linear in image pixels and runs in the existing IO job.
 */
internal fun borderConnectedNearWhiteMask(
    pixels:IntArray,width:Int,height:Int
):BooleanArray {
    require(width>0 && height>0 && width.toLong()*height==pixels.size.toLong())
    val seen=BooleanArray(pixels.size)
    val queue=IntArray(pixels.size)
    var head=0
    var tail=0
    fun candidate(index:Int):Boolean {
        val argb=pixels[index]
        val alpha=(argb ushr 24) and 255
        if(alpha<16)return true
        val red=(argb ushr 16) and 255
        val green=(argb ushr 8) and 255
        val blue=argb and 255
        return minOf(red,green,blue)>230 && maxOf(red,green,blue)-minOf(red,green,blue)<18
    }
    fun enqueue(index:Int) {
        if(!seen[index]&&candidate(index)) {
            seen[index]=true
            queue[tail++]=index
        }
    }
    for(x in 0 until width) {
        enqueue(x)
        enqueue((height-1)*width+x)
    }
    for(y in 0 until height) {
        enqueue(y*width)
        enqueue(y*width+width-1)
    }
    while(head<tail) {
        val index=queue[head++]
        val x=index%width
        val y=index/width
        if(x>0)enqueue(index-1)
        if(x+1<width)enqueue(index+1)
        if(y>0)enqueue(index-width)
        if(y+1<height)enqueue(index+width)
    }
    return seen
}

internal object StickerFactory {
    suspend fun prepare(
        context:Context,uri:Uri,account:String,removeWhite:Boolean=false
    ):Images.Prepared=withContext(Dispatchers.IO) {
        require(account.isNotBlank()) {"Sign in before creating a sticker."}
        val directory=File(context.filesDir,"media/$account").apply {mkdirs()}
        val source=ImageDecoder.createSource(context.contentResolver,uri)
        val bitmap=ImageDecoder.decodeBitmap(source) {decoder,info,_->
            val width=info.size.width
            val height=info.size.height
            require(width>0 && height>0 && width.toLong()*height<=25_000_000) {
                "Choose an image under 25 megapixels."
            }
            val ratio=minOf(1.0,480.0/maxOf(width,height))
            decoder.setTargetSize(maxOf(1,(width*ratio).toInt()),
                maxOf(1,(height*ratio).toInt()))
            decoder.allocator=ImageDecoder.ALLOCATOR_SOFTWARE
        }
        val output=File(directory,"${UUID.randomUUID()}.png")
        try {
            val sticker=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888)
            try {
                sticker.eraseColor(Color.TRANSPARENT)
                val content=if(removeWhite) {
                    val pixels=IntArray(bitmap.width*bitmap.height)
                    bitmap.getPixels(pixels,0,bitmap.width,0,0,bitmap.width,bitmap.height)
                    val background=borderConnectedNearWhiteMask(
                        pixels,bitmap.width,bitmap.height)
                    for(i in pixels.indices) {
                        if(!background[i])continue
                        val rgb=pixels[i]
                        val red=Color.red(rgb)
                        val green=Color.green(rgb)
                        val blue=Color.blue(rgb)
                        // Feather only the contiguous boundary background.
                        // White details enclosed by a non-white subject stay visible.
                        val minimum=minOf(red,green,blue)
                        val opacity=((255-minimum)*255/25).coerceIn(0,255)
                        pixels[i]=Color.argb(Color.alpha(rgb)*opacity/255,red,green,blue)
                    }
                    Bitmap.createBitmap(pixels,bitmap.width,bitmap.height,
                        Bitmap.Config.ARGB_8888)
                } else bitmap
                try {
                    val canvas=Canvas(sticker)
                    val scale=480f/maxOf(content.width,content.height)
                    val w=content.width*scale
                    val h=content.height*scale
                    canvas.drawBitmap(content,
                        Rect(0,0,content.width,content.height),
                        RectF((512f-w)/2,(512f-h)/2,(512f+w)/2,(512f+h)/2),
                        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                } finally {if(content!==bitmap)content.recycle()}
                // A simple 512px image may be larger than the 512 KB sticker
                // quota. Reduce drawn pixel density within the same RGBA
                // canvas, leaving an honest transparent border.
                for(target in listOf(512,400,320,256,192)) {
                    val candidate=if(target==512)sticker else {
                        val small=Bitmap.createScaledBitmap(sticker,target,target,true)
                        val padded=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888)
                        Canvas(padded).drawBitmap(small,
                            ((512-target)/2).toFloat(),((512-target)/2).toFloat(),Paint())
                        small.recycle()
                        padded
                    }
                    try {
                        output.outputStream().use {stream->
                            if(!candidate.compress(Bitmap.CompressFormat.PNG,100,stream))
                                throw IOException("Sticker could not be encoded.")
                        }
                    } finally {if(candidate!==sticker)candidate.recycle()}
                    if(output.length() in 100..524288)break
                }
                if(output.length() !in 100..524288)
                    throw IOException("Sticker is too detailed. Choose a simpler picture.")
            } finally {sticker.recycle()}
            Images.Prepared(output,512,512)
        } catch(e:Exception) {output.delete();throw e}
        finally {bitmap.recycle()}
    }
}
