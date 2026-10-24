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
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.util.concurrent.TimeUnit

class LiquidMessagingService:FirebaseMessagingService() {
    override fun onNewToken(token:String) {DeviceRegistrationWork.enqueue(this,token)}
    override fun onMessageReceived(message:RemoteMessage) {
        // Untrusted FCM message data must not control device registration.
        schedule(message.data.filterKeys {it!="token" && it!="registerDevice"})
    }
    private fun schedule(data:Map<String,String>) {
        val input=Data.Builder();data.forEach {(key,value)->input.putString(key,value)}
        // App-owned timestamp, persisted with the work and never accepted from push data.
        if(data["event"]=="incoming_call")input.putLong("callReceivedAt",System.currentTimeMillis())
        val request=OneTimeWorkRequestBuilder<ReceiveWorker>().setInputData(input.build()).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        if(data["event"]=="incoming_call") {
            // FCM grants a brief expedited-work window for high-priority call messages.
            // Still respect WorkManager fallback quotas and validate the call on the server.
            // Pre-Android 12 expedited work requires a foreground worker. Incoming
            // invitations must never start a microphone/camera foreground service.
            if(android.os.Build.VERSION.SDK_INT>=31)
                request.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            request.setBackoffCriteria(BackoffPolicy.LINEAR,PushRecovery.CALL_BACKOFF_MS,TimeUnit.MILLISECONDS)
        }
        data["recipientId"]?.let {request.addTag("account-$it")}
        val recipient=data["recipientId"]
        val messageId=data["messageId"]
        val callId=data["callId"]
        if(data["event"]=="incoming_call" && recipient!=null && callId!=null &&
           runCatching { UUID.fromString(recipient);UUID.fromString(callId) }.isSuccess) {
            WorkManager.getInstance(this).enqueueUniqueWork(
                "call-push-$recipient-$callId",ExistingWorkPolicy.KEEP,request.build())
        } else if(data["event"]!="incoming_call" && recipient!=null && messageId!=null &&
           runCatching { UUID.fromString(recipient);UUID.fromString(messageId) }.isSuccess) {
            // KEEP coalesces duplicate push deliveries while a worker is pending/running.
            // Persistent receipts in show() also cover re-deliveries after work completes.
            WorkManager.getInstance(this).enqueueUniqueWork(
                "message-push-" + recipient + "-" + messageId, ExistingWorkPolicy.KEEP, request.build())
        } else WorkManager.getInstance(this).enqueue(request.build())
    }
}
/** One durable FCM registration chain; a new token or login replaces an obsolete attempt. */
internal object DeviceRegistrationWork {
    fun enqueue(context:Context,token:String?=null) {
        val data=Data.Builder().putBoolean("registerDevice",true)
        token?.takeIf {it.isNotBlank()}?.let {data.putString("token",it)}
        val request=OneTimeWorkRequestBuilder<ReceiveWorker>()
            .setInputData(data.build())
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30L,TimeUnit.SECONDS)
            .addTag("device-registration").build()
        WorkManager.getInstance(context).enqueueUniqueWork("device-registration",ExistingWorkPolicy.REPLACE,request)
    }
}

class ReceiveWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params) {
    override suspend fun doWork():Result {
        val app=applicationContext as LiquidApplication
        app.backend.restore()
        val account=app.backend.session.value?.userId ?: return Result.success()
        if(inputData.getBoolean("registerDevice",false)) {
            return try {
                // The worker can re-fetch a token when no onNewToken callback arrived after login.
                val token=inputData.getString("token")?.takeIf {it.isNotBlank()}
                    ?: com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
                if(token.isBlank())throw java.io.IOException("FCM token unavailable")
                if(app.backend.session.value?.userId!=account)return Result.success()
                app.backend.registerDevice(token)
                if(app.backend.session.value?.userId!=account)return Result.success()
                applicationContext.getSharedPreferences("device",Context.MODE_PRIVATE)
                    .edit().putString("fcm_token",token).apply()
                Result.success()
            } catch(e:CancellationException){throw e}
              catch(e:Exception) {
                if(PushRecovery.retryRegistration(e,runAttemptCount))Result.retry() else Result.failure()
              }
        }
        if(inputData.getString("recipientId")!=account)return Result.success()
        if(inputData.getString("event")=="incoming_call") {
            val receivedAt=inputData.getLong("callReceivedAt",0)
            if(PushRecovery.callExpired(receivedAt,System.currentTimeMillis()))return Result.success()
            val callId=inputData.getString("callId") ?: return Result.success()
            val conversation=inputData.getString("conversationId") ?: return Result.success()
            if(runCatching {UUID.fromString(callId);UUID.fromString(conversation)}.isFailure)return Result.success()
            return try {
                if(!com.pritam.liquidchat.BuildConfig.ENABLE_EXPERIMENTAL_CALLS)return Result.success()
                // Never trust FCM for call state or identity; fetch the authorized snapshot.
                val response=org.json.JSONObject(app.backend.rpc("call_poll",
                    org.json.JSONObject().put("p_call",callId).put("p_after",0)))
                val call=response.optJSONObject("call") ?: return Result.success()
                val expiresAt=java.time.Instant.parse(call.getString("expires_at"))
                val valid=call.optString("id")==callId &&
                    call.optString("conversation_id")==conversation &&
                    call.optString("callee_id")==account &&
                    call.optString("state")=="ringing" &&
                    expiresAt.isAfter(java.time.Instant.now())
                if(!valid)return Result.success()
                if(app.backend.session.value?.userId==account && !app.repository.foreground.value) {
                    val remaining=java.time.Duration.between(java.time.Instant.now(),expiresAt).toMillis()
                    if(remaining>0&&showIncoming(app,callId,account,call.optString("kind")=="video",remaining,app.repository.dao.chat(account,conversation)?.name ?: "Liquid Chat")) {
                        // notify() enqueues work in system_server; acknowledge only after it is posted.
                        repeat(5) {
                            if(app.backend.session.value?.userId!=account||!expiresAt.isAfter(java.time.Instant.now()))return Result.success()
                            if(app.getSystemService(NotificationManager::class.java).activeNotifications.any {it.id==callId.hashCode()}) {
                                app.backend.rpc("call_update",org.json.JSONObject().put("p_call",callId).put("p_payload",org.json.JSONObject().put("action","presented")))
                                return Result.success()
                            }
                            kotlinx.coroutines.delay(200)
                        }
                    }
                }
                Result.success()
            } catch(e:CancellationException){throw e}
              catch(e:Exception){
                  val transient=if(e is ApiException)e.status==408 || e.status==429 || e.status>=500 else e is java.io.IOException
                  if(transient && PushRecovery.retryCall(receivedAt,System.currentTimeMillis(),runAttemptCount))Result.retry()
                  else Result.success()
              }
        }
        val cid=inputData.getString("conversationId") ?: return Result.failure()
        val id=inputData.getString("messageId") ?: return Result.failure()
        if(runCatching {UUID.fromString(cid);UUID.fromString(id)}.isFailure)return Result.failure()
        // Skip expensive repeat sync/RPC if this device already posted this exact message.
        if(MessageNotificationDedupe.wasPosted(applicationContext,account,id))return Result.success()
        return try {
            deliverMessagePush(
                sync={app.repository.sync()},
                preview={
                    // RPC enforces membership, sender, mute, block, deletion and
                    // per-user visibility. An empty/paginated Room cache is not authority.
                    val raw=app.backend.rpc("notification_preview",org.json.JSONObject().put("p_id",id))
                    if(raw=="null")null else org.json.JSONObject(raw).takeIf {it.optString("conversation_id")==cid}
                },
                canPost={app.backend.session.value?.userId==account &&
                    !handledInForegroundChat(app.backend.session.value?.userId,account,
                        app.repository.foreground.value,app.repository.active.value,cid)},
                post={preview->show(app,cid,id,preview.getString("title"),preview.getString("body"),account,preview.optBoolean("previews"))},
                onSuppressed={
                    // This authenticated message has already been handled by the
                    // foreground active chat. Persist the same dedupe receipt as
                    // a visible notification, preventing a later duplicate FCM
                    // from alerting after the user leaves this conversation.
                    // Account switches and any other suppression reason MUST NOT
                    // generate a receipt for a different session.
                    if(handledInForegroundChat(app.backend.session.value?.userId,account,
                            app.repository.foreground.value,app.repository.active.value,cid))
                        MessageNotificationDedupe.claim(applicationContext,account,id)
                }
            )
            Result.success()
        } catch(e:CancellationException){throw e}catch(_:Exception){if(runAttemptCount<5)Result.retry() else Result.failure()}
    }
    private fun showIncoming(context:Context,callId:String,account:String,video:Boolean,remaining:Long,name:String):Boolean {
        if(DeviceAlertPreferences.disableAll(context,account)) {
            MessageNotificationDedupe.claim(context,account,"call-$callId")
            return false
        }
        if(android.os.Build.VERSION.SDK_INT>=33 &&
           ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return false
        val manager=context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("incoming_calls_v2","Incoming calls",NotificationManager.IMPORTANCE_HIGH).apply {
            description="One-to-one voice and video call invitations"
            enableVibration(true)
            setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE),android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE).build())
        })
        if(!manager.areNotificationsEnabled()||manager.getNotificationChannel("incoming_calls_v2")?.importance==NotificationManager.IMPORTANCE_NONE)return false
        val intent=Intent(context,MainActivity::class.java)
            .putExtra("callId",callId).putExtra("accountId",account)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending=PendingIntent.getActivity(context,callId.hashCode(),intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val answer=PendingIntent.getActivity(context,callId.hashCode(),Intent(context,com.pritam.liquidchat.calls.CallAnswerActivity::class.java).putExtra("callId",callId).putExtra("accountId",account),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val decline=PendingIntent.getBroadcast(context,callId.hashCode(),Intent(context,com.pritam.liquidchat.calls.DeclineCallReceiver::class.java).putExtra("callId",callId).putExtra("accountId",account),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        MessageNotificationDedupe.postOnce(context,account,"call-$callId") {
            manager.notify(callId.hashCode(),NotificationCompat.Builder(context,"incoming_calls_v2")
            .setSmallIcon(LiquidNotificationIcon.resource(context))
            .setContentTitle(name)
            .setStyle(NotificationCompat.CallStyle.forIncomingCall(androidx.core.app.Person.Builder().setName(name).setImportant(true).build(),decline,answer).setIsVideo(video))
            .setFullScreenIntent(pending,true)
            .setContentText(if(video)"Video call · Tap to answer" else "Voice call · Tap to answer")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setTimeoutAfter(remaining.coerceAtMost(PushRecovery.CALL_WINDOW_MS))
            .build())
        }
        return true
    }

    private fun show(context:Context,cid:String,id:String,title:String,body:String,account:String,previews:Boolean) {
        if(DeviceAlertPreferences.disableAll(context,account)) {
            // Dedupe suppressed pushes so they do not surface after notifications
            // are re-enabled. Message sync has already been handled above.
            MessageNotificationDedupe.claim(context,account,id)
            return
        }
        if(android.os.Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return
        val manager=context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("messages","Messages",NotificationManager.IMPORTANCE_HIGH))
        // Per-contact sound/vibration is controlled by an opt-in Android channel.
        // The user must configure sound preferences through the system UI.
        val hideSender=ConversationNotificationChannels.hiddenSender(context,account,cid)
        val channel=ConversationNotificationChannels.channelForMessage(context,account,cid,title)
        // Local per-conversation privacy is deliberately stricter than the
        // server's global previews flag. Neither the collapsed notification nor
        // BigTextStyle may contain a suppressed message body.
        val privateChat=ConversationNotificationChannels.hiddenPreview(context,account,cid) || hideSender
        val showBody=notificationBodyAllowed(previews,privateChat)
        val visibleBody=notificationBodyForDisplay(body,previews,privateChat)
        val visibleTitle=notificationTitleForDisplay(title,hideSender)
        val intent=Intent(context,MainActivity::class.java).putExtra("conversationId",cid).putExtra("accountId",account).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending=PendingIntent.getActivity(context,id.hashCode(),intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification=NotificationCompat.Builder(context,channel)
            .setSmallIcon(LiquidNotificationIcon.resource(context)).setContentTitle(visibleTitle).setContentText(visibleBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(visibleBody)).setContentIntent(pending)
            .setAutoCancel(true).setOnlyAlertOnce(true)
            .setVisibility(if(showBody)NotificationCompat.VISIBILITY_PRIVATE else NotificationCompat.VISIBILITY_SECRET)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setSilent(DeviceAlertPreferences.shouldSilence(context,account))
            .setTimeoutAfter(if(DeviceAlertPreferences.autoHideMessages(context,account))
                5_000L else 0L)
            .build()
        // Commit before notifying: notifications dismissed by the user must never alert again.
        MessageNotificationDedupe.postOnce(context,account,id) {
            manager.notify(id.hashCode(),notification)
        }
    }
}
