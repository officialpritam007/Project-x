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

/**
 * MBWhatsApp 3.3.4's packaged mb_preferences.xml has a top icon followed by
 * rounded, divided option groups and a separate navigation host. Recreate
 * that structure with six *working Liquid Chat routes*, not copied WhatsApp
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
    val all=visual+themes+controls
}

@Composable private fun MbReferenceActionRow(
    route:MbReferencePreference,
    onNavigate:(String)->Unit
) {
    Row(Modifier.fillMaxWidth().heightIn(min=56.dp).clip(Glass.compact)
        .clickable(role=Role.Button,onClick={onNavigate(route.target)})
        .padding(horizontal=12.dp,vertical=12.dp),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(14.dp)) {
        Icon(route.icon,contentDescription=null,
            modifier=Modifier.size(23.dp),tint=MaterialTheme.colorScheme.onSurface)
        Text(route.label,Modifier.weight(1f),style=MaterialTheme.typography.bodyLarge)
        Icon(Icons.Outlined.ChevronRight,contentDescription=null,
            tint=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.size(22.dp))
    }
}

@Composable private fun MbReferenceSection(
    heading:String,
    hint:String,
    routes:List<MbReferencePreference>,
    onNavigate:(String)->Unit
) {
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(heading.uppercase(),Modifier.padding(start=12.dp),
            style=MaterialTheme.typography.labelMedium,
            fontWeight=FontWeight.SemiBold,
            color=MaterialTheme.colorScheme.primary)
        GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
            Text(hint,style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            routes.forEachIndexed {i,route->
                if(i>0) HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
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
        .padding(horizontal=16.dp,vertical=12.dp)
        .padding(bottom=98.dp+WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick=onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack,contentDescription="Back to Settings")
            }
            Text("Liquid Chat Preferences",style=MaterialTheme.typography.headlineSmall)
        }
        GlassSurface(Modifier.fillMaxWidth(),Glass.hero) {
            Row(Modifier.fillMaxWidth().padding(18.dp),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(72.dp).background(
                    MaterialTheme.colorScheme.primaryContainer,CircleShape)
                    .semantics {contentDescription="Liquid Chat Preferences icon"},
                    contentAlignment=Alignment.Center) {
                    Icon(Icons.Outlined.Tune,contentDescription=null,
                        modifier=Modifier.size(34.dp),tint=MaterialTheme.colorScheme.primary)
                }
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text("Personalize Liquid Chat",style=MaterialTheme.typography.titleLarge)
                    Text("Home, conversations, themes and privacy",
                        style=MaterialTheme.typography.bodySmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
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
        Text("These settings control the actual features of Liquid Chat.",
            Modifier.fillMaxWidth().padding(horizontal=12.dp),
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
