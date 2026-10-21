package com.pritam.liquidchat.ui

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.*
import org.junit.Test

/** Structural preflight only; Android still parses and validates the font. */
class MbImportedFontDirectoryTest {
    private fun directory():ByteArray {
        val tags=listOf("cmap","head","maxp","name","glyf","loca")
        val offset=12+tags.size*16
        val bytes=ByteArray(offset+tags.size*4)
        val buffer=ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        buffer.putInt(0,0x00010000)
        buffer.putShort(4,tags.size.toShort())
        tags.forEachIndexed {i,tag->
            val base=12+i*16
            tag.toByteArray(Charsets.US_ASCII).copyInto(bytes,base)
            buffer.putInt(base+8,offset+i*4)
            buffer.putInt(base+12,4)
        }
        return bytes
    }
    @Test fun truncatedAndCollectionFilesRejected() {
        assertFalse(isSupportedFont(ByteArray(11)))
        assertFalse(isSupportedFont(directory().apply {"ttcf".toByteArray().copyInto(this)}))
        assertFalse(isSupportedFont(directory().copyOf(50)))
    }
    @Test fun unsignedOffsetsAndHugeLengthsRejected() {
        assertFalse(isSupportedFont(directory().apply {
            ByteBuffer.wrap(this).order(ByteOrder.BIG_ENDIAN).putInt(20,-1)
        }))
        assertFalse(isSupportedFont(directory().apply {
            ByteBuffer.wrap(this).order(ByteOrder.BIG_ENDIAN).putInt(24,Int.MAX_VALUE)
        }))
    }
    @Test fun duplicateAndMissingRequiredTablesRejected() {
        assertFalse(isSupportedFont(directory().apply {"head".toByteArray().copyInto(this,12)}))
        assertFalse(isSupportedFont(directory().apply {"XXXX".toByteArray().copyInto(this,12)}))
    }
    @Test fun supportedDirectoryOnlyAndBoundedSize() {
        assertTrue(isSupportedFont(directory()))
        assertFalse(isSupportedFont(ByteArray(MbImportedFonts.MAX_BYTES+1)))
    }
}
