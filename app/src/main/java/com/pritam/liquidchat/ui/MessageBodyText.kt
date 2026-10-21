package com.pritam.liquidchat.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

/** Preview/link colors use stable readable values on both glass light and dark themes. */
internal fun linkedTextColor(preset:String,themeColor:Color,dark:Boolean):Color = when(preset) {
    "Blue"->if(dark)Color(0xFF93C5FD) else Color(0xFF1D4ED8)
    "Green"->if(dark)Color(0xFF86EFAC) else Color(0xFF047857)
    "Purple"->if(dark)Color(0xFFC4B5FD) else Color(0xFF7C3AED)
    "Rose"->if(dark)Color(0xFFFDA4AF) else Color(0xFFBE123C)
    "Amber"->if(dark)Color(0xFFFCD34D) else Color(0xFF92400E)
    else->themeColor
}

/** Inline links remain plain text during message selection; no automatic URL requests. */
@Composable internal fun MessageBodyText(
    body:String,fontSize:Int,expanded:Boolean,linksEnabled:Boolean,
    textColor:Color=Color.Unspecified,onTextLayout:(TextLayoutResult)->Unit
) {
    val uriHandler=LocalUriHandler.current
    val color=linkedTextColor(LocalDisplayOptions.current.linkColor,MaterialTheme.colorScheme.primary,LocalGlassDark.current)
    var failed by remember(body) {mutableStateOf(false)}
    val annotated=remember(body,linksEnabled,color,uriHandler) {
        buildAnnotatedString {
            append(body)
            if(linksEnabled)messageWebLinks(body).forEach {link->
                val browserUrl=androidBrowserUrl(link)
                addLink(LinkAnnotation.Url(browserUrl,
                    styles=TextLinkStyles(style=SpanStyle(color=color,textDecoration=TextDecoration.Underline)),
                    linkInteractionListener={
                        try {uriHandler.openUri(browserUrl)}catch(_:Exception) {failed=true}
                    }),link.start,link.end)
            }
        }
    }
    Text(annotated,style=MaterialTheme.typography.bodyLarge.copy(fontSize=fontSize.sp,color=textColor),
        maxLines=if(expanded)Int.MAX_VALUE else 12,overflow=TextOverflow.Ellipsis,onTextLayout=onTextLayout)
    if(failed)LiquidAlertDialog(onDismissRequest={failed=false},
        text={Text("No app could open this web link. You can copy the message instead.")},
        confirmButton={androidx.compose.material3.TextButton(onClick={failed=false}) {Text("OK")}})
}
