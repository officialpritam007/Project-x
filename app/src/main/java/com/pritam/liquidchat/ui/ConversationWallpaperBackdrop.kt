package com.pritam.liquidchat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest

/**
 * Wallpaper rendering is independent of the chat/composer state machine.
 * Splitting it from ConversationScreen reduces Compose compiler work and
 * preserves private, account-scoped photo/video wallpaper handling.
 */
@Composable internal fun ConversationWallpaperBackdrop(
    owner:String?, cid:String, palette:String,
    wallpaperRevision:Int,showWallpaper:Boolean,dim:Int,foreground:Boolean
) {
    val context=LocalContext.current
    val backgroundBrush=if(showWallpaper)
        wallpaper(if(palette=="Photo" || palette=="Video") "Ice" else palette,LocalGlassDark.current)
    else Brush.linearGradient(
        listOf(MaterialTheme.colorScheme.background,MaterialTheme.colorScheme.background)
    )
    Box(Modifier.fillMaxSize().glassSource(.5f).background(backgroundBrush)) {
        if(showWallpaper && palette=="Video" && owner!=null) {
            val videoFile=remember(owner,cid,wallpaperRevision) {
                runCatching {
                    com.pritam.liquidchat.data.PrivateVideoWallpaper.location(context,owner,cid)
                }.getOrNull()
            }
            if(videoFile?.isFile==true) {
                LocalVideoWallpaper(videoFile,foreground,wallpaperRevision,Modifier.matchParentSize())
                Box(Modifier.matchParentSize().background(
                    if(LocalGlassDark.current)
                        Color.Black.copy(alpha=wallpaperDimAlpha(.46f,dim))
                    else Color.White.copy(alpha=wallpaperDimAlpha(.30f,dim))
                ))
            }
        }
        if(showWallpaper && palette=="Photo" && owner!=null) {
            val photoFile=remember(owner,cid,wallpaperRevision) {
                runCatching {
                    com.pritam.liquidchat.data.PersonalWallpaper.location(context,owner,cid)
                }.getOrNull()
            }
            if(photoFile?.isFile==true) {
                val request=remember(photoFile,wallpaperRevision) {
                    ImageRequest.Builder(context).data(photoFile)
                        .memoryCachePolicy(CachePolicy.DISABLED)
                        .diskCachePolicy(CachePolicy.DISABLED).build()
                }
                AsyncImage(
                    model=request,contentDescription=null,contentScale=ContentScale.Crop,
                    modifier=Modifier.matchParentSize()
                )
                Box(Modifier.matchParentSize().background(
                    if(LocalGlassDark.current)
                        Color.Black.copy(alpha=wallpaperDimAlpha(.46f,dim))
                    else Color.White.copy(alpha=wallpaperDimAlpha(.30f,dim))
                ))
            }
        }
    }
}
