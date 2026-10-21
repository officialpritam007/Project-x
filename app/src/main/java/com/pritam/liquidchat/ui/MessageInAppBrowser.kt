package com.pritam.liquidchat.ui

import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.net.URI
import java.util.Locale

/** Deliberately isolated: no auth headers, JS bridge or Liquid Chat cookies. */
internal fun safeBrowserDestination(value:String):Boolean = runCatching {
    val uri=URI(value)
    uri.scheme?.lowercase(Locale.ROOT) in setOf("http","https") &&
        !uri.host.isNullOrBlank() && uri.rawUserInfo==null &&
        uri.port in -1..65535 && value.length<=2048
}.getOrDefault(false)

@Composable internal fun MessageInAppBrowser(link:String,onDismiss:()->Unit) {
    if(!safeBrowserDestination(link)) {
        LiquidAlertDialog(onDismissRequest=onDismiss,
            text={Text("This link cannot be opened safely.")},
            confirmButton={TextButton(onClick=onDismiss) {Text("Close")}})
        return
    }
    val context=LocalContext.current
    var scripts by remember(link) {mutableStateOf(false)}
    var currentUrl by remember(link) {mutableStateOf(link)}
    var canGoBack by remember(link) {mutableStateOf(false)}
    val browser=remember(link) {
        WebView(context).apply {
            setBackgroundColor(android.graphics.Color.WHITE)
            settings.javaScriptEnabled=false
            settings.domStorageEnabled=false
            settings.allowFileAccess=false
            settings.allowContentAccess=false
            settings.setSupportMultipleWindows(false)
            settings.cacheMode=WebSettings.LOAD_NO_CACHE
            settings.safeBrowsingEnabled=true
            isLongClickable=false
            android.webkit.CookieManager.getInstance().setAcceptCookie(false)
            android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this,false)
            webViewClient=object:WebViewClient() {
                override fun shouldOverrideUrlLoading(view:WebView?,request:WebResourceRequest):Boolean =
                    !safeBrowserDestination(request.url.toString())

                override fun onPageFinished(view:WebView?,url:String?) {
                    super.onPageFinished(view,url)
                    if(url!=null)currentUrl=url
                    canGoBack=view?.canGoBack()==true
                }
            }
            loadUrl(link)
        }
    }
    BackHandler {if(canGoBack)browser.goBack() else onDismiss()}
    DisposableEffect(browser) {
        onDispose {
            browser.stopLoading()
            browser.clearHistory()
            browser.destroy()
        }
    }
    Dialog(onDismissRequest=onDismiss,
        properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal=8.dp),
                    verticalAlignment=Alignment.CenterVertically) {
                    IconButton(onClick={if(canGoBack)browser.goBack() else onDismiss()}) {
                        Icon(Icons.Outlined.ArrowBack,"Back")
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Liquid Chat Browser",style=MaterialTheme.typography.titleMedium)
                        Text(runCatching {URI(currentUrl).host.orEmpty()}.getOrDefault(""),
                            style=MaterialTheme.typography.labelSmall,
                            color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1)
                    }
                    IconButton(onClick={browser.reload()}) {
                        Icon(Icons.Outlined.Refresh,"Refresh webpage")
                    }
                    TextButton(onClick=onDismiss) {Text("Close")}
                }
                Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),
                    verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Website scripts",style=MaterialTheme.typography.labelLarge)
                        Text("Off by default. Enable only for sites you trust.",
                            style=MaterialTheme.typography.labelSmall,
                            color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked=scripts,onCheckedChange={scripts=it})
                }
                HorizontalDivider()
                AndroidView(
                    factory={browser},
                    modifier=Modifier.fillMaxWidth().weight(1f),
                    update={view->
                        if(view.settings.javaScriptEnabled!=scripts) {
                            view.settings.javaScriptEnabled=scripts
                            view.reload()
                        }
                        val url=view.url
                        if(url!=null && url!=currentUrl)currentUrl=url
                        val available=view.canGoBack()
                        if(available!=canGoBack)canGoBack=available
                    }
                )
            }
        }
    }
}
