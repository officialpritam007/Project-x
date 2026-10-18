package com.pritam.liquidchat.calls

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.pritam.liquidchat.MainActivity
import com.pritam.liquidchat.R

/**
 * User-started microphone/camera foreground service for an accepted or outgoing WebRTC call.
 * It is NEVER started by FCM or a background worker: Android 14+ restricts while-in-use
 * microphone/camera service starts. The call engine stays in this process; UI/owner teardown
 * ends media immediately. This is not a Telecom integration or a process-death call restore.
 */
class ActiveCallService : Service() {
    companion object {
        private const val CHANNEL="active_calls"
        private const val ID=7319
        private const val START_AUDIO="com.pritam.liquidchat.calls.START_AUDIO"
        private const val START_VIDEO="com.pritam.liquidchat.calls.START_VIDEO"
        private const val END="com.pritam.liquidchat.calls.END"
        private const val STOP="com.pritam.liquidchat.calls.STOP"

        @Volatile var running=false
            private set
        @Volatile var onHangup:(()->Unit)?=null

        fun start(context:Context,video:Boolean) {
            val permission=ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)
            if(permission!=PackageManager.PERMISSION_GRANTED) throw SecurityException("Microphone permission required")
            if(video && ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)
                throw SecurityException("Camera permission required")
            val action=if(video) START_VIDEO else START_AUDIO
            ContextCompat.startForegroundService(context,
                Intent(context,ActiveCallService::class.java).setAction(action))
        }

        fun stop(context:Context) {
            context.stopService(Intent(context,ActiveCallService::class.java))
        }
    }

    override fun onBind(intent:Intent?):IBinder?=null

    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        when(intent?.action) {
            STOP -> { stopSelf(); return START_NOT_STICKY }
            END -> {
                // Internal, explicit immutable PendingIntent only. No exported receivers.
                onHangup?.invoke()
                stopSelf()
                return START_NOT_STICKY
            }
            START_AUDIO,START_VIDEO -> {
                val video=intent.action==START_VIDEO
                val manager=getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(NotificationChannel(
                    CHANNEL,"Active calls",NotificationManager.IMPORTANCE_LOW).apply {
                    description="Ongoing voice and video calls"
                    setSound(null,null)
                })
                val resume=PendingIntent.getActivity(this,ID,
                    Intent(this,MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val end=PendingIntent.getService(this,ID+1,
                    Intent(this,ActiveCallService::class.java).setAction(END),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val notification:Notification=NotificationCompat.Builder(this,CHANNEL)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("Liquid Chat call")
                    .setContentText(if(video)"Video call in progress" else "Voice call in progress")
                    .setContentIntent(resume)
                    .setOngoing(true)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel,"End call",end)
                    .build()
                val serviceType=ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                    (if(video) ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA else 0)
                if(Build.VERSION.SDK_INT>=29) startForeground(ID,notification,serviceType)
                else startForeground(ID,notification)
                running=true
                return START_NOT_STICKY
            }
        }
        stopSelf()
        return START_NOT_STICKY
    }
    override fun onDestroy() {
        running=false
        super.onDestroy()
    }
}
