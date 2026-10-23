package com.pritam.liquidchat.ui

import android.Manifest
import org.json.JSONArray
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Call summaries are loaded only from the existing authenticated, participant-scoped RPC. */
internal data class CallHistoryRow(
    val id:String,
    val conversation:String,
    val peer:String,
    val name:String,
    val avatar:String,
    val kind:String,
    val outcome:String,
    val created:String,
    val answered:String?,
    val ended:String?,
    val outgoing:Boolean
) {
    val missed get()=outcome=="missed" && !outgoing
    val durationSeconds:Long? get() {
        val start=answered?.let {runCatching {Instant.parse(it)}.getOrNull()} ?: return null
        val end=ended?.let {runCatching {Instant.parse(it)}.getOrNull()} ?: return null
        return Duration.between(start,end).seconds.takeIf {it>=0}
    }
    val direction:String get()=when {
        missed->"Missed"
        outcome=="missed"->"No answer"
        outcome=="declined"->"Declined"
        outgoing->"Outgoing"
        else->"Incoming"
    }
    val timeLabel:String get()=runCatching {
        Instant.parse(created).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd MMM · h:mm a"))
    }.getOrDefault("")
    companion object {
        fun parse(data:JSONArray,account:String):List<CallHistoryRow> = (0 until data.length()).mapNotNull {i->
            runCatching {
                val j=data.getJSONObject(i)
                CallHistoryRow(
                    id=j.getString("id"),
                    conversation=j.getString("conversation_id"),
                    peer=j.getString("peer_id"),
                    name=j.optString("display_name").ifBlank {"Contact"},
                    avatar=j.optString("avatar_id").takeUnless {it=="null"}.orEmpty(),
                    kind=j.optString("kind","audio"),
                    outcome=j.optString("outcome",j.optString("state")),
                    created=j.getString("created_at"),
                    answered=j.optString("answered_at").takeUnless {it.isBlank()||it=="null"},
                    ended=j.optString("ended_at").takeUnless {it.isBlank()||it=="null"},
                    outgoing=j.getString("caller_id")==account
                )
            }.getOrNull()
        }
    }
}

/** Keep the server cursor/count separate from rows that cannot be rendered. */
internal data class CallHistoryPage(val rows:List<CallHistoryRow>,val before:String?,val more:Boolean)
internal fun callHistoryPage(data:JSONArray,account:String)=CallHistoryPage(
    CallHistoryRow.parse(data,account),
    data.optJSONObject(data.length()-1)?.optString("created_at")?.takeIf {it.isNotBlank()},
    data.length()>=50
)

/** In-memory only; never modifies the server page cursor or filtered row history. */
internal fun filterCallHistory(rows:List<CallHistoryRow>,selection:String):List<CallHistoryRow> = when(selection) {
    "Missed"->rows.filter {it.missed}
    "Voice"->rows.filter {it.kind=="audio"}
    "Video"->rows.filter {it.kind=="video"}
    else->rows
}

/** Same runtime microphone/camera permissions as the conversation call buttons. */
internal fun callHistoryCanRedial(callsEnabled:Boolean,kind:String,hasAccount:Boolean,callInProgress:Boolean):Boolean =
    callsEnabled && hasAccount && !callInProgress && kind in listOf("audio","video")

internal fun callHistoryRequiredPermissions(kind:String):Array<String> =
    if(kind=="video") arrayOf(Manifest.permission.RECORD_AUDIO,Manifest.permission.CAMERA)
    else arrayOf(Manifest.permission.RECORD_AUDIO)

internal fun elapsedLabel(seconds:Long):String {
    val hours=seconds/3600
    val minutes=(seconds%3600)/60
    return if(hours>0) "${hours}h ${minutes}m" else if(minutes>0) "${minutes}m ${seconds%60}s" else "${seconds}s"
}

