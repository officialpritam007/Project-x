package com.pritam.liquidchat.ui

import android.content.Intent
import androidx.compose.ui.platform.LocalContext

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Recovery UI never displays tokens, account IDs or raw server errors. */
@Composable internal fun MbRecoveryScreen(
    loading:Boolean,
    busy:Boolean,
    onRetry:()->Unit,
    onSignOut:()->Unit,
    modifier:Modifier=Modifier
) {
    val context=LocalContext.current
    Box(modifier.fillMaxSize().padding(horizontal=24.dp),contentAlignment=Alignment.Center) {
        Column(Modifier.widthIn(max=480.dp).fillMaxWidth()
            .verticalScroll(rememberScrollState()).padding(vertical=24.dp),
            verticalArrangement=Arrangement.spacedBy(20.dp)) {
            GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
                Box(Modifier.size(72.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer,CircleShape),
                    contentAlignment=Alignment.Center) {
                    if(loading)CircularProgressIndicator(Modifier.size(34.dp)
                        .semantics {contentDescription="Loading profile"},strokeWidth=3.dp)
                    else Icon(Icons.Outlined.CloudOff,contentDescription=null,
                        modifier=Modifier.size(34.dp),tint=MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Text(if(loading)"Loading your profile" else "Let's get you connected",
                    style=MaterialTheme.typography.headlineSmall,
                    modifier=Modifier.semantics {heading();liveRegion=LiveRegionMode.Polite})
                Text(if(loading)
                    "Liquid Chat is opening your profile. This may take a moment on a slow connection."
                    else "We couldn't load your profile. Check your internet connection and try again.",
                    style=MaterialTheme.typography.bodyLarge,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
                if(!loading)Text("Retry reloads your profile without clearing messages or app data.",
                    style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Button(onClick=onRetry,enabled=!busy,
                    modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text("Retry")}
                if(!loading) OutlinedButton(onClick={
                    // No raw exception, account, contact, access token, message
                    // or media path is ever included in the report body.
                    val body="Liquid Chat problem report\\n" +
                        "Issue: Unable to load profile\\n" +
                        "App version: ${com.pritam.liquidchat.BuildConfig.VERSION_NAME}\\n" +
                        "Android API: ${android.os.Build.VERSION.SDK_INT}\\n\\n" +
                        "Please describe what happened:\\n"
                    val share=Intent(Intent.ACTION_SEND).apply {
                        type="text/plain"
                        putExtra(Intent.EXTRA_SUBJECT,"Liquid Chat problem report")
                        putExtra(Intent.EXTRA_TEXT,body)
                    }
                    runCatching {
                        context.startActivity(Intent.createChooser(share,"Send report"))
                    }
                },modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {
                    Text("Send report")
                }
                TextButton(onClick=onSignOut,
                    modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text("Sign out")}
            }
        }
    }
}
