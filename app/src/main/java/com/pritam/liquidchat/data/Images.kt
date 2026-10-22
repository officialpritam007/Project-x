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

    enum class Edit {
        CROP_SQUARE, CROP_PORTRAIT, CROP_WIDE,
        ROTATE_LEFT, ROTATE_RIGHT, FLIP_HORIZONTAL, FLIP_VERTICAL, GRAYSCALE
    }

    /** Normalized crop is independent of device pixel density and zoom. */
    data class CropBounds(val left:Float,val top:Float,val right:Float,val bottom:Float) {
        fun valid():Boolean = left.isFinite()&&top.isFinite()&&right.isFinite()&&bottom.isFinite() &&
            left>=0f&&top>=0f&&right<=1f&&bottom<=1f &&
            right-left>=.03f&&bottom-top>=.03f
    }

    /** Keep edits sandboxed to the same authenticated account's prepared image.
     * The source is never overwritten, which makes Undo and retry lossless.
     */
    suspend fun cropSelection(context:Context,account:String,prepared:Prepared,bounds:CropBounds):Prepared =
        withContext(Dispatchers.IO) {
            require(account.isNotBlank())
            require(bounds.valid()) {"Drag to select a larger crop area."}
            val directory=File(context.filesDir,"media/$account").canonicalFile
            require(prepared.file.isFile && prepared.file.canonicalFile.parentFile==directory) {
                "The photo is not available for this account."
            }
            val output=File(directory,"${UUID.randomUUID()}.jpg")
            var original:Bitmap?=null
            var cropped:Bitmap?=null
            try {
                original=android.graphics.BitmapFactory.decodeFile(prepared.file.absolutePath)
                    ?: throw IOException("Could not decode selected photo.")
                val source=original ?: throw IOException("Photo is unavailable.")
                val left=(bounds.left*source.width).toInt().coerceIn(0,source.width-1)
                val top=(bounds.top*source.height).toInt().coerceIn(0,source.height-1)
                val right=kotlin.math.ceil(bounds.right.toDouble()*source.width).toInt()
                    .coerceIn(left+1,source.width)
                val bottom=kotlin.math.ceil(bounds.bottom.toDouble()*source.height).toInt()
                    .coerceIn(top+1,source.height)
                cropped=Bitmap.createBitmap(source,left,top,right-left,bottom-top)
                val result=cropped ?: throw IOException("Crop failed.")
                output.outputStream().use {
                    check(result.compress(Bitmap.CompressFormat.JPEG,88,it))
                }
                if(output.length()>9_500_000L)output.outputStream().use {
                    check(result.compress(Bitmap.CompressFormat.JPEG,70,it))
                }
                if(output.length() !in 1..9_500_000L)
                    throw IOException("Cropped photo exceeds the upload limit.")
                Prepared(output,result.width,result.height)
            } catch(e:Exception) {
                output.delete()
                throw e
            } finally {
                if(cropped!==original)cropped?.recycle()
                original?.recycle()
            }
        }

    /** User-added photo artwork is rendered into the private local draft, never a public URL. */
    fun normalizedOverlayText(text:String):String =
        text.replace(Regex("\\s+")," ").trim().take(64)

    suspend fun addTextOverlay(context:Context,account:String,prepared:Prepared,text:String):Prepared =
        withContext(Dispatchers.IO) {
            require(account.isNotBlank())
            val label=normalizedOverlayText(text)
            require(label.isNotBlank()) {"Enter text to place on the photo."}
            val directory=File(context.filesDir,"media/$account").canonicalFile
            require(prepared.file.isFile&&prepared.file.canonicalFile.parentFile==directory) {
                "The photo does not belong to this account."
            }
            val output=File(directory,"${UUID.randomUUID()}.jpg")
            var original:Bitmap?=null
            var annotated:Bitmap?=null
            try {
                original=android.graphics.BitmapFactory.decodeFile(prepared.file.absolutePath)
                    ?: throw IOException("Cannot open the selected photo.")
                val source=original ?: throw IOException("Cannot decode the photo.")
                annotated=source.copy(Bitmap.Config.ARGB_8888,true)
                    ?: throw IOException("Cannot create an editable photo.")
                val canvas=android.graphics.Canvas(annotated!!)
                val lineLength=if(annotated!!.width>=650)28 else 18
                val lines=label.chunked(lineLength).take(4)
                val fill=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color=android.graphics.Color.WHITE
                    textAlign=android.graphics.Paint.Align.CENTER
                    typeface=android.graphics.Typeface.create(
                        android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD)
                    textSize=(annotated!!.width*.061f).coerceIn(12f,68f)
                }
                while(lines.any {fill.measureText(it)>annotated!!.width*.90f} &&
                    fill.textSize>12f)fill.textSize-=1f
                val outline=android.graphics.Paint(fill).apply {
                    color=android.graphics.Color.BLACK
                    style=android.graphics.Paint.Style.STROKE
                    strokeWidth=(fill.textSize*.14f).coerceAtLeast(3f)
                    strokeJoin=android.graphics.Paint.Join.ROUND
                }
                val lineHeight=fill.textSize*1.18f
                val firstY=(annotated!!.height*.78f).coerceAtMost(
                    annotated!!.height-lineHeight*lines.size).coerceAtLeast(fill.textSize)
                lines.forEachIndexed {index,line->
                    val y=firstY+index*lineHeight
                    canvas.drawText(line,annotated!!.width/2f,y,outline)
                    canvas.drawText(line,annotated!!.width/2f,y,fill)
                }
                val result=annotated ?: throw IOException("Text overlay failed.")
                output.outputStream().use {
                    check(result.compress(Bitmap.CompressFormat.JPEG,88,it))
                }
                if(output.length()>9_500_000L)output.outputStream().use {
                    check(result.compress(Bitmap.CompressFormat.JPEG,70,it))
                }
                if(output.length() !in 1..9_500_000L)
                    throw IOException("Edited photo exceeds the upload limit.")
                Prepared(output,result.width,result.height)
            } catch(e:Exception) {
                output.delete()
                throw e
            } finally {
                if(annotated!==original)annotated?.recycle()
                original?.recycle()
            }
        }

    data class DrawPoint(val x:Float,val y:Float) {
        fun valid():Boolean=x.isFinite()&&y.isFinite()&&x in 0f..1f&&y in 0f..1f
    }
    data class DrawStroke(val points:List<DrawPoint>,val argb:Int,val widthFraction:Float=.008f) {
        fun valid():Boolean=points.isNotEmpty()&&points.size<=500 &&
            points.all {it.valid()}&&widthFraction.isFinite()&&widthFraction in .001f..0.05f
    }
    fun validDrawing(strokes:List<DrawStroke>):Boolean =
        strokes.isNotEmpty()&&strokes.size<=25&&strokes.all {it.valid()}

    /** Bounded local bitmap drawing; output is a true private JPEG, never a
     * screen-only Compose overlay or an external image-processing API.
     */
    suspend fun drawStrokes(
        context:Context,account:String,prepared:Prepared,strokes:List<DrawStroke>
    ):Prepared=withContext(Dispatchers.IO) {
        require(account.isNotBlank())
        require(validDrawing(strokes)) {"Draw a stroke on the image before applying."}
        val directory=File(context.filesDir,"media/$account").canonicalFile
        require(prepared.file.isFile&&prepared.file.canonicalFile.parentFile==directory) {
            "The photo does not belong to this account."
        }
        val output=File(directory,"${UUID.randomUUID()}.jpg")
        var original:Bitmap?=null
        var canvasImage:Bitmap?=null
        try {
            original=android.graphics.BitmapFactory.decodeFile(prepared.file.absolutePath)
                ?: throw IOException("Cannot open photo for drawing.")
            val source=original ?: throw IOException("Photo unavailable.")
            canvasImage=source.copy(Bitmap.Config.ARGB_8888,true)
                ?: throw IOException("Cannot create a drawing layer.")
            val image=canvasImage ?: throw IOException("Drawing layer unavailable.")
            val canvas=android.graphics.Canvas(image)
            val edge=minOf(image.width,image.height).toFloat()
            strokes.forEach {stroke->
                val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    style=android.graphics.Paint.Style.STROKE
                    color=stroke.argb or (0xFF shl 24)
                    strokeWidth=(edge*stroke.widthFraction).coerceAtLeast(2f)
                    strokeCap=android.graphics.Paint.Cap.ROUND
                    strokeJoin=android.graphics.Paint.Join.ROUND
                }
                val first=stroke.points.first()
                if(stroke.points.size==1) {
                    val fill=android.graphics.Paint(paint).apply {
                        style=android.graphics.Paint.Style.FILL
                    }
                    canvas.drawCircle(first.x*image.width,first.y*image.height,
                        paint.strokeWidth/2f,fill)
                } else {
                    val path=android.graphics.Path().apply {
                        moveTo(first.x*image.width,first.y*image.height)
                        stroke.points.drop(1).forEach {point->
                            lineTo(point.x*image.width,point.y*image.height)
                        }
                    }
                    canvas.drawPath(path,paint)
                }
            }
            output.outputStream().use {
                check(image.compress(Bitmap.CompressFormat.JPEG,88,it))
            }
            if(output.length()>9_500_000L)output.outputStream().use {
                check(image.compress(Bitmap.CompressFormat.JPEG,70,it))
            }
            if(output.length() !in 1..9_500_000L)
                throw IOException("Annotated image exceeds the upload limit.")
            Prepared(output,image.width,image.height)
        } catch(e:Exception) {
            output.delete()
            throw e
        } finally {
            if(canvasImage!==original)canvasImage?.recycle()
            original?.recycle()
        }
    }

    /** Preserve a centered crop using integer aspect arithmetic (no stretching). */
    private fun centeredCrop(source:Bitmap,aspectWidth:Int,aspectHeight:Int):Bitmap {
        require(aspectWidth>0 && aspectHeight>0)
        val wide=source.width.toLong()*aspectHeight>=
            source.height.toLong()*aspectWidth
        val width=if(wide)
            (source.height.toLong()*aspectWidth/aspectHeight).toInt().coerceIn(1,source.width)
            else source.width
        val height=if(wide)source.height
            else (source.width.toLong()*aspectHeight/aspectWidth).toInt().coerceIn(1,source.height)
        return Bitmap.createBitmap(source,(source.width-width)/2,
            (source.height-height)/2,width,height)
    }

    /**
     * Apply a local edit to a previously prepared JPEG without altering its
     * original until the replacement has been fully encoded.
     *
     * This is a real image edit, not a pretend sticker type. The edited photo
     * continues through the same authenticated Supabase media outbox.
     */
    suspend fun edit(context:Context,account:String,prepared:Prepared,edit:Edit):Prepared =
        withContext(Dispatchers.IO) {
            require(account.isNotBlank())
            val directory=File(context.filesDir,"media/$account").canonicalFile
            require(prepared.file.isFile && prepared.file.canonicalFile.parentFile==directory) {
                "The selected photo no longer belongs to this account."
            }
            val output=File(directory,"${UUID.randomUUID()}.jpg")
            var original:Bitmap?=null
            var transformed:Bitmap?=null
            try {
                original=android.graphics.BitmapFactory.decodeFile(prepared.file.absolutePath)
                    ?: throw IOException("Could not open the selected photo.")
                val source=original ?: throw IOException("Could not decode the selected photo.")
                transformed=when(edit) {
                    Edit.CROP_SQUARE -> centeredCrop(source,1,1)
                    Edit.CROP_PORTRAIT -> centeredCrop(source,4,5)
                    Edit.CROP_WIDE -> centeredCrop(source,16,9)
                    Edit.ROTATE_LEFT -> Bitmap.createBitmap(
                        source,0,0,source.width,source.height,
                        android.graphics.Matrix().apply {postRotate(-90f)},true)
                    Edit.ROTATE_RIGHT -> Bitmap.createBitmap(source,0,0,source.width,source.height,
                        android.graphics.Matrix().apply {postRotate(90f)},true)
                    Edit.FLIP_HORIZONTAL -> Bitmap.createBitmap(source,0,0,source.width,source.height,
                        android.graphics.Matrix().apply {postScale(-1f,1f)},true)
                    Edit.FLIP_VERTICAL -> Bitmap.createBitmap(source,0,0,source.width,source.height,
                        android.graphics.Matrix().apply {postScale(1f,-1f)},true)
                    Edit.GRAYSCALE -> {
                        val destination=Bitmap.createBitmap(source.width,source.height,Bitmap.Config.ARGB_8888)
                        val matrix=android.graphics.ColorMatrix().apply {setSaturation(0f)}
                        android.graphics.Canvas(destination).drawBitmap(source,0f,0f,
                            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                colorFilter=android.graphics.ColorMatrixColorFilter(matrix)
                            })
                        destination
                    }
                }
                val result=transformed ?: throw IOException("Image editing failed.")
                output.outputStream().use {stream->
                    check(result.compress(Bitmap.CompressFormat.JPEG,88,stream))
                }
                if(output.length()>9_500_000L)output.outputStream().use {stream->
                    check(result.compress(Bitmap.CompressFormat.JPEG,70,stream))
                }
                if(output.length() !in 1..9_500_000L)throw IOException("Edited photo exceeds upload limit.")
                Prepared(output,result.width,result.height)
            } catch(e:Exception) {
                output.delete()
                throw e
            } finally {
                if(transformed!==original)transformed?.recycle()
                original?.recycle()
            }
        }
    suspend fun prepare(context:Context,uri:Uri,account:String,hd:Boolean=false):Prepared=withContext(Dispatchers.IO) {
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
                val scale=minOf(1.0,(if(hd)3072.0 else 2048.0)/maxOf(w,h))
                decoder.setTargetSize(maxOf(1,(w*scale).toInt()),maxOf(1,(h*scale).toInt()))
                decoder.allocator=ImageDecoder.ALLOCATOR_SOFTWARE
            }
            val w=bitmap.width;val h=bitmap.height
            try {
                compressed.outputStream().use {
                    check(bitmap.compress(Bitmap.CompressFormat.JPEG,if(hd)90 else 85,it))
                }
                if(compressed.length()>9_500_000L)compressed.outputStream().use {
                    check(bitmap.compress(Bitmap.CompressFormat.JPEG,72,it))
                }
                if(compressed.length()>9_500_000L)throw IOException(
                    "Image exceeds the upload limit. Choose a smaller photo.")
            } finally {bitmap.recycle()}
            Prepared(compressed,w,h)
        } catch(e:Exception) {compressed.delete();throw e} finally {original.delete()}
    }
}
