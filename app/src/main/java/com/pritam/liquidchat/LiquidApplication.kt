package com.pritam.liquidchat

import android.app.Application
import com.pritam.liquidchat.data.Backend
import com.pritam.liquidchat.data.SessionStore
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.tasks.await

class LiquidApplication : Application() {
    private val scope=kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob()+kotlinx.coroutines.Dispatchers.IO)
    override fun onCreate() {
        super.onCreate()
        if(BuildConfig.FCM_APP_ID.isNotBlank()&&BuildConfig.FCM_PROJECT_ID.isNotBlank()&&BuildConfig.FCM_API_KEY.isNotBlank()&&BuildConfig.FCM_SENDER_ID.isNotBlank()) {
            if(com.google.firebase.FirebaseApp.getApps(this).isEmpty()) com.google.firebase.FirebaseApp.initializeApp(this,com.google.firebase.FirebaseOptions.Builder().setApplicationId(BuildConfig.FCM_APP_ID).setProjectId(BuildConfig.FCM_PROJECT_ID).setApiKey(BuildConfig.FCM_API_KEY).setGcmSenderId(BuildConfig.FCM_SENDER_ID).build())
            scope.launch {
                backend.restore()
                backend.session.map {it?.userId}.distinctUntilChanged().collectLatest {id->
                    if(id!=null) runCatching {val token=com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await();getSharedPreferences("device",MODE_PRIVATE).edit().putString("fcm_token",token).apply();backend.registerDevice(token)}
                }
            }
        }
    }
    val database by lazy { androidx.room.Room.databaseBuilder(this,com.pritam.liquidchat.data.LocalDatabase::class.java,"liquid-chat-v1.db").addMigrations(com.pritam.liquidchat.data.Migration1To2,com.pritam.liquidchat.data.Migration2To3,com.pritam.liquidchat.data.Migration3To4).build() }
    val repository by lazy { com.pritam.liquidchat.data.ChatRepository(this,backend,database) }
    val backend by lazy { Backend(SessionStore(this)) }
}
