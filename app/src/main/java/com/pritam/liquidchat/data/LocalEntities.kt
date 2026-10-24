package com.pritam.liquidchat.data

import androidx.compose.runtime.Immutable
import androidx.room.*

/** Local Room entities: same package/names/schema; DAO and migrations stay intact. */
@Immutable
@Entity(tableName="chats",primaryKeys=["account","id"])
data class Chat(
    val account:String,val id:String,val peer:String,val name:String,val username:String,
    val preview:String="",val lastSeq:Long=0,val unread:Int=0,
    val favorite:Boolean=false,val archived:Boolean=false,val muted:Boolean=false,
    val syncSeq:Long=0,val deletedSeq:Long=0,val peerDelivered:Long=0,val peerRead:Long=0,
    @ColumnInfo(defaultValue="''") val lastAt:String="",@ColumnInfo(defaultValue="''") val avatarId:String="",@ColumnInfo(defaultValue="0") val pendingAt:Long=0,
    @ColumnInfo(defaultValue="0") val changeCursor:Long=0,
    @ColumnInfo(defaultValue="0") val pinnedAt:Long=0,
    @ColumnInfo(defaultValue="0") val manualUnread:Boolean=false
)
@Immutable
@Entity(tableName="messages",primaryKeys=["account","id"],indices=[Index(value=["account","conversation","seq"])])
data class Message(
    val account:String,val id:String,val conversation:String,val sender:String,val body:String,
    val seq:Long=0,val localTime:Long=System.currentTimeMillis(),val serverTime:String="",
    val status:String="QUEUED",val attempts:Int=0,val error:String="",
    @ColumnInfo(defaultValue="''") val mediaId:String="",@ColumnInfo(defaultValue="''") val localImage:String="",@ColumnInfo(defaultValue="0") val imageWidth:Int=0,@ColumnInfo(defaultValue="0") val imageHeight:Int=0,
    @ColumnInfo(defaultValue="'image'") val mediaKind:String="image",@ColumnInfo(defaultValue="0") val durationMs:Int=0,
    @ColumnInfo(defaultValue="''") val replyTo:String="",
    @ColumnInfo(defaultValue="''") val editedAt:String="",
    @ColumnInfo(defaultValue="''") val deletedAt:String="",
    @ColumnInfo(defaultValue="'{}'") val reactions:String="{}",
    @ColumnInfo(defaultValue="0") val starred:Boolean=false
)
/**
 * The accepted server snapshot wins over any stale local QUEUED/SENDING/FAILED write.
 * Identical snapshots must not invalidate observing Room flows and recompose bubbles.
 */
internal fun shouldPersistMessage(previous:Message?,incoming:Message):Boolean =
    !(previous?.status=="SENT" && incoming.status!="SENT") &&
    // Deletion is irreversible. A delayed history/worker snapshot must never
    // bring back a body or attachment after its tombstone has been accepted.
    !(previous?.deletedAt?.isNotEmpty()==true && incoming.deletedAt.isEmpty()) &&
    previous!=incoming

@Immutable
@Entity(tableName="contacts",primaryKeys=["account","id"])
data class Contact(val account:String,val id:String,val name:String,val username:String,val avatarId:String="",val bio:String="")
@Entity(tableName="cached_profile",primaryKeys=["account"])
data class CachedProfile(val account:String,val json:String)

@Entity(tableName="hidden_messages",primaryKeys=["account","id"])
data class HiddenMessage(val account:String,val id:String)

@Entity(tableName="drafts",primaryKeys=["account","conversation"])
data class Draft(val account:String,val conversation:String,val body:String,val replyTo:String="",val updatedAt:Long=System.currentTimeMillis())
@Entity(tableName="chat_appearance",primaryKeys=["account","conversation"])
data class ChatAppearance(val account:String,val conversation:String,val palette:String="Ice")

/** Future messages live outside the immediate outbox until their due time. */
@Entity(tableName="scheduled_texts",primaryKeys=["account","id"],
    indices=[Index(value=["account","conversation","sendAt"])])
data class ScheduledText(
    val account:String,val id:String,val conversation:String,val body:String,
    val replyTo:String="",val sendAt:Long,val createdAt:Long=System.currentTimeMillis()
)

