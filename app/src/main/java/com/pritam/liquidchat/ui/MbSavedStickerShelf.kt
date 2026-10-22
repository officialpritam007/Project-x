package com.pritam.liquidchat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File

/** Only saved, account-local sticker files supplied by the caller are presented.
 * Both variants preserve the caller's authenticated send/confirmation/delete logic.
 */
@Composable internal fun MbSavedStickerShelf(
    stickers:List<File>,enabled:Boolean,onSend:(File)->Unit,onDelete:(File)->Unit
) {
    if(referenceListUsesFlatRows(LocalDisplayOptions.current)) {
        LazyVerticalGrid(
            columns=GridCells.Fixed(3),
            modifier=Modifier.fillMaxWidth().heightIn(max=360.dp),
            contentPadding=PaddingValues(4.dp),
            horizontalArrangement=Arrangement.spacedBy(6.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)
        ) {
            items(stickers,key={it.absolutePath}) {file->
                Column(Modifier.fillMaxWidth().testTag("sticker-tile"),
                    horizontalAlignment=Alignment.CenterHorizontally) {
                    AsyncImage(model=file,contentDescription="Saved sticker",
                        modifier=Modifier.size(78.dp))
                    Row(verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.Center) {
                        TextButton(onClick={onSend(file)},enabled=enabled,
                            contentPadding=PaddingValues(horizontal=4.dp)) {Text("Send")}
                        IconButton(onClick={onDelete(file)},enabled=enabled,
                            modifier=Modifier.size(36.dp)) {
                            Icon(Icons.Outlined.DeleteOutline,"Delete saved sticker",
                                modifier=Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    } else {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max=360.dp),
            verticalArrangement=Arrangement.spacedBy(6.dp)) {
            items(stickers,key={it.absolutePath}) {file->
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    AsyncImage(model=file,contentDescription="Saved sticker",
                        modifier=Modifier.size(76.dp))
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick={onSend(file)},enabled=enabled) {Text("Send")}
                    IconButton(onClick={onDelete(file)},enabled=enabled) {
                        Icon(Icons.Outlined.DeleteOutline,"Delete saved sticker")
                    }
                }
                HorizontalDivider()
            }
        }
    }
}
