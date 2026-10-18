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
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.BorderStroke
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pritam.liquidchat.AppViewModel

@Composable fun LiquidApp(model: AppViewModel) {
    val context=androidx.compose.ui.platform.LocalContext.current
    val permission=androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) {}
    val display by model.display.collectAsStateWithLifecycle()
    val allChats by model.chats.collectAsStateWithLifecycle()
    val active by model.active.collectAsStateWithLifecycle()
    val currentCall by model.calls.view.collectAsStateWithLifecycle()
    val logoutPrompt by model.logoutPrompt.collectAsStateWithLifecycle()
    val unsent by model.unsent.collectAsStateWithLifecycle()
    val ready by model.ready.collectAsStateWithLifecycle()
    val session by model.session.collectAsStateWithLifecycle()
    val chats=remember(allChats,session?.userId) {allChats.filter {it.account==session?.userId}}
    val profile by model.profile.collectAsStateWithLifecycle()
    val loaded by model.profileLoaded.collectAsStateWithLifecycle()
    val acceptedPolicy by model.acceptedPolicy.collectAsStateWithLifecycle()
    val profileError by model.profileError.collectAsStateWithLifecycle()
    val busy by model.busy.collectAsStateWithLifecycle()
    val notice by model.notice.collectAsStateWithLifecycle()
    val recovery by model.recovery.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(notice) { notice?.let {snackbar.showSnackbar(it);model.clearNotice()} }

    LaunchedEffect(session?.userId,loaded,acceptedPolicy) {
        if(session!=null&&acceptedPolicy&&loaded&&profile!=null&&android.os.Build.VERSION.SDK_INT>=33&&com.google.firebase.FirebaseApp.getApps(context).isNotEmpty()) {
            val prefs=context.getSharedPreferences("device",android.content.Context.MODE_PRIVATE)
            if(!prefs.getBoolean("asked_notifications",false)) {prefs.edit().putBoolean("asked_notifications",true).apply();permission.launch(android.Manifest.permission.POST_NOTIFICATIONS)}
        }
    }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    key(session?.userId) {
    val conversationStates=androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    val dark=when(display.appearance) {"Light"->false;"Dark"->true;else->androidx.compose.foundation.isSystemInDarkTheme()}
    CompositionLocalProvider(LocalDisplayOptions provides display) {
    LiquidTheme(dark) {
        VoicePlaybackLifecycle(session?.userId to active)
        if(logoutPrompt) AlertDialog(onDismissRequest=model::dismissLogout,title={Text("Discard unsent messages?")},text={Text("$unsent unsent messages will be removed from this device when you sign out.")},confirmButton={TextButton(onClick=model::finishLogout) {Text("Sign out")}},dismissButton={TextButton(onClick=model::dismissLogout) {Text("Cancel")}})
        Box(Modifier.fillMaxSize()) {
        Box(Modifier.matchParentSize().glassSource(0f).background(glassBackground()))
        Scaffold(snackbarHost={SnackbarHost(snackbar)}, containerColor=Color.Transparent, bottomBar={
            if(session!=null && acceptedPolicy && loaded && profile!=null && !recovery && active==null) GlassNavigation(tab) {tab=it}
        }) { padding ->
            if(!ready) { Box(Modifier.fillMaxSize().padding(padding)) {CircularProgressIndicator()} }
            else if(session==null) { Box(Modifier.padding(padding)) {AuthScreen(model,busy)} }
            else if(recovery) {Box(Modifier.padding(padding)) {RecoveryScreen(model,busy)}}
            else if(!acceptedPolicy) {Box(Modifier.padding(padding)) {UserPolicyScreen(busy,model::acceptPolicy,model::logout)}}
            else if(!loaded||(profile!=null&&profile?.optString("id")!=session?.userId)) {Column(Modifier.padding(padding).padding(24.dp)) {Text(profileError ?: "Loading your profile…");Button(onClick={model.action {model.loadProfile()}},enabled=!busy) {Text("Retry")};TextButton(onClick=model::logout) {Text("Sign out")}}}
            else if(profile==null) {Box(Modifier.padding(padding)) {ProfileScreen(model,busy)}}
            else if(active!=null) {conversationStates.SaveableStateProvider(active!!) {Box(Modifier.padding(padding)) {ConversationScreen(model,active!!,chats,busy)}}}
            else Box(Modifier.fillMaxSize().padding(top=padding.calculateTopPadding())) {
                conversationStates.SaveableStateProvider("main-tab-$tab") {
                    when(tab) {0->ChatsScreen(model,chats,onContacts={tab=1},onProfile={tab=3});1->ContactsScreen(model,chats,busy);2->SettingsScreen(model,busy);else->MyProfileScreen(model,busy,onSettings={tab=2})}
                }
            }
        }
        if(currentCall!=null && session!=null && acceptedPolicy) {
            com.pritam.liquidchat.calls.CallOverlay(model.calls,chats.firstOrNull {it.id==currentCall?.conversation}?.name,model.backend,chats.firstOrNull {it.id==currentCall?.conversation}?.avatarId.orEmpty())
        }
        }
    }
    }
    }
}
