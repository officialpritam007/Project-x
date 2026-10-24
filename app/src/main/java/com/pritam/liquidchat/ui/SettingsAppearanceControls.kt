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

/** Dedicated appearance controls; settings routing remains in SettingsScreen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun SettingsAppearanceControls(model:AppViewModel,display:DisplayOptions,section:String?) {
        if(settingsPanelFor(section)==SettingsPanel.APPEARANCE) {
            if(section=="Liquid Chat General Settings") GlassSurface(Modifier.fillMaxWidth(),Glass.hero) {
                Column(Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("General settings",style=MaterialTheme.typography.titleLarge)
                    Text("Customize appearance, motion and storage using working Liquid Chat controls.",
                        style=MaterialTheme.typography.bodySmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
                Text(if(section=="Liquid Chat General Settings")"Theme & typography" else "Appearance",
                    style=MaterialTheme.typography.titleMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {listOf("System","Light","Dark").forEach {value->FilterChip(selected=display.appearance==value,onClick={model.setDisplay(display.copy(appearance=value))},label={Text(value)})}}
            Text("Main theme color",style=MaterialTheme.typography.labelLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf("Cyan","Blue","Green","Purple","Rose","Amber").forEach {value->
                    FilterChip(selected=display.accentColor==value,onClick={model.setDisplay(display.copy(accentColor=value))},label={Text(value)})
                }
            }
            Text("Custom accent color · #RRGGBB",style=MaterialTheme.typography.labelLarge)
            var accentHexInput by rememberSaveable {mutableStateOf(display.customAccentHex)}
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value=accentHexInput,
                    onValueChange={accentHexInput=it.take(7)},
                    label={Text("Hex color")},
                    singleLine=true,
                    isError=accentHexInput.isNotBlank() && normalizedAccentHex(accentHexInput)==null,
                    modifier=Modifier.weight(1f)
                )
                FilledTonalButton(
                    enabled=normalizedAccentHex(accentHexInput)!=null,
                    onClick={
                        normalizedAccentHex(accentHexInput)?.let {hex->
                            model.setDisplay(display.copy(accentColor="Custom",customAccentHex=hex))
                        }
                    }
                ) {Text("Apply")}
            }
            MbColorPickerButton("Main theme color",
                normalizedAccentHex(accentHexInput)?:display.customAccentHex) {hex->
                accentHexInput=hex
                model.setDisplay(display.copy(accentColor="Custom",customAccentHex=hex))
            }
            if(display.accentColor=="Custom") Text(
                "Custom accent applied across Liquid Chat · ${display.customAccentHex}",
                style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.primary
            )
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Random accent on next launch")
                    Text("Choose a different Liquid Chat color on each new app session.",
                        style=MaterialTheme.typography.bodySmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked=display.randomAccentOnLaunch,
                    onCheckedChange={model.setDisplay(display.copy(randomAccentOnLaunch=it))})
            }
            Text("App font family",style=MaterialTheme.typography.labelLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf("System","Serif","Mono","Cursive").forEach {value ->
                    FilterChip(selected=display.fontPreset==value,
                        onClick={model.setDisplay(display.copy(fontPreset=value))},label={Text(value)})
                }
            }
            Text("Preset choices use Android's built-in fonts. Imported fonts stay on this device.",
                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Bold title font family",style=MaterialTheme.typography.labelLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf("Follow","System","Serif","Mono","Cursive").forEach {value->
                    FilterChip(selected=display.titleFontPreset==value,
                        onClick={model.setDisplay(display.copy(titleFontPreset=value))},
                        label={Text(if(value=="Follow")"Follow app font" else value)})
                }
            }
            Text("Headings and titles can use a separate font without changing message text.",
                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            MbFontImportSettings(
                onChooseBody={model.setDisplay(display.copy(fontPreset="Imported"))},
                onChooseTitle={model.setDisplay(display.copy(titleFontPreset="Imported"))},
                onRemoveBody={
                    if(model.display.value.fontPreset=="Imported")
                        model.setDisplay(model.display.value.copy(fontPreset="System"))
                },
                onRemoveTitle={
                    if(model.display.value.titleFontPreset=="Imported")
                        model.setDisplay(model.display.value.copy(titleFontPreset="Follow"))
                }
            )
            }
            GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
                Text("Motion & accessibility",style=MaterialTheme.typography.titleMedium)
            Text("Animation level",style=MaterialTheme.typography.labelLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {listOf("Normal","Balanced","Extra").forEach {value->FilterChip(selected=display.animation==value,onClick={model.setDisplay(display.copy(animation=value))},enabled=!display.reducedMotion,label={Text(value)})}}
            Row(verticalAlignment=Alignment.CenterVertically) {Text("Reduced motion",Modifier.weight(1f));Switch(display.reducedMotion,{model.setDisplay(display.copy(reducedMotion=it))})}
            }
            GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
                Text("Liquid Glass effects",style=MaterialTheme.typography.titleMedium)
            Row(verticalAlignment=Alignment.CenterVertically) {Text("Reduced transparency",Modifier.weight(1f));Switch(display.reducedTransparency,{model.setDisplay(display.copy(reducedTransparency=it))})}
            HorizontalDivider()
            Text("Liquid Glass blur intensity",style=MaterialTheme.typography.titleSmall)
            Text("${display.blurIntensity}%",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(value=display.blurIntensity.toFloat(),onValueChange={value->model.setDisplay(display.copy(blurIntensity=value.toInt()))},valueRange=0f..100f,enabled=!display.reducedTransparency,modifier=Modifier.fillMaxWidth())
            Text("Lower values reduce blur; Reduced transparency overrides this setting.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        }
        // MB General's cache control reuses the already-safe Coil-only cleanup.
        // Never expose "clear all" actions that would erase the durable Room outbox.
        if(section=="Liquid Chat General Settings") {
            val accountId by model.session.collectAsStateWithLifecycle()
            MbQuickReplySettings(accountId?.userId)
            DataStorage(model)
        }
        
if(section=="Liquid Chat Home Screen") {
            GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
                Text("Home Screen",style=MaterialTheme.typography.titleLarge)
                Text("Personalize your Liquid Chat inbox; all settings work with existing conversations.", 
                    style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
                Text("Bottom navigation & tabs",style=MaterialTheme.typography.titleMedium)
                Text("Navigation bar",style=MaterialTheme.typography.titleSmall)
                Row(Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    listOf("Reference" to "iOS navigation", "Liquid" to "Liquid capsule").forEach { (value,label)->
                        FilterChip(selected=display.navBarStyle==value,
                            onClick={model.setDisplay(display.copy(navBarStyle=value))},
                            label={Text(label)})
                    }
                }
            Text("Hold and drag a handle to reorder tabs; use arrows for accessible controls.",
                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            MbNavigationReorderRows(display.navigationOrder) {next->
                model.setDisplay(display.copy(navigationOrder=next))
            }
            TextButton(onClick={model.setDisplay(display.copy(navigationOrder=MbNavigationOrder.DEFAULT))},
                enabled=display.navigationOrder!=MbNavigationOrder.DEFAULT) {
                Text("Reset navigation order")
            }
            HorizontalDivider()

            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("Show navigation labels",Modifier.weight(1f))
                Switch(display.showNavLabels,{model.setDisplay(display.copy(showNavLabels=it))},
                    enabled=!display.hideNavIcons)
            }
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Hide navigation icons")
                    Text("Labels stay visible so every tab remains identifiable.",
                        style=MaterialTheme.typography.bodySmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked=display.hideNavIcons,
                    onCheckedChange={model.setDisplay(display.copy(hideNavIcons=it))})
            }
            Text("Navigation icon size · ${display.navIconSize} dp",style=MaterialTheme.typography.titleSmall)
            Slider(value=display.navIconSize.toFloat(),onValueChange={model.setDisplay(display.copy(navIconSize=it.toInt()))},
                valueRange=20f..50f,steps=29,modifier=Modifier.fillMaxWidth())
            Text("Navigation label size · ${display.navLabelSize} sp",style=MaterialTheme.typography.titleSmall)
            Slider(value=display.navLabelSize.toFloat(),onValueChange={model.setDisplay(display.copy(navLabelSize=it.toInt()))},
                valueRange=8f..20f,steps=11,enabled=showEffectiveNavigationLabels(display),modifier=Modifier.fillMaxWidth())
            }
            GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
                Text("Inbox header & filters",style=MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text("Hide filter chips",Modifier.weight(1f))
                    Switch(checked=display.hideHomeFilters,
                        onCheckedChange={model.setDisplay(display.copy(hideHomeFilters=it))})
                }
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Hide Stories carousel")
                        Text("Stories remain available from the Home options menu.",
                            style=MaterialTheme.typography.bodySmall,
                            color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked=display.hideStoriesCarousel,
                        onCheckedChange={model.setDisplay(display.copy(hideStoriesCarousel=it))})
                }
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Hide home search",style=MaterialTheme.typography.titleSmall)
                        Text("The search field disappears but can be restored here.",
                            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked=display.hideHomeSearch,
                        onCheckedChange={model.setDisplay(display.copy(hideHomeSearch=it))})
                }
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text("Hide photo shortcut",Modifier.weight(1f))
                    Switch(checked=display.hidePhotoShortcut,
                        onCheckedChange={model.setDisplay(display.copy(hidePhotoShortcut=it))})
                }
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("Show my profile name as title",Modifier.weight(1f))
                Switch(display.showProfileAsTitle,{model.setDisplay(display.copy(showProfileAsTitle=it))})
            }
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Hide Archive filter")
                    Text("Archived conversations remain saved.",style=MaterialTheme.typography.bodySmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(display.hideArchiveAccess,{model.setDisplay(display.copy(hideArchiveAccess=it))})
            }
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Hide chat-list message previews",style=MaterialTheme.typography.titleSmall)
                    Text("Shows a generic label instead of the last message in Chats. Does not change push notification previews.",
                        style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked=display.hideHomePreviews,
                    onCheckedChange={model.setDisplay(display.copy(hideHomePreviews=it))})
            }
            HorizontalDivider()
            Text("Header title size · ${display.headerTitleSize} sp",style=MaterialTheme.typography.titleSmall)
            Slider(value=display.headerTitleSize.toFloat(),onValueChange={model.setDisplay(display.copy(headerTitleSize=it.toInt()))},
                valueRange=20f..30f,steps=9,modifier=Modifier.fillMaxWidth())
            }
            GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
                Text("Home wallpaper",style=MaterialTheme.typography.titleMedium)
                Text("Photo wallpaper dimming · ${display.homeWallpaperDim}%",style=MaterialTheme.typography.titleSmall)
            Slider(value=display.homeWallpaperDim.toFloat(),onValueChange={model.setDisplay(display.copy(homeWallpaperDim=it.toInt()))},
                valueRange=0f..100f,modifier=Modifier.fillMaxWidth())
            Text("50% keeps the previous Home wallpaper appearance. Changes apply to locally selected photos.",
                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
                Text("Chat list & typography",style=MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Show chat dividers",style=MaterialTheme.typography.titleSmall)
                        Text("Show separators between conversation rows.",
                            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked=display.showHomeDividers,
                        onCheckedChange={model.setDisplay(display.copy(showHomeDividers=it))})
                }
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text("Show pinned chat indicators",Modifier.weight(1f))
                    Switch(checked=display.showPinBadges,
                        onCheckedChange={model.setDisplay(display.copy(showPinBadges=it))})
                }
                Text("Chat avatar size · ${display.homeAvatarSize} dp",style=MaterialTheme.typography.titleSmall)
            Slider(value=display.homeAvatarSize.toFloat(),onValueChange={model.setDisplay(display.copy(homeAvatarSize=it.toInt()))},
                valueRange=40f..68f,steps=27,modifier=Modifier.fillMaxWidth())
            Text("Conversation row spacing · ${display.homeRowPadding} dp",style=MaterialTheme.typography.titleSmall)
            Slider(value=display.homeRowPadding.toFloat(),onValueChange={model.setDisplay(display.copy(homeRowPadding=it.toInt()))},
                valueRange=6f..20f,steps=13,modifier=Modifier.fillMaxWidth())
            Text("Adjusts avatar diameter and vertical padding without removing unread badges, previews or chat actions.",
                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider()
            Text("Conversation name size · ${display.homeChatNameSize} sp",style=MaterialTheme.typography.titleSmall)
            Slider(value=display.homeChatNameSize.toFloat(),onValueChange={model.setDisplay(display.copy(homeChatNameSize=it.toInt()))},
                valueRange=10f..30f,steps=19,modifier=Modifier.fillMaxWidth())
            Text("Conversation preview size · ${display.homeChatPreviewSize} sp",style=MaterialTheme.typography.titleSmall)
            Slider(value=display.homeChatPreviewSize.toFloat(),onValueChange={model.setDisplay(display.copy(homeChatPreviewSize=it.toInt()))},
                valueRange=12f..18f,steps=5,modifier=Modifier.fillMaxWidth())

            }
        }
                if(section=="Liquid Chat Conversation Screen") {
            GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
                Text("Conversation Screen",style=MaterialTheme.typography.titleLarge)
                Text("Customize private chats without changing messages, delivery or call behavior.",
                    style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            MbContactInfoStyleSettingsCard()
            MbExpandableSettingsCard("Conversation header") {
                Text("Contact name size · ${display.chatHeaderNameSize} sp",style=MaterialTheme.typography.titleSmall)
                Slider(value=display.chatHeaderNameSize.toFloat(),
                    onValueChange={model.setDisplay(display.copy(chatHeaderNameSize=it.toInt()))},
                    valueRange=12f..24f,steps=11,modifier=Modifier.fillMaxWidth())
                MbReferenceChoicePicker(
                    title="Call, video and menu icon colors",
                    choices=listOf("Theme","Accent","Text"),
                    selected=display.chatHeaderIconColor,
                    onSelect={mode->model.setDisplay(display.copy(chatHeaderIconColor=mode))})
                Text("Changing icon colors never changes contact actions or calling availability.",
                    style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            MbExpandableSettingsCard("Media upload quality") {
                MbReferenceChoicePicker(title="Photo upload quality",
                    choices=listOf("Standard","HD"),
                    selected=display.photoQuality,
                    onSelect={quality->
                        model.setDisplay(display.copy(photoQuality=quality))
                    })
                MbReferenceChoicePicker(title="Video upload quality",
                    choices=listOf("Standard","HD"),
                    selected=display.videoQuality,
                    onSelect={quality->
                        model.setDisplay(display.copy(videoQuality=quality))
                    })
                Text("Photos use device-private JPEG processing. Video quality selects 480p or 720p on-device H.264 encoding; clips stay within 30 seconds and 8 MB.",
                    style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            MbExpandableSettingsCard("Copy message preferences") {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Include sender and time when copying")
                        Text("Off copies message text only, including multi-selection.",
                            style=MaterialTheme.typography.bodySmall,
                            color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked=display.copySenderAndTime,
                        onCheckedChange={model.setDisplay(display.copy(copySenderAndTime=it))})
                }
            }
            MbExpandableSettingsCard("Wallpaper & contrast") {
                Text("Photo wallpaper dimming · ${display.chatWallpaperDim}%",style=MaterialTheme.typography.titleSmall)
            Slider(value=display.chatWallpaperDim.toFloat(),onValueChange={model.setDisplay(display.copy(chatWallpaperDim=it.toInt()))},
                valueRange=0f..100f,modifier=Modifier.fillMaxWidth())
            Text("50% preserves the original conversation wallpaper contrast. Only locally selected photos use this control.",
                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            MbExpandableSettingsCard("Message bubbles") {
            if(display.navBarStyle=="Reference")
                MbReferenceSeekbarRow("Bubble transparency",display.bubbleOpacity,
                    45..100,"%",onApply={opacity->
                        model.setDisplay(display.copy(bubbleOpacity=opacity))
                    })
            else {
            Text("Bubble opacity · ${display.bubbleOpacity}%",style=MaterialTheme.typography.titleSmall)
            Slider(value=display.bubbleOpacity.toFloat(),
                onValueChange={model.setDisplay(display.copy(bubbleOpacity=it.toInt()))},
                valueRange=45f..100f,steps=54,modifier=Modifier.fillMaxWidth())
            }
                MbReferenceBubbleStylePicker(
                    selected=display.bubbleStyle,
                    onSelect={style->
                        model.setDisplay(display.copy(bubbleStyle=style))
                    })
                MbReferenceChoicePicker(title="Message bubble color",
                    choices=listOf("Original","Green","Lavender","Rose","Night"),
                    selected=display.bubbleColor,
                    onSelect={palette->
                        model.setDisplay(display.copy(bubbleColor=palette))
                    })
            Text("Incoming and outgoing bubble colors",style=MaterialTheme.typography.titleSmall)
            Text("Follow uses the shared bubble palette above; independent colors override it.",
                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            listOf(false,true).forEach {mine->
                val selected=if(mine)display.outgoingBubblePreset else display.incomingBubblePreset
                Text(if(mine)"Outgoing bubble" else "Incoming bubble",style=MaterialTheme.typography.labelMedium)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    listOf("Follow","Original","Green","Lavender","Rose","Night").forEach {palette->
                        FilterChip(selected=selected==palette,
                            onClick={model.setDisplay(if(mine)display.copy(outgoingBubblePreset=palette)
                                else display.copy(incomingBubblePreset=palette))},label={Text(palette)})
                    }
                }
            }

            Text("Pick independent bubble colors",style=MaterialTheme.typography.titleSmall)
            listOf(true,false).forEach {mine->
                val current=if(mine)display.outgoingBubbleHex else display.incomingBubbleHex
                var input by rememberSaveable(mine) {mutableStateOf(current)}
                MbColorPickerButton(if(mine)"Outgoing bubble" else "Incoming bubble",current) {hex->
                    input=hex
                    model.setDisplay(if(mine)
                        display.copy(outgoingBubbleHex=hex,outgoingBubblePreset="Custom")
                    else display.copy(incomingBubbleHex=hex,incomingBubblePreset="Custom"))
                }
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value=input,
                        onValueChange={input=it.take(7)},
                        label={Text(if(mine)"Outgoing #RRGGBB" else "Incoming #RRGGBB")},
                        isError=input.isNotEmpty() && normalizedAccentHex(input)==null,
                        singleLine=true,
                        modifier=Modifier.weight(1f)
                    )
                    FilledTonalButton(
                        enabled=normalizedAccentHex(input)!=null,
                        onClick={
                            normalizedAccentHex(input)?.let {hex->
                                model.setDisplay(if(mine)
                                    display.copy(outgoingBubbleHex=hex,outgoingBubblePreset="Custom")
                                else display.copy(incomingBubbleHex=hex,incomingBubblePreset="Custom"))
                            }
                        }
                    ) {Text("Apply")}
                }
            }
            Text("Message ink & timestamps",style=MaterialTheme.typography.titleSmall)
            listOf("Outgoing text","Incoming text","Quoted text","Message time").forEach {target->
                val selected=when(target) {
                    "Outgoing text"->display.outgoingInk
                    "Incoming text"->display.incomingInk
                    "Quoted text"->display.quotedInk
                    else->display.timestampInk
                }
                Text(target,style=MaterialTheme.typography.labelMedium)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    listOf("Theme","Blue","Green","Purple","Rose","Amber","Custom").forEach {palette->
                        FilterChip(selected=selected==palette,
                            onClick={model.setDisplay(when(target) {
                                "Outgoing text"->display.copy(outgoingInk=palette)
                                "Incoming text"->display.copy(incomingInk=palette)
                                "Quoted text"->display.copy(quotedInk=palette)
                                else->display.copy(timestampInk=palette)
                            })},label={Text(palette)})
                    }
                }
                val currentHex=when(target) {
                    "Outgoing text"->display.outgoingInkHex
                    "Incoming text"->display.incomingInkHex
                    "Quoted text"->display.quotedInkHex
                    else->display.timestampInkHex
                }
                var hexInput by rememberSaveable(target) {mutableStateOf(currentHex)}
                MbColorPickerButton(target,currentHex) {hex->
                    hexInput=hex
                    model.setDisplay(when(target) {
                        "Outgoing text"->display.copy(outgoingInk="Custom",outgoingInkHex=hex)
                        "Incoming text"->display.copy(incomingInk="Custom",incomingInkHex=hex)
                        "Quoted text"->display.copy(quotedInk="Custom",quotedInkHex=hex)
                        else->display.copy(timestampInk="Custom",timestampInkHex=hex)
                    })
                }
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value=hexInput,
                        onValueChange={hexInput=it.take(7)},
                        label={Text("Custom #RRGGBB")},
                        singleLine=true,
                        isError=hexInput.isNotEmpty()&&normalizedAccentHex(hexInput)==null,
                        modifier=Modifier.weight(1f)
                    )
                    FilledTonalButton(
                        enabled=normalizedAccentHex(hexInput)!=null,
                        onClick={
                            normalizedAccentHex(hexInput)?.let {hex->
                                model.setDisplay(when(target) {
                                    "Outgoing text"->display.copy(outgoingInk="Custom",outgoingInkHex=hex)
                                    "Incoming text"->display.copy(incomingInk="Custom",incomingInkHex=hex)
                                    "Quoted text"->display.copy(quotedInk="Custom",quotedInkHex=hex)
                                    else->display.copy(timestampInk="Custom",timestampInkHex=hex)
                                })
                            }
                        }
                    ) {Text("Apply")}
                }
            }
            }
            GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
                Text("Reactions & text",style=MaterialTheme.typography.titleMedium)
                Text("Quick reaction emojis · choose 1–6",style=MaterialTheme.typography.titleSmall)
            Text("Only reactions accepted by the current messaging server can be selected.",
                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            androidx.compose.foundation.layout.FlowRow(
                modifier=Modifier.fillMaxWidth(),
                horizontalArrangement=Arrangement.spacedBy(8.dp),
                verticalArrangement=Arrangement.spacedBy(4.dp)) {
                val selected=MbQuickReactions.selected(display.quickReactions)
                MbQuickReactions.available.forEach {emoji->
                    FilterChip(selected=emoji in selected,
                        onClick={model.setDisplay(display.copy(
                            quickReactions=MbQuickReactions.toggle(display.quickReactions,emoji)))},
                        label={Text(emoji)},
                        enabled=(emoji in selected && selected.size>1) ||
                                (emoji !in selected && selected.size<6))
                }
            }
            TextButton(onClick={model.setDisplay(display.copy(
                quickReactions=MbQuickReactions.defaultSetting))}) {Text("Reset quick reactions")}
            Text("Web link color",style=MaterialTheme.typography.titleSmall)
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf("Theme","Blue","Green","Purple","Rose","Amber").forEach {palette->
                    FilterChip(selected=display.linkColor==palette,
                        onClick={model.setDisplay(display.copy(linkColor=palette))},label={Text(palette)})
                }
            }
            Text("Applies to real HTTPS/HTTP links in message text without changing the link or browser action.",
                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(display.navBarStyle=="Reference") {
                MbReferenceSeekbarRow("Message font size",display.messageFontSize,
                    10..30,"sp",onApply={size->
                        model.setDisplay(display.copy(messageFontSize=size))
                    })
                // Reference's emoji scale controls this device's real picker.
                MbReferenceSeekbarRow("Emoji picker size",display.emojiPickerSize,
                    18..32,"sp",onApply={size->
                        model.setDisplay(display.copy(emojiPickerSize=size))
                    })
            } else {
            Text("Message font size",style=MaterialTheme.typography.titleSmall)
            Text("${display.messageFontSize} sp",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(value=display.messageFontSize.toFloat(),onValueChange={value->model.setDisplay(display.copy(messageFontSize=value.toInt()))},
                valueRange=10f..30f,steps=19,modifier=Modifier.fillMaxWidth())
            // MB 3.3.4 mb_emoji_sizer: changes the real emoji picker, not message content.
            Text("Emoji picker size · ${display.emojiPickerSize} sp",style=MaterialTheme.typography.titleSmall)
            Slider(value=display.emojiPickerSize.toFloat(),
                onValueChange={value->model.setDisplay(display.copy(emojiPickerSize=value.toInt()))},
                valueRange=18f..32f,steps=13,modifier=Modifier.fillMaxWidth())            }
            }
            SettingsMessageBubblePreview(display)

        }
}
