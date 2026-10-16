package com.pritam.liquidchat.ui

import androidx.compose.runtime.staticCompositionLocalOf

data class DisplayOptions(val appearance:String="System",val animation:String="Balanced",val reducedMotion:Boolean=false,val reducedTransparency:Boolean=false)
val LocalDisplayOptions=staticCompositionLocalOf {DisplayOptions()}
