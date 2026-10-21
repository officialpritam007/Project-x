package com.pritam.liquidchat.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Hiding only changes navigation placement; destinations remain reachable. */
internal data class MbVisibleNavigation(val visible:List<Int>,val overflow:List<Int>)

internal fun navigationVisibilityPlan(order:String?,hidden:Set<Int>):MbVisibleNavigation {
    val ids=MbNavigationOrder.parse(order)
    return MbVisibleNavigation(ids.filterNot {it in hidden},ids.filter {it in hidden})
}

internal object MbNavigationVisibility {
    private const val PREFS="liquid_navigation_visibility_v1"
    private const val KEY="hidden_tabs"
    fun parse(raw:String?):Set<Int> {
        if(raw.isNullOrBlank())return emptySet()
        val ids=raw.split(',').map {it.trim().toIntOrNull()?:return emptySet()}
        return ids.takeIf {values->values.all {it in 0..4}}?.toSet()?:emptySet()
    }
    fun preferences(context:Context):SharedPreferences=
        context.applicationContext.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
    fun read(prefs:SharedPreferences):Set<Int> =
        parse(runCatching {prefs.getString(KEY,"")}.getOrNull())
    fun setHidden(context:Context,destination:Int,hidden:Boolean) {
        require(destination in 0..4)
        val prefs=preferences(context)
        val next=read(prefs).toMutableSet().apply {if(hidden)add(destination) else remove(destination)}
        prefs.edit().putString(KEY,next.sorted().joinToString(",")).apply()
    }
}

@Composable internal fun rememberHiddenNavigationTabs():Set<Int> {
    val app=LocalContext.current.applicationContext
    val prefs=remember(app) {MbNavigationVisibility.preferences(app)}
    var hidden by remember(prefs) {mutableStateOf(MbNavigationVisibility.read(prefs))}
    DisposableEffect(prefs) {
        val listener=SharedPreferences.OnSharedPreferenceChangeListener {_,_->
            hidden=MbNavigationVisibility.read(prefs)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        hidden=MbNavigationVisibility.read(prefs)
        onDispose {prefs.unregisterOnSharedPreferenceChangeListener(listener)}
    }
    return hidden
}

@Composable internal fun MbNavigationVisibilitySettingsCard() {
    val context=LocalContext.current
    val hidden=rememberHiddenNavigationTabs()
    GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
        Text("Visible navigation tabs",style=MaterialTheme.typography.titleMedium)
        Text("Hidden tabs move into More. Calling, contacts, profile and all other destinations stay available.",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        MbNavigationOrder.titles.forEachIndexed {index,title->
            Row(Modifier.fillMaxWidth().heightIn(min=52.dp)
                .toggleable(value=index !in hidden,role=Role.Switch,onValueChange={show->
                    MbNavigationVisibility.setHidden(context,index,!show)
                }).semantics {contentDescription="Show $title tab"},
                verticalAlignment=Alignment.CenterVertically) {
                Text(title,Modifier.weight(1f),style=MaterialTheme.typography.bodyLarge)
                Switch(checked=index !in hidden,onCheckedChange=null)
            }
        }
        Text("A device-only preference. All tabs are reachable even if every tab is hidden.",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable internal fun MbNavigationOverflow(
    destinations:List<Int>,
    current:Int,
    referenceBar:Boolean,
    onTab:(Int)->Unit,
    modifier:Modifier=Modifier
) {
    val display=LocalDisplayOptions.current
    var expanded by remember {mutableStateOf(false)}
    val selected=current in destinations
    val iconSize=if(referenceBar&&display.navIconSize==24)30 else display.navIconSize
    Box(modifier) {
        Column(Modifier.fillMaxWidth()
            .then(if(referenceBar)Modifier.height(56.dp) else Modifier.heightIn(min=54.dp))
            .clip(if(referenceBar)Glass.compact else Glass.capsule)
            .background(if(selected&&!referenceBar)MaterialTheme.colorScheme.primary.copy(alpha=.18f) else Color.Transparent)
            .clickable(role=Role.Button) {expanded=true}
            .semantics {contentDescription="More navigation";this.selected=selected},
            horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
            if(!display.hideNavIcons)Icon(Icons.Outlined.MoreHoriz,contentDescription=null,
                modifier=Modifier.size(iconSize.dp),
                tint=if(selected)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            if(showEffectiveNavigationLabels(display))Text("More",fontSize=display.navLabelSize.sp,
                maxLines=1,overflow=TextOverflow.Ellipsis,
                color=if(selected)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            if(referenceBar&&selected)Box(Modifier.width(56.dp).height(3.dp)
                .clip(Glass.capsule).background(MaterialTheme.colorScheme.primary))
        }
        LiquidDropdownMenu(expanded,onDismissRequest={expanded=false}) {
            destinations.forEach {destination->
                DropdownMenuItem(text={Text(MbNavigationOrder.titles[destination])},
                    onClick={expanded=false;onTab(destination)},
                    trailingIcon={if(current==destination)Icon(Icons.Outlined.Check,contentDescription="Current tab")},
                    modifier=Modifier.semantics {contentDescription=MbNavigationOrder.titles[destination];this.selected=current==destination})
            }
        }
    }
}
