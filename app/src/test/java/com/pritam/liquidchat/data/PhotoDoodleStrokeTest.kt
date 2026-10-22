package com.pritam.liquidchat.data

import org.junit.Assert.*
import org.junit.Test

class PhotoDoodleStrokeTest {
    private val red=0xFFFF0000.toInt()

    @Test fun validStrokeRequiresFiniteBoundedPoints() {
        val stroke=Images.DrawStroke(
            listOf(Images.DrawPoint(.1f,.2f),Images.DrawPoint(.9f,.85f)),red)
        assertTrue(stroke.valid())
        assertTrue(Images.validDrawing(listOf(stroke)))
        assertFalse(Images.DrawPoint(Float.NaN,.1f).valid())
        assertFalse(Images.DrawStroke(listOf(Images.DrawPoint(-.1f,.5f)),red).valid())
        assertFalse(Images.DrawStroke(emptyList(),red).valid())
    }
    @Test fun drawingLimitsNumberOfStrokesAndPoints() {
        val stroke=Images.DrawStroke(listOf(Images.DrawPoint(.3f,.3f)),red)
        assertFalse(Images.validDrawing(emptyList()))
        assertTrue(Images.validDrawing(List(25){stroke}))
        assertFalse(Images.validDrawing(List(26){stroke}))
        assertFalse(Images.DrawStroke(List(501){Images.DrawPoint(.2f,.4f)},red).valid())
        assertFalse(Images.DrawStroke(listOf(Images.DrawPoint(.5f,.5f)),red,.06f).valid())
    }
}
