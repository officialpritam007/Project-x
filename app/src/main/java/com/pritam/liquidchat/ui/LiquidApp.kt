package com.pritam.liquidchat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pritam.liquidchat.AppViewModel

@Composable fun LiquidApp(model: AppViewModel) {
    val context=androidx.compose.ui.platform.LocalContext.current
    val permission=androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) {}
    val display by model.display.collectAsStateWithLifecycle()
    val chats by model.chats.collectAsStateWithLifecycle()
    val active by model.active.collectAsStateWithLifecycle()
    val ready by model.ready.collectAsStateWithLifecycle()
    val session by model.session.collectAsStateWithLifecycle()
    val profile by model.profile.collectAsStateWithLifecycle()
    val loaded by model.profileLoaded.collectAsStateWithLifecycle()
    val busy by model.busy.collectAsStateWithLifecycle()
    val notice by model.notice.collectAsStateWithLifecycle()
    val recovery by model.recovery.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(notice) { notice?.let {snackbar.showSnackbar(it);model.clearNotice()} }

    LaunchedEffect(session?.userId,loaded) {
        if(session!=null&&loaded&&profile!=null&&android.os.Build.VERSION.SDK_INT>=33&&com.google.firebase.FirebaseApp.getApps(context).isNotEmpty()) {
            val prefs=context.getSharedPreferences("device",android.content.Context.MODE_PRIVATE)
            if(!prefs.getBoolean("asked_notifications",false)) {prefs.edit().putBoolean("asked_notifications",true).apply();permission.launch(android.Manifest.permission.POST_NOTIFICATIONS)}
        }
    }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val dark=when(display.appearance) {"Light"->false;"Dark"->true;else->androidx.compose.foundation.isSystemInDarkTheme()}
    CompositionLocalProvider(LocalDisplayOptions provides display) {
    LiquidTheme(dark) {
        Scaffold(snackbarHost={SnackbarHost(snackbar)}, containerColor=MaterialTheme.colorScheme.background, bottomBar={
            if(session!=null && loaded && profile!=null && !recovery && active==null) NavigationBar(containerColor=MaterialTheme.colorScheme.surface, modifier=Modifier.navigationBarsPadding()) {
                listOf("Chats", "Contacts", "Settings").forEachIndexed { i,title ->
                    NavigationBarItem(selected=tab==i, onClick={tab=i}, icon={ Icon(listOf(Icons.Outlined.ChatBubbleOutline,Icons.Outlined.PeopleOutline,Icons.Outlined.Settings)[i], title) }, label={Text(title)})
                }
            }
        }) { padding ->
            if(!ready) { Box(Modifier.fillMaxSize().padding(padding)) {CircularProgressIndicator()} }
            else if(session==null) { Box(Modifier.padding(padding)) {AuthScreen(model,busy)} }
            else if(recovery) {Box(Modifier.padding(padding)) {RecoveryScreen(model,busy)}}
            else if(!loaded) {Column(Modifier.padding(padding).padding(24.dp)) {Text("Loading your profile…");Button(onClick={model.action {model.loadProfile()}},enabled=!busy) {Text("Retry")};TextButton(onClick=model::logout) {Text("Sign out")}}}
            else if(profile==null) {Box(Modifier.padding(padding)) {ProfileScreen(model,busy)}}
            else if(active!=null) {Box(Modifier.padding(padding)) {ConversationScreen(model,active!!,chats,busy)}}
            else Box(Modifier.fillMaxSize().padding(padding)) {
                when(tab) {0->ChatsScreen(model,chats);1->ContactsScreen(model,chats,busy);else->SettingsScreen(model,busy)}
            }
        }
    }
    }
}
