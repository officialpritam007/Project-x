package com.pritam.liquidchat.ui

import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.platform.PlatformTextInputMethodRequest

/** Add the Android privacy hint without replacing actions or another owner's flags. */
internal fun incognitoImeOptions(original:Int,enabled:Boolean):Int =
    if(enabled) original or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING else original

/**
 * Preserve the connection, composition, selection and keyboard actions;
 * change only the no-personalized-learning hint.
 */
internal class MbIncognitoTextInputRequest(
    private val delegate:PlatformTextInputMethodRequest,
    private val enabled:Boolean
):PlatformTextInputMethodRequest {
    override fun createInputConnection(outAttributes:EditorInfo):InputConnection {
        val connection=delegate.createInputConnection(outAttributes)
        outAttributes.imeOptions=incognitoImeOptions(outAttributes.imeOptions,enabled)
        return connection
    }
}

/** Optional IME hint; individual keyboards may ignore it. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable internal fun MbIncognitoKeyboard(
    enabled:Boolean,
    content:@Composable ()->Unit
) {
    val interceptor=remember(enabled) {
        PlatformTextInputInterceptor {request,nextHandler->
            nextHandler.startInputMethod(MbIncognitoTextInputRequest(request,enabled))
        }
    }
    InterceptPlatformTextInput(interceptor,content)
}
