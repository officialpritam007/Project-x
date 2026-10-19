package com.pritam.liquidchat.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test

class UploadProgressBodyTest {
    @Test fun reportsRealMonotonicBytesAndPreservesExactPayloadAcrossRetries() {
        val payload=ByteArray(65_537) {(it%251).toByte()}
        val values=mutableListOf<Int>()
        val body=UploadProgressBody(payload.toRequestBody("image/jpeg".toMediaType()),values::add)
        repeat(2) {
            values.clear()
            val output=Buffer()
            body.writeTo(output)
            assertArrayEquals(payload,output.readByteArray())
            assertEquals(payload.size.toLong(),body.contentLength())
            assertEquals(0,values.first())
            assertEquals(100,values.last())
            assertTrue(values.zipWithNext().all {(a,b)->a<b})
            assertEquals("image/jpeg",body.contentType().toString())
        }
    }
}
