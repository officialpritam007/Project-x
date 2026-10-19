package com.pritam.liquidchat.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pritam.liquidchat.LiquidApplication
import kotlinx.coroutines.CancellationException

/**
 * At-least-once job execution is safe: the database moves a scheduled UUID into
 * the ordinary idempotent message outbox in one transaction. Never send for a
 * different authenticated account, even if WorkManager survived process death.
 */
class ScheduledTextWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params) {
    override suspend fun doWork():Result {
        val expected=inputData.getString("account") ?: return Result.failure()
        val scheduledId=inputData.getString("scheduledId") ?: return Result.failure()
        val app=applicationContext as LiquidApplication
        return try {
            app.backend.restore()
            if(app.backend.session.value?.userId!=expected) return Result.success()
            app.repository.dispatchScheduledText(expected,scheduledId)
            Result.success()
        } catch(e:CancellationException) {throw e}
        catch(_:Exception) {if(runAttemptCount<5)Result.retry() else Result.failure()}
    }
}
