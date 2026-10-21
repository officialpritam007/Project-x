package com.pritam.liquidchat.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Color
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

/**
 * Image-based Liquid Chat theme seed. Processes up to 96x96 pixels on-device;
 * the original image is never copied, uploaded or included in theme exports.
 */
internal object PhotoAccentSampler {
    suspend fun extract(context:Context,uri:Uri):String = withContext(Dispatchers.IO) {
        val source=ImageDecoder.createSource(context.contentResolver,uri)
        val image=ImageDecoder.decodeBitmap(source) {decoder,info,_->
            val w=info.size.width
            val h=info.size.height
            require(w>0&&h>0&&w.toLong()*h<=50_000_000L) {
                "Choose a smaller theme photo."
            }
            val ratio=minOf(1.0,96.0/maxOf(w,h))
            decoder.setTargetSize(maxOf(1,(w*ratio).toInt()),
                maxOf(1,(h*ratio).toInt()))
            decoder.allocator=ImageDecoder.ALLOCATOR_SOFTWARE
        }
        try {
            val pixels=IntArray(image.width*image.height)
            image.getPixels(pixels,0,image.width,0,0,image.width,image.height)
            val sumR=DoubleArray(12)
            val sumG=DoubleArray(12)
            val sumB=DoubleArray(12)
            val weights=DoubleArray(12)
            val hsv=FloatArray(3)
            pixels.forEach {pixel->
                if(Color.alpha(pixel)<220)return@forEach
                Color.colorToHSV(pixel,hsv)
                val saturation=hsv[1].toDouble()
                val brightness=hsv[2].toDouble()
                if(saturation<.16||brightness<.12||brightness>.94)return@forEach
                val bucket=((hsv[0]/30f).toInt()).coerceIn(0,11)
                val weight=saturation*saturation*(.4+brightness)
                sumR[bucket]+=Color.red(pixel)*weight
                sumG[bucket]+=Color.green(pixel)*weight
                sumB[bucket]+=Color.blue(pixel)*weight
                weights[bucket]+=weight
            }
            val chosen=weights.indices.maxByOrNull {weights[it]} ?: 0
            if(weights[chosen]<.1)throw IOException(
                "No suitable accent color found. Choose a more colorful photo.")
            val red=(sumR[chosen]/weights[chosen]).toInt().coerceIn(0,255)
            val green=(sumG[chosen]/weights[chosen]).toInt().coerceIn(0,255)
            val blue=(sumB[chosen]/weights[chosen]).toInt().coerceIn(0,255)
            String.format(Locale.ROOT,"#%02X%02X%02X",red,green,blue)
        } finally {image.recycle()}
    }
}
