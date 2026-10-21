package com.pritam.liquidchat.ui

import org.junit.Assert.*
import org.junit.Test

class MbColorPickerTest {
    @Test fun roundtripPrimariesAndHexPalette() {
        listOf("#FF0000","#00FF00","#0000FF","#FFFFFF","#000000",
            "#808080","#2563EB","#01FE7F","#EFA317","#B314F2").forEach {
            assertEquals(it,MbHsvColor.fromHex(it)!!.toHex())
        }
    }
    @Test fun hueEndpointsAndOpaqueClamping() {
        listOf("#FF0000","#FFFF00","#00FF00","#00FFFF","#0000FF","#FF00FF","#FF0000")
            .forEachIndexed {index,hex->
                assertEquals(hex,MbHsvColor(index*60f,1f,1f).toHex())
            }
        assertEquals("#FFFFFF",MbHsvColor(140f,0f,1f).toHex())
        assertEquals("#000000",MbHsvColor(140f,1f,0f).toHex())
        assertEquals("#FF0000",MbHsvColor(-10f,2f,2f).toHex())
    }
    @Test fun refusesNonColorOrHtmlLikeInput() {
        listOf("#FFF","#00112233","red","javascript:alert(1)","#ZZZZZZ","").forEach {
            assertNull(MbHsvColor.fromHex(it))
        }
    }
}
