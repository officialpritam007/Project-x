package com.pritam.liquidchat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** MB expandable settings counterpart with one keyboard/TalkBack accessible target.
 * Expansion is visual only; state changes still go through the existing owner.
 */
@Composable internal fun MbExpandableSettingsCard(
    title:String,
    initiallyExpanded:Boolean=true,
    content:@Composable ColumnScope.()->Unit
) {
    var expanded by rememberSaveable(title) {mutableStateOf(initiallyExpanded)}
    val reduced=LocalDisplayOptions.current.reducedMotion
    GlassSurface(Modifier.fillMaxWidth(),Glass.hero) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(Modifier.fillMaxWidth().heightIn(min=44.dp)
                .clickable(role=Role.Button) {expanded=!expanded}
                .semantics {stateDescription=if(expanded)"Expanded" else "Collapsed"},
                verticalAlignment=Alignment.CenterVertically) {
                Text(title,Modifier.weight(1f),
                    style=MaterialTheme.typography.titleMedium)
                Icon(if(expanded)Icons.Outlined.KeyboardArrowUp
                    else Icons.Outlined.KeyboardArrowDown,
                    contentDescription=if(expanded)"Collapse $title" else "Expand $title")
            }
            AnimatedVisibility(
                visible=expanded,
                enter=expandVertically(animationSpec=tween(if(reduced)0 else 240))+
                    fadeIn(animationSpec=tween(if(reduced)0 else 180)),
                exit=shrinkVertically(animationSpec=tween(if(reduced)0 else 200))+
                    fadeOut(animationSpec=tween(if(reduced)0 else 120))
            ) {
                Column(Modifier.fillMaxWidth().padding(top=6.dp),
                    verticalArrangement=Arrangement.spacedBy(8.dp),
                    content=content)
            }
        }
    }
}
