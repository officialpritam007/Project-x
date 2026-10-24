package com.pritam.liquidchat.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pritam.liquidchat.AppViewModel
import com.pritam.liquidchat.BuildConfig

private data class MbSettingsDashboardRoute(
    val title:String,val target:String,val icon:ImageVector
)

private val dashboardPrimary=listOf(
    MbSettingsDashboardRoute("Liquid Chat Preferences","Liquid Chat Preferences",
        Icons.Outlined.Palette),
    MbSettingsDashboardRoute("Themes","Liquid Chat Themes",Icons.Outlined.ColorLens),
    MbSettingsDashboardRoute("Tips and help","Liquid Chat Help",
        Icons.Outlined.Campaign),
    MbSettingsDashboardRoute("Service status","Liquid Chat Service Status",
        Icons.Outlined.CloudSync),
    MbSettingsDashboardRoute("Power Saving","Power Saving",
        Icons.Outlined.BatteryFull),
    MbSettingsDashboardRoute("Notifications","Liquid Chat Notifications",
        Icons.Outlined.NotificationsNone),
    MbSettingsDashboardRoute("App information","Liquid Chat Updates",
        Icons.Outlined.Info)
)
private val dashboardChats=listOf(
    MbSettingsDashboardRoute("Add Lists","Chat Folders",Icons.Outlined.FavoriteBorder),
    MbSettingsDashboardRoute("Starred messages","Starred Messages",Icons.Outlined.StarOutline),
    MbSettingsDashboardRoute("Chat Settings","Chat Settings",
        Icons.Outlined.ChatBubbleOutline)
)
private val dashboardAccount=listOf(
    MbSettingsDashboardRoute("Account","Account",Icons.Outlined.Key),
    MbSettingsDashboardRoute("Privacy","Liquid Chat Privacy",Icons.Outlined.Lock),
    MbSettingsDashboardRoute("Appearance","Appearance",Icons.Outlined.Palette),
    MbSettingsDashboardRoute("Data and storage","Data and Storage",
        Icons.Outlined.Storage),
    MbSettingsDashboardRoute("Invite a friend","Invite a friend",
        Icons.Outlined.Share)
)

@Composable private fun DashboardGroup(
    routes:List<MbSettingsDashboardRoute>,onSection:(String)->Unit
) {
    Surface(
        modifier=Modifier.fillMaxWidth(),
        shape=RoundedCornerShape(24.dp),
        color=MaterialTheme.colorScheme.surface,
        tonalElevation=0.dp,
        border=BorderStroke(1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha=.30f))
    ) {
        Column {
            routes.forEachIndexed {index,route->
                if(index>0)HorizontalDivider(
                    Modifier.padding(start=50.dp,end=14.dp),
                    thickness=.5.dp,
                    color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.7f))
                Row(
                    Modifier.fillMaxWidth().heightIn(min=48.dp)
                        .clickable(role=Role.Button,onClick={onSection(route.target)})
                        .padding(start=15.dp,end=11.dp,top=10.dp,bottom=10.dp),
                    horizontalArrangement=Arrangement.spacedBy(14.dp),
                    verticalAlignment=Alignment.CenterVertically
                ) {
                    Icon(route.icon,contentDescription=null,
                        modifier=Modifier.size(22.dp),
                        tint=MaterialTheme.colorScheme.onSurface)
                    Text(route.title,modifier=Modifier.weight(1f),fontSize=15.sp,
                        fontWeight=FontWeight.SemiBold)
                    Icon(Icons.Outlined.ChevronRight,contentDescription=null,
                        modifier=Modifier.size(18.dp),
                        tint=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** Recorded Settings home is separate from the mod preferences navigation.
 * Liquid Chat owns every route: no third-party profile status, groups, linked
 * WhatsApp device entitlement, or pretend premium purchase action.
 */
@Composable internal fun MbReferenceSettingsDashboard(
    model:AppViewModel,onSection:(String)->Unit
) {
    val profile by model.profile.collectAsStateWithLifecycle()
    val name=profile?.optString("display_name").orEmpty().ifBlank {"My account"}
    val avatar=profile?.optString("avatar_id").orEmpty().takeIf {
        it.isNotBlank()&&it!="null"
    }.orEmpty()
    val context=LocalContext.current
    var settingsQuery by rememberSaveable { mutableStateOf("") }
    val normalizedQuery=settingsQuery.trim()
    fun matches(route:MbSettingsDashboardRoute):Boolean {
        if(normalizedQuery.isEmpty())return true
        val searchable=when(route.target) {
            "Liquid Chat Preferences"->"theme font bubble animation glass navigation customize"
            "Chat Folders"->"list favorites starred folder organize chats"
            "Chat Settings"->"message bubble emoji sticker composer quick replies"
            "Liquid Chat Notifications"->"alerts quiet dnd sound ringtone vibration"
            "Liquid Chat Privacy"->"read receipts block last seen online privacy"
            "Data and Storage"->"cache clean media files download storage"
            "Appearance"->"dark light background color icon font"
            "Liquid Chat Service Status"->"service control connection supabase fcm push notification sync refresh"
            "Liquid Chat Updates"->"version release what's new developer info"
            else->""
        }
        return route.title.contains(normalizedQuery,ignoreCase=true) ||
            route.target.contains(normalizedQuery,ignoreCase=true) ||
            searchable.contains(normalizedQuery,ignoreCase=true)
    }
    val primaryResults=dashboardPrimary.filter(::matches)
    val chatResults=dashboardChats.filter(::matches)
    val accountResults=dashboardAccount.filter(::matches)
    val openSettingsAction:(String)->Unit={target->
        if(target=="Invite a friend") {
            val username=profile?.optString("username").orEmpty().trim()
            val invitation=if(username.isBlank()) "Let's chat on Liquid Chat."
                else "Find me on Liquid Chat: @$username"
            val intent=android.content.Intent(android.content.Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(android.content.Intent.EXTRA_TEXT,invitation)
            context.startActivity(android.content.Intent.createChooser(
                intent,"Invite a friend"))
        } else onSection(target)
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal=15.dp)
        .padding(bottom=98.dp+WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        verticalArrangement=Arrangement.spacedBy(17.dp)) {
        Row(Modifier.fillMaxWidth().height(54.dp),
            verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick={onSection("Appearance")}) {
                Icon(Icons.Outlined.Palette,contentDescription="Appearance settings")
            }
            Spacer(Modifier.weight(1f))
            Text("Settings",style=MaterialTheme.typography.titleLarge,
                fontWeight=FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick={onSection("Chat Folders")}) {
                Icon(Icons.Outlined.AddCircleOutline,contentDescription="Create chat list")
            }
            IconButton(onClick={onSection("Account")}) {
                Icon(Icons.Outlined.Edit,contentDescription="Edit profile")
            }
        }
        Column(Modifier.fillMaxWidth().clickable(role=Role.Button) {
                onSection("Account")
            }.padding(top=2.dp,bottom=14.dp),
            horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.spacedBy(7.dp)) {
            Text("YOUR PROFILE",fontSize=13.sp,
                color=MaterialTheme.colorScheme.primary,
                fontWeight=FontWeight.SemiBold)
            Avatar(name,avatar,model.backend,self=true,size=116.dp)
            Text(name,fontWeight=FontWeight.Bold,
                style=MaterialTheme.typography.headlineSmall,maxLines=2,
                textAlign=TextAlign.Center)
            val username=profile?.optString("username").orEmpty()
            if(username.isNotBlank())
                Text("@$username",fontSize=13.sp,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick={onSection("Liquid Chat Updates")}) {
                Text("What's new?")
            }
        }
        OutlinedTextField(
            value=settingsQuery,
            onValueChange={settingsQuery=it},
            modifier=Modifier.fillMaxWidth(),
            label={Text("Search settings")},
            leadingIcon={Icon(Icons.Outlined.Search,contentDescription=null)},
            trailingIcon={
                if(settingsQuery.isNotEmpty()) IconButton(onClick={settingsQuery=""}) {
                    Icon(Icons.Outlined.Close,contentDescription="Clear settings search")
                }
            },
            singleLine=true,
            shape=RoundedCornerShape(20.dp)
        )
        if(primaryResults.isNotEmpty())DashboardGroup(primaryResults,onSection)
        if(chatResults.isNotEmpty())DashboardGroup(chatResults,onSection)
        if(accountResults.isNotEmpty())DashboardGroup(accountResults,openSettingsAction)
        if(primaryResults.isEmpty()&&chatResults.isEmpty()&&accountResults.isEmpty()) {
            Text("No matching settings. Try Preferences for detailed controls.",
                modifier=Modifier.fillMaxWidth().padding(vertical=20.dp),
                style=MaterialTheme.typography.bodyMedium,
                color=MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign=TextAlign.Center)
        }
        Text("Liquid Chat · ${BuildConfig.VERSION_NAME}",modifier=Modifier.fillMaxWidth(),
            textAlign=TextAlign.Center,
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
