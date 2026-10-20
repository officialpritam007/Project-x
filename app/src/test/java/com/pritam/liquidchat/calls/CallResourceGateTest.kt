package com.pritam.liquidchat.calls

import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.*
import org.junit.Test

class CallResourceGateTest {
    @Test fun disposalWaitsForAnInFlightNativeOperationAndRejectsLateWork() {
        val gate=CallResourceGate()
        val entered=CountDownLatch(1)
        val release=CountDownLatch(1)
        val closeAttempted=CountDownLatch(1)
        val disposed=CountDownLatch(1)
        val order=AtomicInteger()
        val pool=Executors.newFixedThreadPool(2)
        try {
            val operation=pool.submit {
                gate.withOpen {
                    entered.countDown()
                    check(release.await(5,TimeUnit.SECONDS))
                    assertEquals(0,order.getAndIncrement())
                }
            }
            assertTrue(entered.await(5,TimeUnit.SECONDS))
            val close=pool.submit {
                closeAttempted.countDown()
                gate.close {
                    assertEquals(1,order.getAndIncrement())
                    disposed.countDown()
                }
            }
            assertTrue(closeAttempted.await(5,TimeUnit.SECONDS))
            assertFalse("Native disposal overlapped the in-flight operation",disposed.await(100,TimeUnit.MILLISECONDS))
            release.countDown()
            operation.get(5,TimeUnit.SECONDS)
            close.get(5,TimeUnit.SECONDS)
            assertTrue(gate.closed)
            assertEquals(2,order.get())
            gate.ifOpen {error("Late ICE must not reach a disposed peer")}
            try {
                gate.withOpen {error("Late SDP must not reach a disposed peer")}
                fail("Ended call must reject SDP")
            } catch(expected:IOException) {assertEquals("Call already ended",expected.message)}
        } finally {release.countDown();pool.shutdownNow()}
    }

    @Test fun repeatedOrFailedDisposalCannotReopenOrDisposeTwice() {
        val gate=CallResourceGate()
        val calls=AtomicInteger()
        try {gate.close {calls.incrementAndGet();throw IllegalStateException("native fixture failure")}}
        catch(_:IllegalStateException) {}
        gate.close {calls.incrementAndGet()}
        gate.ifOpen {calls.incrementAndGet()}
        assertEquals(1,calls.get())
        assertTrue(gate.closed)
    }
}
