package com.pritam.liquidchat.data

import java.util.concurrent.atomic.AtomicLong

/** Monotonic persisted draft revisions, including multiple edits in one millisecond. */
internal object DraftVersions {
    private val clock = AtomicLong(0)
    fun observe(version: Long) { clock.updateAndGet { maxOf(it, version) } }
    fun next(): Long = clock.updateAndGet { maxOf(System.currentTimeMillis(), it + 1) }
}
