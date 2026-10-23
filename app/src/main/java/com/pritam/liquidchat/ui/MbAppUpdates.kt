package com.pritam.liquidchat.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pritam.liquidchat.BuildConfig

/** Updates belong to this app's distribution, never to the reference APK's updater. */
@Composable internal fun MbAppUpdates() {
    val context=LocalContext.current
    var notice by rememberSaveable {mutableStateOf<String?>(null)}
    fun openPage(path:String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://github.com/officialpritam007/Project-x/$path")))
        }.onFailure {notice="A browser could not be opened. Please try again after installing or enabling a browser."}
    }
    GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
        Text("Liquid Chat",style=MaterialTheme.typography.titleLarge)
        Text("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            style=MaterialTheme.typography.bodyLarge)
        Text(if(BuildConfig.DEBUG)"Development build" else "Release build",
            style=MaterialTheme.typography.labelMedium,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
    GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
        Text("App updates",style=MaterialTheme.typography.titleMedium)
        Text("View published releases and release notes for Liquid Chat. Opening this page does not download or install an APK.",
            style=MaterialTheme.typography.bodyMedium)
        Button(onClick={openPage("releases")},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {
            Text("Open Liquid Chat releases")
        }
        Text("Android requires an update to use the same app identity and a compatible signing key. Keep the existing app installed to preserve its local data.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        notice?.let {Text(it,color=MaterialTheme.colorScheme.error)}
    }
    GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
        Text("Project & support",style=MaterialTheme.typography.titleMedium)
        TextButton(onClick={openPage("issues")},modifier=Modifier.heightIn(min=48.dp)) {
            Text("Report a Liquid Chat issue")
        }
    }
}


/** Public project metadata only: no fabricated payment method, premium plan or identity. */
@Composable internal fun MbDeveloperInformation() {
    val context=LocalContext.current
    var error by remember {mutableStateOf<String?>(null)}
    fun openVerifiedUrl(url:String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)))
        }.onSuccess {error=null}
            .onFailure {error="Unable to open the browser on this device."}
    }
    GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
        Text("Developer information",style=MaterialTheme.typography.titleLarge)
        Text("Liquid Chat is developed in the Project X repository. View the public source, releases and issue tracker for authentic project information.",
            style=MaterialTheme.typography.bodyMedium)
        Text("Repository: officialpritam007/Project-x",
            style=MaterialTheme.typography.labelLarge,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick={openVerifiedUrl("https://github.com/officialpritam007")}) {
            Text("Open developer profile")
        }
        TextButton(onClick={openVerifiedUrl("https://github.com/officialpritam007/Project-x")}) {
            Text("Open source repository")
        }
        TextButton(onClick={openVerifiedUrl("https://github.com/officialpritam007/Project-x/issues")}) {
            Text("Support and issue reports")
        }
        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
    }
    GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
        Text("Donations",style=MaterialTheme.typography.titleMedium)
        Text("No verified donation destination is configured. This app does not request payments or unlock paid features.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
