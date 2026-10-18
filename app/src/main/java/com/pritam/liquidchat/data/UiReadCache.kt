package com.pritam.liquidchat.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Ephemeral read-through cache for authenticated UI GET-style RPCs and signed image URLs.
 * Data never reaches disk, is partitioned by account and cleared on account changes/logout.
 * A per-key mutex coalesces concurrent requests without serializing unrelated resources.
 */
class UiReadCache(
    private val clock:()->Long = System::currentTimeMillis
) {
    private data class Entry(val value:String,val validUntil:Long)
    private val monitor=Any()
    private val entries=LinkedHashMap<String,Entry>(128,0.75f,true)
    private val gates=mutableMapOf<String,Mutex>()
    private var owner:String?=null

    private fun switchUser(account:String?) {
        if(owner!=account) {
            entries.clear()
            gates.clear()
            owner=account
        }
    }

    fun clear()=synchronized(monitor) {
        entries.clear()
        gates.clear()
        owner=null
    }

    /** Returns a saved result, including an expired result when requested for stale-while-refresh UI. */
    fun peek(account:String?,key:String,allowStale:Boolean=false):String?=synchronized(monitor) {
        if(account.isNullOrBlank()) {switchUser(null);return@synchronized null}
        switchUser(account)
        val entry=entries[key] ?: return@synchronized null
        if(allowStale||entry.validUntil>clock())entry.value else null
    }

    fun invalidate(account:String?,key:String)=synchronized(monitor) {
        if(account.isNullOrBlank())return@synchronized
        switchUser(account)
        entries.remove(key)
    }

    suspend fun load(
        account:String,key:String,ttlMs:Long,force:Boolean=false,
        fetch:suspend ()->String
    ):String {
        require(ttlMs in 1..300_000L)
        val gate=synchronized(monitor) {
            switchUser(account)
            gates.getOrPut(key) {Mutex()}
        }
        return gate.withLock {
            if(!force) peek(account,key)?.let {return@withLock it}
            val result=fetch()
            synchronized(monitor) {
                if(owner==account) {
                    entries[key]=Entry(result,clock()+ttlMs)
                    while(entries.size>128)entries.remove(entries.keys.first())
                }
            }
            result
        }
    }
}
