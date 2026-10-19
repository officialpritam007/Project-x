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
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {uri->if(uri!=null)model.action {
        val id=model.backend.session.value?.userId ?: return@action
        val image=Images.prepare(context,uri,id)
        try {val uploaded=model.backend.uploadImage(image.file,null);model.setAvatar(uploaded.getString("id"))} finally {image.file.delete()}
    }}
    if(edit) {BackHandler {edit=false};Column(Modifier.verticalScroll(rememberScrollState())) {TextButton(onClick={edit=false}){Text("Back to Profile")};ProfileScreen(model,busy)};return}
    MomentsPanel(model,"post",mine=true,archived=archived,modifier=Modifier.fillMaxSize().padding(horizontal=12.dp),bottomPadding=84.dp+WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),header={
    Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            IconButton(onClick={archived=!archived}) {Icon(Icons.Outlined.GridView,"Toggle archived posts")}
            Box {IconButton(onClick={menu=true}){Icon(Icons.Outlined.MoreVert,"Profile options")}
                DropdownMenu(menu,{menu=false}) {
                    DropdownMenuItem(text={Text("Edit profile")},onClick={edit=true;menu=false})
                    val avatarId=profile?.optString("avatar_id").orEmpty()
                    if(avatarId.isNotEmpty()&&avatarId!="null")
                        DropdownMenuItem(text={Text("Remove photo")},onClick={menu=false;model.action {model.setAvatar()}},enabled=!busy)
                }
            }
        }
        ProfileHero(model,large=true)
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
        GlassCard(Modifier.fillMaxWidth()) {Text("@${profile?.optString("username").orEmpty()}",fontSize=19.sp,fontWeight=FontWeight.Bold);Text("Username",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);profile?.optString("bio")?.takeIf {it.isNotBlank()}?.let {Text(it)}}
        GlassSurface(shape=Glass.capsule) {
            Row(Modifier.padding(5.dp)) {listOf("Posts","Archived").forEachIndexed {i,title->
                Text(title,Modifier.clip(Glass.capsule).background(if(archived==(i==1))MaterialTheme.colorScheme.primaryContainer else Color.Transparent).clickable {archived=i==1}.padding(horizontal=17.dp,vertical=8.dp),fontSize=14.sp,fontWeight=FontWeight.Bold,color=if(archived==(i==1))MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }}
        }
    }
    })
}

@Composable fun SettingsHome(model:AppViewModel,onSection:(String)->Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start=12.dp,end=12.dp,bottom=90.dp+WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        GlassHeader("Liquid Chat Settings")
        Box(Modifier.clickable {onSection("Account")}) {ProfileHero(model)}
        GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
            val rows=listOf(
                Triple("Account","Username, Bio, Sign-in",0xFF00A8D8),
                Triple("Chat Settings","Appearance, Animations, Accessibility",0xFFF9A600),
                Triple("Privacy & Security","Last Seen, Read Receipts",0xFF11C748),
                Triple("Notifications","Message Previews",0xFFFF3565),
                Triple("Data and Storage","Cache, Media Limits",0xFF357EEB),
                Triple("Chat Folders","Coming soon",0xFF07ACD7),
                Triple("Devices","Coming soon",0xFF00B6C9),
                Triple("Power Saving","Reduced Motion, Android Battery Settings",0xFFF99020),
                Triple("Language","Coming soon · English",0xFFB851E0))
            val icons=listOf(
                Icons.Outlined.Person,Icons.Outlined.ChatBubbleOutline,Icons.Outlined.Lock,
                Icons.Outlined.Notifications,Icons.Outlined.Storage,Icons.Outlined.Dashboard,
                Icons.Outlined.Devices,Icons.Outlined.BatteryFull,Icons.Outlined.Language
            )
            rows.forEachIndexed {i,(title,subtitle,color)->
                Row(Modifier.fillMaxWidth().heightIn(min=49.dp).clickable {onSection(title)},verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(36.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer,Glass.compact),
                        contentAlignment=Alignment.Center
                    ) {
                        Icon(icons[i],null,Modifier.size(20.dp),
                            tint=MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Column(Modifier.weight(1f)) {Text(title,fontSize=16.sp,fontWeight=FontWeight.Bold);Text(subtitle,fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                }
                if(i<rows.lastIndex)HorizontalDivider(color=Color.White.copy(alpha=.6f))
            }
        }
        Text("Developed by Pritam Pal · © 2026 Liquid Chat",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(bottom=12.dp))
    }
}
