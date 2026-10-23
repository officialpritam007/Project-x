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
                if(referenceListUsesFlatRows(LocalDisplayOptions.current)) {
                    if(menu)LiquidActionSheet("Profile options",onDismiss={menu=false}) {
                        GlassActionRow("Edit profile",Icons.Outlined.Edit,onClick={
                            menu=false;edit=true
                        })
                        val avatarId=profile?.optString("avatar_id").orEmpty()
                        if(avatarId.isNotEmpty()&&avatarId!="null") {
                            GlassActionRow("Remove photo",Icons.Outlined.DeleteOutline,
                                destructive=true,enabled=!busy,onClick={
                                    menu=false;model.action {model.setAvatar()}
                                })
                        }
                    }
                } else {
                    LiquidDropdownMenu(menu,{menu=false}) {
                        DropdownMenuItem(text={Text("Edit profile")},onClick={edit=true;menu=false})
                        val avatarId=profile?.optString("avatar_id").orEmpty()
                        if(avatarId.isNotEmpty()&&avatarId!="null")
                            DropdownMenuItem(text={Text("Remove photo")},
                                onClick={menu=false;model.action {model.setAvatar()}},enabled=!busy)
                    }
                }
            }
        }
        GlassCard(Modifier.fillMaxWidth(),
            if(referenceListUsesFlatRows(LocalDisplayOptions.current))Glass.compact else Glass.hero) {
            ProfileHero(model,large=true)
            if(!referenceListUsesFlatRows(LocalDisplayOptions.current)) {
                Text("Liquid Chat profile",Modifier.fillMaxWidth(),
                    style=MaterialTheme.typography.labelMedium,
                    color=MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign=androidx.compose.ui.text.style.TextAlign.Center)
            }
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
        Row(Modifier.fillMaxWidth().padding(5.dp),
            horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            listOf("Posts","Archived").forEachIndexed {i,title->
                MbReferenceFilterTab(label=title,selected=archived==(i==1),
                    onClick={archived=i==1},modifier=Modifier.weight(1f))
            }
        }
    }
    })
}

