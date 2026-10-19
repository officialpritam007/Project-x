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
    val messageFontSize:Int=16,
    val showNavLabels:Boolean=false,
    val navIconSize:Int=24,
    val navLabelSize:Int=11,
    val headerTitleSize:Int=25,
    val homeChatNameSize:Int=16,
    val homeChatPreviewSize:Int=14,
    val accentColor:String="Cyan",
    val showProfileAsTitle:Boolean=false,
    val hideArchiveAccess:Boolean=false,
    val fontPreset:String="System",
    val hideOutgoingTyping:Boolean=false
)
val LocalDisplayOptions=staticCompositionLocalOf {DisplayOptions()}
