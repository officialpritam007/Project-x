package com.pritam.liquidchat.data

import android.content.Context
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Intercepted HTTP fixtures only: these tests never contact or modify the production backend. */
class BackendCacheIsolationTest {
    private fun session(access: String, user: String = "cache-alice", expires: Long = System.currentTimeMillis() / 1000 + 3600) =
        JSONObject().put("user", JSONObject().put("id", user).put("email", "$user@example.invalid"))
            .put("access_token", access).put("refresh_token", "fixture-refresh").put("expires_at", expires).toString()

    private fun response(request: Request, body: String = "[]", status: Int = 200) = Response.Builder()
        .request(request).protocol(Protocol.HTTP_1_1).code(status).message("Fixture")
        .body(body.toResponseBody("application/json".toMediaType())).build()

    private suspend fun fixture(name: String, http: OkHttpClient, stored: String, test: suspend (Backend) -> Unit) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferenceName = "backend-cache-$name"
        val store = SessionStore(context, preferenceName)
        val backend = Backend(store, http)
        try {
            store.write("session", stored)
            backend.restore()
            test(backend)
        } finally {
            backend.forgetLocal()
            context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE).edit().clear().commit()
        }
    }

    private fun jwt(role: String = "authenticated", expires: Long = 100, issued: Long = 10): String {
        val claims = JSONObject().put("sub", "cache-alice").put("role", role).put("session_id", "fixture-auth-session")
            .put("aud", "authenticated").put("exp", expires).put("iat", issued)
        return "fixture." + Base64.encodeToString(claims.toString().toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING) + ".signature"
    }

    private suspend fun refresh(changedPermissions: Boolean) {
        val oldAccess = jwt()
        val freshAccess = jwt(if (changedPermissions) "restricted" else "authenticated", expires = 200, issued = 20)
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            response(request, if (request.url.encodedPath == "/auth/v1/token") session(freshAccess) else "[]")
        }.build()
        fixture("refresh-$changedPermissions", http, session(oldAccess, expires = 0)) { backend ->
            backend.uiReads.load("cache-alice", "signed-image:avatar", 40_000) { "signed fixture" }
            val epoch = backend.uiReads.sessionEpoch.value
            assertEquals("[]", backend.request("/rest/v1/fixture"))
            assertEquals(freshAccess, backend.session.value?.access)
            if (changedPermissions) {
                assertNotEquals(epoch, backend.uiReads.sessionEpoch.value)
                assertNull(backend.uiReads.peek("cache-alice", "signed-image:avatar", allowStale = true))
            } else {
                assertEquals(epoch, backend.uiReads.sessionEpoch.value)
                assertEquals("signed fixture", backend.uiReads.peek("cache-alice", "signed-image:avatar"))
            }
        }
    }

    @Test fun normalTokenRenewalRetainsReadCacheAndImageIdentity() = runBlocking { refresh(false) }
    @Test fun changedJwtPermissionsInvalidateSameUserReadCache() = runBlocking { refresh(true) }

    @Test fun forbiddenResponseClearsVisibleProtectedDataOnlyOncePerAccessToken() = runBlocking {
        val http = OkHttpClient.Builder().addInterceptor { chain -> response(chain.request(), "{\"code\":\"42501\"}", 403) }.build()
        fixture("forbidden", http, session(jwt())) { backend ->
            backend.uiReads.load("cache-alice", "moments:post", 40_000) { "private fixture" }
            val before = backend.uiReads.sessionEpoch.value
            assertTrue(runCatching { backend.request("/rest/v1/fixture") }.exceptionOrNull() is ApiException)
            val denied = backend.uiReads.sessionEpoch.value
            assertNotEquals(before, denied)
            assertNull(backend.uiReads.peek("cache-alice", "moments:post", allowStale = true))
            assertTrue(runCatching { backend.request("/rest/v1/fixture") }.exceptionOrNull() is ApiException)
            assertEquals("Repeated 403 must not create an endless UI reload loop", denied, backend.uiReads.sessionEpoch.value)
        }
    }

    private suspend fun reauthenticate(switchAwayAndBack: Boolean) = coroutineScope {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            if (request.url.encodedPath == "/rest/v1/fixture") {
                started.countDown()
                check(release.await(10, TimeUnit.SECONDS)) { "Fixture read was not released" }
                response(request, "old private response")
            } else if (request.url.encodedPath == "/auth/v1/token") {
                val login = JSONObject(okio.Buffer().also { request.body!!.writeTo(it) }.readUtf8())
                response(request, session("new-access-${login.getString("email")}", if (login.getString("email").startsWith("bob")) "cache-bob" else "cache-alice"))
            } else error("Unexpected fixture request")
        }.build()
        fixture("reauth-$switchAwayAndBack", http, session("old-access")) { backend ->
            val old = async(Dispatchers.IO) { runCatching { backend.request("/rest/v1/fixture") } }
            try {
                assertTrue(withContext(Dispatchers.IO) { started.await(5, TimeUnit.SECONDS) })
                if (switchAwayAndBack) backend.signIn("bob@example.invalid", "fixture")
                backend.signIn("alice@example.invalid", "fixture")
                release.countDown()
                assertTrue("Same user ID must not admit a previous auth-session result", old.await().exceptionOrNull() is IOException)
            } finally {
                release.countDown()
                old.await()
            }
        }
    }

    @Test fun sameUserNewLoginRejectsPreviousInFlightResponse() = runBlocking { reauthenticate(false) }
    @Test fun switchingAccountsAndBackRejectsPreviousInFlightResponse() = runBlocking { reauthenticate(true) }

    @Test fun successfulMutationsInvalidateDependentFirstPagesAndAvatarUrls() = runBlocking {
        val http = OkHttpClient.Builder().addInterceptor { chain -> response(chain.request(), "{}") }.build()
        fixture("mutations", http, session(jwt())) { backend ->
            suspend fun seed() {
                backend.uiReads.load("cache-alice", "moments:post", 45_000) { "posts" }
                backend.uiReads.load("cache-alice", "calls:recent", 60_000) { "calls" }
                backend.uiReads.load("cache-alice", "signed-image:avatar", 40_000) { "avatar" }
            }
            seed()
            val epoch = backend.uiReads.sessionEpoch.value
            backend.rpc("publish_moment")
            assertNull(backend.uiReads.peek("cache-alice", "moments:post", allowStale = true))
            assertEquals("calls", backend.uiReads.peek("cache-alice", "calls:recent"))
            assertEquals("avatar", backend.uiReads.peek("cache-alice", "signed-image:avatar"))
            backend.rpc("call_hangup")
            assertNull(backend.uiReads.peek("cache-alice", "calls:recent", allowStale = true))
            seed()
            backend.rpc("set_avatar")
            assertNull(backend.uiReads.peek("cache-alice", "signed-image:avatar", allowStale = true))
            assertNull(backend.uiReads.peek("cache-alice", "moments:post", allowStale = true))
            assertNull(backend.uiReads.peek("cache-alice", "calls:recent", allowStale = true))
            assertEquals("Avatar edit must not log the user into a new visibility context", epoch, backend.uiReads.sessionEpoch.value)
            seed()
            backend.rpc("set_block")
            assertNotEquals(epoch, backend.uiReads.sessionEpoch.value)
            assertNull(backend.uiReads.peek("cache-alice", "signed-image:avatar", allowStale = true))
        }
    }
}
