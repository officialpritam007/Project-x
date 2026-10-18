package com.pritam.liquidchat.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

/**
 * Bounded, memory-only read cache. Only Backend's authenticated session can activate an
 * account. Late requests from another session can neither evict nor populate its data.
 * Nothing in this cache is written to device storage.
 */
class UiReadCache(private val clock:()->Long=System::currentTimeMillis) {
    private data class Entry(val value:String,val validUntil:Long)
    private val monitor=Any()
    private val entries=LinkedHashMap<String,Entry>(128,0.75f,true)
    private val gates=mutableMapOf<String,Mutex>()
    private var owner:String?=null
    private var generation=0L

    fun activate(account:String?)=synchronized(monitor) {
        if(owner!=account) {
            entries.clear()
            gates.clear()
            owner=account
            generation++
        }
    }

    fun clear()=synchronized(monitor) {
        entries.clear()
        gates.clear()
        owner=null
        generation++
    }

    fun peek(account:String?,key:String,allowStale:Boolean=false):String?=synchronized(monitor) {
        if(account.isNullOrBlank()||owner!=account)return@synchronized null
        val entry=entries[key] ?: return@synchronized null
        if(allowStale||entry.validUntil>clock())entry.value else null
    }

    fun invalidate(account:String?,key:String)=synchronized(monitor) {
        if(owner==account&&account!=null)entries.remove(key)
    }

    fun invalidatePrefix(account:String?,prefix:String)=synchronized(monitor) {
        if(owner==account&&account!=null) {
            val iterator=entries.keys.iterator()
            while(iterator.hasNext())if(iterator.next().startsWith(prefix))iterator.remove()
        }
    }

    suspend fun load(
        account:String,key:String,ttlMs:Long,force:Boolean=false,
        fetch:suspend ()->String
    ):String {
        require(account.isNotBlank()&&ttlMs in 1..300_000L)
        val (gate,epoch)=synchronized(monitor) {
            if(owner!=account)throw IOException("Account changed")
            (gates.getOrPut(key) {Mutex()}) to generation
        }
        return gate.withLock {
            synchronized(monitor) {
                if(owner!=account||generation!=epoch)throw IOException("Account changed")
            }
            if(!force)peek(account,key)?.let {return@withLock it}
            val result=fetch()
            synchronized(monitor) {
                if(owner==account&&generation==epoch) {
                    entries[key]=Entry(result,clock()+ttlMs)
                    while(entries.size>128)entries.remove(entries.keys.first())
                }
            }
            result
        }
    }
}
