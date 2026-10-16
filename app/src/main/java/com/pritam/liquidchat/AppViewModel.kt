package com.pritam.liquidchat

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pritam.liquidchat.data.Backend
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class AppViewModel(app: Application) : AndroidViewModel(app) {
    val backend: Backend=(app as LiquidApplication).backend
    val session=backend.session
    val recovery=backend.recovery
    private val _ready=MutableStateFlow(false)
    val ready=_ready.asStateFlow()
    private val _busy=MutableStateFlow(false)
    val busy=_busy.asStateFlow()
    private val _notice=MutableStateFlow<String?>(null)
    val notice=_notice.asStateFlow()
    private val _profile=MutableStateFlow<JSONObject?>(null)
    val profile=_profile.asStateFlow()
    private val _profileLoaded=MutableStateFlow(false)
    val profileLoaded=_profileLoaded.asStateFlow()
    init {
        viewModelScope.launch { backend.restore(); _ready.value=true }
        viewModelScope.launch {
            session.map { it?.userId }.distinctUntilChanged().collectLatest { id ->
                _profile.value=null; _profileLoaded.value=false
                if(id!=null) loadProfile() else _profileLoaded.value=true
            }
        }
    }
    fun clearNotice() { _notice.value=null }
    fun action(block: suspend () -> Unit) {
        if(_busy.value) return
        _busy.value=true; _notice.value=null
        viewModelScope.launch {
            try { block() } catch(e: CancellationException) { throw e }
            catch(e: Exception) { _notice.value=e.message ?: "Connection unavailable. Try again." }
            finally { _busy.value=false }
        }
    }
    suspend fun loadProfile() {
        val id=session.value?.userId ?: return
        try {
            val a=JSONArray(backend.request("/rest/v1/profiles?id=eq.$id&select=*&limit=1"))
            if(session.value?.userId==id) { _profile.value=a.optJSONObject(0);_profileLoaded.value=true }
        } catch(e: CancellationException) { throw e }
        catch(e:Exception) { _notice.value=e.message ?: "Cannot load profile. Retry when connected." }
    }
    fun signIn(email:String,password:String)=action { backend.signIn(email,password) }
    fun register(email:String,password:String)=action {
        if(!backend.register(email,password)) _notice.value="Check your email to verify your account, then sign in."
    }
    fun reset(email:String)=action { backend.reset(email);_notice.value="If that address can receive a reset, an email is on its way. Open it on this device." }
    fun callback(uri: Uri)=action { backend.callback(uri) }
    fun saveProfile(username:String,name:String,bio:String)=action {
        backend.rpc("save_profile",JSONObject().put("p_username",username).put("p_name",name).put("p_bio",bio));loadProfile()
    }
    fun logout()=action { backend.logout() }
}
