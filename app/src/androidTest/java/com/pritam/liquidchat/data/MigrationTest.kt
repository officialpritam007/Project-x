package com.pritam.liquidchat.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @Test fun v1OutboxSurvivesTheMediaAndContactsUpgrade()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>();val name="migration-fixture.db"
        context.deleteDatabase(name)
        val file=context.getDatabasePath(name);file.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file,null).use {sql->
            sql.execSQL("CREATE TABLE chats(account TEXT NOT NULL,id TEXT NOT NULL,peer TEXT NOT NULL,name TEXT NOT NULL,username TEXT NOT NULL,preview TEXT NOT NULL,lastSeq INTEGER NOT NULL,unread INTEGER NOT NULL,favorite INTEGER NOT NULL,archived INTEGER NOT NULL,muted INTEGER NOT NULL,syncSeq INTEGER NOT NULL,deletedSeq INTEGER NOT NULL,peerDelivered INTEGER NOT NULL,peerRead INTEGER NOT NULL,PRIMARY KEY(account,id))")
            sql.execSQL("CREATE TABLE messages(account TEXT NOT NULL,id TEXT NOT NULL,conversation TEXT NOT NULL,sender TEXT NOT NULL,body TEXT NOT NULL,seq INTEGER NOT NULL,localTime INTEGER NOT NULL,serverTime TEXT NOT NULL,status TEXT NOT NULL,attempts INTEGER NOT NULL,error TEXT NOT NULL,PRIMARY KEY(account,id))")
            sql.execSQL("CREATE INDEX index_messages_account_conversation_seq ON messages(account,conversation,seq)")
            sql.execSQL("CREATE TABLE cached_profile(account TEXT NOT NULL,json TEXT NOT NULL,PRIMARY KEY(account))")
            sql.execSQL("INSERT INTO messages VALUES('a','pending-id','chat','a','must survive',0,123,'','QUEUED',2,'')")
            sql.version=1
        }
        val upgraded=Room.databaseBuilder(context,LocalDatabase::class.java,name).addMigrations(Migration1To2,Migration2To3,Migration3To4,Migration4To5,Migration5To6).build()
        try {
            val m=upgraded.dao().pending("a").single()
            assertEquals("pending-id",m.id);assertEquals("must survive",m.body);assertEquals(2,m.attempts)
            assertEquals("image",m.mediaKind);assertEquals(0,m.durationMs);assertEquals("",m.mediaId);assertEquals("",m.localImage);assertEquals(0,m.imageWidth)
            upgraded.dao().putContacts(listOf(Contact("a","b","B","bee")))
            upgraded.dao().putDraft(Draft("a","chat","Draft after migration"))
            assertEquals("Draft after migration",upgraded.dao().draft("a","chat")?.body)
        } finally {upgraded.close();context.deleteDatabase(name)}
    }
    @Test fun sessionSecretsAreEncryptedAndCanBeRemoved() {
        val context=ApplicationProvider.getApplicationContext<Context>();val store=SessionStore(context)
        store.write("test-secret","not-a-real-token")
        try {
            assertFalse(context.getSharedPreferences("auth_v1",Context.MODE_PRIVATE).getString("test-secret","")!!.contains("not-a-real-token"))
            assertEquals("not-a-real-token",SessionStore(context).read("test-secret"))
        } finally {store.write("test-secret",null)}
        assertNull(store.read("test-secret"))
    }
}
