package com.pritam.liquidchat.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class OfflineLogoutTest {
    @Test fun offlineLogoutClearsEncryptedTokensAndAccountCacheWithoutNetworkWrites()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val preferences="logout-fixture"
        val store=SessionStore(context,preferences)
        val db=Room.inMemoryDatabaseBuilder(context,LocalDatabase::class.java).build()
        val http=OkHttpClient.Builder().addInterceptor {throw IOException("isolated offline fixture")}.build()
        val backend=Backend(store,http)
        val repository=ChatRepository(context,backend,db)
        try {
            store.write("session",JSONObject().put("user",JSONObject().put("id","logout-fixture").put("email","fixture@example.invalid"))
                .put("access_token","fixture-access").put("refresh_token","fixture-refresh").put("expires_at",System.currentTimeMillis()/1000+3600).toString())
            store.write("device_token","fixture-device");store.write("pkce","fixture-pkce")
            backend.restore()
            db.dao().putDraft(Draft("logout-fixture","chat","private draft"))
            db.dao().put(Message("logout-fixture","pending","chat","logout-fixture","private text"))
            db.dao().put(Message("other-account","other","chat","other-account","retained"))
            val revoked=localLogout({backend.logout()},{backend.forgetLocal();repository.clearAccount("logout-fixture")})
            assertFalse(revoked)
            assertNull(backend.session.value)
            for(key in listOf("session","pkce","device_token"))assertNull(store.read(key))
            assertNull(db.dao().draft("logout-fixture","chat"))
            assertNull(db.dao().message("logout-fixture","pending"))
            assertNotNull(db.dao().message("other-account","other"))
        } finally {
            backend.forgetLocal();db.close()
            context.getSharedPreferences(preferences,Context.MODE_PRIVATE).edit().clear().commit()
        }
    }
}
