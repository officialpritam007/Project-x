package com.pritam.liquidchat

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import com.pritam.liquidchat.calls.CallView
import com.pritam.liquidchat.ui.LiquidApp
import com.pritam.liquidchat.ui.MbAppShortcuts

/** Standard Android window protection; not a replacement for encryption. */
internal fun screenCaptureWindowFlag(preventScreenshots:Boolean):Int =
    if(preventScreenshots)android.view.WindowManager.LayoutParams.FLAG_SECURE else 0

/** Only a server-validated, full call overlay may be displayed over the device lock. */
internal fun callWindowMayShowWhenLocked(call:CallView?,policyAccepted:Boolean=true):Boolean=
    policyAccepted&&call?.id!=null&&!call.minimized

class MainActivity : ComponentActivity() {
    private val model: AppViewModel by viewModels()
    private var shortcutRequest by mutableStateOf<Pair<Int,Int>?>(null)
    private var shortcutSequence=0
    private var pipWindow by mutableStateOf(false)
    private var pipAllowed by mutableStateOf(false)
    private var resumed=false
    private fun pipEligible():Boolean {
        val call=model.calls.view.value
        return !getSystemService(android.app.KeyguardManager::class.java).isKeyguardLocked&&call?.video==true&&call.connectedAt>0&&call.id!=null&&!call.held&&
            model.acceptedPolicy.value&&model.session.value!=null&&com.pritam.liquidchat.calls.ActiveCallService.running&&
            packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }
    private fun updateCallWindow() {
        val call=model.calls.view.value
        val keep=resumed&&call?.video==true&&call.id!=null&&!call.unansweredIncoming
        if(keep)window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if(packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            val end=android.app.PendingIntent.getService(this,7320,
                Intent(this,com.pritam.liquidchat.calls.ActiveCallService::class.java).setAction("com.pritam.liquidchat.calls.END"),
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
            val params=android.app.PictureInPictureParams.Builder().setAspectRatio(android.util.Rational(3,4))
                .setActions(listOf(android.app.RemoteAction(android.graphics.drawable.Icon.createWithResource(this,android.R.drawable.ic_menu_close_clear_cancel),"End call","End call",end)))
            if(android.os.Build.VERSION.SDK_INT>=31)params.setAutoEnterEnabled(pipEligible())
            setPictureInPictureParams(params.build())
        }
    }
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if(pipEligible()) {
            model.calls.setSystemPip(true)
            if(!runCatching {enterPictureInPictureMode(android.app.PictureInPictureParams.Builder().setAspectRatio(android.util.Rational(3,4)).build())}.getOrDefault(false))model.calls.setSystemPip(false)
        }
    }
    override fun onPictureInPictureModeChanged(active:Boolean,configuration:android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(active,configuration)
        pipWindow=active
        model.calls.setSystemPip(active)
        if(!active&&!resumed)model.calls.pauseForBackground()
    }
    override fun onResume() {super.onResume();resumed=true;updateCallWindow()}
    override fun onPause() {resumed=false;updateCallWindow();super.onPause()}


    /** A launcher shortcut is navigation only, never an authentication bypass. */
    private fun captureShortcut(launch:Intent?) {
        val destination=launch?.getIntExtra(MbAppShortcuts.EXTRA_TAB,-1) ?: -1
        if(destination in 0..4) {
            shortcutSequence+=1
            shortcutRequest=destination to shortcutSequence
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Intent extras are untrusted on an exported Activity. Observe validated
        // controller state for the whole Activity lifetime, including while stopped,
        // and revoke window access on hangup, error, logout or minimization.
        lifecycleScope.launch {
            combine(model.calls.view,model.acceptedPolicy,model.session) {call,accepted,session->
                call to (accepted&&session!=null)
            }.collect {(call,accepted)->
                // Match LiquidApp's overlay gate; the consent/auth screens must
                // never inherit over-lock access while the call UI is hidden.
                val overLock=callWindowMayShowWhenLocked(call,accepted)
                pipAllowed=accepted&&call?.id!=null
                updateCallWindow()
                setShowWhenLocked(overLock)
                setTurnScreenOn(overLock&&call?.unansweredIncoming==true)
            }
        }
        lifecycleScope.launch {
            model.display.collect {options ->
                val flag=android.view.WindowManager.LayoutParams.FLAG_SECURE
                if(screenCaptureWindowFlag(options.preventScreenshots)!=0)
                    window.addFlags(flag)
                else window.clearFlags(flag)
            }
        }
        enableEdgeToEdge()
        intent?.data?.let(model::callback)
        intent?.getStringExtra("conversationId")?.let {model.openNotification(it,intent?.getStringExtra("accountId"))}
        intent?.getStringExtra("callId")?.let {id->intent?.getStringExtra("accountId")?.let {model.calls.openIncomingNotification(id,it,com.pritam.liquidchat.calls.CallAnswerActivity.consume(id,it))}}
        captureShortcut(intent)
        setContent {
            if(pipWindow)androidx.compose.material3.Surface(color=androidx.compose.ui.graphics.Color.Black) {
                if(pipAllowed)com.pritam.liquidchat.calls.CallOverlay(model.calls,null)
            } else LiquidApp(model,shortcutRequest)
        }
    }
    override fun onStart() {super.onStart();model.repository.foreground.value=true}
    override fun onStop() {model.calls.pauseForBackground();model.repository.foreground.value=false;super.onStop()}
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureShortcut(intent)
        intent.data?.let(model::callback)
        intent.getStringExtra("conversationId")?.let {model.openNotification(it,intent.getStringExtra("accountId"))}
        intent.getStringExtra("callId")?.let {id->intent.getStringExtra("accountId")?.let {model.calls.openIncomingNotification(id,it,com.pritam.liquidchat.calls.CallAnswerActivity.consume(id,it))}}
    }
}
