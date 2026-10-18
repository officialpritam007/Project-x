package com.pritam.liquidchat.calls

import android.app.NotificationManager
import android.content.*
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.work.*
import com.pritam.liquidchat.LiquidApplication
import com.pritam.liquidchat.MainActivity
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import java.util.UUID

/** Only the system's explicit PendingIntent can enter this non-exported activity. */
class CallAnswerActivity:ComponentActivity() {
    companion object {
        private var pending:Pair<String,String>?=null
        fun consume(id:String,account:String):Boolean {
            val matches=pending==(id to account)
            if(matches)pending=null
            return matches
        }
    }
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        val id=intent.getStringExtra("callId")
        val account=intent.getStringExtra("accountId")
        if(id!=null&&account!=null) {
            pending=id to account
            startActivity(Intent(this,MainActivity::class.java).putExtra("callId",id).putExtra("accountId",account)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        }
        finish()
    }
}
class DeclineCallReceiver:BroadcastReceiver() {
    override fun onReceive(context:Context,intent:Intent) {
        val id=intent.getStringExtra("callId") ?: return
        val account=intent.getStringExtra("accountId") ?: return
        if(runCatching {UUID.fromString(id);UUID.fromString(account)}.isFailure)return
        context.getSystemService(NotificationManager::class.java).cancel(id.hashCode())
        WorkManager.getInstance(context).enqueueUniqueWork("decline-$id",ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<DeclineCallWorker>().setInputData(workDataOf("callId" to id,"accountId" to account))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).addTag("account-$account").build())
    }
}
class DeclineCallWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params) {
    override suspend fun doWork():Result {
        val backend=(applicationContext as LiquidApplication).backend
        backend.restore()
        val id=inputData.getString("callId") ?: return Result.success()
        val account=inputData.getString("accountId") ?: return Result.success()
        if(backend.session.value?.userId!=account)return Result.success()
        return try {
            backend.rpc("call_respond",JSONObject().put("p_call",id).put("p_accept",false))
            Result.success()
        } catch(e:CancellationException){throw e}
        catch(e:Exception){if(e is com.pritam.liquidchat.data.ApiException&&e.status in listOf(400,401,403,404))Result.success() else if(runAttemptCount<2)Result.retry() else Result.failure()}
    }
}
