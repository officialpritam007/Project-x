package com.pritam.liquidchat.data

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject

/** Events only schedule an authorized database catch-up. Never apply untrusted payloads to Room. */
class RealtimeHints {
    private val client=OkHttpClient.Builder().build()
    fun listen(access:String)=callbackFlow<Unit> {
        val topic="realtime:liquid-session"
        val ref=java.util.concurrent.atomic.AtomicLong(1)
        fun frame(event:String,payload:JSONObject,channel:String=topic)=JSONObject().put("topic",channel).put("event",event).put("payload",payload).put("ref",ref.getAndIncrement().toString()).toString()
        val socket=client.newWebSocket(Request.Builder().url(Backend.URL.replace("https://","wss://")+"/realtime/v1/websocket?apikey="+Backend.PUBLIC_KEY+"&vsn=1.0.0").build(),object:WebSocketListener(){
            override fun onOpen(webSocket:WebSocket,response:Response) {
                val changes=JSONArray()
                listOf("conversations","messages","conversation_state").forEach { changes.put(JSONObject().put("event","*").put("schema","public").put("table",it)) }
                webSocket.send(frame("phx_join",JSONObject().put("access_token",access).put("config",JSONObject().put("postgres_changes",changes).put("broadcast",JSONObject().put("self",false)).put("presence",JSONObject().put("key","")))))
            }
            override fun onMessage(webSocket:WebSocket,text:String) {
                val j=runCatching {JSONObject(text)}.getOrNull() ?: return
                when(j.optString("event")) {
                    "postgres_changes"->trySend(Unit)
                    "phx_reply"->if(j.optString("topic")==topic && j.optJSONObject("payload")?.optString("status")=="ok") trySend(Unit)
                    "phx_error","phx_close"->close()
                }
            }
            override fun onFailure(webSocket:WebSocket,t:Throwable,response:Response?) { close(t) }
            override fun onClosed(webSocket:WebSocket,code:Int,reason:String) {close()}
        })
        val heartbeat=launch {while(isActive) {delay(25000);socket.send(frame("heartbeat",JSONObject(),"phoenix"))}}
        awaitClose {heartbeat.cancel();socket.close(1000,"Session inactive")}
    }
}
