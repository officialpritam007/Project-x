package com.pritam.liquidchat.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.ui.platform.LocalContext
import com.pritam.liquidchat.data.Images
import com.pritam.liquidchat.data.StickerFactory
import com.pritam.liquidchat.data.StickerLibrary
import com.pritam.liquidchat.data.PdfDocuments
import com.pritam.liquidchat.data.PreparedPdf
import com.pritam.liquidchat.data.PreparedChatVideo
import com.pritam.liquidchat.data.ChatVideo
import com.pritam.liquidchat.data.StoryVideo
import com.pritam.liquidchat.data.SavedMedia
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.pritam.liquidchat.AppViewModel
import com.pritam.liquidchat.data.Chat
import com.pritam.liquidchat.data.Message
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable fun SettingsScreen(model:AppViewModel,busy:Boolean) {
    // Open the reference Settings dashboard first; Preferences stays one tap away.
    // Never bury account, help, storage and chat-list routes behind a Back action.
    var section by rememberSaveable {mutableStateOf<String?>(null)}
    var fromPreferences by rememberSaveable {mutableStateOf(false)}
    var returnToHelp by rememberSaveable {mutableStateOf(false)}
    fun backToParent() {
        val target=if(returnToHelp)"Liquid Chat Help"
            else referenceSettingsBackTarget(section,fromPreferences)
        returnToHelp=false
        section=target
        if(target==null)fromPreferences=false
    }
    var unavailable by remember {mutableStateOf<String?>(null)}
    if(section==null) {
        SettingsHome(model) {title->
            val destination=title
            if(destination in listOf("Devices","Language")) unavailable=destination else {
                fromPreferences=false
                returnToHelp=false
                section=destination
            }
        }
        unavailable?.let {title->LiquidAlertDialog(onDismissRequest={unavailable=null},title={Text(title)},text={Text(when(title){"Language"->"English is the current app language.";"Chat Folders"->"Use All, Favorite, Archive and Unread in the Chats menu.";"Data and Storage"->"Shared images are loaded on demand. Automatic downloads are not enabled.";else->"Device management is not available in this version yet."})},confirmButton={TextButton(onClick={unavailable=null}){Text("OK")}})}
        return
    }
    BackHandler {backToParent()}

    if(section=="Liquid Chat Preferences") {
        MbPreferencesLanding(
            onBack={section=null;fromPreferences=false;returnToHelp=false},
            onNavigate={destination->fromPreferences=true;returnToHelp=false;section=destination}
        )
        return
    }
    if(section=="Starred Messages") {
        val activeSession by model.session.collectAsStateWithLifecycle()
        val availableChats by model.chats.collectAsStateWithLifecycle()
        val owner=activeSession?.userId
        if(owner!=null) {
            MbAllStarredMessages(model,availableChats,owner,onDismiss={
                section=null
                fromPreferences=false
            })
        } else {
            Text("Sign in to view starred messages.",
                modifier=Modifier.padding(24.dp),
                style=MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val profile by model.profile.collectAsStateWithLifecycle()
    val settings by model.repository.privateSettings.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {model.action {model.repository.loadSettings()}}
    val unsent by model.unsent.collectAsStateWithLifecycle()
    val display by model.display.collectAsStateWithLifecycle()
    val context=LocalContext.current
    var themeFeedback by remember {mutableStateOf<String?>(null)}
    var resetThemeDialog by remember {mutableStateOf(false)}
    val exportTheme=rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(MbThemeTransfer.MIME_TYPE)) {uri->
        if(uri!=null)model.action {
            val json=MbThemeTransfer.export(model.display.value)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val stream=context.contentResolver.openOutputStream(uri) ?: error("Cannot open theme file for writing.")
                stream.use {it.write(json.toByteArray(Charsets.UTF_8));it.flush()}
            }
            themeFeedback="Theme exported successfully."
        }
    }
    val importTheme=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {uri->
        if(uri!=null)model.action {
            val json=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val stream=context.contentResolver.openInputStream(uri) ?: error("Cannot open selected file.")
                stream.use {input ->
                    val buffer=ByteArray(4096)
                    val collected=java.io.ByteArrayOutputStream()
                    while(true) {
                        val n=input.read(buffer)
                        if(n<0)break
                        if(n==0)continue
                        require(collected.size()+n<=MbThemeTransfer.MAX_IMPORT_BYTES) {"Theme file is too large."}
                        collected.write(buffer,0,n)
                    }
                    collected.toString(Charsets.UTF_8.name())
                }
            }
            val next=MbThemeTransfer.decode(json,model.display.value)
            model.setDisplay(next)
            themeFeedback="Theme imported and applied."
        }
    }
    val photoThemePicker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {uri->
        if(uri!=null)model.action {
            val accent=PhotoAccentSampler.extract(context,uri)
            model.setDisplay(model.display.value.copy(accentColor="Custom",
                customAccentHex=accent))
            themeFeedback="Photo theme color applied: $accent"
        }
    }
    if(resetThemeDialog) LiquidAlertDialog(
        onDismissRequest={resetThemeDialog=false},
        title={Text("Restore default appearance?")},
        text={Text("Your custom colors, type sizes, wallpaper dimming and quick reactions will reset. Screenshots, outgoing typing, hidden inbox previews, accessibility options, messages and saved photos are unchanged.")},
        confirmButton={TextButton(onClick={
            model.setDisplay(MbThemeTransfer.resetAppearance(model.display.value))
            themeFeedback="Default appearance restored."
            resetThemeDialog=false
        }) {Text("Restore defaults")}},
        dismissButton={TextButton(onClick={resetThemeDialog=false}) {Text("Cancel")}}
    )
    val photoPicker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {uri->if(uri!=null)model.action {
        val id=model.backend.session.value?.userId ?: return@action
        val image=Images.prepare(context,uri,id)
        try {val uploaded=model.backend.uploadImage(image.file,null);model.setAvatar(uploaded.getString("id"))} finally {image.file.delete()}
    }}
    var confirm by remember {mutableStateOf(false)}
    var edit by remember {mutableStateOf(false)}
    var deleteAccount by remember {mutableStateOf(false)}
    var showPolicy by remember {mutableStateOf(false)}
    var password by remember {mutableStateOf("")}
    if(edit) {BackHandler {edit=false};Column {TextButton(onClick={edit=false}) {Text("Back to Settings")};ProfileScreen(model,busy)};return}
    val referenceSection=referenceListUsesFlatRows(display)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal=if(referenceSection)20.dp else 16.dp,
            vertical=if(referenceSection)8.dp else 12.dp)
        .padding(bottom=98.dp+WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        verticalArrangement=Arrangement.spacedBy(if(referenceSection)12.dp else 20.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min=if(referenceSection)47.dp else 0.dp),
            verticalAlignment=Alignment.CenterVertically) {IconButton(onClick={backToParent()}){Icon(Icons.AutoMirrored.Outlined.ArrowBack,"Back to Settings")};
            Text(section.orEmpty().removePrefix("Liquid Chat "),
                style=if(referenceSection)MaterialTheme.typography.titleLarge
                    else MaterialTheme.typography.headlineSmall)}
        if(section=="Liquid Chat Help") MbTipsAndHelp {destination->
            returnToHelp=true
            section=destination
        }
        if(section=="Liquid Chat Service Status") MbServiceStatus(model)
        if(section=="Liquid Chat Updates") MbAppUpdates()
        if(section=="Liquid Chat Developer Information") MbDeveloperInformation()
        if(section=="Liquid Chat Multimedia")
            MbRecordedMultimediaSettings(display,model::setDisplay)
        if(section=="Liquid Chat Animations")
            MbRecordedAnimationSettings(display,model::setDisplay)
        if(section=="Liquid Chat VIP Features")
            MbRecordedExtraFeaturesSettings(onNavigate={section=it})
        if(section=="Liquid Chat General Settings") MbGeneralAppearanceSettings(display,model::setDisplay)
        if(section=="Liquid Chat Home Screen") MbStoryPlaybackSettingsCard()
        if(section=="Liquid Chat Home Screen") HomeInteractionSettingsCard(model,display)
        if(section=="Liquid Chat Home Screen") MbNavigationVisibilitySettingsCard()
        if(section=="Liquid Chat Notifications") MbConversationSoundSettings()
        if(section=="Liquid Chat Conversation Screen") MbConversationInteractionSettings(model,display)
        if(section=="Liquid Chat Conversation Screen") ConversationKeyboardStickerSettingsCard(model,display)
        if(section=="Liquid Chat General Settings" || section=="Appearance") LauncherIconSettings()
        if(section=="Liquid Chat General Settings") MbTouchEffectsSettingsCard()
        if(section=="Data and Storage") DataStorage(model)
        if(section=="Chat Folders") MbChatFoldersSettings(model)
        if(section=="Liquid Chat Themes") GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
            Text("Themes",style=MaterialTheme.typography.titleLarge)
            Text("Save your current Liquid Chat appearance to a JSON theme, or import a saved theme.",style=MaterialTheme.typography.bodyMedium)
            Text("Imported themes change only this device's colors, navigation and chat appearance. No messages, accounts or contacts are exported.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider()
            FilledTonalButton(onClick={photoThemePicker.launch("image/*")},
                enabled=!busy,modifier=Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.AddPhotoAlternate,null)
                Spacer(Modifier.width(8.dp))
                Text("Create accent from photo")
            }
            Text("Choose a photo on this device to pick its dominant color. No photo is uploaded or stored.",
                style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
            FilledTonalButton(onClick={exportTheme.launch("LiquidChat-Theme.json")},modifier=Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.FileUpload,null)
                Spacer(Modifier.width(8.dp))
                Text("Export current theme")
            }
            OutlinedButton(onClick={importTheme.launch(arrayOf("application/json","text/plain"))},modifier=Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.FileDownload,null)
                Spacer(Modifier.width(8.dp))
                Text("Import Liquid Chat theme")
            }
            HorizontalDivider()
            OutlinedButton(onClick={resetThemeDialog=true},modifier=Modifier.fillMaxWidth()) {
                Text("Restore default appearance")
            }
            Text("This resets colors, fonts, bubble and navigation appearance. It preserves privacy settings, reduced-motion/transparency preferences, local photo files and messages.",
                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Import only Liquid Chat theme .json files.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)

            HorizontalDivider()
            Text("Saved themes on this device",style=MaterialTheme.typography.titleMedium)
            var bookmarkName by rememberSaveable {mutableStateOf("")}
            var savedThemeNames by remember {mutableStateOf(LiquidThemeLibrary.names(context))}
            var removeThemeName by remember {mutableStateOf<String?>(null)}
            var previewThemeName by remember {mutableStateOf<String?>(null)}
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value=bookmarkName,onValueChange={bookmarkName=it.take(36)},
                    label={Text("Theme name")},singleLine=true,modifier=Modifier.weight(1f))
                FilledTonalButton(enabled=bookmarkName.trim().isNotEmpty(),onClick={
                    runCatching {LiquidThemeLibrary.save(context,bookmarkName,model.display.value)}
                        .onSuccess {
                            savedThemeNames=LiquidThemeLibrary.names(context)
                            bookmarkName=""
                            themeFeedback="Theme saved on this device."
                        }
                        .onFailure {themeFeedback=it.message ?: "Unable to save theme."}
                }) {Text("Save")}
            }
            if(savedThemeNames.isEmpty()) Text("No saved themes yet.",
                style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
            savedThemeNames.forEach {name->
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text(name,Modifier.weight(1f),maxLines=1,
                        overflow=TextOverflow.Ellipsis)
                    TextButton(onClick={previewThemeName=name}) {Text("Preview")}
                    TextButton(onClick={
                        runCatching {
                            model.setDisplay(LiquidThemeLibrary.restore(context,name,model.display.value))
                        }.onSuccess {themeFeedback="Theme applied: $name"}
                            .onFailure {themeFeedback=it.message ?: "Unable to apply theme."}
                    }) {Text("Apply")}
                    IconButton(onClick={removeThemeName=name}) {
                        Icon(Icons.Outlined.DeleteOutline,"Delete saved theme: $name")
                    }
                }
            }
            previewThemeName?.let {name->
                val candidate=runCatching {
                    LiquidThemeLibrary.restore(context,name,model.display.value)
                }.getOrNull()
                if(candidate!=null) {
                    LiquidAlertDialog(
                        onDismissRequest={previewThemeName=null},
                        title={Text(name,maxLines=1,
                            overflow=TextOverflow.Ellipsis)},
                        text={MbThemePreview(candidate)},
                        confirmButton={TextButton(onClick={
                            model.setDisplay(candidate)
                            previewThemeName=null
                            themeFeedback="Theme applied: $name"
                        }) {Text("Apply theme")}},
                        dismissButton={TextButton(onClick={previewThemeName=null}) {
                            Text("Cancel")
                        }})
                } else LaunchedEffect(name) {
                    themeFeedback="Saved theme cannot be read."
                    previewThemeName=null
                }
            }
            removeThemeName?.let {name->
                LiquidAlertDialog(onDismissRequest={removeThemeName=null},
                    title={Text("Delete saved theme?")},
                    text={Text(name)},
                    confirmButton={TextButton(onClick={
                        LiquidThemeLibrary.delete(context,name)
                        savedThemeNames=LiquidThemeLibrary.names(context)
                        removeThemeName=null
                        themeFeedback="Saved theme deleted."
                    }) {Text("Delete")}},
                    dismissButton={TextButton(onClick={removeThemeName=null}) {Text("Cancel")}})
            }

            themeFeedback?.let {Text(it,color=MaterialTheme.colorScheme.primary)}
        }
        if(section=="Account") GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
            val avatar=profile?.optString("avatar_id").orEmpty()
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                Avatar(profile?.optString("display_name").orEmpty(),
                    avatar.takeUnless {it=="null"}.orEmpty(),model.backend,self=true,size=72.dp)
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text(profile?.optString("display_name").orEmpty().ifBlank {"My profile"},
                        style=MaterialTheme.typography.titleLarge,maxLines=2)
                    Text("@${profile?.optString("username").orEmpty()}",
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                    profile?.optString("bio")?.takeIf {it.isNotBlank()}?.let {
                        Text(it,style=MaterialTheme.typography.bodySmall,maxLines=3)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            FilledTonalButton(onClick={edit=true},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {
                Text("Edit profile")
            }
            OutlinedButton(
                onClick={photoPicker.launch("image/*")},enabled=!busy,
                modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)
            ) {Text(if(avatar.isBlank()||avatar=="null")"Add profile photo" else "Change profile photo")}
            if(avatar.isNotBlank()&&avatar!="null") TextButton(
                onClick={model.action {model.setAvatar()}},enabled=!busy,
                modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)
            ) {Text("Remove photo")}
        }
        if(section=="Privacy & Security") GlassCard(Modifier.fillMaxWidth()) {Text("Your privacy matters",fontWeight=FontWeight.SemiBold);Text("Chat access is restricted to participants. This version does not use end-to-end encryption.")}
        // Home and Conversation have their own appearance/interaction cards
        // even though their route classification is OTHER, not APPEARANCE.
        if(settingsPanelFor(section)==SettingsPanel.APPEARANCE ||
            section=="Liquid Chat Home Screen" ||
            section=="Liquid Chat Conversation Screen") {
            SettingsAppearanceControls(model,display,section)
        }
        if(settingsPanelFor(section)==SettingsPanel.POWER_SAVING) GlassCard(Modifier.fillMaxWidth()) {
            Text("Power Saving",style=MaterialTheme.typography.titleMedium)
            Text("Reduce animations to lower visual activity and improve battery comfort.",
                style=MaterialTheme.typography.bodyMedium,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth().heightIn(min=56.dp)
                .toggleable(value=display.reducedMotion,role=Role.Switch,onValueChange={
                    model.setDisplay(display.copy(reducedMotion=it))
                }),verticalAlignment=Alignment.CenterVertically) {
                Text("Reduced motion",Modifier.weight(1f))
                Switch(checked=display.reducedMotion,onCheckedChange=null)
            }
            HorizontalDivider()
            Text("Media downloads",style=MaterialTheme.typography.titleSmall)
            Text("Photos and voice messages load on demand. This app does not enable automatic background media downloads.",
                color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Background delivery",style=MaterialTheme.typography.titleSmall)
            Text("Android battery restrictions may delay notifications and incoming calls. The operating system controls these settings.",
                color=MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick={
                val intent=android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.parse("package:${context.packageName}"))
                runCatching {context.startActivity(intent)}
            }) {Text("Open Android app settings")}
        }
        if(section in listOf("Privacy & Security","Notifications","Liquid Chat Privacy","Liquid Chat Notifications")) {
            val privacyPage=section=="Privacy & Security" || section=="Liquid Chat Privacy"
            if(privacyPage) GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
                Text("Device privacy",style=MaterialTheme.typography.titleMedium)
                Text("Local screen and typing protection for this device.",
                    style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)

                Row(Modifier.fillMaxWidth().heightIn(min=56.dp)
                    .toggleable(value=display.preventScreenshots,role=Role.Switch,onValueChange={
                        model.setDisplay(display.copy(preventScreenshots=it))
                    }),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Block screenshots and app previews",style=MaterialTheme.typography.titleSmall)
                        Text("Android blocks screenshots and app-switcher thumbnails on this phone; a separate camera can still photograph the screen.",
                            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked=display.preventScreenshots,onCheckedChange=null)
                }
                HorizontalDivider()
                Row(Modifier.fillMaxWidth().heightIn(min=56.dp)
                    .toggleable(value=display.hideOutgoingTyping,role=Role.Switch,onValueChange={
                        model.setDisplay(display.copy(hideOutgoingTyping=it))
                    }),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Hide outgoing typing",style=MaterialTheme.typography.titleSmall)
                        Text("This device will not advertise typing to contacts. Other signed-in devices have separate settings.",
                            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked=display.hideOutgoingTyping,onCheckedChange=null)
                }
                HorizontalDivider()

            }
            GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
                Text(if(privacyPage)"Online presence & read receipts" else "Notification previews",
                    style=MaterialTheme.typography.titleMedium)
                if(privacyPage) {
                val loaded=settings.has("show_presence") && settings.has("read_receipts") && settings.has("notification_previews")
                val enabled=ghostModeActive(settings.optBoolean("show_presence",true),settings.optBoolean("read_receipts",true))
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Ghost Mode",style=MaterialTheme.typography.titleSmall)
                        Text("Hide online/last seen and new read receipts for your account.",
                            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked=enabled,enabled=loaded&&!busy,onCheckedChange={checked ->
                        model.action {
                            val next=ghostModeSettings(checked)
                            model.repository.settings(next.first,next.second,settings.optBoolean("notification_previews",true))
                        }
                    })
                }
                Text("Ghost Mode uses server-enforced account privacy. Switching it off restores both privacy controls; it cannot retract receipts already sent.",
                    style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider()

                }
            val requiredKey=if(privacyPage)"show_presence" else "notification_previews"
            if(!settings.has(requiredKey)) {
                Text(if(privacyPage)"Connect to load your privacy settings."
                    else "Connect to load your notification settings.")
                TextButton(onClick={model.action {model.repository.loadSettings()}},enabled=!busy) {
                    Text("Retry")
                }
            }
            listOf("show_presence" to "Online & last seen","read_receipts" to "Read receipts","notification_previews" to "Notification previews").filter {if(section=="Notifications" || section=="Liquid Chat Notifications")it.first=="notification_previews" else it.first!="notification_previews"}.forEach { (key,label)->
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text(label,Modifier.weight(1f))
                    Switch(checked=settings.optBoolean(key,true),enabled=!busy&&settings.has(key),onCheckedChange={checked->model.action {
                        model.repository.settings(if(key=="show_presence")checked else settings.optBoolean("show_presence",true),if(key=="read_receipts")checked else settings.optBoolean("read_receipts",true),if(key=="notification_previews")checked else settings.optBoolean("notification_previews",true))
                    }})
                }
            }

            }
        }
        if(section=="Notifications" || section=="Liquid Chat Notifications") GlassCard(
            Modifier.fillMaxWidth(),Glass.hero
        ) {
            Text("Notification icon",style=MaterialTheme.typography.titleMedium)
            Text("Changes the icon on future Liquid Chat message and call notifications.",
                style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf("Classic","Outline").forEach {style->
                    FilterChip(selected=display.notificationIconStyle==style,
                        onClick={model.setDisplay(display.copy(notificationIconStyle=style))},
                        label={Text(style)})
                }
            }
        }
        if(section=="Notifications" || section=="Liquid Chat Notifications") {
            val currentAccount by model.session.collectAsStateWithLifecycle()
            DeviceAlertSettings(currentAccount?.userId)
            NotificationSystemSettings()
        }
        if(section=="Account") OutlinedButton(onClick={confirm=true},enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text("Sign out")}
        if(section=="Account") TextButton(onClick={deleteAccount=true},enabled=!busy) {Text("Delete account",color=MaterialTheme.colorScheme.error)}
        if(section in listOf("Account","Privacy & Security","Liquid Chat Privacy")) {
            TextButton(onClick={showPolicy=true}) {Text("Privacy & User Policy")}
        }
        if(section in listOf("Account","Liquid Chat Developer Information","Liquid Chat Updates")) {
            Text("Developed by Pritam Pal\n© 2026 Liquid Chat",style=MaterialTheme.typography.bodySmall)
        }
    }
    if(showPolicy)LiquidAlertDialog(onDismissRequest={showPolicy=false},title={Text("Privacy & User Policy")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){UserPolicyText()}},confirmButton={TextButton(onClick={showPolicy=false}){Text("Done")}})
    if(deleteAccount) LiquidAlertDialog(onDismissRequest={deleteAccount=false;password=""},title={Text("Permanently delete account?")},text={Column(Modifier.verticalScroll(rememberScrollState())) {
        Text("Your profile, sessions, device tokens and owned media will be removed. Sent text remains in the other person's history under Deleted account. $unsent unsent messages will be discarded.")
        Text("Enter your password to verify. For Google accounts, sign in with Google again, then return here and confirm within five minutes.")
        PasswordField(password,{password=it},"Password (email accounts)")
        TextButton(onClick={model.action {val url=model.backend.googleUrl();context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse(url)))}}) {Text("Verify with Google")}
    }},confirmButton={TextButton(onClick={model.deleteAccount(password);password="";deleteAccount=false},enabled=!busy) {Text("Delete permanently")}},dismissButton={TextButton(onClick={deleteAccount=false;password=""}) {Text("Cancel")}})
    if(confirm) LiquidAlertDialog(onDismissRequest={confirm=false},title={Text("Sign out?")},text={Text(if(unsent>0)"$unsent unsent messages will be discarded. Saved local history, drafts and jobs will be removed from this device." else "Saved local history and account data will be removed from this device.")},confirmButton={TextButton(onClick={confirm=false;model.finishLogout()}) {Text("Sign out")}},dismissButton={TextButton(onClick={confirm=false}) {Text("Cancel")}})
}

