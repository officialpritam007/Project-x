package com.pritam.liquidchat.ui

import android.content.Intent
import android.net.Uri
import android.util.Patterns
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.pritam.liquidchat.AppViewModel
import com.pritam.liquidchat.R

@Composable fun Brand() {
    Image(painterResource(R.drawable.ic_liquid),"Liquid Chat logo",Modifier.size(64.dp))
    Text("Liquid Chat",style=MaterialTheme.typography.headlineLarge)
    Text("A little closer, every day.",color=MaterialTheme.colorScheme.onSurfaceVariant)
}
@Composable fun PasswordField(value:String,onChange:(String)->Unit,label:String="Password") {
    var show by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(value,onChange,label={Text(label)},singleLine=true,modifier=Modifier.fillMaxWidth(),shape=Glass.compact,
        keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password),
        visualTransformation=if(show) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon={IconButton(onClick={show=!show}) {Icon(if(show) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,if(show) "Hide password" else "Show password")}})
}
@Composable fun AuthScreen(model:AppViewModel,busy:Boolean) {
    var mode by rememberSaveable { mutableIntStateOf(0) }
    var email by rememberSaveable { mutableStateOf("") }
    // Passwords intentionally do not enter saved-instance-state backups.
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val context=LocalContext.current
    val emailValid=Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Spacer(Modifier.height(32.dp));Brand();Spacer(Modifier.height(16.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Text(listOf("Welcome back","Create your account","Reset your password")[mode],style=MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(email,{email=it},label={Text("Email")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email),modifier=Modifier.fillMaxWidth(),shape=Glass.compact,isError=email.isNotEmpty()&&!emailValid)
            if(mode!=2) {Spacer(Modifier.height(12.dp));PasswordField(password,{password=it})}
            if(mode==1) {Spacer(Modifier.height(12.dp));PasswordField(confirmation,{confirmation=it},"Confirm password");Text("Use at least 8 characters.",style=MaterialTheme.typography.bodySmall)}
            Spacer(Modifier.height(20.dp))
            Button(onClick={when(mode){0->model.signIn(email,password);1->model.register(email,password);else->model.reset(email)}},enabled=!busy&&emailValid&&(mode==2||(password.length>=8&&(mode!=1||password==confirmation))),modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {
                if(busy) CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp) else Text(listOf("Sign in","Create account","Send reset email")[mode])
            }
            if(mode==0) {
                TextButton(onClick={mode=2},enabled=!busy) {Text("Forgot password?")}
                OutlinedButton(onClick={model.action { val url=model.backend.googleUrl();context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url))) }},enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text("Continue with Google")}
            }
            TextButton(onClick={mode=if(mode==0)1 else 0;password="";confirmation="";model.clearNotice()},enabled=!busy) {Text(if(mode==0) "Create a new account" else "Back to sign in")}
        }
        Text("Developed by Pritam Pal",style=MaterialTheme.typography.bodySmall)
    }
}
@Composable fun ProfileScreen(model:AppViewModel,busy:Boolean) {
    var name by rememberSaveable { mutableStateOf("") };var username by rememberSaveable { mutableStateOf("") };var bio by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Brand();Text("Make yourself at home",style=MaterialTheme.typography.titleLarge)
        OutlinedTextField(name,{name=it.take(60)},label={Text("Display name")},modifier=Modifier.fillMaxWidth(),shape=Glass.compact)
        OutlinedTextField(username,{username=it.lowercase().take(24)},label={Text("Unique username")},supportingText={Text("3–24 lowercase letters, numbers or underscores")},singleLine=true,modifier=Modifier.fillMaxWidth(),shape=Glass.compact)
        OutlinedTextField(bio,{bio=it.take(160)},label={Text("Bio (optional)")},modifier=Modifier.fillMaxWidth(),shape=Glass.compact)
        Button(onClick={model.saveProfile(username,name,bio)},enabled=!busy&&name.isNotBlank()&&username.matches(Regex("[a-z0-9_]{3,24}")),modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text(if(busy) "Saving…" else "Continue")}
        TextButton(onClick=model::logout,enabled=!busy) {Text("Sign out")}
    }
}
@Composable fun RecoveryScreen(model:AppViewModel,busy:Boolean) {
    var password by remember { mutableStateOf("") }
    Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        Text("Choose a new password",style=MaterialTheme.typography.headlineMedium)
        PasswordField(password,{password=it})
        Button(onClick={model.action {model.backend.updatePassword(password)}},enabled=!busy&&password.length>=8) {Text("Save password")}
    }
}
