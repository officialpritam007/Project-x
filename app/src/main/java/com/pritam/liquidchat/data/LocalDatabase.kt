package com.pritam.liquidchat.data

import androidx.compose.runtime.Immutable
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao interface ChatDao {
    @Query("select * from scheduled_texts where account=:account and conversation=:cid order by sendAt,id")
    fun scheduledTexts(account:String,cid:String):Flow<List<ScheduledText>>
    @Query("select * from scheduled_texts where account=:account order by sendAt,id")
    suspend fun allScheduledTexts(account:String):List<ScheduledText>
    @Query("select * from scheduled_texts where account=:account and id=:id")
    suspend fun scheduledText(account:String,id:String):ScheduledText?
    @Upsert suspend fun putScheduledText(value:ScheduledText)
    @Query("delete from scheduled_texts where account=:account and id=:id")
    suspend fun deleteScheduledText(account:String,id:String):Int
    @Query("delete from scheduled_texts where account=:account")
    suspend fun clearScheduledTexts(account:String)
    @Query("delete from scheduled_texts where account=:account and conversation=:cid")
    suspend fun clearScheduledConversation(account:String,cid:String)
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
    @Query("delete from chat_appearance where account=:account and conversation=:cid") suspend fun removeAppearance(account:String,cid:String)
    @Query("select * from contacts where account=:account order by name,id") fun contacts(account:String):Flow<List<Contact>>
    @Query("select * from contacts where account=:account") suspend fun allContacts(account:String):List<Contact>
    @Query("delete from contacts where account=:account and id=:id") suspend fun removeContact(account:String,id:String)
    @Query("delete from contacts where account=:account") suspend fun clearContacts(account:String)
    @Upsert suspend fun putContacts(contacts:List<Contact>)
    @Query("select * from chats where account=:account order by pinnedAt desc,pendingAt desc,lastSeq desc,id desc") fun chats(account:String):Flow<List<Chat>>
    @Query("select * from chats where account=:account") suspend fun allChats(account:String):List<Chat>
    @Query("select * from chats where account=:account and id=:id") suspend fun chat(account:String,id:String):Chat?
    @Query("update chats set syncSeq=:seq where account=:account and id=:cid and syncSeq<:seq") suspend fun syncCursor(account:String,cid:String,seq:Long)
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
    @Query("select * from messages where account=:account and id=:id") fun observeMessage(account:String,id:String):Flow<Message?>
    @Query("delete from messages where account=:account and id=:id") suspend fun removeMessage(account:String,id:String)
    @Query("update chats set changeCursor=:cursor where account=:account and id=:cid and changeCursor<:cursor") suspend fun changeCursor(account:String,cid:String,cursor:Long)
    @Upsert suspend fun put(message:Message)
    @Transaction suspend fun putPreservingAcceptance(message:Message) {
        // Avoid redundant timeline invalidations on duplicate FCM/realtime/history
        // snapshots while preserving server acceptance over stale worker state.
        if(!shouldPersistMessage(this.message(message.account,message.id),message))return
        put(message)
    }
    @Upsert suspend fun putAll(messages:List<Message>)
    @Query("select coalesce(max(seq),0) from messages where account=:account and conversation=:cid") suspend fun maxSeq(account:String,cid:String):Long
    @Query("select min(seq) from messages where account=:account and conversation=:cid and seq>0") suspend fun minSeq(account:String,cid:String):Long?
    @Query("select coalesce(max(seq),0) from messages where account=:account and conversation=:cid and sender<>:account") suspend fun receivedSeq(account:String,cid:String):Long
    @Query("select exists(select 1 from messages where account=:account and conversation=:cid and seq>0 and seq<=:watermark)") suspend fun hasPrunableMessages(account:String,cid:String,watermark:Long):Boolean
    @Query("delete from messages where account=:account and conversation=:cid and seq>0 and seq<=:watermark") suspend fun prune(account:String,cid:String,watermark:Long)
    @Query("delete from messages where account=:account and conversation=:cid") suspend fun removeHistory(account:String,cid:String)
    @Query("select * from messages where account=:account and deletedAt='' and body like '%' || :query || '%' escape '\\' order by localTime desc limit 100") suspend fun search(account:String,query:String):List<Message>
    /** Cached stars only. Revoked, deleted and locally hidden records are excluded. */
    @Query("""SELECT * FROM messages WHERE account=:account AND starred=1
        AND status='SENT' AND deletedAt=''
        AND NOT EXISTS (SELECT 1 FROM hidden_messages
            WHERE hidden_messages.account=messages.account AND hidden_messages.id=messages.id)
        ORDER BY localTime DESC,id DESC LIMIT 200""")
    fun starredMessages(account:String):Flow<List<Message>>
    @Upsert suspend fun cacheProfile(profile:CachedProfile)
    @Query("select * from cached_profile where account=:account") suspend fun profile(account:String):CachedProfile?
    @Query("delete from messages where account=:account") suspend fun clearMessages(account:String)
    @Query("delete from chats where account=:account") suspend fun clearChats(account:String)
    @Query("delete from cached_profile where account=:account") suspend fun clearProfile(account:String)
}
@Database(entities=[Chat::class,Message::class,CachedProfile::class,Contact::class,Draft::class,ChatAppearance::class,HiddenMessage::class,ScheduledText::class],version=8,exportSchema=false)
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

/** Additive migration: never touch existing messages, chats, media or drafts. */
val Migration7To8=object:androidx.room.migration.Migration(7,8) {
    override fun migrate(db:androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS scheduled_texts (account TEXT NOT NULL, id TEXT NOT NULL, conversation TEXT NOT NULL, body TEXT NOT NULL, replyTo TEXT NOT NULL, sendAt INTEGER NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(account,id))")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_scheduled_texts_account_conversation_sendAt ON scheduled_texts(account,conversation,sendAt)")
    }
}
