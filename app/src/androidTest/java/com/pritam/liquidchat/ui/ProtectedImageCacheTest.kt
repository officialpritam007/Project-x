package com.pritam.liquidchat.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.SystemClock
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import coil.Coil
import coil.ImageLoader
import com.pritam.liquidchat.data.Backend
import com.pritam.liquidchat.data.SessionStore
import com.pritam.liquidchat.data.imageUrl
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicInteger

/** All HTTP is intercepted; these tests never access live accounts or media. */
class ProtectedImageCacheTest {
    @get:Rule val compose=createComposeRule()
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private val signs=AtomicInteger()
    private val downloads=AtomicInteger()
    private var ttl=60
    private var revokeAfterFirst=false
    private var rejectFirst=false
    private var rejectEveryDownload=false
    private fun response(request:Request,body:ResponseBody,status:Int=200)=Response.Builder()
        .request(request).protocol(Protocol.HTTP_1_1).code(status).message("fixture").body(body).build()
    private fun json(request:Request,value:JSONObject)=response(request,value.toString().toResponseBody("application/json".toMediaType()))
    private fun session()=JSONObject().put("user",JSONObject().put("id","image-fixture"))
        .put("access_token","fixture-access").put("refresh_token","fixture-refresh")
        .put("expires_at",System.currentTimeMillis()/1000+3600)
    private fun backend():Backend {
        val store=SessionStore(context,"protected-image-fixture")
        val http=OkHttpClient.Builder().addInterceptor {chain->
            val request=chain.request()
            when(request.url.encodedPath) {
                "/auth/v1/token"->json(request,session())
                "/functions/v1/media"->{
                    val n=signs.incrementAndGet()
                    if(revokeAfterFirst&&n>1)response(request,"{}".toResponseBody("application/json".toMediaType()),403)
                    else json(request,JSONObject().put("url","https://media.example.invalid/$n.png").put("expires_in",ttl))
                }
                "/rest/v1/rpc/set_avatar"->json(request,JSONObject())
                else->error("Unexpected backend request")
            }
        }.build()
        return Backend(store,http).also {runBlocking {it.signIn("fixture@example.invalid","fixture")}}
    }
    private fun imageLoader():ImageLoader {
        return ImageLoader.Builder(context).okHttpClient(OkHttpClient.Builder().addInterceptor {chain->
            val n=downloads.incrementAndGet()
            val link=chain.request().url.pathSegments.last().substringBefore('.').toInt()
            val color=when(link){1->Color.RED;2->Color.BLUE;else->Color.GREEN}
            val bitmap=Bitmap.createBitmap(16,16,Bitmap.Config.ARGB_8888).apply {eraseColor(color)}
            val bytes=ByteArrayOutputStream().also {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}.toByteArray()
            bitmap.recycle()
            response(chain.request(),bytes.toResponseBody("image/png".toMediaType()),if(rejectEveryDownload||(rejectFirst&&n==1))403 else 200)
        }.build()).build()
    }
    private fun fixture(body:(Backend)->Unit) {
        val original=Coil.imageLoader(context)
        val loader=imageLoader();val backend=backend()
        Coil.setImageLoader(loader)
        try {body(backend)} finally {
            runBlocking {backend.forgetLocal()}
            Coil.setImageLoader(original);loader.shutdown()
            context.getSharedPreferences("protected-image-fixture",Context.MODE_PRIVATE).edit().clear().commit()
        }
    }
    private fun awaitImage(color:Int=Color.RED) {
        compose.waitUntil(10_000) {downloads.get()>0}
        compose.waitUntil(10_000) {
            runCatching {compose.onNodeWithContentDescription("Fixture image").captureToImage().asAndroidBitmap().let {it.getPixel(it.width/2,it.height/2)==color}}.getOrDefault(false)
        }
    }
    private fun assertTransferCountsStay(expected:Int) {
        val deadline=SystemClock.uptimeMillis()+500
        // Async image work can finish after a placeholder first appears. Observe
        // a bounded real interval so a delayed extra request cannot pass unnoticed.
        compose.waitUntil(2_000) {
            assertEquals(expected,signs.get());assertEquals(expected,downloads.get())
            SystemClock.uptimeMillis()>=deadline
        }
    }
    private fun awaitRetry() {
        compose.waitUntil(10_000) {compose.onAllNodesWithText("Retry image").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Retry image").assertIsDisplayed()
    }
    @Test fun tabReturnReusesBitmapAndAvatarMutationFetchesNewImage()=fixture {backend->
        val shown=mutableStateOf(true)
        compose.setContent {LiquidTheme(false) {if(shown.value)ProtectedImage(backend,"avatar-fixture",description="Fixture image",modifier=Modifier.size(64.dp))}}
        awaitImage();assertEquals(1,signs.get());assertEquals(1,downloads.get())
        compose.runOnIdle {shown.value=false};compose.waitForIdle()
        compose.runOnIdle {shown.value=true};awaitImage()
        assertTransferCountsStay(1)
        runBlocking {backend.rpc("set_avatar")}
        compose.waitUntil(10_000) {downloads.get()==2};awaitImage(Color.BLUE)
        assertEquals(2,signs.get())
        // A fresh login of the same user cannot reuse the previous session's bitmap.
        runBlocking {backend.signIn("fixture@example.invalid","fixture")}
        compose.waitUntil(10_000) {downloads.get()==3};awaitImage(Color.GREEN)
        assertEquals(3,signs.get())
    }
    @Test fun expiredImageTransferRenewsLinkOnceAutomatically() {
        rejectFirst=true
        fixture {backend->
            compose.setContent {LiquidTheme(false) {ProtectedImage(backend,"avatar-fixture",description="Fixture image",modifier=Modifier.size(64.dp))}}
            awaitImage(Color.BLUE);assertEquals(2,signs.get());assertEquals(2,downloads.get())
            compose.onNodeWithText("Retry image").assertDoesNotExist()
        }
    }
    @Test fun expiredPermissionIsRecheckedAndRevokedImageDisappears() {
        ttl=3;revokeAfterFirst=true
        fixture {backend->
            compose.setContent {LiquidTheme(false) {ProtectedImage(backend,"avatar-fixture",description="Fixture image",modifier=Modifier.size(64.dp))}}
            awaitImage()
            awaitRetry()
            compose.onNodeWithContentDescription("Fixture image").assertDoesNotExist()
            assertEquals(1,downloads.get())
            assertTrue(signs.get() in 2..3)
        }
    }
    @Test fun repeatedTransferRejectionStopsAndManualRetryIsBounded() {
        rejectEveryDownload=true
        fixture {backend->
            compose.setContent {LiquidTheme(false) {ProtectedImage(backend,"avatar-fixture",description="Fixture image",modifier=Modifier.size(64.dp))}}
            compose.waitUntil(10_000) {downloads.get()>=2}
            awaitRetry()
            assertTransferCountsStay(2)
            compose.onNodeWithText("Retry image").performClick()
            compose.waitUntil(10_000) {downloads.get()>=4}
            awaitRetry()
            assertTransferCountsStay(4)
        }
    }
    @Test fun serverLifetimeOverridesLongerLocalCacheAndForceBypassesIt()=fixture {backend->
        ttl=1
        var now=0L
        runBlocking {
            assertEquals("https://media.example.invalid/1.png",backend.imageUrl("image-fixture","avatar-fixture",clock={now}))
            now=800
            assertEquals("https://media.example.invalid/1.png",backend.imageUrl("image-fixture","avatar-fixture",clock={now}))
            now=901
            assertEquals("https://media.example.invalid/2.png",backend.imageUrl("image-fixture","avatar-fixture",clock={now}))
            assertEquals("https://media.example.invalid/3.png",backend.imageUrl("image-fixture","avatar-fixture",force=true,clock={now}))
        }
    }
}
