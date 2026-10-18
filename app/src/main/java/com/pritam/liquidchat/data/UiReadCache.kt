package com.pritam.liquidchat.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

/** Bounded, memory-only reads. Session and mutation boundaries also invalidate in-flight reads. */
class UiReadCache(private val clock: () -> Long = { System.nanoTime() / 1_000_000L }) {
    private data class Entry(val value: String, val validUntil: Long)
    private class Resource(var version: Long) {
        val gate = Mutex()
        var entry: Entry? = null
        var completed = 0L
        var readers = 0
    }

    private val monitor = Any()
    private val resources = LinkedHashMap<String, Resource>(128, 0.75f, true)
    private var owner: String? = null
    private var authorization: String? = null
    private var generation = 0L
    private var nextVersion = 0L
    private val _revision = MutableStateFlow(0L)
    val revision = _revision.asStateFlow()
    private val _sessionEpoch = MutableStateFlow(0L)
    val sessionEpoch = _sessionEpoch.asStateFlow()

    fun activate(account: String?, authorization: String? = null, force: Boolean = false) = synchronized(monitor) {
        val activeAccount = account?.takeIf { it.isNotBlank() }
        if (owner != activeAccount || this.authorization != authorization || force) {
            resources.clear()
            owner = activeAccount
            this.authorization = authorization
            generation++
            _sessionEpoch.value = generation
            _revision.value++
        }
    }

    fun clear() = activate(null, force = true)

    /** Stable during ordinary reads and token renewal; changes when the visibility context changes. */
    fun epoch(account: String?): Long? = synchronized(monitor) {
        generation.takeIf { !account.isNullOrBlank() && owner == account }
    }

    /** Observers can refresh one resource without resetting unrelated images or feeds. */
    fun version(account: String?, key: String): Long? = synchronized(monitor) {
        if (account.isNullOrBlank() || owner != account) return@synchronized null
        resource(key).version.also { trim() }
    }

    fun peek(account: String?, key: String, allowStale: Boolean = false): String? = synchronized(monitor) {
        if (account.isNullOrBlank() || owner != account) return@synchronized null
        val entry = resources[key]?.entry ?: return@synchronized null
        if (allowStale || entry.validUntil > clock()) entry.value else null
    }

    fun invalidate(account: String?, key: String) = synchronized(monitor) {
        if (account.isNullOrBlank() || owner != account) return@synchronized
        resources[key]?.let { reset(it) }
        _revision.value++
    }

    fun invalidatePrefix(account: String?, prefix: String) = synchronized(monitor) {
        if (account.isNullOrBlank() || owner != account) return@synchronized
        resources.forEach { (key, resource) -> if (key.startsWith(prefix)) reset(resource) }
        _revision.value++
    }

    private fun resource(key: String) = resources.getOrPut(key) { Resource(++nextVersion) }
    private fun reset(resource: Resource) {
        resource.entry = null
        resource.version = ++nextVersion
    }
    private fun checkSession(account: String, epoch: Long) {
        if (owner != account || generation != epoch) throw IOException("Account or access changed. Please try again.")
    }
    private fun trim() {
        val iterator = resources.entries.iterator()
        while (resources.size > 128 && iterator.hasNext()) {
            if (iterator.next().value.readers == 0) iterator.remove()
        }
    }

    suspend fun load(
        account: String, key: String, ttlMs: Long, force: Boolean = false,
        fetch: suspend () -> String
    ): String {
        require(account.isNotBlank() && ttlMs in 1..300_000L)
        val (resource, epoch, completedAtStart) = synchronized(monitor) {
            if (owner != account) throw IOException("Account changed. Please try again.")
            val resource = resource(key)
            resource.readers++
            Triple(resource, generation, resource.completed)
        }
        try {
            return resource.gate.withLock {
                while (true) {
                    val version = synchronized(monitor) {
                        checkSession(account, epoch)
                        // A successful refresh satisfies all callers already waiting for it.
                        if (!force || resource.completed != completedAtStart) {
                            resource.entry?.takeIf { it.validUntil > clock() }?.let { return@withLock it.value }
                        }
                        resource.version
                    }
                    val result = fetch()
                    synchronized(monitor) {
                        checkSession(account, epoch)
                        if (resource.version == version) {
                            resource.entry = Entry(result, clock() + ttlMs)
                            resource.completed++
                            return@withLock result
                        }
                    }
                    // A mutation finished during the fetch. Read again before exposing its old result.
                }
                @Suppress("UNREACHABLE_CODE")
                error("Unreachable")
            }
        } finally {
            synchronized(monitor) {
                resource.readers--
                trim()
            }
        }
    }
}
