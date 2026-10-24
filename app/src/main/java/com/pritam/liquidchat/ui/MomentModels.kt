package com.pritam.liquidchat.ui

import androidx.compose.runtime.Immutable
import org.json.JSONArray

@Immutable data class Moment(
    val id:String,val owner:String,val media:String,val name:String,val caption:String,
    val created:String,val expires:String?,val width:Int,val height:Int,
    val mediaKind:String="image",val durationMs:Long=0L
)
/** Shared cache key for the same participant-scoped RPC across Home and the Story/Post sheet. */
internal fun momentsCacheKey(kind:String,mine:Boolean,archived:Boolean=false):String =
    "moments:$kind:${if(mine)"mine" else "contacts"}:${if(archived)"archived" else "active"}"

internal fun unexpiredMoments(
    kind:String,items:List<Moment>,now:java.time.Instant=java.time.Instant.now()
):List<Moment> {
    if(kind!="story")return items
    return items.filter {moment->
        moment.expires==null||runCatching {
            java.time.Instant.parse(moment.expires).isAfter(now)
        }.getOrDefault(false)
    }
}

/** The next cursor belongs to the server page, even when some stories expire locally. */
internal data class MomentPage(val items:List<Moment>,val cursor:Moment?,val more:Boolean)
internal fun momentPage(kind:String,raw:List<Moment>,now:java.time.Instant=java.time.Instant.now()) =
    MomentPage(unexpiredMoments(kind,raw,now),raw.lastOrNull(),raw.size>=30)

internal fun parseMoments(array:JSONArray):List<Moment> =(0 until array.length()).map {index->
    val j=array.getJSONObject(index)
    Moment(j.getString("id"),j.getString("owner_id"),j.getString("media_id"),
        j.getString("display_name"),j.getString("caption"),j.getString("created_at"),
        if(j.isNull("expires_at"))null else j.getString("expires_at"),j.getInt("width"),j.getInt("height"),
        j.optString("media_kind","image"),j.optLong("duration_ms",0L))
}

