package com.pritam.liquidchat.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class UiReadCacheTest {
    @Test fun firstPageUsesTtlAndExplicitRefreshBypassesIt() = runBlocking {
        var now = 100L
        var requests = 0
        val cache = UiReadCache { now }.apply { activate("alice") }
        suspend fun load(force: Boolean = false) = cache.load("alice", "moments:post", 20, force) { "page-${++requests}" }
        assertEquals("page-1", load())
        now = 119
        assertEquals("page-1", load())
        assertEquals("page-2", load(force = true))
        now = 139
        assertNull(cache.peek("alice", "moments:post"))
        assertEquals("page-2", cache.peek("alice", "moments:post", allowStale = true))
        assertEquals("page-3", load())
        assertEquals(3, requests)
    }

    @Test fun concurrentReadsAndRefreshesShareTheSameSuccessfulFetch() = runBlocking {
        val cache = UiReadCache().apply { activate("alice") }
        var requests = 0
        cache.load("alice", "calls:recent", 60_000) { "cached" }
        val release = CompletableDeferred<Unit>()
        val refreshes = (1..12).map {
            async(start = CoroutineStart.UNDISPATCHED) {
                cache.load("alice", "calls:recent", 60_000, force = true) {
                    requests++
                    release.await()
                    "fresh"
                }
            }
        }
        release.complete(Unit)
        assertEquals(List(12) { "fresh" }, refreshes.awaitAll())
        assertEquals(1, requests)
    }

    @Test fun mutationDuringFetchDiscardsTheOldPageAndRefetchesBeforeReturning() = runBlocking {
        val cache = UiReadCache().apply { activate("alice") }
        val release = CompletableDeferred<Unit>()
        var requests = 0
        val read = async(start = CoroutineStart.UNDISPATCHED) {
            cache.load("alice", "moments:story", 20_000) {
                requests++
                if (requests == 1) { release.await(); "deleted story" } else "new page"
            }
        }
        cache.invalidatePrefix("alice", "moments:")
        val refresh = async(start = CoroutineStart.UNDISPATCHED) {
            cache.load("alice", "moments:story", 20_000, force = true) { error("Must share the post-mutation fetch") }
        }
        release.complete(Unit)
        assertEquals("new page", read.await())
        assertEquals("new page", refresh.await())
        assertEquals("new page", cache.peek("alice", "moments:story"))
        assertEquals(2, requests)
    }

    @Test fun latePreviousAccountResponseIsRejectedWithoutReplacingCurrentCache() = runBlocking {
        val cache = UiReadCache().apply { activate("alice") }
        val release = CompletableDeferred<Unit>()
        val oldRead = async(start = CoroutineStart.UNDISPATCHED) {
            runCatching { cache.load("alice", "calls:recent", 60_000) { release.await(); "alice's calls" } }
        }
        cache.activate("bob")
        cache.load("bob", "calls:recent", 60_000) { "bob's calls" }
        release.complete(Unit)
        assertTrue(oldRead.await().exceptionOrNull() is IOException)
        assertNull(cache.peek("alice", "calls:recent", allowStale = true))
        assertEquals("bob's calls", cache.peek("bob", "calls:recent"))
    }

    @Test fun logoutAndLoginToTheSameAccountAlsoRejectsThePreviousSessionResponse() = runBlocking {
        val cache = UiReadCache().apply { activate("alice", "scope") }
        val release = CompletableDeferred<Unit>()
        val oldEpoch = cache.epoch("alice")
        val oldRead = async(start = CoroutineStart.UNDISPATCHED) {
            runCatching { cache.load("alice", "signed-image:avatar", 40_000) { release.await(); "old signed url" } }
        }
        cache.clear()
        cache.activate("alice", "scope")
        release.complete(Unit)
        assertNotEquals(oldEpoch, cache.epoch("alice"))
        assertTrue(oldRead.await().exceptionOrNull() is IOException)
        assertNull(cache.peek("alice", "signed-image:avatar", allowStale = true))
    }

    @Test fun authorizationBoundaryClearsEvenSameAccountButOrdinaryActivationRetainsReads() = runBlocking {
        val cache = UiReadCache().apply { activate("alice", "normal") }
        cache.load("alice", "moments:post", 30_000) { "private post" }
        val epoch = cache.sessionEpoch.value
        cache.activate("alice", "normal")
        assertEquals(epoch, cache.sessionEpoch.value)
        assertEquals("private post", cache.peek("alice", "moments:post"))
        cache.activate("alice", "restricted")
        assertNotEquals(epoch, cache.sessionEpoch.value)
        assertNull(cache.peek("alice", "moments:post", allowStale = true))
    }

    @Test fun prefixInvalidationOnlyChangesMatchingResourceIdentity() = runBlocking {
        val cache = UiReadCache().apply { activate("alice") }
        cache.load("alice", "calls:recent", 60_000) { "calls" }
        cache.load("alice", "signed-image:avatar", 40_000) { "avatar" }
        val epoch = cache.epoch("alice")
        val calls = cache.version("alice", "calls:recent")
        val image = cache.version("alice", "signed-image:avatar")
        val revision = cache.revision.value
        cache.invalidatePrefix("bob", "signed-image:")
        assertEquals(revision, cache.revision.value)
        cache.invalidatePrefix("alice", "signed-image:")
        assertEquals(epoch, cache.epoch("alice"))
        assertEquals(calls, cache.version("alice", "calls:recent"))
        assertNotEquals(image, cache.version("alice", "signed-image:avatar"))
        assertEquals("calls", cache.peek("alice", "calls:recent"))
        assertNull(cache.peek("alice", "signed-image:avatar", allowStale = true))
    }

    @Test fun failedOrCancelledFetchDoesNotPoisonTheNextRead() = runBlocking {
        val cache = UiReadCache().apply { activate("alice") }
        val failed = runCatching { cache.load("alice", "moments:post", 10_000) { throw IOException("offline") } }
        assertTrue(failed.isFailure)
        val waiting = CompletableDeferred<Unit>()
        val cancelled = launch(start = CoroutineStart.UNDISPATCHED) {
            cache.load("alice", "moments:post", 10_000) { waiting.await(); "cancelled" }
        }
        cancelled.cancelAndJoin()
        assertEquals("recovered", cache.load("alice", "moments:post", 10_000) { "recovered" })
    }

    @Test fun leastRecentlyUsedResourcesAreBoundedAndInvalidatedDataCannotBePeekedAsStale() = runBlocking {
        val cache = UiReadCache().apply { activate("alice") }
        repeat(128) { index -> cache.load("alice", "image:$index", 40_000) { "$index" } }
        assertEquals("0", cache.peek("alice", "image:0"))
        cache.load("alice", "image:128", 40_000) { "128" }
        assertNull(cache.peek("alice", "image:1", allowStale = true))
        assertEquals("0", cache.peek("alice", "image:0"))
        cache.invalidate("alice", "image:0")
        assertNull(cache.peek("alice", "image:0", allowStale = true))
    }
}
