package com.pritam.liquidchat.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/** Every HTTP request is intercepted; no provider credentials or HTTP writes. */
class BackendSessionTest {
    private fun session(access:String,expires:Long)=JSONObject()
        .put("user",JSONObject().put("id","session-fixture").put("email","fixture@example.invalid"))
        .put("access_token",access).put("refresh_token","fixture-refresh").put("expires_at",expires).toString()
    private fun response(request:Request,status:Int,body:String)=Response.Builder()
        .request(request).protocol(Protocol.HTTP_1_1).code(status).message(if(status==200)"OK" else "Unauthorized")
        .body(body.toResponseBody("application/json".toMediaType())).build()

    private suspend fun exercise(forceUnauthorized:Boolean) {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val name=if(forceUnauthorized)"session-401-fixture" else "session-expired-fixture"
        val store=SessionStore(context,name)
        val refreshes=AtomicInteger()
        val accepted=AtomicInteger()
        val http=OkHttpClient.Builder().addInterceptor {chain->
            val request=chain.request()
            if(request.url.encodedPath=="/auth/v1/token") {
                assertEquals("refresh_token",request.url.queryParameter("grant_type"))
                refreshes.incrementAndGet()
                response(request,200,session("fixture-renewed-access",System.currentTimeMillis()/1000+3600))
            } else if(request.url.encodedPath=="/rest/v1/fixture") {
                if(request.header("Authorization")=="Bearer fixture-renewed-access") {
                    accepted.incrementAndGet();response(request,200,"[]")
                } else response(request,401,"{\"code\":\"invalid_jwt\"}")
            } else error("Unexpected fixture request")
        }.build()
        val backend=Backend(store,http)
        try {
            store.write("session",session("fixture-old-access",System.currentTimeMillis()/1000+if(forceUnauthorized)3600 else -60))
            backend.restore()
            coroutineScope {
                (1..10).map {async(Dispatchers.Default) {backend.request("/rest/v1/fixture")}}.awaitAll().forEach {assertEquals("[]",it)}
            }
            assertEquals(1,refreshes.get())
            assertEquals(10,accepted.get())
            assertEquals("fixture-renewed-access",backend.session.value?.access)
        } finally {
            backend.forgetLocal()
            context.getSharedPreferences(name,Context.MODE_PRIVATE).edit().clear().commit()
        }
    }
    @Test fun expiredTokenRefreshIsSerializedAcrossConcurrentRequests()=runBlocking {exercise(false)}
    @Test fun simultaneous401ResponsesShareOneRefreshAndRecover()=runBlocking {exercise(true)}
}
