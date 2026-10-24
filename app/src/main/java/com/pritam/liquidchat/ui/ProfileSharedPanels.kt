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

/** Composable-only account presentation shared by profile and Settings. */
@Composable fun ProfileHero(model:AppViewModel,large:Boolean=false) {
    val profile by model.profile.collectAsStateWithLifecycle()
    val name=profile?.optString("display_name").orEmpty()
    val avatar=profile?.optString("avatar_id").orEmpty()
    val reference=referenceListUsesFlatRows(LocalDisplayOptions.current)
    Column(Modifier.fillMaxWidth().padding(vertical=6.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(if(large && reference)120.dp else if(large)103.dp else 96.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary,CircleShape)
            .border(2.dp,MaterialTheme.colorScheme.surface.copy(alpha=.9f),CircleShape),contentAlignment=Alignment.Center) {
            Text(name.split(" ").filter {it.isNotBlank()}.take(2)
                .joinToString("") {it.take(1)}.uppercase(),
                fontSize=39.sp,color=MaterialTheme.colorScheme.onPrimary)
            if(avatar.isNotEmpty()&&avatar!="null") ProtectedImage(model.backend,avatar,
                description="Your profile photo",modifier=Modifier.fillMaxSize(),
                scale=androidx.compose.ui.layout.ContentScale.Crop,
                showLoadingSurface=false)
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

@Composable internal fun PremiumSettingsTile(
    title:String,subtitle:String,
    icon:androidx.compose.ui.graphics.vector.ImageVector,
    onClick:()->Unit
) {
    val reference=LocalDisplayOptions.current.navBarStyle=="Reference"
    Row(
        Modifier.fillMaxWidth().heightIn(min=if(reference)48.dp else 61.dp)
            .clip(Glass.compact).clickable(onClick=onClick)
            .padding(horizontal=if(reference)12.dp else 8.dp,
                vertical=if(reference)8.dp else 7.dp),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(if(reference)16.dp else 12.dp)
    ) {
        if(reference) {
            Icon(icon,null,Modifier.size(24.dp),
                tint=MaterialTheme.colorScheme.primary)
        } else Box(
            Modifier.size(38.dp).background(
                MaterialTheme.colorScheme.primary.copy(alpha=.11f),Glass.compact
            ),contentAlignment=Alignment.Center
        ) {
            Icon(icon,null,Modifier.size(21.dp),tint=MaterialTheme.colorScheme.primary)
        }
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)) {
            Text(title,style=MaterialTheme.typography.titleSmall,maxLines=1)
            if(!reference)Text(subtitle,style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2)
        }
        Icon(Icons.Outlined.ChevronRight,null,Modifier.size(if(reference)24.dp else 19.dp),
            tint=MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=.7f))
    }
}

