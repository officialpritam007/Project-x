package com.pritam.liquidchat

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.pritam.liquidchat.calls.CallView
import com.pritam.liquidchat.ui.LiquidApp

/** Only a server-validated, full call overlay may be displayed over the device lock. */
internal fun callWindowMayShowWhenLocked(call:CallView?):Boolean=call?.id!=null&&!call.minimized

class MainActivity : ComponentActivity() {
    private val model: AppViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Intent extras are untrusted on an exported Activity. Observe validated
        // controller state for the whole Activity lifetime, including while stopped,
        // and revoke window access on hangup, error, logout or minimization.
        lifecycleScope.launch {
            model.calls.view.collect {call->
                val overLock=callWindowMayShowWhenLocked(call)
                setShowWhenLocked(overLock)
                setTurnScreenOn(overLock&&call?.unansweredIncoming==true)
            }
        }
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
        intent.data?.let(model::callback)
        intent.getStringExtra("conversationId")?.let {model.openNotification(it,intent.getStringExtra("accountId"))}
        intent.getStringExtra("callId")?.let {id->intent.getStringExtra("accountId")?.let {model.calls.openIncomingNotification(id,it,com.pritam.liquidchat.calls.CallAnswerActivity.consume(id,it))}}
    }
}
