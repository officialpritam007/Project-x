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
    val reference=LocalDisplayOptions.current.navBarStyle=="Reference"
    Row(Modifier.fillMaxWidth().heightIn(min=if(reference)48.dp else 52.dp)
        .clip(Glass.compact)
        .clickable(enabled=enabled,role=Role.Button,onClick=onClick)
        .padding(start=if(reference)15.dp else 12.dp,
            end=if(reference)10.dp else 12.dp,
            top=if(reference)8.dp else 12.dp,
            bottom=if(reference)8.dp else 12.dp),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(if(reference)16.dp else 14.dp)) {
        Icon(icon,null,Modifier.size(if(reference)24.dp else 23.dp),
            tint=tint.copy(alpha=if(enabled)1f else .38f))
        Text(label,Modifier.weight(1f),style=MaterialTheme.typography.bodyLarge,
            color=tint.copy(alpha=if(enabled)1f else .38f))
    }
}

/** Action lists scroll as a whole on short screens instead of clipping their final action. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun LiquidActionSheet(
    title:String,onDismiss:()->Unit,content:@Composable ColumnScope.()->Unit
) {
    val reference=LocalDisplayOptions.current.navBarStyle=="Reference"
    ModalBottomSheet(onDismissRequest=onDismiss,
        sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),
        shape=androidx.compose.foundation.shape.RoundedCornerShape(
            topStart=if(reference)22.dp else 30.dp,
            topEnd=if(reference)22.dp else 30.dp),
        containerColor=glassPopupColor(),tonalElevation=0.dp) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal=if(reference)12.dp else 20.dp)
            .padding(bottom=16.dp),
            verticalArrangement=Arrangement.spacedBy(if(reference)0.dp else 4.dp)) {
            Text(title,style=MaterialTheme.typography.titleLarge,
                modifier=Modifier.padding(horizontal=if(reference)15.dp else 12.dp,
                    vertical=8.dp))
            content()
            TextButton(onClick=onDismiss,modifier=Modifier.align(Alignment.End)) {
                Text("Done")
            }
        }
    }
}

@Composable fun DesignIcon(asset:String,description:String?,size:Int=24) {
    val context=LocalContext.current
    val request=remember(context,asset) {ImageRequest.Builder(context).data("file:///android_asset/figma/$asset.svg").decoderFactory(SvgDecoder.Factory()).build()}
    AsyncImage(model=request,contentDescription=description,modifier=Modifier.size(size.dp),
        colorFilter=LocalAppChromeInk.current?.let {androidx.compose.ui.graphics.ColorFilter.tint(it)})
}
/** Authentic, owner-supplied APK navigation art, stored without any foreign branding.
 * Only used for matching visible Liquid Chat destinations in Reference mode.
 */
@Composable internal fun LiquidReferenceIcon(
    asset:String,description:String?,size:Int,tint:Color
) {
    val context=LocalContext.current
    val request=remember(context,asset) {
        ImageRequest.Builder(context)
            .data("file:///android_asset/liquid_reference/${asset}.png")
            .build()
    }
    AsyncImage(model=request,contentDescription=description,
        modifier=Modifier.size(size.dp),
        colorFilter=androidx.compose.ui.graphics.ColorFilter.tint(tint))
}
/** Never allow a fully blank navigation button when icons are hidden. */
internal fun showEffectiveNavigationLabels(options:DisplayOptions):Boolean =
    options.showNavLabels || options.hideNavIcons

@Composable fun GlassNavigation(
    tab:Int,unreadChats:Int=0,modifier:Modifier=Modifier,
    applyNavigationInset:Boolean=true,onTab:(Int)->Unit
) {
    val display=LocalDisplayOptions.current
    val titles=MbNavigationOrder.titles
    val hidden=rememberHiddenNavigationTabs()
    val plan=navigationVisibilityPlan(display.navigationOrder,hidden)
    // The reference mb_bottom_iosv2.xml uses five equal icon-and-label slots.
    // Preserve the original Liquid capsule as a selectable alternative.
    val referenceBar=display.navBarStyle=="Reference"
    // MB 3.3.4 res/layout-v1/mb_bottom_iosv2.xml:
    // 56dp row, five weighted columns, 30dp icons and 11sp captions.
    // User-adjusted size preferences still take precedence over defaults.
    val referenceIconSize=if(referenceBar&&display.navIconSize==24)30 else display.navIconSize
    // Keep the original Figma assets for the four existing tabs.
    val off=listOf("icon_7_223","icon_7_210","icon_7_214","icon_7_218")
    val on=listOf("icon_7_206","icon_7_227","icon_7_248","icon_7_269")
    GlassChromeSurface(
        modifier.fillMaxWidth()
            .then(if(referenceBar)Modifier else
                Modifier.padding(horizontal=22.dp,vertical=9.dp))
            .then(if(applyNavigationInset)Modifier.navigationBarsPadding() else Modifier),
        if(referenceBar)Glass.compact else Glass.capsule
    ) {
        Row(Modifier.fillMaxWidth()
            .then(if(referenceBar)Modifier.height(56.dp).padding(horizontal=6.dp)
                else Modifier.padding(6.dp))
            .selectableGroup()) {
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
                    Modifier.weight(1f)
                        .then(if(referenceBar)Modifier.height(56.dp)
                            else Modifier.heightIn(min=54.dp))
                        .clip(if(referenceBar)Glass.compact else Glass.capsule)
                        .background(if(referenceBar)Color.Transparent else selection)
                        .clickable(role=Role.Tab) {onTab(i)}
                        .semantics {this.selected=selected;contentDescription=title},
                    contentAlignment=Alignment.Center
                ) {
                    Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                        if(!display.hideNavIcons) {
                            // Only show a real account-scoped unread count; never a
                            // hard-coded reference badge or count from another tab.
                            BadgedBox(badge={
                                if(i==0&&unreadChats>0) Badge {
                                    Text(if(unreadChats>99)"99+" else unreadChats.toString(),
                                        fontSize=9.sp)
                                }
                            }) {
                                if(referenceBar && i in 0..3) {
                                    // Real visual counterparts: Chats, Calls,
                                    // Contacts (people), Settings. Profile keeps
                                    // its Liquid identity rather than a false
                                    // WhatsApp Status/Communities destination.
                                    val iconName=when(i) {
                                        0->"chats"
                                        1->"calls"
                                        2->"contacts"
                                        else->"settings"
                                    }
                                    LiquidReferenceIcon(
                                        "ref_${iconName}_${if(selected)"on" else "off"}",
                                        null,referenceIconSize,
                                        if(selected)MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant)
                                } else if(i==1) {
                                    Icon(androidx.compose.material.icons.Icons.Outlined.Call,null,
                                        modifier=Modifier.size(referenceIconSize.dp),
                                        tint=if(tab==i)MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    val originalIndex=if(i>1)i-1 else i
                                    DesignIcon(if(tab==i)on[originalIndex] else off[originalIndex],
                                        null,referenceIconSize)
                                }
                            }
                        }
                        if(showEffectiveNavigationLabels(display)) Text(title,fontSize=display.navLabelSize.sp,maxLines=1,
                            overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            color=if(selected)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    // The underline belongs to the parent Box, not ColumnScope.
                    // Aligning BottomCenter on a Column child does not compile.
                    if(referenceBar&&selected) Box(
                        Modifier.align(Alignment.BottomCenter).fillMaxWidth(.92f).height(3.dp)
                            .clip(Glass.capsule).background(MaterialTheme.colorScheme.primary)
                    )
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
    val display=LocalDisplayOptions.current
    // Reference res/layout-v1/mb_home_toolbar.xml specifies a 47dp home
    // toolbar, a 17sp title and 28dp action icons. Preserve the explicit
    // typography adjustment when the owner has customized headerTitleSize.
    val referenceHeader=brand&&display.navBarStyle=="Reference"
    val titleSize=if(referenceHeader&&display.headerTitleSize==25)17
        else display.headerTitleSize
    GlassChromeSurface(Modifier.fillMaxWidth(),if(referenceHeader)Glass.compact else Glass.hero) {
        Row(Modifier.fillMaxWidth().heightIn(min=if(referenceHeader)47.dp else 56.dp)
            .padding(start=if(referenceHeader)16.dp else 20.dp,
                end=if(referenceHeader)12.dp else 8.dp),
            verticalAlignment=Alignment.CenterVertically) {
            if(brand) {
                Icon(painter=androidx.compose.ui.res.painterResource(com.pritam.liquidchat.R.drawable.ic_liquid),
                    contentDescription="Liquid Chat",modifier=Modifier.size(if(referenceHeader)28.dp else 22.dp),tint=Color.Unspecified)
                Spacer(Modifier.width(if(referenceHeader)8.dp else 10.dp))
            }
            Text(title,Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis,
                fontSize=titleSize.sp,fontWeight=FontWeight.SemiBold,
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
