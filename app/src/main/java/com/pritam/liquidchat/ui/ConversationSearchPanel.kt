package com.pritam.liquidchat.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * One self-contained search/filter surface. The parent owns search state and
 * message focus, so closing this panel never changes downloaded private data.
 */
@Composable internal fun ConversationSearchPanel(
    query:String,
    sender:SearchSender,
    media:SearchMedia,
    period:SearchPeriod,
    matchCount:Int,
    flatReferenceRows:Boolean,
    onQueryChange:(String)->Unit,
    onClose:()->Unit,
    onSenderChange:(SearchSender)->Unit,
    onMediaChange:(SearchMedia)->Unit,
    onPeriodChange:(SearchPeriod)->Unit,
    onPrevious:()->Unit,
    onNext:()->Unit
) {
    GlassSurface(
        Modifier.fillMaxWidth().padding(horizontal=if(flatReferenceRows)0.dp else 8.dp),
        if(flatReferenceRows) Glass.compact else Glass.card
    ) {
        Column(Modifier.padding(8.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                GlassSearch(query,onQueryChange,"Search this chat",Modifier.weight(1f))
                IconButton(onClick=onClose) {
                    Icon(Icons.Outlined.Close,"Close search")
                }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                ConversationSearchFilterMenu(
                    "Sender",sender.label,SearchSender.entries.map {it.label}
                ) {onSenderChange(SearchSender.entries[it])}
                ConversationSearchFilterMenu(
                    "Type",media.label,SearchMedia.entries.map {it.label}
                ) {onMediaChange(SearchMedia.entries[it])}
                ConversationSearchFilterMenu(
                    "Date",period.label,SearchPeriod.entries.map {it.label}
                ) {onPeriodChange(SearchPeriod.entries[it])}
            }
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text("Downloaded messages only · $matchCount matches",Modifier.weight(1f),
                    style=MaterialTheme.typography.labelSmall)
                IconButton(onClick=onPrevious,enabled=matchCount>0) {
                    Icon(Icons.Outlined.KeyboardArrowUp,"Previous match")
                }
                IconButton(onClick=onNext,enabled=matchCount>0) {
                    Icon(Icons.Outlined.KeyboardArrowDown,"Next match")
                }
            }
        }
    }
}

@Composable private fun ConversationSearchFilterMenu(
    title:String,selected:String,options:List<String>,onSelect:(Int)->Unit
) {
    var expanded by remember {mutableStateOf(false)}
    Box {
        TextButton(onClick={expanded=true}) {Text("$title: $selected")}
        LiquidDropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
            options.forEachIndexed {index,label->
                DropdownMenuItem(text={Text(label)},onClick={onSelect(index);expanded=false})
            }
        }
    }
}
