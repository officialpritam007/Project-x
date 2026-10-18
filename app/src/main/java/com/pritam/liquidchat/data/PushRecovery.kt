package com.pritam.liquidchat.data

/** Local retry budget only; the authorized server snapshot always decides call validity. */
internal object PushRecovery {
    const val CALL_WINDOW_MS = 45_000L
    const val CALL_BACKOFF_MS = 10_000L
    const val MAX_CALL_RETRIES = 3
    const val MAX_REGISTRATION_RETRIES = 6

    /** Retry temporary FCM/network/service failures, but not rejected credentials. */
    fun retryRegistration(error:Exception,attempt:Int):Boolean =
        attempt in 0 until MAX_REGISTRATION_RETRIES &&
            (error !is ApiException || error.status==408 || error.status==429 || error.status>=500)


    fun callExpired(receivedAt:Long,now:Long):Boolean =
        receivedAt<=0 || now<receivedAt || now-receivedAt>=CALL_WINDOW_MS

    fun retryCall(receivedAt:Long,now:Long,attempt:Int):Boolean {
        if(attempt !in 0 until MAX_CALL_RETRIES || callExpired(receivedAt,now))return false
        // WorkManager LINEAR backoff: 10s, 20s, 30s. Do not enqueue a retry
        // whose earliest start already falls outside the invitation window.
        return CALL_BACKOFF_MS*(attempt+1)<CALL_WINDOW_MS-(now-receivedAt)
    }
}

/** Cache pagination/sync races must not override an authorized notification preview. */
internal suspend fun <T:Any> deliverMessagePush(
    sync:suspend ()->Unit,
    preview:suspend ()->T?,
    canPost:()->Boolean,
    post:(T)->Unit
) {
    sync()
    val authorized=preview() ?: return
    // Recheck account/active conversation after both suspending operations.
    if(canPost())post(authorized)
}
