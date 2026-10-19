package com.pritam.liquidchat.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.material.icons.outlined.Call
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.request.ImageRequest
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import androidx.compose.ui.platform.LocalContext

@Composable fun DesignIcon(asset:String,description:String?,size:Int=24) {
    val context=LocalContext.current
    val request=remember(context,asset) {ImageRequest.Builder(context).data("file:///android_asset/figma/$asset.svg").decoderFactory(SvgDecoder.Factory()).build()}
    AsyncImage(model=request,contentDescription=description,modifier=Modifier.size(size.dp))
}
@Composable fun GlassNavigation(tab:Int,onTab:(Int)->Unit) {
    val titles=listOf("Chats","Calls","Contacts","Settings","Profile")
    // Keep the original Figma assets for the four existing tabs.
    val off=listOf("icon_7_223","icon_7_210","icon_7_214","icon_7_218")
    val on=listOf("icon_7_206","icon_7_227","icon_7_248","icon_7_269")
    GlassSurface(Modifier.fillMaxWidth().padding(horizontal=22.dp,vertical=9.dp).navigationBarsPadding(),Glass.capsule) {
        Row(Modifier.padding(6.dp).selectableGroup()) {
            titles.forEachIndexed {i,title->
                val selected=tab==i
                val reducedMotion=LocalDisplayOptions.current.reducedMotion
                val selection by animateColorAsState(
                    targetValue=if(selected)MaterialTheme.colorScheme.primary.copy(alpha=.18f) else Color.Transparent,
                    animationSpec=tween(durationMillis=if(reducedMotion)0 else 220),
                    label="navigation selection"
                )
                Box(
                    Modifier.weight(1f).heightIn(min=54.dp)
                        .clip(Glass.capsule)
                        .background(selection)
                        .clickable(role=Role.Tab) {onTab(i)}
                        .semantics {this.selected=selected;contentDescription=title},
                    contentAlignment=Alignment.Center
                ) {
                    if(i==1) {
                        Icon(androidx.compose.material.icons.Icons.Outlined.Call,null,tint=if(tab==i)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        val originalIndex=if(i>1)i-1 else i
                        DesignIcon(if(tab==i)on[originalIndex] else off[originalIndex],null)
                    }
                }
            }
        }
    }
}
@Composable fun GlassSearch(value:String,onValue:(String)->Unit,hint:String,modifier:Modifier=Modifier) {
    GlassSurface(modifier.fillMaxWidth(),Glass.capsule) {
        Row(Modifier.heightIn(min=48.dp).padding(horizontal=16.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            DesignIcon("icon_7_44",null,18)
            BasicTextField(value,onValue,modifier=Modifier.weight(1f).semantics {contentDescription=hint},singleLine=true,textStyle=MaterialTheme.typography.bodyLarge.copy(color=MaterialTheme.colorScheme.onSurface,fontWeight=FontWeight.Medium),cursorBrush=SolidColor(MaterialTheme.colorScheme.primary),decorationBox={inner->Box {if(value.isEmpty())Text(hint,color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=16.sp,fontWeight=FontWeight.Medium);inner()}})
        }
    }
}
@Composable fun GlassHeader(title:String,brand:Boolean=false,trailing:@Composable ()->Unit={}) {
    GlassSurface(Modifier.fillMaxWidth(),Glass.hero) {
        Row(Modifier.fillMaxWidth().heightIn(min=56.dp).padding(start=20.dp,end=8.dp),verticalAlignment=Alignment.CenterVertically) {
            Text(title,Modifier.weight(1f),fontSize=25.sp,fontWeight=FontWeight.SemiBold,color=if(brand)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            trailing()
        }
    }
}

/**
 * A single visual language for modal actions in Chats, Settings and Calls.
 * Keep Material's dialog focus/scrim semantics and existing action callbacks.
 * A highly translucent fallback is intentional for separate Android windows,
 * where Haze cannot reliably capture the underlying Activity.
 */
@Composable fun LiquidAlertDialog(
    onDismissRequest:()->Unit,
    confirmButton:@Composable ()->Unit,
    modifier:Modifier=Modifier,
    dismissButton:(@Composable ()->Unit)?=null,
    title:(@Composable ()->Unit)?=null,
    text:(@Composable ()->Unit)?=null
) {
    val dark=LocalGlassDark.current
    val reduced=LocalDisplayOptions.current.reducedTransparency
    AlertDialog(
        onDismissRequest=onDismissRequest,
        confirmButton=confirmButton,
        dismissButton=dismissButton,
        title=title,
        text=text,
        modifier=modifier
            .shadow(18.dp,Glass.dialog)
            .clip(Glass.dialog)
            .then(Modifier.background(
                Brush.linearGradient(listOf(
                    Color.White.copy(alpha=if(dark).10f else .28f),
                    Color.Transparent,
                    Color.White.copy(alpha=if(dark).05f else .12f)
                )),Glass.dialog
            ))
            .then(Modifier),
        shape=Glass.dialog,
        containerColor=if(dark)Color(0xFF22364B).copy(alpha=if(reduced)1f else .97f)
            else Color(0xFFF8FCFF).copy(alpha=if(reduced)1f else .96f),
        tonalElevation=0.dp,
        titleContentColor=MaterialTheme.colorScheme.onSurface,
        textContentColor=MaterialTheme.colorScheme.onSurfaceVariant
    )
}
