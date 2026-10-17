package com.pritam.liquidchat.ui

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val titles=listOf("Chats","Contacts","Settings","Profile")
    val off=listOf("icon_7_223","icon_7_210","icon_7_214","icon_7_218")
    val on=listOf("icon_7_206","icon_7_227","icon_7_248","icon_7_269")
    GlassSurface(Modifier.fillMaxWidth().padding(horizontal=22.dp,vertical=9.dp).navigationBarsPadding(),Glass.capsule) {
        Row(Modifier.padding(6.dp).selectableGroup()) {
            titles.forEachIndexed {i,title->
                Box(Modifier.weight(1f).height(54.dp).clip(Glass.capsule).background(if(tab==i)Color(0xFF45CAEE).copy(alpha=.25f) else Color.Transparent).clickable(role=Role.Tab) {onTab(i)}.semantics {selected=tab==i;contentDescription=title},contentAlignment=Alignment.Center) {
                    DesignIcon(if(tab==i)on[i] else off[i],null)
                }
            }
        }
    }
}
@Composable fun GlassSearch(value:String,onValue:(String)->Unit,hint:String,modifier:Modifier=Modifier) {
    GlassSurface(modifier.fillMaxWidth(),Glass.capsule) {
        Row(Modifier.heightIn(min=44.dp).padding(horizontal=16.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            DesignIcon("icon_7_44",null,18)
            BasicTextField(value,onValue,modifier=Modifier.weight(1f).semantics {contentDescription=hint},singleLine=true,textStyle=MaterialTheme.typography.bodyLarge.copy(color=MaterialTheme.colorScheme.onSurface,fontWeight=FontWeight.Bold),cursorBrush=SolidColor(MaterialTheme.colorScheme.primary),decorationBox={inner->Box {if(value.isEmpty())Text(hint,color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=18.sp,fontWeight=FontWeight.Bold);inner()}})
        }
    }
}
@Composable fun GlassHeader(title:String,brand:Boolean=false,trailing:@Composable ()->Unit={}) {
    GlassSurface(Modifier.fillMaxWidth(),Glass.hero) {
        Row(Modifier.fillMaxWidth().heightIn(min=56.dp).padding(start=20.dp,end=8.dp),verticalAlignment=Alignment.CenterVertically) {
            Text(title,Modifier.weight(1f),fontSize=26.sp,fontWeight=FontWeight.Bold,color=if(brand)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            trailing()
        }
    }
}
