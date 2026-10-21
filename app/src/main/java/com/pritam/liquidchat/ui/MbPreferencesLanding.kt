package com.pritam.liquidchat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The supplied reference APK's packaged preferences layout has a top icon and
 * rounded, divided option groups and a separate navigation host. Recreate
 * that structure with six *working Liquid Chat routes*, without copying foreign
 * services or dead switches. The app already owns the bottom navigation host.
 */
internal data class MbReferencePreference(
    val label:String,
    val target:String,
    val icon:ImageVector
)

internal object MbReferencePreferenceSections {
    val visual=listOf(
        MbReferencePreference("General Settings","Liquid Chat General Settings",Icons.Outlined.Palette),
        MbReferencePreference("Home Screen","Liquid Chat Home Screen",Icons.Outlined.Home),
        MbReferencePreference("Conversation Screen","Liquid Chat Conversation Screen",Icons.Outlined.ChatBubbleOutline)
    )
    val themes=listOf(
        MbReferencePreference("Themes","Liquid Chat Themes",Icons.Outlined.ColorLens)
    )
    val controls=listOf(
        MbReferencePreference("Privacy","Liquid Chat Privacy",Icons.Outlined.Lock),
        MbReferencePreference("Notifications","Liquid Chat Notifications",Icons.Outlined.Notifications)
    )
    val utility=listOf(
        MbReferencePreference("My account","Account",Icons.Outlined.PersonOutline),
        MbReferencePreference("Data & storage","Data and Storage",Icons.Outlined.Storage),
        MbReferencePreference("Power saving","Power Saving",Icons.Outlined.BatteryFull),
        MbReferencePreference("Tips & Help","Liquid Chat Help",Icons.Outlined.HelpOutline),
        MbReferencePreference("App updates","Liquid Chat Updates",Icons.Outlined.HelpOutline)
    )
    val all=visual+themes+controls+utility
}

@Composable private fun MbReferenceActionRow(
    route:MbReferencePreference,
    onNavigate:(String)->Unit
) {
    // res/layout-v1/mb_item_settings.xml: 24dp leading/trailing icon,
    // 15dp leading inset, 10dp trailing inset and 8dp vertical padding.
    Row(Modifier.fillMaxWidth().heightIn(min=48.dp).clip(Glass.compact)
        .clickable(role=Role.Button,onClick={onNavigate(route.target)})
        .padding(start=15.dp,end=10.dp,top=8.dp,bottom=8.dp),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(16.dp)) {
        Icon(route.icon,contentDescription=null,
            modifier=Modifier.size(24.dp),tint=MaterialTheme.colorScheme.onSurface)
        Text(route.label,Modifier.weight(1f),style=MaterialTheme.typography.bodyLarge)
        Icon(Icons.Outlined.ChevronRight,contentDescription=null,
            tint=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.size(24.dp))
    }
}

@Composable private fun MbReferenceSection(
    heading:String,
    hint:String,
    routes:List<MbReferencePreference>,
    onNavigate:(String)->Unit
) {
    Column(verticalArrangement=Arrangement.spacedBy(3.dp)) {
        // Reference mb_preferences.xml positions 14sp all-caps section labels
        // 40dp from the viewport edge; this column itself has 20dp margins.
        Text(heading.uppercase(),Modifier.padding(start=20.dp,top=12.dp),
            fontSize=14.sp,fontWeight=FontWeight.SemiBold,
            color=MaterialTheme.colorScheme.primary)
        GlassCard(Modifier.fillMaxWidth(),Glass.compact) {
            routes.forEachIndexed {i,route->
                if(i>0) HorizontalDivider(
                    modifier=Modifier.padding(start=60.dp),
                    color=MaterialTheme.colorScheme.outlineVariant)
                MbReferenceActionRow(route,onNavigate)
            }
        }
    }
}

@Composable internal fun MbPreferencesLanding(
    onBack:()->Unit,
    onNavigate:(String)->Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal=20.dp,vertical=10.dp)
        .padding(bottom=98.dp+WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        verticalArrangement=Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick=onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack,contentDescription="Back to Settings")
            }
            Text("Liquid Chat Preferences",style=MaterialTheme.typography.headlineSmall)
        }
        // Reference uses a centered 110dp launcher graphic above grouped rows.
        // Show the Liquid Chat logo, never the third-party app branding.
        Box(Modifier.fillMaxWidth().padding(top=10.dp,bottom=8.dp),
            contentAlignment=Alignment.Center) {
            Icon(painter=androidx.compose.ui.res.painterResource(
                com.pritam.liquidchat.R.drawable.ic_liquid),
                contentDescription="Liquid Chat Preferences icon",
                modifier=Modifier.size(110.dp).padding(10.dp),
                tint=androidx.compose.ui.graphics.Color.Unspecified)
        }
        MbReferenceSection("Customization",
            "Customize your device's app appearance. Chat messages and contacts are unchanged.",
            MbReferencePreferenceSections.visual,onNavigate)
        MbReferenceSection("Themes",
            "Import, export or reset your Liquid Chat visual preferences.",
            MbReferencePreferenceSections.themes,onNavigate)
        MbReferenceSection("Privacy & alerts",
            "Manage actual account privacy and Android notification controls.",
            MbReferencePreferenceSections.controls,onNavigate)
        MbReferenceSection("Account & storage",
            "Profile, device cache and reduced-motion controls already available in Liquid Chat.",
            MbReferencePreferenceSections.utility,onNavigate)
        Text("These settings control the actual features of Liquid Chat.",
            Modifier.fillMaxWidth().padding(horizontal=12.dp),
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
