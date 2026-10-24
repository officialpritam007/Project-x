package com.pritam.liquidchat.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.ui.focus.onFocusChanged

/** Shared navigation shell used by Chats, Calls, Contacts, Settings, Profile. */
/** Never allow a fully blank navigation button when icons are hidden. */
internal fun showEffectiveNavigationLabels(options:DisplayOptions):Boolean =
    options.showNavLabels || options.hideNavIcons

/** Scale the reference navigation with the user's icon/label preferences. */
internal fun referenceNavigationHeight(options:DisplayOptions):Int {
    val icon=if(options.hideNavIcons)0
        else if(options.navIconSize==24)30 else options.navIconSize
    val labelSpace=if(showEffectiveNavigationLabels(options))
        options.navLabelSize+14 else 12
    return maxOf(56,icon+labelSpace)
}

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
    val referenceHeight=referenceNavigationHeight(display).dp
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
            .then(if(referenceBar)Modifier.height(referenceHeight).padding(horizontal=6.dp)
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
                        .then(if(referenceBar)Modifier.height(referenceHeight)
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
                    navigationHeight=referenceHeight,
                    onTab=onTab,
                    modifier=Modifier.weight(1f)
                )
            }
        }
    }
}
