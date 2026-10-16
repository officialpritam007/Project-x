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
        enableEdgeToEdge()
        intent?.data?.let(model::callback)
        setContent { LiquidApp(model) }
    }
    override fun onStart() {super.onStart();model.repository.foreground.value=true}
    override fun onStop() {model.repository.foreground.value=false;super.onStop()}
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let(model::callback)
    }
}
