package com.pritam.liquidchat.data

import org.json.JSONObject
import java.io.IOException

/** Cache only URLs whose server-issued lifetime still includes a transfer safety margin. */
internal suspend fun Backend.imageUrl(
    account:String,id:String,force:Boolean=false,clock:()->Long={System.nanoTime()/1_000_000L}
):String {
    require(id.isNotBlank() && id!="null")
    val key="signed-image:$id"
    repeat(2) {attempt->
        val encoded=uiReads.load(account,key,40_000L,force=force||attempt>0) {
            val started=clock()
            val response=JSONObject(request("/functions/v1/media","POST",JSONObject().put("action","download").put("id",id)))
            val lifetime=response.optLong("expires_in",60).coerceIn(1,300)*1000
            val url=response.getString("url")
            if(!url.startsWith("https://"))throw IOException("Invalid protected image URL")
            JSONObject().put("url",url)
                .put("valid_until",started+lifetime-minOf(5_000L,lifetime/10)).toString()
        }
        val response=runCatching {JSONObject(encoded)}.getOrNull()
        if(response!=null&&response.optLong("valid_until")>clock())return response.getString("url")
        // The next attempt forces a new link without resetting the bitmap identity.
    }
    throw IOException("Image link expired. Retry when connected.")
}
