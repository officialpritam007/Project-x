package com.pritam.liquidchat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.BorderStroke
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pritam.liquidchat.AppViewModel

/**
 * Keep a valid cached profile visible during transient request failures. An
 * initial failure without cached profile is a recoverable error, not onboarding.
 */
internal fun profileRequiresRetry(loaded:Boolean,profileId:String?,account:String?,error:String?):Boolean =
    !loaded || (profileId!=null && profileId!=account) || (profileId==null && error!=null)

@Composable fun LiquidApp(model: AppViewModel,shortcutRequest:Pair<Int,Int>?=null) {
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
    val sessionEpoch by model.backend.uiReads.sessionEpoch.collectAsStateWithLifecycle()
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
    LaunchedEffect(shortcutRequest,session?.userId) {
        if(shortcutRequest!=null && session!=null)tab=shortcutRequest.first
    }
    key(session?.userId,sessionEpoch) {
    val conversationStates=androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    val dark=when(display.appearance) {"Light"->false;"Dark"->true;else->androidx.compose.foundation.isSystemInDarkTheme()}
    CompositionLocalProvider(LocalDisplayOptions provides display) {
    LiquidTheme(dark) {
        // Match status/navigation icon brightness to the actual app palette even
        // when the user overrides Android's system dark/light appearance.
        val chromeView=androidx.compose.ui.platform.LocalView.current
        val chromeBackground=MaterialTheme.colorScheme.background
        val chromeDarkIcons=chromeBackground.luminance()>.5f
        SideEffect {
            (context as? android.app.Activity)?.window?.let {window->
                val bars=androidx.core.view.WindowCompat.getInsetsController(window,chromeView)
                bars.isAppearanceLightStatusBars=chromeDarkIcons
                bars.isAppearanceLightNavigationBars=chromeDarkIcons
                window.statusBarColor=android.graphics.Color.TRANSPARENT
                window.navigationBarColor=android.graphics.Color.TRANSPARENT
            }
        }
        VoicePlaybackLifecycle(model,session?.userId to active)
        if(logoutPrompt) LiquidAlertDialog(onDismissRequest=model::dismissLogout,title={Text("Discard unsent messages?")},text={Text("$unsent unsent messages will be removed from this device when you sign out.")},confirmButton={TextButton(onClick=model::finishLogout) {Text("Sign out")}},dismissButton={TextButton(onClick=model::dismissLogout) {Text("Cancel")}})
        MbTouchHost {
        Box(Modifier.matchParentSize().glassSource(0f).background(glassBackground()))
        Scaffold(snackbarHost={SnackbarHost(snackbar)}, containerColor=Color.Transparent,
            topBar={
                if(currentCall?.minimized==true && (currentCall?.video!=true || currentCall?.compact==true)) {
                    val caller=chats.firstOrNull {it.id==currentCall?.conversation}
                    currentCall?.let {MinimizedCallPill(model.calls,it,caller?.name)}
                }
            }, bottomBar={
            if(session!=null && acceptedPolicy && loaded && profile!=null && !recovery && active==null) {
                val unread=chats.count {it.unread>0 || it.manualUnread}
                val referenceNewChat=display.navBarStyle=="Reference" &&
                    tab==0 && !display.hideHomeFloatingButtons
                if(referenceNewChat) {
                    // Reference mb_home_tab places its 56dp new-chat action
                    // *beside* the bottom navigation, not above the chat list.
                    // Both controls share a single system navigation inset.
                    Row(Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=4.dp)
                        .navigationBarsPadding(),
                        verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        GlassNavigation(tab,unreadChats=unread,
                            modifier=Modifier.weight(1f),
                            applyNavigationInset=false) {tab=it}
                        GlassChromeSurface(Modifier.size(56.dp),Glass.capsule) {
                            IconButton(onClick={tab=2},modifier=Modifier.fillMaxSize()) {
                                Icon(Icons.Outlined.Add,contentDescription="New chat",
                                    tint=MaterialTheme.colorScheme.primary,
                                    modifier=Modifier.size(30.dp))
                            }
                        }
                    }
                } else {
                    GlassNavigation(tab,unreadChats=unread) {tab=it}
                }
            }
        }) { padding ->
            if(!ready) { Box(Modifier.fillMaxSize().padding(padding)) {CircularProgressIndicator()} }
            else if(session==null) { Box(Modifier.padding(padding)) {AuthScreen(model,busy)} }
            else if(recovery) {Box(Modifier.padding(padding)) {RecoveryScreen(model,busy)}}
            else if(!acceptedPolicy) {Box(Modifier.padding(padding)) {UserPolicyScreen(busy,model::acceptPolicy,model::logout)}}
            else if(profileRequiresRetry(loaded,profile?.optString("id"),session?.userId,profileError)) {
                MbRecoveryScreen(loading=!loaded || busy,busy=busy,
                    onRetry={model.action {model.loadProfile()}},onSignOut=model::logout,
                    modifier=Modifier.padding(padding))
            }
            else if(profile==null) {Box(Modifier.padding(padding)) {ProfileScreen(model,busy)}}
            else if(active!=null) {conversationStates.SaveableStateProvider(active!!) {Box(Modifier.padding(padding)) {
                MbIncognitoKeyboard(display.incognitoKeyboard) {ConversationScreen(model,active!!,chats,busy)}
            }}}
            else Box(Modifier.fillMaxSize().padding(top=padding.calculateTopPadding())) {
                conversationStates.SaveableStateProvider("main-tab-$tab") {
                    when(tab) {
                        0->ChatsScreen(model,chats,onContacts={tab=2},onProfile={tab=4})
                        1->CallsScreen(model,chats)
                        2->ContactsScreen(model,chats,busy)
                        3->SettingsScreen(model,busy)
                        else->MyProfileScreen(model,busy,onSettings={tab=3})
                    }
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
