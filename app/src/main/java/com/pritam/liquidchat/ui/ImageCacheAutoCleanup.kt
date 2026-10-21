package com.pritam.liquidchat.ui

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Optional periodic cleanup of Coil's *temporary image cache only*.
 *
 * No database tables, credentials, drafts, pending messages, personal wallpaper
 * files, downloaded voice recordings or Supabase/Cloudinary media are touched.
 * WorkManager carries the user's selection across app process restarts/reboots;
 * Android may defer background work, so this is not an exact clock-time promise.
 */
internal object ImageCacheAutoCleanup {
    private const val PREFERENCES="liquid_chat_cache_maintenance_v1"
    private const val KEY="period"
    private const val JOB="liquid_chat_image_cache_maintenance"
    const val OFF="Off"
    const val DAILY="Daily"
    const val WEEKLY="Weekly"
    val choices=listOf(OFF,DAILY,WEEKLY)

    fun preference(context:Context):String =
        context.getSharedPreferences(PREFERENCES,Context.MODE_PRIVATE)
            .getString(KEY,OFF)?.takeIf {it in choices} ?: OFF

    fun update(context:Context,choice:String) {
        require(choice in choices)
        val manager=WorkManager.getInstance(context.applicationContext)
        if(choice==OFF) {
            manager.cancelUniqueWork(JOB)
        } else {
            val days=if(choice==DAILY)1L else 7L
            val request=PeriodicWorkRequestBuilder<ImageCacheCleanupWorker>(
                days,TimeUnit.DAYS
            ).addTag("cache-images-only").build()
            manager.enqueueUniquePeriodicWork(JOB,ExistingPeriodicWorkPolicy.REPLACE,request)
        }
        context.getSharedPreferences(PREFERENCES,Context.MODE_PRIVATE)
            .edit().putString(KEY,choice).apply()
    }
}

/** Run off the UI thread and ask WorkManager to retry transient storage failures. */
internal class ImageCacheCleanupWorker(
    appContext:Context,params:WorkerParameters
):CoroutineWorker(appContext,params) {
    override suspend fun doWork():Result = withContext(Dispatchers.IO) {
        runCatching {
            val imageLoader=coil.Coil.imageLoader(applicationContext)
            imageLoader.diskCache?.clear()
            imageLoader.memoryCache?.clear()
            Result.success()
        }.getOrElse {Result.retry()}
    }
}
