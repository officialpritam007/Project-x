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

@Composable fun ProfileHero(model:AppViewModel,large:Boolean=false) {
    val profile by model.profile.collectAsStateWithLifecycle()
    val name=profile?.optString("display_name").orEmpty()
    val avatar=profile?.optString("avatar_id").orEmpty()
    Column(Modifier.fillMaxWidth().padding(vertical=6.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(if(large)103.dp else 96.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF18C5DA),Color(0xFF008CB9)))).border(2.dp,Color.White.copy(alpha=.9f),CircleShape),contentAlignment=Alignment.Center) {
            if(avatar.isNotEmpty()&&avatar!="null") ProtectedImage(model.backend,avatar,description="Your profile photo",modifier=Modifier.fillMaxSize(),scale=androidx.compose.ui.layout.ContentScale.Crop)
            else Text(name.split(" ").filter {it.isNotBlank()}.take(2).joinToString("") {it.take(1)}.uppercase(),fontSize=39.sp,color=Color.White)
        }
        Text(name.ifBlank {"My Profile"},fontSize=25.sp,fontWeight=FontWeight.Bold)
        // A user profile is not a presence indicator. Never label anyone online without
        // an authenticated, live presence observation that respects privacy settings.
        val username=profile?.optString("username").orEmpty().takeIf {it.isNotBlank()}
        val bio=profile?.optString("bio").orEmpty().takeIf {it.isNotBlank()}
        username?.let {Text("@$it",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        if(large&&bio!=null)Text(bio,fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines=2)
    }
}
@Composable fun MyProfileScreen(model:AppViewModel,busy:Boolean,onSettings:()->Unit) {
    val profile by model.profile.collectAsStateWithLifecycle()
    var edit by rememberSaveable {mutableStateOf(false)}
    var archived by rememberSaveable {mutableStateOf(false)}
    var menu by remember {mutableStateOf(false)}
    val context=LocalContext.current
    val clipboard=LocalClipboardManager.current
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {uri->if(uri!=null)model.action {
        val id=model.backend.session.value?.userId ?: return@action
        val image=Images.prepare(context,uri,id)
        try {val uploaded=model.backend.uploadImage(image.file,null);model.setAvatar(uploaded.getString("id"))} finally {image.file.delete()}
    }}
    if(edit) {BackHandler {edit=false};Column(Modifier.verticalScroll(rememberScrollState())) {TextButton(onClick={edit=false}){Text("Back to Profile")};ProfileScreen(model,busy)};return}
    MomentsPanel(model,"post",mine=true,archived=archived,modifier=Modifier.fillMaxSize().padding(horizontal=12.dp),bottomPadding=84.dp+WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),header={
    Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.SpaceBetween) {
            IconButton(onClick={archived=!archived}) {Icon(Icons.Outlined.GridView,"Toggle archived posts")}
            Text("My Profile",style=MaterialTheme.typography.titleLarge,
                fontWeight=FontWeight.SemiBold)
            Box {IconButton(onClick={menu=true}){Icon(Icons.Outlined.MoreVert,"Profile options")}
                LiquidDropdownMenu(menu,{menu=false}) {
                    DropdownMenuItem(text={Text("Edit profile")},onClick={edit=true;menu=false})
                    val avatarId=profile?.optString("avatar_id").orEmpty()
                    if(avatarId.isNotEmpty()&&avatarId!="null")
                        DropdownMenuItem(text={Text("Remove photo")},onClick={menu=false;model.action {model.setAvatar()}},enabled=!busy)
                }
            }
        }
        GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
            ProfileHero(model,large=true)
            Text("Liquid Chat profile",Modifier.fillMaxWidth(),
                style=MaterialTheme.typography.labelMedium,
                color=MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign=androidx.compose.ui.text.style.TextAlign.Center)
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            val actions=listOf(
                "Set Photo" to Icons.Outlined.PhotoCamera,
                "Edit Info" to Icons.Outlined.Edit,
                "Settings" to Icons.Outlined.Settings
            )
            actions.forEachIndexed {i,(label,icon)->
                GlassSurface(Modifier.weight(1f)) {
                    Column(
                        Modifier.fillMaxWidth().heightIn(min=72.dp)
                            .clickable(enabled=!busy) {
                                when(i){0->picker.launch("image/*");1->edit=true;else->onSettings()}
                            }.padding(8.dp),
                        horizontalAlignment=Alignment.CenterHorizontally,
                        verticalArrangement=Arrangement.Center
                    ) {
                        Icon(icon,contentDescription=null,tint=MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        Text(label,fontSize=13.sp,fontWeight=FontWeight.SemiBold)
                    }
                }
            }
        }
        GlassCard(Modifier.fillMaxWidth()) {
            val username=profile?.optString("username").orEmpty()
            Text("@$username",fontSize=19.sp,fontWeight=FontWeight.Bold)
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("Username",fontSize=13.sp,modifier=Modifier.weight(1f),
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
                if(username.isNotBlank())IconButton(onClick={
                    clipboard.setText(AnnotatedString("@$username"))
                }) {Icon(Icons.Outlined.ContentCopy,"Copy username")}
            }
            profile?.optString("bio")?.takeIf {it.isNotBlank()}?.let {bio->
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text(bio,modifier=Modifier.weight(1f))
                    IconButton(onClick={clipboard.setText(AnnotatedString(bio))}) {
                        Icon(Icons.Outlined.ContentCopy,"Copy profile description")
                    }
                }
            }
        }
        GlassSurface(shape=Glass.capsule) {
            Row(Modifier.padding(5.dp)) {listOf("Posts","Archived").forEachIndexed {i,title->
                Text(title,Modifier.clip(Glass.capsule).background(if(archived==(i==1))MaterialTheme.colorScheme.primaryContainer else Color.Transparent).clickable {archived=i==1}.padding(horizontal=17.dp,vertical=8.dp),fontSize=14.sp,fontWeight=FontWeight.Bold,color=if(archived==(i==1))MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }}
        }
    }
    })
}

@Composable private fun PremiumSettingsTile(
    title:String,subtitle:String,
    icon:androidx.compose.ui.graphics.vector.ImageVector,
    onClick:()->Unit
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min=61.dp)
            .clip(Glass.compact).clickable(onClick=onClick)
            .padding(horizontal=8.dp,vertical=7.dp),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(38.dp).background(
                MaterialTheme.colorScheme.primary.copy(alpha=.11f),Glass.compact
            ),contentAlignment=Alignment.Center
        ) {
            Icon(icon,null,Modifier.size(21.dp),tint=MaterialTheme.colorScheme.primary)
        }
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)) {
            Text(title,style=MaterialTheme.typography.titleSmall,maxLines=1)
            Text(subtitle,style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2)
        }
        Icon(Icons.Outlined.ChevronRight,null,Modifier.size(19.dp),
            tint=MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=.7f))
    }
}

@Composable fun SettingsHome(model:AppViewModel,onSection:(String)->Unit) {
    val profile by model.profile.collectAsStateWithLifecycle()
    val name=profile?.optString("display_name").orEmpty().ifBlank {"My account"}
    val avatar=profile?.optString("avatar_id").orEmpty().takeIf {it!="null"}.orEmpty()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal=16.dp)
            .padding(bottom=98.dp+WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        verticalArrangement=Arrangement.spacedBy(14.dp)
    ) {
        GlassHeader("Liquid Chat Settings")
        Text("YOUR ACCOUNT",style=MaterialTheme.typography.labelMedium,
            color=MaterialTheme.colorScheme.onSurfaceVariant,
            modifier=Modifier.padding(start=10.dp,top=3.dp))
        GlassSurface(Modifier.fillMaxWidth().clickable {onSection("Account")},Glass.hero) {
            Row(
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
