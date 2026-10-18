package com.pritam.liquidchat.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pritam.liquidchat.LiquidApplication
import com.pritam.liquidchat.MainActivity
import com.pritam.liquidchat.R
import kotlinx.coroutines.CancellationException
import java.util.UUID

class LiquidMessagingService:FirebaseMessagingService() {
    override fun onNewToken(token:String) {schedule(mapOf("token" to token))}
    override fun onMessageReceived(message:RemoteMessage) {schedule(message.data)}
    private fun schedule(data:Map<String,String>) {
        val input=Data.Builder();data.forEach {(key,value)->input.putString(key,value)}
        data["token"]?.let {getSharedPreferences("device",Context.MODE_PRIVATE).edit().putString("fcm_token",it).apply()}
        val request=OneTimeWorkRequestBuilder<ReceiveWorker>().setInputData(input.build()).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        if(data.containsKey("token"))request.addTag("device-registration")
        data["recipientId"]?.let {request.addTag("account-$it")}
        WorkManager.getInstance(this).enqueue(request.build())
    }
}
class ReceiveWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params) {
    override suspend fun doWork():Result {
        val app=applicationContext as LiquidApplication
        app.backend.restore()
        val account=app.backend.session.value?.userId ?: return Result.success()
        inputData.getString("token")?.let {token->
            return try {app.backend.registerDevice(token);Result.success()}catch(e:CancellationException){throw e}catch(_:Exception){if(runAttemptCount<5)Result.retry() else Result.failure()}
        }
        if(inputData.getString("recipientId")!=account)return Result.success()
        if(inputData.getString("event")=="incoming_call") {
            val callId=inputData.getString("callId") ?: return Result.success()
            val conversation=inputData.getString("conversationId") ?: return Result.success()
            if(runCatching {UUID.fromString(callId);UUID.fromString(conversation)}.isFailure)return Result.success()
            return try {
                if(!com.pritam.liquidchat.BuildConfig.ENABLE_EXPERIMENTAL_CALLS)return Result.success()
                // Never trust FCM for call state or identity; fetch the authorized snapshot.
                val response=org.json.JSONObject(app.backend.rpc("call_poll",
                    org.json.JSONObject().put("p_call",callId).put("p_after",0)))
                val call=response.optJSONObject("call") ?: return Result.success()
                val valid=call.optString("id")==callId &&
                    call.optString("conversation_id")==conversation &&
                    call.optString("callee_id")==account &&
                    call.optString("state")=="ringing" &&
                    java.time.Instant.parse(call.getString("expires_at")).isAfter(java.time.Instant.now())
                if(!valid)return Result.success()
                if(app.backend.session.value?.userId==account && !app.repository.foreground.value) {
                    showIncoming(app,callId,conversation,account,call.optString("kind")=="video")
                }
                Result.success()
            } catch(e:CancellationException){throw e}
              catch(_:Exception){Result.success()} // Expired/offline invitations must not retry indefinitely.
        }
        val cid=inputData.getString("conversationId") ?: return Result.failure()
        val id=inputData.getString("messageId") ?: return Result.failure()
        if(runCatching {UUID.fromString(cid);UUID.fromString(id)}.isFailure)return Result.failure()
        return try {
            app.repository.sync()
            val message=app.repository.dao.message(account,id) ?: return Result.success()
            val chat=app.repository.dao.chat(account,cid) ?: return Result.success()
            val raw=app.backend.rpc("notification_preview",org.json.JSONObject().put("p_id",id))
            if(raw=="null")return Result.success()
            val preview=org.json.JSONObject(raw)
            if(preview.optString("conversation_id")!=cid||message.conversation!=cid||message.sender==account)return Result.success()
            if(app.backend.session.value?.userId==account && !chat.muted && message.deletedAt.isEmpty() && message.seq>chat.deletedSeq && !(app.repository.foreground.value&&app.repository.active.value==cid)) {
                show(app,cid,id,preview.getString("title"),preview.getString("body"),account,preview.optBoolean("previews"))
            }
            Result.success()
        } catch(e:CancellationException){throw e}catch(_:Exception){if(runAttemptCount<5)Result.retry() else Result.failure()}
    }
    private fun showIncoming(context:Context,callId:String,cid:String,account:String,video:Boolean) {
        if(android.os.Build.VERSION.SDK_INT>=33 &&
           ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return
        val manager=context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("calls","Incoming calls",NotificationManager.IMPORTANCE_HIGH).apply {
            description="One-to-one voice and video call invitations"
        })
        val intent=Intent(context,MainActivity::class.java)
            .putExtra("callId",callId).putExtra("accountId",account)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending=PendingIntent.getActivity(context,callId.hashCode(),intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(callId.hashCode(),NotificationCompat.Builder(context,"calls")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Incoming Liquid Chat call")
            .setContentText(if(video)"Video call · Tap to answer" else "Voice call · Tap to answer")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setTimeoutAfter(45000L)
            .build())
    }

    private fun show(context:Context,cid:String,id:String,title:String,body:String,account:String,previews:Boolean) {
        if(android.os.Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return
        val manager=context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("messages","Messages",NotificationManager.IMPORTANCE_HIGH))
        val intent=Intent(context,MainActivity::class.java).putExtra("conversationId",cid).putExtra("accountId",account).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending=PendingIntent.getActivity(context,id.hashCode(),intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(id.hashCode(),NotificationCompat.Builder(context,"messages").setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(body).setStyle(NotificationCompat.BigTextStyle().bigText(body)).setContentIntent(pending).setAutoCancel(true).setOnlyAlertOnce(true).setVisibility(if(previews)NotificationCompat.VISIBILITY_PRIVATE else NotificationCompat.VISIBILITY_SECRET).setCategory(NotificationCompat.CATEGORY_MESSAGE).build())
    }
}
