package com.pritam.liquidchat.data

import org.junit.Assert.*
import org.junit.Test

class DeliveryTest {
    @Test fun serverAcceptanceDoesNotMeanRecipientDelivery() {
        assertEquals("Sent",Delivery.label("SENT",42,41,40))
        assertEquals("Delivered",Delivery.label("SENT",42,42,41))
        assertEquals("Read",Delivery.label("SENT",42,42,42))
    }
    @Test fun receiptsNeverAcknowledgeAnUnacceptedLocalMessage() {
        assertEquals("Queued",Delivery.label("QUEUED",0,100,100))
        assertEquals("Sending",Delivery.label("SENDING",0,100,100))
        assertEquals("Failed",Delivery.label("FAILED",0,100,100))
    }
    @Test fun failuresNeedAnExplicitRetryAndRetriesAreBounded() {
        assertFalse(Delivery.retryable("FAILED",0,true))
        assertFalse(Delivery.retryable("SENDING",5,true))
        assertFalse(Delivery.retryable("SENDING",1,false))
        assertTrue(Delivery.retryable("SENDING",4,true))
    }
}
