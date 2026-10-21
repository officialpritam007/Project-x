package com.pritam.liquidchat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal data class MessageEditVersion(
    val version:Int,val before:String,val after:String,val editedAt:String
)
internal fun parseEditVersions(raw:String):List<MessageEditVersion> {
    val array=JSONArray(raw)
    return (0 until minOf(array.length(),10)).mapNotNull {i->
        val row=array.optJSONObject(i) ?: return@mapNotNull null
        val before=row.optString("old_body")
        val after=row.optString("new_body")
        if(before.isBlank() || after.isBlank())return@mapNotNull null
        MessageEditVersion(row.optInt("version"),before,after,row.optString("edited_at"))
    }
}

/** Mark only the changed substring; unchanged prefix/suffix remain legible. */
internal fun highlightedEditText(
    original:String,edited:String,showAfter:Boolean,accent:Color
):AnnotatedString {
    val prefix=original.commonPrefixWith(edited).length
    val suffix=original.substring(prefix).commonSuffixWith(edited.substring(prefix)).length
    val content=if(showAfter)edited else original
    val changeEnd=(content.length-suffix).coerceAtLeast(prefix)
    return buildAnnotatedString {
        append(content.substring(0,prefix))
        withStyle(SpanStyle(background=accent.copy(alpha=.24f),
            color=accent)) {append(content.substring(prefix,changeEnd))}
        append(content.substring(changeEnd))
    }
}

@Composable internal fun MessageEditHistoryDialog(
    revisions:List<MessageEditVersion>,loading:Boolean,error:String?,
    onDismiss:()->Unit
) {
    LiquidAlertDialog(onDismissRequest=onDismiss,
        title={Text("Edited message history")},
        text={
            Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("Only saved revisions for this visible message are shown. Earlier edits made before this feature was enabled cannot be recovered.",
                    style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
                when {
                    loading->CircularProgressIndicator()
                    error!=null->Text(error,color=MaterialTheme.colorScheme.error)
                    revisions.isEmpty()->Text("No stored revisions for this message.")
                    else->LazyColumn(Modifier.fillMaxWidth().heightIn(max=440.dp),
                        verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        items(revisions,key={it.version}) {revision->
                            GlassCard(Modifier.fillMaxWidth(),Glass.compact) {
                                val date=runCatching {
                                    Instant.parse(revision.editedAt)
                                        .atZone(ZoneId.systemDefault())
                                        .format(DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm"))
                                }.getOrDefault("Revision ${revision.version}")
                                Text("Revision ${revision.version} · $date",
                                    style=MaterialTheme.typography.labelMedium)
                                Text("Before",style=MaterialTheme.typography.labelSmall,
                                    color=MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(highlightedEditText(revision.before,revision.after,false,
                                    MaterialTheme.colorScheme.error),
                                    style=MaterialTheme.typography.bodyMedium)
                                HorizontalDivider()
                                Text("After",style=MaterialTheme.typography.labelSmall,
                                    color=MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(highlightedEditText(revision.before,revision.after,true,
                                    MaterialTheme.colorScheme.primary),
                                    style=MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        },
        confirmButton={TextButton(onClick=onDismiss) {Text("Done")}}
    )
}
