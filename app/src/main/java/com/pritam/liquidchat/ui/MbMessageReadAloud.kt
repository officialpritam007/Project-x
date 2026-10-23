package com.pritam.liquidchat.ui

import android.content.Context
import android.speech.tts.TextToSpeech

/**
 * Speech is started only after an explicit message action and confirmation.
 * Text is never stored in preferences or logs. The installed Android TTS
 * engine may use a network service; that disclosure is shown before speaking.
 *
 * The owning conversation closes this resource when its account or screen
 * changes so one user's message cannot be spoken in another session.
 */
internal class MbMessageReadAloud(context:Context):TextToSpeech.OnInitListener {
    private var engine:TextToSpeech?=null
    private var ready=false
    private var closed=false
    private var queuedText:String?=null

    init {
        engine=TextToSpeech(context.applicationContext,this)
    }

    override fun onInit(status:Int) {
        if(closed)return
        ready=status==TextToSpeech.SUCCESS
        if(ready)flush() else queuedText=null
    }

    fun speak(value:String) {
        if(closed)return
        // Keep the optional read-aloud action bounded and discard whitespace.
        queuedText=value.trim().take(2000).takeIf {it.isNotBlank()}
        if(ready)flush()
    }

    private fun flush() {
        val text=queuedText ?: return
        queuedText=null
        engine?.speak(text,TextToSpeech.QUEUE_FLUSH,null,"liquid-chat-explicit-read")
    }

    fun stop() {
        queuedText=null
        engine?.stop()
    }

    fun close() {
        if(closed)return
        closed=true
        stop()
        engine?.shutdown()
        engine=null
    }
}
