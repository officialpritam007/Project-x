package com.pritam.liquidchat

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.pritam.liquidchat.ui.LiquidApp

class MainActivity : ComponentActivity() {
    private val model: AppViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(intent?.hasExtra("callId")==true){setShowWhenLocked(true);setTurnScreenOn(true)}
        enableEdgeToEdge()
        intent?.data?.let(model::callback)
        intent?.getStringExtra("conversationId")?.let {model.openNotification(it,intent?.getStringExtra("accountId"))}
        intent?.getStringExtra("callId")?.let {id->intent?.getStringExtra("accountId")?.let {model.calls.openIncomingNotification(id,it,com.pritam.liquidchat.calls.CallAnswerActivity.consume(id,it))}}
        setContent { LiquidApp(model) }
    }
    override fun onStart() {super.onStart();model.repository.foreground.value=true}
    override fun onStop() {model.repository.foreground.value=false;super.onStop()}
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if(intent.hasExtra("callId")){setShowWhenLocked(true);setTurnScreenOn(true)}
        intent.data?.let(model::callback)
        intent.getStringExtra("conversationId")?.let {model.openNotification(it,intent.getStringExtra("accountId"))}
        intent.getStringExtra("callId")?.let {id->intent.getStringExtra("accountId")?.let {model.calls.openIncomingNotification(id,it,com.pritam.liquidchat.calls.CallAnswerActivity.consume(id,it))}}
    }
}
