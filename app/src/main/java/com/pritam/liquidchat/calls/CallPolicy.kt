package com.pritam.liquidchat.calls

/** Route IDs are device identities; manual choice survives newly connected headphones. */
internal fun preferredCallDevice(devices:List<Pair<Int,Int>>,manual:Int?,video:Boolean):Int? =
    devices.firstOrNull {it.first==manual}?.first
        ?: devices.firstOrNull {it.second in listOf(7,23,26,27)}?.first
        ?: devices.firstOrNull {it.second in listOf(3,4,11,22)}?.first
        ?: devices.firstOrNull {it.second==if(video)2 else 1}?.first

internal fun invitationStatus(presented:Boolean)=if(presented)"Ringing" else "Calling"

/** Interruption never discards explicit mute/camera intent. */
internal data class CaptureIntent(val muted:Boolean=false,val camera:Boolean=true,val held:Boolean=false) {
    val sendAudio get()=!muted&&!held
    val captureVideo get()=camera&&!held
}

internal fun canAutoHideCallControls(status:String,menuOpen:Boolean,permissionUi:Boolean,accessibility:Boolean)=
    status=="Connected"&&!menuOpen&&!permissionUi&&!accessibility
