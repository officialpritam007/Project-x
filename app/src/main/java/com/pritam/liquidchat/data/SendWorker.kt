package com.pritam.liquidchat.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pritam.liquidchat.LiquidApplication
import kotlinx.coroutines.CancellationException

class SendWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params) {
    override suspend fun doWork():Result {
        val app=applicationContext as LiquidApplication
        app.backend.restore()
        val expected=inputData.getString("account") ?: return Result.failure()
        if(app.backend.session.value?.userId!=expected)return Result.success()
        return try {if(app.repository.drain(expected)) Result.success() else Result.retry()}
        catch(e:CancellationException) {throw e}
        catch(_:Exception) {if(runAttemptCount<5)Result.retry() else Result.failure()}
    }
}
