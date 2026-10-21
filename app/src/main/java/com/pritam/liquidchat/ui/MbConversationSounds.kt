package com.pritam.liquidchat.ui

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pritam.liquidchat.AppViewModel
import com.pritam.liquidchat.data.Message
import java.time.Instant

/** Device-only foreground cues; FCM/notification channels remain authoritative. */
internal object MbConversationSounds {
    private const val PREFS="liquid_foreground_conversation_sounds_v1"
    fun enabled(context:Context,incoming:Boolean):Boolean =
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
            .getBoolean(if(incoming)"incoming" else "outgoing_text",false)
    fun setEnabled(context:Context,incoming:Boolean,enabled:Boolean) {
        check(context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
            .putBoolean(if(incoming)"incoming" else "outgoing_text",enabled).commit()) {
            "Could not save conversation sound preference."
        }
    }
}

/** Sequence and fresh server time suppress duplicate/backfill alerts on chat reopen. */
internal class IncomingConversationCueGate {
    private var armed=false
    private var maximumSequence=0L
    private var armedAt=0L

    fun observe(messages:List<Message>,account:String,conversation:String,
                active:Boolean,ready:Boolean,chatSequence:Long,now:Long):Boolean {
        val visible=messages.filter {it.account==account && it.conversation==conversation}
        val newest=visible.maxOfOrNull {it.seq} ?: 0L
        if(!active || !ready) {
            armed=false
            maximumSequence=maxOf(maximumSequence,newest)
            return false
        }
        if(!armed) {
            armed=true
            armedAt=now
            maximumSequence=maxOf(maximumSequence,newest,chatSequence)
            return false
        }
        val arrived=visible.any {message->
            message.seq>maximumSequence && message.sender!=account &&
                message.status=="SENT" && message.deletedAt.isEmpty() &&
                runCatching {Instant.parse(message.serverTime).toEpochMilli()}
                    .getOrNull()?.let {it>=armedAt && it<=now+5000L && now-it<=30000L}==true
        }
        maximumSequence=maxOf(maximumSequence,newest)
        return arrived
    }
}

/** Do not play during calls, DND, silent/vibrate mode, background or music. */
internal class ForegroundConversationCue(private val context:Context) {
    private val handler=Handler(Looper.getMainLooper())
    private var tone:ToneGenerator?=null
    private var lastCueAt:Long?=null
    private val release=Runnable {stop()}

    fun play(incoming:Boolean,allowed:Boolean) {
        if(!allowed || !MbConversationSounds.enabled(context,incoming))return
        val now=SystemClock.elapsedRealtime()
        if(lastCueAt?.let {now-it<450L}==true)return
        val permitted=runCatching {
            val audio=context.getSystemService(AudioManager::class.java)
            val alerts=context.getSystemService(NotificationManager::class.java)
            audio.ringerMode==AudioManager.RINGER_MODE_NORMAL &&
                audio.mode==AudioManager.MODE_NORMAL && !audio.isMusicActive &&
                audio.getStreamVolume(AudioManager.STREAM_NOTIFICATION)>0 &&
                alerts.currentInterruptionFilter==NotificationManager.INTERRUPTION_FILTER_ALL
        }.getOrDefault(false)
        if(!permitted)return
        stop()
        runCatching {
            val player=ToneGenerator(AudioManager.STREAM_NOTIFICATION,22)
            tone=player
            if(player.startTone(if(incoming)ToneGenerator.TONE_PROP_BEEP
                else ToneGenerator.TONE_PROP_ACK,65)) {
                lastCueAt=now
                handler.postDelayed(release,110L)
            } else stop()
        }.onFailure {stop()}
    }

    fun stop() {
        handler.removeCallbacks(release)
        tone?.let {player->
            runCatching {player.stopTone()}
            runCatching {player.release()}
        }
        tone=null
    }
}

/** Returned callback signals a manual text was queued; it never claims delivery. */
@Composable
internal fun rememberConversationSendCue(
    model:AppViewModel,conversation:String,account:String?,messages:List<Message>,
    ready:Boolean,chatSequence:Long,muted:Boolean,foreground:Boolean
):()->Unit {
    val context=LocalContext.current.applicationContext
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    val lifecycleState by lifecycle.currentStateFlow.collectAsState()
    val currentMuted=rememberUpdatedState(muted)
    val player=remember(context,account,conversation) {ForegroundConversationCue(context)}
    val gate=remember(account,conversation) {IncomingConversationCueGate()}
    fun allowed():Boolean=account!=null && model.session.value?.userId==account &&
        model.repository.active.value==conversation && model.repository.foreground.value &&
        lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
        !currentMuted.value && model.calls.view.value==null
    DisposableEffect(player,lifecycle) {
        val observer=LifecycleEventObserver {_,event->
            if(event==Lifecycle.Event.ON_PAUSE || event==Lifecycle.Event.ON_STOP)player.stop()
        }
        lifecycle.addObserver(observer)
        onDispose {lifecycle.removeObserver(observer);player.stop()}
    }
    LaunchedEffect(messages,ready,foreground,lifecycleState,account,conversation) {
        if(account!=null) {
            val active=foreground && lifecycleState.isAtLeast(Lifecycle.State.RESUMED) &&
                model.repository.active.value==conversation && model.session.value?.userId==account
            if(gate.observe(messages,account,conversation,active,ready,chatSequence,
                    System.currentTimeMillis()))player.play(true,allowed())
            if(!active)player.stop()
        }
    }
    return {player.play(false,allowed())}
}

@Composable internal fun MbConversationSoundSettings() {
    val context=LocalContext.current
    var incoming by remember {mutableStateOf(MbConversationSounds.enabled(context,true))}
    var outgoing by remember {mutableStateOf(MbConversationSounds.enabled(context,false))}
    var error by remember {mutableStateOf<String?>(null)}
    fun update(isIncoming:Boolean,value:Boolean) {
        runCatching {MbConversationSounds.setEnabled(context,isIncoming,value)}
            .onSuccess {if(isIncoming)incoming=value else outgoing=value;error=null}
            .onFailure {error=it.message ?: "Could not save sound preference."}
    }
    GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
        Text("Conversation sounds",style=MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text("Incoming message cue",Modifier.weight(1f))
            Switch(checked=incoming,onCheckedChange={update(true,it)})
        }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text("Outgoing text cue",Modifier.weight(1f))
            Switch(checked=outgoing,onCheckedChange={update(false,it)})
        }
        Text("On this phone only. Muted chats, active calls, background, silent/vibrate mode and Do Not Disturb remain silent.",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Text("A send cue means text reached the local queue, not that it was delivered.",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        error?.let {Text(it,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)}
    }
}
