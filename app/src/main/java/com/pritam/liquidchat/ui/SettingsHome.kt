package com.pritam.liquidchat.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pritam.liquidchat.AppViewModel
import com.pritam.liquidchat.data.Images

@Composable fun SettingsHome(model:AppViewModel,onSection:(String)->Unit) {
    if(referenceListUsesFlatRows(LocalDisplayOptions.current)) {
        MbReferenceSettingsDashboard(model,onSection)
        return
    }
    val profile by model.profile.collectAsStateWithLifecycle()
    val name=profile?.optString("display_name").orEmpty().ifBlank {"My account"}
    val avatar=profile?.optString("avatar_id").orEmpty().takeIf {it!="null"}.orEmpty()
    val reference=LocalDisplayOptions.current.navBarStyle=="Reference"
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal=if(reference)20.dp else 16.dp)
            .padding(bottom=98.dp+WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        verticalArrangement=Arrangement.spacedBy(14.dp)
    ) {
        GlassHeader("Liquid Chat Settings")
        Text("YOUR ACCOUNT",style=MaterialTheme.typography.labelMedium,
            color=MaterialTheme.colorScheme.onSurfaceVariant,
            modifier=Modifier.padding(start=10.dp,top=3.dp))
        GlassSurface(Modifier.fillMaxWidth().clickable {onSection("Account")},Glass.hero) {
            if(reference) {
                Column(Modifier.fillMaxWidth().padding(bottom=15.dp),
                    horizontalAlignment=Alignment.CenterHorizontally,
                    verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    // Reference mb_settings.xml: a 150dp visual hero with a
                    // centered 120dp avatar, followed by account text rows.
                    Box(Modifier.fillMaxWidth().height(150.dp),
                        contentAlignment=Alignment.Center) {
                        Avatar(name,avatar,model.backend,self=true,size=120.dp)
                    }
                    Text(name,style=MaterialTheme.typography.titleLarge,maxLines=2)
                    Text("@"+profile?.optString("username").orEmpty(),
                        style=MaterialTheme.typography.bodySmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines=1)
                    Text("Tap to edit Liquid Chat profile",
                        style=MaterialTheme.typography.labelSmall,
                        color=MaterialTheme.colorScheme.primary)
                }
            } else Row(
                Modifier.fillMaxWidth().heightIn(min=95.dp).padding(16.dp),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(14.dp)
            ) {
                Avatar(name,avatar,model.backend,self=true,size=66.dp)
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                    Text(name,style=MaterialTheme.typography.titleLarge,maxLines=1)
                    Text("@"+profile?.optString("username").orEmpty(),
                        style=MaterialTheme.typography.bodySmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1)
                    Text("Account",style=MaterialTheme.typography.labelMedium,
                        color=MaterialTheme.colorScheme.primary)
                }
                Icon(Icons.Outlined.ChevronRight,"Open account",tint=MaterialTheme.colorScheme.primary)
            }
        }
        Text("PREFERENCES",style=MaterialTheme.typography.labelMedium,
            color=MaterialTheme.colorScheme.onSurfaceVariant,
            modifier=Modifier.padding(start=10.dp,top=4.dp))
        GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
            val rows=listOf(
                Triple("Liquid Chat Preferences","General and conversation customization",Icons.Outlined.Palette),
                Triple("Chat Settings","Appearance, motion and accessibility",Icons.Outlined.ChatBubbleOutline),
                Triple("Privacy & Security","Last seen, read receipts and privacy",Icons.Outlined.Lock),
                Triple("Notifications","Previews and Android notification controls",Icons.Outlined.Notifications),
                Triple("Data and Storage","Storage, cache and media limits",Icons.Outlined.Storage)
            )
            rows.forEachIndexed {i,(title,subtitle,icon)->
                PremiumSettingsTile(title,subtitle,icon) {onSection(title)}
                if(i<rows.lastIndex)HorizontalDivider(
                    modifier=Modifier.padding(start=58.dp),
                    color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.5f))
            }
        }
        Text("MORE",style=MaterialTheme.typography.labelMedium,
            color=MaterialTheme.colorScheme.onSurfaceVariant,
            modifier=Modifier.padding(start=10.dp,top=4.dp))
        GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
            // Only show real working destinations. Placeholder routes falsely
            // advertise device linking, translation and server-backed folders.
            val rows=listOf(
                Triple("Power Saving","Reduced motion and battery settings",Icons.Outlined.BatteryFull)
            )
            rows.forEachIndexed {i,(title,subtitle,icon)->
                PremiumSettingsTile(title,subtitle,icon) {onSection(title)}
                if(i<rows.lastIndex)HorizontalDivider(
                    modifier=Modifier.padding(start=58.dp),
                    color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.5f))
            }
        }
        Text("Liquid Chat · © 2026",style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant,
            modifier=Modifier.fillMaxWidth().padding(vertical=13.dp),
            textAlign=androidx.compose.ui.text.style.TextAlign.Center)
    }
}
