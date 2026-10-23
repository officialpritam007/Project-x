package com.pritam.liquidchat.ui

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

/** Account-scoped device-only chat folders, never written to Supabase messages. */
internal data class MbChatFolder(
    val id:String,val name:String,val members:Set<String>
)
internal object MbChatFolders {
    private const val PREFS="liquid_chat_folders_v1"
    private const val MAX_FOLDERS=12
    private const val MAX_MEMBERS=200
    private val uuid=Regex("[0-9a-fA-F-]{36}")

    private fun key(account:String):String {
        require(account.matches(uuid)) {"Sign in before managing folders."}
        return "folders:$account"
    }
    fun list(context:Context,account:String?):List<MbChatFolder> {
        if(account.isNullOrBlank()||!account.matches(uuid))return emptyList()
        val raw=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
            .getString(key(account),"[]") ?: "[]"
        return runCatching {
            val array=JSONArray(raw)
            buildList {
                for(i in 0 until minOf(array.length(),MAX_FOLDERS)) {
                    val item=array.optJSONObject(i) ?: continue
                    val id=item.optString("id")
                    val name=item.optString("name")
                    if(!id.matches(uuid)||name.length !in 1..24 ||
                        any {it.id==id || it.name.equals(name,true)})continue
                    val ids=item.optJSONArray("members") ?: JSONArray()
                    val members=(0 until minOf(ids.length(),MAX_MEMBERS))
                        .mapNotNull {k->ids.optString(k).takeIf {it.matches(uuid)}}
                        .toSet()
                    add(MbChatFolder(id,name,members))
                }
            }
        }.getOrDefault(emptyList())
    }
    private fun persist(context:Context,account:String,folders:List<MbChatFolder>) {
        val array=JSONArray()
        folders.forEach {folder->
            val members=JSONArray()
            folder.members.forEach(members::put)
            array.put(JSONObject().put("id",folder.id).put("name",folder.name)
                .put("members",members))
        }
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
            .putString(key(account),array.toString()).apply()
    }
    fun create(context:Context,account:String,name:String):List<MbChatFolder> {
        val title=name.trim()
        require(title.length in 1..24 && title.all {!it.isISOControl()}) {
            "Folder name must be 1–24 characters."
        }
        val existing=list(context,account)
        require(existing.size<MAX_FOLDERS) {"Maximum 12 folders."}
        require(existing.none {it.name.lowercase(Locale.ROOT)==title.lowercase(Locale.ROOT)}) {
            "A folder with this name already exists."
        }
        val updated=existing+MbChatFolder(UUID.randomUUID().toString(),title,emptySet())
        persist(context,account,updated)
        return updated
    }
    /** Rename the visible list without altering its stable ID or chat membership. */
    fun rename(context:Context,account:String,id:String,name:String):List<MbChatFolder> {
        val title=name.trim()
        require(title.length in 1..24 && title.all {!it.isISOControl()}) {
            "Folder name must be 1–24 characters."
        }
        val existing=list(context,account)
        require(existing.any {it.id==id}) {"Folder does not exist."}
        require(existing.none {it.id!=id && it.name.equals(title,ignoreCase=true)}) {
            "A folder with this name already exists."
        }
        val updated=existing.map {folder->
            if(folder.id==id)folder.copy(name=title) else folder
        }
        persist(context,account,updated)
        return updated
    }
    fun remove(context:Context,account:String,id:String):List<MbChatFolder> {
        val updated=list(context,account).filterNot {it.id==id}
        persist(context,account,updated)
        return updated
    }
    fun membership(
        context:Context,account:String,folderId:String,chatId:String,enabled:Boolean
    ):List<MbChatFolder> {
        require(chatId.matches(uuid))
        val current=list(context,account)
        require(current.any {it.id==folderId}) {"Folder does not exist."}
        val updated=current.map {folder->
            if(folder.id!=folderId)folder else {
                val members=if(enabled)folder.members+chatId else folder.members-chatId
                require(members.size<=MAX_MEMBERS) {"Folder is full."}
                folder.copy(members=members)
            }
        }
        persist(context,account,updated)
        return updated
    }
    fun clearAccount(context:Context,account:String) {
        if(account.matches(uuid))context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
            .edit().remove(key(account)).apply()
    }
}
