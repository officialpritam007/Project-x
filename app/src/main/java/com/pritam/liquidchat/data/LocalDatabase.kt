package com.pritam.liquidchat.data

import androidx.compose.runtime.Immutable
import androidx.room.*
import kotlinx.coroutines.flow.Flow

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

@Dao interface ChatDao {
    @Query("select exists(select 1 from hidden_messages where account=:account and id=:id)") suspend fun hidden(account:String,id:String):Boolean
    @Upsert suspend fun hide(message:HiddenMessage)
    @Query("delete from hidden_messages where account=:account") suspend fun clearHidden(account:String)
    @Query("select * from drafts where account=:account and conversation=:cid") suspend fun draft(account:String,cid:String):Draft?
    @Upsert suspend fun putDraft(draft:Draft)
    @Query("delete from drafts where account=:account and conversation=:cid") suspend fun removeDraft(account:String,cid:String)
    @Query("delete from drafts where account=:account and conversation=:cid and body=:body and replyTo=:replyTo and updatedAt=:version") suspend fun consumeDraft(account:String,cid:String,body:String,replyTo:String,version:Long):Int
    @Query("delete from drafts where account=:account") suspend fun clearDrafts(account:String)
    @Query("select * from chat_appearance where account=:account and conversation=:cid") suspend fun appearance(account:String,cid:String):ChatAppearance?
    @Upsert suspend fun putAppearance(appearance:ChatAppearance)
    @Query("delete from chat_appearance where account=:account") suspend fun clearAppearance(account:String)
    @Query("select * from contacts where account=:account order by name,id") fun contacts(account:String):Flow<List<Contact>>
    @Query("select * from contacts where account=:account") suspend fun allContacts(account:String):List<Contact>
    @Query("delete from contacts where account=:account and id=:id") suspend fun removeContact(account:String,id:String)
    @Query("delete from contacts where account=:account") suspend fun clearContacts(account:String)
    @Upsert suspend fun putContacts(contacts:List<Contact>)
    @Query("select * from chats where account=:account order by pinnedAt desc,pendingAt desc,lastSeq desc,id desc") fun chats(account:String):Flow<List<Chat>>
    @Query("select * from chats where account=:account") suspend fun allChats(account:String):List<Chat>
    @Query("select * from chats where account=:account and id=:id") suspend fun chat(account:String,id:String):Chat?
    @Query("update chats set syncSeq=:seq where account=:account and id=:cid") suspend fun syncCursor(account:String,cid:String,seq:Long)
    @Upsert suspend fun putChats(chats:List<Chat>)
    @Query("delete from chats where account=:account and id=:id") suspend fun deleteChat(account:String,id:String)
    @Query("select * from messages where account=:account and conversation=:cid order by case when seq=0 then 1 else 0 end, seq,localTime,id") fun messages(account:String,cid:String):Flow<List<Message>>
    @Query("select * from messages where account=:account and status in ('QUEUED','SENDING') order by localTime,id") suspend fun pending(account:String):List<Message>
    @Query("select count(*) from messages where account=:account and status in ('QUEUED','SENDING','FAILED')") fun unsent(account:String):Flow<Int>
    @Query("update chats set preview=:preview,pendingAt=:time where account=:account and id=:cid") suspend fun pendingPreview(account:String,cid:String,preview:String,time:Long)
    @Query("select * from messages where account=:account and conversation=:cid and status!='SENT' order by localTime desc,id desc limit 1") suspend fun latestUnsent(account:String,cid:String):Message?
    @Query("select * from messages where account=:account and conversation=:cid order by case when seq=0 then 1 else 0 end desc,seq desc,localTime desc,id desc limit 1") suspend fun latestMessage(account:String,cid:String):Message?
    @Query("update chats set preview=:preview,pendingAt=:pending,lastAt=:lastAt where account=:account and id=:cid") suspend fun localPreview(account:String,cid:String,preview:String,pending:Long,lastAt:String)
    @Query("select * from messages where account=:account and id=:id") suspend fun message(account:String,id:String):Message?
    @Query("delete from messages where account=:account and id=:id") suspend fun removeMessage(account:String,id:String)
    @Query("update chats set changeCursor=:cursor where account=:account and id=:cid") suspend fun changeCursor(account:String,cid:String,cursor:Long)
    @Upsert suspend fun put(message:Message)
    @Transaction suspend fun putPreservingAcceptance(message:Message) {
        // A concurrent server snapshot wins over stale worker progress/failure.
        if(message.status!="SENT" && this.message(message.account,message.id)?.status=="SENT")return
        put(message)
    }
    @Upsert suspend fun putAll(messages:List<Message>)
    @Query("select coalesce(max(seq),0) from messages where account=:account and conversation=:cid") suspend fun maxSeq(account:String,cid:String):Long
    @Query("select min(seq) from messages where account=:account and conversation=:cid and seq>0") suspend fun minSeq(account:String,cid:String):Long?
    @Query("select coalesce(max(seq),0) from messages where account=:account and conversation=:cid and sender<>:account") suspend fun receivedSeq(account:String,cid:String):Long
    @Query("delete from messages where account=:account and conversation=:cid and seq>0 and seq<=:watermark") suspend fun prune(account:String,cid:String,watermark:Long)
    @Query("delete from messages where account=:account and conversation=:cid") suspend fun removeHistory(account:String,cid:String)
    @Query("select * from messages where account=:account and body like '%' || :query || '%' escape '\\' order by localTime desc limit 100") suspend fun search(account:String,query:String):List<Message>
    @Upsert suspend fun cacheProfile(profile:CachedProfile)
    @Query("select * from cached_profile where account=:account") suspend fun profile(account:String):CachedProfile?
    @Query("delete from messages where account=:account") suspend fun clearMessages(account:String)
    @Query("delete from chats where account=:account") suspend fun clearChats(account:String)
    @Query("delete from cached_profile where account=:account") suspend fun clearProfile(account:String)
}
@Database(entities=[Chat::class,Message::class,CachedProfile::class,Contact::class,Draft::class,ChatAppearance::class,HiddenMessage::class],version=7,exportSchema=false)
abstract class LocalDatabase:RoomDatabase() { abstract fun dao():ChatDao }

val Migration1To2=object:androidx.room.migration.Migration(1,2) {
    override fun migrate(db:androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE chats ADD COLUMN lastAt TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE chats ADD COLUMN avatarId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE messages ADD COLUMN mediaId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE messages ADD COLUMN localImage TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE messages ADD COLUMN imageWidth INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE messages ADD COLUMN imageHeight INTEGER NOT NULL DEFAULT 0")
    }
}

val Migration2To3=object:androidx.room.migration.Migration(2,3) {
    override fun migrate(db:androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS contacts (account TEXT NOT NULL,id TEXT NOT NULL,name TEXT NOT NULL,username TEXT NOT NULL,avatarId TEXT NOT NULL,bio TEXT NOT NULL,PRIMARY KEY(account,id))")
        db.execSQL("ALTER TABLE chats ADD COLUMN pendingAt INTEGER NOT NULL DEFAULT 0")
    }
}

val Migration3To4=object:androidx.room.migration.Migration(3,4) {
    override fun migrate(db:androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE messages ADD COLUMN mediaKind TEXT NOT NULL DEFAULT 'image'")
        db.execSQL("ALTER TABLE messages ADD COLUMN durationMs INTEGER NOT NULL DEFAULT 0")
    }
}

val Migration4To5=object:androidx.room.migration.Migration(4,5) {
    override fun migrate(db:androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS drafts (account TEXT NOT NULL,conversation TEXT NOT NULL,body TEXT NOT NULL,replyTo TEXT NOT NULL,updatedAt INTEGER NOT NULL,PRIMARY KEY(account,conversation))")
        db.execSQL("CREATE TABLE IF NOT EXISTS chat_appearance (account TEXT NOT NULL,conversation TEXT NOT NULL,palette TEXT NOT NULL,PRIMARY KEY(account,conversation))")
    }
}

val Migration5To6=object:androidx.room.migration.Migration(5,6) {
    override fun migrate(db:androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE chats ADD COLUMN changeCursor INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE chats ADD COLUMN pinnedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE chats ADD COLUMN manualUnread INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE messages ADD COLUMN replyTo TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE messages ADD COLUMN editedAt TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE messages ADD COLUMN deletedAt TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE messages ADD COLUMN reactions TEXT NOT NULL DEFAULT '{}'")
        db.execSQL("ALTER TABLE messages ADD COLUMN starred INTEGER NOT NULL DEFAULT 0")
    }
}

val Migration6To7=object:androidx.room.migration.Migration(6,7) {
    override fun migrate(db:androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS hidden_messages (account TEXT NOT NULL,id TEXT NOT NULL,PRIMARY KEY(account,id))")
    }
}
