package com.pritam.liquidchat

import android.app.Application
import com.pritam.liquidchat.data.Backend
import com.pritam.liquidchat.data.SessionStore

class LiquidApplication : Application() {
    val backend by lazy { Backend(SessionStore(this)) }
}
