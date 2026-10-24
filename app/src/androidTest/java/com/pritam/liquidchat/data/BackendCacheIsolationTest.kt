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
import java.util.concurrent.atomic.AtomicBoolean

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
            val scope = backend.profileCacheScope("cache-alice")
            assertNotNull(scope)
            assertEquals("[]", backend.request("/rest/v1/fixture"))
            assertEquals(freshAccess, backend.session.value?.access)
            val context = ApplicationProvider.getApplicationContext<Context>()
            val stored = SessionStore(context, "backend-cache-refresh-$changedPermissions").read("session")!!
            assertEquals(backend.profileCacheScope("cache-alice"), JSONObject(stored).getString("liquid_profile_cache_scope"))
            if (changedPermissions) {
                assertNotEquals(epoch, backend.uiReads.sessionEpoch.value)
                assertNotEquals(scope, backend.profileCacheScope("cache-alice"))
                assertNull(backend.uiReads.peek("cache-alice", "signed-image:avatar", allowStale = true))
            } else {
                assertEquals(epoch, backend.uiReads.sessionEpoch.value)
                assertEquals(scope, backend.profileCacheScope("cache-alice"))
                assertEquals("signed fixture", backend.uiReads.peek("cache-alice", "signed-image:avatar"))
            }
        }
    }

    @Test fun normalTokenRenewalRetainsReadCacheAndImageIdentity() = runBlocking { refresh(false) }
    @Test fun changedJwtPermissionsInvalidateSameUserReadCache() = runBlocking { refresh(true) }

    @Test fun unrelatedForbiddenResponseDoesNotInvalidateOtherProtectedReads() = runBlocking {
        val http = OkHttpClient.Builder().addInterceptor { chain -> response(chain.request(), "{\"code\":\"42501\"}", 403) }.build()
        fixture("forbidden", http, session(jwt())) { backend ->
            backend.uiReads.load("cache-alice", "moments:post", 40_000) { "private fixture" }
            val before = backend.uiReads.sessionEpoch.value
            val beforeScope = backend.profileCacheScope("cache-alice")
            assertTrue(runCatching { backend.request("/rest/v1/fixture") }.exceptionOrNull() is ApiException)
            val denied = backend.uiReads.sessionEpoch.value
            val deniedScope = backend.profileCacheScope("cache-alice")
            assertEquals("Unrelated RLS denial must not clear the active session",before, denied)
            assertEquals(beforeScope, deniedScope)
            val context = ApplicationProvider.getApplicationContext<Context>()
            val restarted = Backend(SessionStore(context, "backend-cache-forbidden"), http)
            restarted.restore()
            assertEquals("Restart must not revive the profile from before authorization denial", deniedScope, restarted.profileCacheScope("cache-alice"))
            assertEquals("private fixture", backend.uiReads.peek("cache-alice", "moments:post", allowStale = true))
            assertTrue(runCatching { backend.request("/rest/v1/fixture") }.exceptionOrNull() is ApiException)
            assertEquals("Repeated 403 must not create an endless UI reload loop", denied, backend.uiReads.sessionEpoch.value)
            assertEquals(deniedScope, backend.profileCacheScope("cache-alice"))
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
            val previousScope = backend.profileCacheScope("cache-alice")
            val old = async(Dispatchers.IO) { runCatching { backend.request("/rest/v1/fixture") } }
            try {
                assertTrue(withContext(Dispatchers.IO) { started.await(5, TimeUnit.SECONDS) })
                if (switchAwayAndBack) backend.signIn("bob@example.invalid", "fixture")
                backend.signIn("alice@example.invalid", "fixture")
                assertNotEquals(previousScope, backend.profileCacheScope("cache-alice"))
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
            val scope = backend.profileCacheScope("cache-alice")
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
            val avatarScope = backend.profileCacheScope("cache-alice")
            assertNotEquals("Previous saved profile must not revive the old avatar", scope, avatarScope)
            backend.rpc("save_profile")
            val editedScope = backend.profileCacheScope("cache-alice")
            assertNotEquals("Previous saved profile must not revive the old name or bio", avatarScope, editedScope)
            assertEquals(epoch, backend.uiReads.sessionEpoch.value)
            seed()
            backend.rpc("set_block")
            assertNotEquals(epoch, backend.uiReads.sessionEpoch.value)
            assertNotEquals(editedScope, backend.profileCacheScope("cache-alice"))
            assertNull(backend.uiReads.peek("cache-alice", "signed-image:avatar", allowStale = true))
        }
    }

    @Test fun profileScopeSurvivesOfflineRestartAndVisibilityRotationIsPersisted() = runBlocking {
        val http = OkHttpClient.Builder().addInterceptor { chain -> response(chain.request(), "{}") }.build()
        fixture("scope-restart", http, session(jwt())) { backend ->
            val context = ApplicationProvider.getApplicationContext<Context>()
            val store = SessionStore(context, "backend-cache-scope-restart")
            val offline = OkHttpClient.Builder().addInterceptor { throw IOException("offline fixture") }.build()
            val scope = backend.profileCacheScope("cache-alice")
            assertNotNull(scope)
            assertEquals(scope, JSONObject(store.read("session")!!).getString("liquid_profile_cache_scope"))
            val restarted = Backend(store, offline)
            restarted.restore()
            assertEquals("Cold offline startup must recognize its own cached profile", scope, restarted.profileCacheScope("cache-alice"))
            assertNull(restarted.profileCacheScope("cache-bob"))

            backend.rpc("save_contact")
            val changedScope = backend.profileCacheScope("cache-alice")
            assertNotEquals(scope, changedScope)
            val afterVisibilityChange = Backend(store, offline)
            afterVisibilityChange.restore()
            assertEquals(changedScope, afterVisibilityChange.profileCacheScope("cache-alice"))
            assertEquals(changedScope, JSONObject(store.read("session")!!).getString("liquid_profile_cache_scope"))
        }
    }

    @Test fun newLoginGetsNewScopeEvenWhenUserAndAuthorizationClaimsAreIdentical() = runBlocking {
        val access = jwt()
        val http = OkHttpClient.Builder().addInterceptor { chain -> response(chain.request(), session(access)) }.build()
        fixture("scope-login", http, session(access)) { backend ->
            val before = backend.profileCacheScope("cache-alice")
            backend.signIn("alice@example.invalid", "fixture")
            val after = backend.profileCacheScope("cache-alice")
            assertNotNull(after)
            assertNotEquals(before, after)
            backend.forgetLocal()
            assertNull(backend.profileCacheScope("cache-alice"))
            backend.signIn("alice@example.invalid", "fixture")
            assertNotEquals(after, backend.profileCacheScope("cache-alice"))
        }
    }

    @Test fun remoteImageRevocationRejectsCachedLinkAndInvalidatesOfflineProfileScope() = runBlocking {
        val revoked = AtomicBoolean(false)
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            assertEquals("/functions/v1/media", request.url.encodedPath)
            if (revoked.get()) response(request, "{\"code\":\"42501\"}", 403)
            else response(request, "{\"url\":\"https://media.example.invalid/fixture.png\",\"expires_in\":60}")
        }.build()
        fixture("image-revoked", http, session(jwt())) { backend ->
            val scope = backend.profileCacheScope("cache-alice")
            assertEquals("https://media.example.invalid/fixture.png", backend.imageUrl("cache-alice", "avatar-fixture"))
            assertNotNull(backend.uiReads.peek("cache-alice", "signed-image:avatar-fixture"))
            revoked.set(true)
            val failure = runCatching { backend.imageUrl("cache-alice", "avatar-fixture", force = true) }.exceptionOrNull()
            assertTrue(failure is ApiException)
            assertEquals(403, (failure as ApiException).status)
            assertNull(backend.uiReads.peek("cache-alice", "signed-image:avatar-fixture", allowStale = true))
            assertNotEquals(scope, backend.profileCacheScope("cache-alice"))
        }
    }
}
