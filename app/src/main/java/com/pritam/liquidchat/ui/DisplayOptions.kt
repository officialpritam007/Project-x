package com.pritam.liquidchat.ui

import androidx.compose.runtime.staticCompositionLocalOf

data class DisplayOptions(
    val appearance:String="System",
    val animation:String="Balanced",
    val reducedMotion:Boolean=false,
    val reducedTransparency:Boolean=false,
    val blurIntensity:Int=55,
    val bubbleStyle:String="Tail",
    val bubbleColor:String="Original",
    val messageFontSize:Int=16
)
val LocalDisplayOptions=staticCompositionLocalOf {DisplayOptions()}
