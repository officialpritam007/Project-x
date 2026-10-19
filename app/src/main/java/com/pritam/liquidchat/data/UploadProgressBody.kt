package com.pritam.liquidchat.data

import okhttp3.RequestBody
import okio.BufferedSink
import okio.ForwardingSink
import okio.buffer

/** Counts actual file bytes written, never treats upload completion as message acceptance. */
internal class UploadProgressBody(private val body:RequestBody,private val progress:(Int)->Unit):RequestBody() {
    override fun contentType()=body.contentType()
    override fun contentLength()=body.contentLength()
    override fun writeTo(sink:BufferedSink) {
        val total=contentLength()
        var written=0L
        var reported=-1
        fun report(value:Int) {if(value!=reported){reported=value;progress(value)}}
        report(0)
        val counting=object:ForwardingSink(sink) {
            override fun write(source:okio.Buffer,byteCount:Long) {
                super.write(source,byteCount)
                written+=byteCount
                if(total>0)report((written*100/total).toInt().coerceIn(0,100))
            }
        }.buffer()
        body.writeTo(counting)
        counting.flush()
        report(100)
    }
}
