package com.pritam.liquidchat.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Rasterize a text-only story locally as a JPEG so the existing authenticated
 * media upload, 24-hour expiry, audience and block policies apply unchanged.
 */
internal object TextStory {
    val palettes=listOf("Indigo","Emerald","Rose","Midnight")
    private val backgrounds=mapOf(
        "Indigo" to Color.rgb(39,43,103),
        "Emerald" to Color.rgb(16,96,81),
        "Rose" to Color.rgb(126,45,77),
        "Midnight" to Color.rgb(19,34,51)
    )

    suspend fun prepare(context:Context,account:String,message:String,palette:String):Images.Prepared =
        withContext(Dispatchers.IO) {
            require(account.isNotBlank()) {"Sign in first."}
            val text=message.trim()
            require(text.isNotEmpty() && text.length<=280) {"Text stories allow up to 280 characters."}
            require(palette in palettes) {"Unknown story background."}
            val directory=File(context.filesDir,"media/$account").apply {mkdirs()}
            val file=File(directory,"text-story-${UUID.randomUUID()}.jpg")
            val bitmap=Bitmap.createBitmap(720,1280,Bitmap.Config.ARGB_8888)
            try {
                val canvas=Canvas(bitmap)
                canvas.drawColor(backgrounds.getValue(palette))
                val paint=TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color=Color.WHITE
                    textSize=52f
                    typeface=Typeface.create(Typeface.DEFAULT,Typeface.BOLD)
                    textAlign=Paint.Align.LEFT
                }
                val contentWidth=608
                val layout=StaticLayout.Builder.obtain(text,0,text.length,paint,contentWidth)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setIncludePad(false)
                    .setLineSpacing(9f,1f)
                    .build()
                require(layout.height<=1000) {"Story text does not fit. Shorten it."}
                canvas.save()
                canvas.translate((720-contentWidth)/2f,(1280-layout.height)/2f)
                layout.draw(canvas)
                canvas.restore()
                file.outputStream().use {
                    if(!bitmap.compress(Bitmap.CompressFormat.JPEG,88,it))
                        throw IOException("Could not create story image.")
                }
                require(file.length() in 100L..9_500_000L) {"Story image exceeds upload limits."}
                Images.Prepared(file,720,1280)
            } catch(e:Exception) {file.delete();throw e}
            finally {bitmap.recycle()}
        }
}
