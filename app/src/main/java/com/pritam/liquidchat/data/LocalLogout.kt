package com.pritam.liquidchat.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/** Remote revocation can fail offline; local erasure must still finish. */
internal suspend fun localLogout(
    revokeRemote:suspend ()->Unit,
    clearLocal:suspend ()->Unit
):Boolean {
    return try {
        revokeRemote()
        true
    } catch(e:CancellationException) {
        throw e
    } catch(_:Exception) {
        false
    } finally {
        withContext(NonCancellable) {clearLocal()}
    }
}
