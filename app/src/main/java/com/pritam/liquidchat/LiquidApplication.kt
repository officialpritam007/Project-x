package com.pritam.liquidchat

import android.app.Application
import com.pritam.liquidchat.data.Backend
import com.pritam.liquidchat.data.SessionStore

class LiquidApplication : Application() {
    val database by lazy { androidx.room.Room.databaseBuilder(this,com.pritam.liquidchat.data.LocalDatabase::class.java,"liquid-chat-v1.db").build() }
    val repository by lazy { com.pritam.liquidchat.data.ChatRepository(this,backend,database) }
    val backend by lazy { Backend(SessionStore(this)) }
}
