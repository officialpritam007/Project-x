package com.pritam.liquidchat.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

/** Inline links remain plain text during message selection; no automatic URL requests. */
@Composable internal fun MessageBodyText(
    body:String,fontSize:Int,expanded:Boolean,linksEnabled:Boolean,
    onTextLayout:(TextLayoutResult)->Unit
) {
    val uriHandler=LocalUriHandler.current
    val color=MaterialTheme.colorScheme.primary
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
    Text(annotated,style=MaterialTheme.typography.bodyLarge.copy(fontSize=fontSize.sp),
        maxLines=if(expanded)Int.MAX_VALUE else 12,overflow=TextOverflow.Ellipsis,onTextLayout=onTextLayout)
    if(failed)LiquidAlertDialog(onDismissRequest={failed=false},
        text={Text("No app could open this web link. You can copy the message instead.")},
        confirmButton={androidx.compose.material3.TextButton(onClick={failed=false}) {Text("OK")}})
}
