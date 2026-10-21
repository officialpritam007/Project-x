package com.pritam.liquidchat.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.request.ImageRequest
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import androidx.compose.ui.platform.LocalContext

/** Opaque fallback keeps separate popup windows readable without backdrop capture. */
@Composable internal fun glassPopupColor():Color =
    if(LocalGlassDark.current) Color(0xFF22364B) else Color(0xFFF8FCFF)

@Composable internal fun LiquidDropdownMenu(
    expanded:Boolean,onDismissRequest:()->Unit,content:@Composable ColumnScope.()->Unit
) {
    MaterialTheme(shapes=MaterialTheme.shapes.copy(extraSmall=Glass.card)) {
        DropdownMenu(expanded=expanded,onDismissRequest=onDismissRequest,
            containerColor=glassPopupColor(),tonalElevation=0.dp,
            modifier=Modifier.widthIn(max=320.dp),content=content)
    }
}

/** Full-width targets wrap at large font scales; destructive actions stay distinct. */
@Composable internal fun GlassActionRow(
    label:String,icon:ImageVector,onClick:()->Unit,
    destructive:Boolean=false,enabled:Boolean=true
) {
    val tint=if(destructive)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(Modifier.fillMaxWidth().heightIn(min=52.dp).clip(Glass.compact)
        .clickable(enabled=enabled,role=Role.Button,onClick=onClick)
        .padding(horizontal=12.dp,vertical=12.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
        Icon(icon,null,Modifier.size(23.dp),tint=tint.copy(alpha=if(enabled)1f else .38f))
        Text(label,Modifier.weight(1f),style=MaterialTheme.typography.bodyLarge,
            color=tint.copy(alpha=if(enabled)1f else .38f))
    }
}

/** Action lists scroll as a whole on short screens instead of clipping their final action. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun LiquidActionSheet(
    title:String,onDismiss:()->Unit,content:@Composable ColumnScope.()->Unit
) {
    ModalBottomSheet(onDismissRequest=onDismiss,
        sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),
        shape=androidx.compose.foundation.shape.RoundedCornerShape(topStart=30.dp,topEnd=30.dp),
        containerColor=glassPopupColor(),tonalElevation=0.dp) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal=20.dp).padding(bottom=16.dp),
            verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(title,style=MaterialTheme.typography.titleLarge,
                modifier=Modifier.padding(horizontal=12.dp,vertical=8.dp))
            content()
            TextButton(onClick=onDismiss,modifier=Modifier.align(Alignment.End)) {Text("Done")}
        }
    }
}

@Composable fun DesignIcon(asset:String,description:String?,size:Int=24) {
    val context=LocalContext.current
    val request=remember(context,asset) {ImageRequest.Builder(context).data("file:///android_asset/figma/$asset.svg").decoderFactory(SvgDecoder.Factory()).build()}
    AsyncImage(model=request,contentDescription=description,modifier=Modifier.size(size.dp),
        colorFilter=LocalAppChromeInk.current?.let {androidx.compose.ui.graphics.ColorFilter.tint(it)})
}
/** Never allow a fully blank navigation button when icons are hidden. */
internal fun showEffectiveNavigationLabels(options:DisplayOptions):Boolean =
    options.showNavLabels || options.hideNavIcons

@Composable fun GlassNavigation(tab:Int,onTab:(Int)->Unit) {
    val display=LocalDisplayOptions.current
    val titles=MbNavigationOrder.titles
    val hidden=rememberHiddenNavigationTabs()
    val plan=navigationVisibilityPlan(display.navigationOrder,hidden)
    // The reference mb_bottom_iosv2.xml uses five equal icon-and-label slots.
    // Preserve the original Liquid capsule as a selectable alternative.
    val referenceBar=display.navBarStyle=="Reference"
    // Keep the original Figma assets for the four existing tabs.
    val off=listOf("icon_7_223","icon_7_210","icon_7_214","icon_7_218")
    val on=listOf("icon_7_206","icon_7_227","icon_7_248","icon_7_269")
    GlassChromeSurface(
        Modifier.fillMaxWidth()
            .padding(horizontal=if(referenceBar)8.dp else 22.dp,vertical=if(referenceBar)4.dp else 9.dp)
            .navigationBarsPadding(),
        if(referenceBar)Glass.card else Glass.capsule
    ) {
        Row(Modifier.padding(if(referenceBar)4.dp else 6.dp).selectableGroup()) {
            plan.visible.forEach {i->
                val title=titles[i]
                val selected=tab==i
                val reducedMotion=display.reducedMotion
                val selection by animateColorAsState(
                    targetValue=if(selected)MaterialTheme.colorScheme.primary.copy(alpha=.18f) else Color.Transparent,
                    animationSpec=tween(durationMillis=if(reducedMotion)0 else when(display.animation) {
                        "Normal" -> 160
                        "Extra" -> 360
                        else -> 240
                    }),
                    label="navigation selection"
                )
                Box(
                    Modifier.weight(1f).heightIn(min=if(referenceBar)60.dp else 54.dp)
                        .clip(if(referenceBar)Glass.compact else Glass.capsule)
                        .background(selection)
                        .clickable(role=Role.Tab) {onTab(i)}
                        .semantics {this.selected=selected;contentDescription=title},
                    contentAlignment=Alignment.Center
                ) {
                    Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                        if(!display.hideNavIcons) {
                        if(i==1) {
                            Icon(androidx.compose.material.icons.Icons.Outlined.Call,null,modifier=Modifier.size(display.navIconSize.dp),
                                tint=if(tab==i)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            val originalIndex=if(i>1)i-1 else i
                            DesignIcon(if(tab==i)on[originalIndex] else off[originalIndex],null,display.navIconSize)
                        }
                        }
                        if(showEffectiveNavigationLabels(display)) Text(title,fontSize=display.navLabelSize.sp,maxLines=1,
                            overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            color=if(selected)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if(plan.overflow.isNotEmpty()) {
                MbNavigationOverflow(
                    destinations=plan.overflow,
                    current=tab,
                    referenceBar=referenceBar,
                    onTab=onTab,
                    modifier=Modifier.weight(1f)
                )
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
    GlassChromeSurface(Modifier.fillMaxWidth(),Glass.hero) {
        Row(Modifier.fillMaxWidth().heightIn(min=56.dp).padding(start=20.dp,end=8.dp),verticalAlignment=Alignment.CenterVertically) {
            if(brand) {
                Icon(painter=androidx.compose.ui.res.painterResource(com.pritam.liquidchat.R.drawable.ic_liquid),
                    contentDescription="Liquid Chat",modifier=Modifier.size(22.dp),tint=Color.Unspecified)
                Spacer(Modifier.width(10.dp))
            }
            Text(title,Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis,
                fontSize=LocalDisplayOptions.current.headerTitleSize.sp,fontWeight=FontWeight.SemiBold,
                color=if(brand)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
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
    dismissButton: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null
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
