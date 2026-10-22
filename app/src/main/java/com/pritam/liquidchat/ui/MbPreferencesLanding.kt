package com.pritam.liquidchat.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal const val REFERENCE_SETTINGS_LANDING="Liquid Chat Preferences"

/** Supported, intentionally Liquid Chat-owned routes seen in the owner's
 * October 22 reference screen recording. No foreign paid account or service API.
 */
internal fun referenceSettingsBackTarget(section:String?,enteredFromReference:Boolean):String?=when {
    section==REFERENCE_SETTINGS_LANDING->null
    enteredFromReference->REFERENCE_SETTINGS_LANDING
    section in setOf("Liquid Chat General Settings","Liquid Chat Conversation Screen",
        "Liquid Chat Home Screen","Liquid Chat Themes","Liquid Chat Privacy",
        "Liquid Chat Notifications","Liquid Chat Help","Liquid Chat Updates",
        "Liquid Chat Multimedia","Liquid Chat Animations","Liquid Chat VIP Features",
        "Liquid Chat Developer Information")->REFERENCE_SETTINGS_LANDING
    else->null
}

internal data class MbReferencePreference(
    val label:String,val target:String,val icon:ImageVector,
    val keywords:String=""
)

internal object MbReferencePreferenceSections {
    val controls=listOf(
        MbReferencePreference("Privacy","Liquid Chat Privacy",Icons.Outlined.LockOutline,keywords="online last seen read receipts hide typing recording block contacts security"),
        MbReferencePreference("Notifications","Liquid Chat Notifications",Icons.Outlined.NotificationsNone,keywords="disable alerts auto hide five seconds dnd do not disturb bubbles vibration ringtone")
    )
    val premium=listOf(
        MbReferencePreference("Extra features","Liquid Chat VIP Features",Icons.Outlined.AutoAwesome,keywords="contact info extras customization local preferences")
    )
    val themes=listOf(
        MbReferencePreference("Themes","Liquid Chat Themes",Icons.Outlined.Palette,keywords="light dark color palette accent white import export appearance")
    )
    val visual=listOf(
        MbReferencePreference("General configuration","Liquid Chat General Settings",Icons.Outlined.Settings,keywords="font text size screen dpi scale blur cache clean notification icon app background launcher"),
        MbReferencePreference("Home Screen","Liquid Chat Home Screen",Icons.Outlined.Home,keywords="navigation tab reordering visibility chats archive list dividers stories glass"),
        MbReferencePreference("Multimedia","Liquid Chat Multimedia",Icons.Outlined.HighQuality,keywords="HD quality photos images videos 720p 480p compression"),
        MbReferencePreference("Conversation Screen","Liquid Chat Conversation Screen",Icons.Outlined.ChatBubbleOutline,keywords="message bubbles emojis reactions stickers keyboard font reply colors"),
        MbReferencePreference("Animations","Liquid Chat Animations",Icons.Outlined.Animation,keywords="reduce motion transparency visual effects blur")
    )
    val utility=listOf(
        MbReferencePreference("My account","Account",Icons.Outlined.PersonOutline),
        MbReferencePreference("Data & storage","Data and Storage",Icons.Outlined.Storage,keywords="cache usage files media cleanup download storage"),
        MbReferencePreference("Power saving","Power Saving",Icons.Outlined.BatteryFull,keywords="battery energy performance"),
        MbReferencePreference("Tips and help","Liquid Chat Help",Icons.Outlined.HelpOutline,keywords="guide support developer questions"),
        MbReferencePreference("Developer information","Liquid Chat Developer Information",Icons.Outlined.Code,keywords="developer open source project donation information funding github"),
        MbReferencePreference("App updates","Liquid Chat Updates",Icons.Outlined.SystemUpdate,keywords="version changelog whats new release")
    )
    val all=controls+premium+themes+visual+utility
}

@Composable private fun MbVideoReferenceRow(
    route:MbReferencePreference,onNavigate:(String)->Unit
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min=46.dp)
            .clickable(role=Role.Button,onClick={onNavigate(route.target)})
            .padding(start=14.dp,end=12.dp,top=9.dp,bottom=9.dp),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(15.dp)
    ) {
        Icon(route.icon,contentDescription=null,
            modifier=Modifier.size(21.dp),tint=MaterialTheme.colorScheme.onSurface)
        Text(route.label,modifier=Modifier.weight(1f),
            style=MaterialTheme.typography.bodyMedium.copy(fontSize=15.sp),
            fontWeight=FontWeight.SemiBold)
        Icon(Icons.Outlined.ChevronRight,contentDescription=null,
            modifier=Modifier.size(19.dp),
            tint=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun MbVideoReferenceGroup(
    rows:List<MbReferencePreference>,
    onNavigate:(String)->Unit
) {
    Surface(modifier=Modifier.fillMaxWidth(),
        shape=RoundedCornerShape(22.dp),
        color=MaterialTheme.colorScheme.surface,
        border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline.copy(alpha=.30f)),
        tonalElevation=0.dp) {
        Column {
            rows.forEachIndexed {index,route->
                if(index>0) HorizontalDivider(
                    modifier=Modifier.padding(start=52.dp,end=14.dp),
                    thickness=.5.dp,
                    color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.48f))
                MbVideoReferenceRow(route,onNavigate)
            }
        }
    }
}

/** The video reference uses a white/light flat Settings canvas, stacked
 * 22dp-outline grouped rows, no redundant headings between major groups,
 * and a centered launcher mark. The actual screens remain fully owned by
 * Liquid Chat and preserve native Android accessibility semantics.
 */
@Composable internal fun MbPreferencesLanding(
    onBack:()->Unit,onNavigate:(String)->Unit
) {
    var preferenceSearch by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal=16.dp)
        .padding(bottom=98.dp+WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        verticalArrangement=Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min=47.dp),
            verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick=onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription="Back to Settings")
            }
            Text("Liquid Chat Preferences",
                style=MaterialTheme.typography.titleLarge,
                fontWeight=FontWeight.SemiBold)
        }
        if(preferenceSearch.isBlank()) Box(Modifier.fillMaxWidth().height(122.dp),
            contentAlignment=Alignment.Center) {
            Icon(painter=androidx.compose.ui.res.painterResource(
                com.pritam.liquidchat.R.drawable.ic_liquid),
                contentDescription="Liquid Chat Preferences icon",
                modifier=Modifier.size(106.dp).padding(10.dp),
                tint=Color.Unspecified)
        }
        OutlinedTextField(
            value=preferenceSearch,
            onValueChange={preferenceSearch=it},
            modifier=Modifier.fillMaxWidth(),
            placeholder={Text("Search preferences")},
            leadingIcon={Icon(Icons.Outlined.Search,contentDescription=null)},
            trailingIcon={
                if(preferenceSearch.isNotEmpty()) IconButton(onClick={preferenceSearch=""}) {
                    Icon(Icons.Outlined.Close,contentDescription="Clear preference search")
                }
            },
            singleLine=true,
            shape=RoundedCornerShape(20.dp)
        )
        if(preferenceSearch.isBlank()) {
            MbVideoReferenceGroup(MbReferencePreferenceSections.controls,onNavigate)
            MbVideoReferenceGroup(MbReferencePreferenceSections.premium,onNavigate)
            MbVideoReferenceGroup(MbReferencePreferenceSections.themes+
                MbReferencePreferenceSections.visual,onNavigate)
            Text("ACCOUNT & SUPPORT",modifier=Modifier.padding(start=10.dp),
                fontSize=12.sp,fontWeight=FontWeight.SemiBold,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
            MbVideoReferenceGroup(MbReferencePreferenceSections.utility,onNavigate)
        } else {
            val matches=MbReferencePreferenceSections.all.filter {
                it.label.contains(preferenceSearch.trim(),ignoreCase=true) ||
                    it.target.contains(preferenceSearch.trim(),ignoreCase=true) ||
                    it.keywords.contains(preferenceSearch.trim(),ignoreCase=true)
            }
            if(matches.isEmpty()) {
                Text("No matching preferences",modifier=Modifier.fillMaxWidth()
                    .padding(vertical=20.dp),
                    style=MaterialTheme.typography.bodyMedium,
                    color=MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign=androidx.compose.ui.text.style.TextAlign.Center)
            } else {
                MbVideoReferenceGroup(matches,onNavigate)
            }
        }
        Text("Liquid Chat · Working settings for your own account and device.",
            modifier=Modifier.fillMaxWidth().padding(bottom=4.dp),
            style=MaterialTheme.typography.labelSmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign=androidx.compose.ui.text.style.TextAlign.Center)
    }
}
