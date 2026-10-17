package com.pritam.liquidchat.data

import android.content.Context
import android.media.MediaRecorder
import android.os.SystemClock
import androidx.compose.runtime.*
import java.io.File
import java.util.UUID

/** Foreground-only capture. Ownership of a finished file transfers to the durable outbox. */
@Stable class VoiceRecorder(private val context:Context,private val account:String) {
    var recording by mutableStateOf(false);private set
    var locked by mutableStateOf(false);private set
    var elapsed by mutableIntStateOf(0);private set
    private var recorder:MediaRecorder?=null
    private var file:File?=null
    private var started=0L
    @Suppress("DEPRECATION") fun start(lock:Boolean=false) {
        if(recording)return
        val output=File(context.filesDir,"media/$account/${UUID.randomUUID()}.m4a").apply {parentFile!!.mkdirs()}
        val media=if(android.os.Build.VERSION.SDK_INT>=31)MediaRecorder(context) else MediaRecorder()
        try {
            media.setAudioSource(MediaRecorder.AudioSource.MIC)
            media.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            media.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            media.setAudioSamplingRate(44100);media.setAudioEncodingBitRate(64000)
            media.setMaxDuration(120000);media.setMaxFileSize(2097152)
            media.setOutputFile(output.absolutePath);media.prepare();media.start()
            file=output;recorder=media;started=SystemClock.elapsedRealtime();elapsed=0;locked=lock;recording=true
        } catch(e:Exception) {media.release();output.delete();throw e}
    }
    fun lock() {locked=true}
    fun tick() {if(recording)elapsed=(SystemClock.elapsedRealtime()-started).toInt().coerceAtMost(120000)}
    fun finish():Pair<File,Int>? {
        if(!recording)return null
        tick();val duration=elapsed;val output=file
        val stopped=runCatching {recorder?.stop()}.isSuccess
        recorder?.release();recorder=null;file=null;recording=false;locked=false
        if(!stopped||duration<500||output==null||output.length() !in 1..2097152) {output?.delete();return null}
        return output to duration
    }
    fun cancel() {runCatching {recorder?.stop()};recorder?.release();recorder=null;file?.delete();file=null;recording=false;locked=false;elapsed=0}
}
