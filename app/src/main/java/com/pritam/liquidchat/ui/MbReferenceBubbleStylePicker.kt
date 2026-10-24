package com.pritam.liquidchat.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A selectable 3-column grid, matching the owner's video modal structure.
 * The shapes are the same function as the real MessageBubble, so a selection
 * genuinely changes the rendered conversation, not a decorative picker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun MbReferenceBubbleStylePicker(
    selected:String,onSelect:(String)->Unit
) {
    val reference=referenceListUsesFlatRows(LocalDisplayOptions.current)
    var expanded by remember {mutableStateOf(false)}
    if(!reference) {
        MbReferenceChoicePicker(
            title="Message bubble style",
            choices=listOf("Tail","Rounded","Compact")+MB_RECORDED_BUBBLE_STYLES,
            selected=selected,onSelect=onSelect)
        return
    }
    OutlinedButton(
        onClick={expanded=true},
        modifier=Modifier.fillMaxWidth().heightIn(min=46.dp),
        contentPadding=PaddingValues(horizontal=14.dp,vertical=6.dp)
    ) {
        Text("Message bubble style",modifier=Modifier.weight(1f))
        Text(selected,style=MaterialTheme.typography.bodySmall)
    }
    if(expanded) ModalBottomSheet(
        onDismissRequest={expanded=false},
        shape=RoundedCornerShape(topStart=22.dp,topEnd=22.dp),
        containerColor=MaterialTheme.colorScheme.surface,
        dragHandle={BottomSheetDefaults.DragHandle()}
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal=17.dp),
            verticalArrangement=Arrangement.spacedBy(13.dp)) {
            Text("Bubble style",style=MaterialTheme.typography.titleLarge)
            val choices=MB_RECORDED_BUBBLE_STYLES + listOf("Tail","Rounded","Compact")
            LazyVerticalGrid(
                columns=GridCells.Fixed(3),
                modifier=Modifier.fillMaxWidth().heightIn(max=410.dp),
                horizontalArrangement=Arrangement.spacedBy(8.dp),
                verticalArrangement=Arrangement.spacedBy(12.dp)
            ) {
                items(choices,key={it}) {option->
                    val active=option==selected
                    Surface(
                        shape=RoundedCornerShape(11.dp),
                        color=if(active)MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface,
                        border=if(active)BorderStroke(1.dp,MaterialTheme.colorScheme.primary)
                            else null,
                        modifier=Modifier.fillMaxWidth()
                            .clickable(role=Role.RadioButton) {
                                onSelect(option)
                                expanded=false
                            }
                            .semantics {contentDescription="Choose bubble style $option"}
                    ) {
                        Column(Modifier.padding(horizontal=6.dp,vertical=10.dp),
                            horizontalAlignment=Alignment.CenterHorizontally,
                            verticalArrangement=Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape=conversationBubbleShape(option,true),
                                color=MaterialTheme.colorScheme.onSurface,
                                modifier=Modifier.width(55.dp).height(30.dp)
                            ) {
                                if(active)Box(contentAlignment=Alignment.Center) {
                                    Icon(Icons.Outlined.Check,contentDescription=null,
                                        tint=MaterialTheme.colorScheme.surface,
                                        modifier=Modifier.size(16.dp))
                                }
                            }
                            Text(option,fontSize=12.sp,maxLines=1)
                        }
                    }
                }
            }
            Text("The selected shape is applied to incoming and outgoing messages.",
                style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
        }
    }
}
