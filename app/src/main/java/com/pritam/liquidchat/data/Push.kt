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
            return try {app.backend.rpc("register_device",org.json.JSONObject().put("p_token",token));Result.success()}catch(e:CancellationException){throw e}catch(_:Exception){if(runAttemptCount<5)Result.retry() else Result.failure()}
        }
        if(inputData.getString("recipientId")!=account)return Result.success()
        val cid=inputData.getString("conversationId") ?: return Result.failure()
        val id=inputData.getString("messageId") ?: return Result.failure()
        if(runCatching {UUID.fromString(cid);UUID.fromString(id)}.isFailure)return Result.failure()
        return try {
            app.repository.sync()
            val message=app.repository.dao.message(account,id) ?: return Result.success()
            val chat=app.repository.dao.chat(account,cid) ?: return Result.success()
            if(app.backend.session.value?.userId==account && !chat.muted && message.seq>chat.deletedSeq && !(app.repository.foreground.value&&app.repository.active.value==cid)) {
                show(app,cid,id,inputData.getString("title") ?: "Liquid Chat",inputData.getString("body") ?: "New message")
            }
            Result.success()
        } catch(e:CancellationException){throw e}catch(_:Exception){if(runAttemptCount<5)Result.retry() else Result.failure()}
    }
    private fun show(context:Context,cid:String,id:String,title:String,body:String) {
        if(android.os.Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return
        val manager=context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("messages","Messages",NotificationManager.IMPORTANCE_HIGH))
        val intent=Intent(context,MainActivity::class.java).putExtra("conversationId",cid).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending=PendingIntent.getActivity(context,id.hashCode(),intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(id.hashCode(),NotificationCompat.Builder(context,"messages").setSmallIcon(R.drawable.ic_liquid).setContentTitle(title).setContentText(body).setStyle(NotificationCompat.BigTextStyle().bigText(body)).setContentIntent(pending).setAutoCancel(true).setOnlyAlertOnce(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setCategory(NotificationCompat.CATEGORY_MESSAGE).build())
    }
}
