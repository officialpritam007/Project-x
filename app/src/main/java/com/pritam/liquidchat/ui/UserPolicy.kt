package com.pritam.liquidchat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

internal const val USER_POLICY_VERSION="2026-10-17-v1"

@Composable internal fun UserPolicyText() {
    Text("Be respectful. Do not share harassment, threats, hateful content, scams, illegal goods, sexual exploitation, child sexual abuse material, or intimate images without consent.")
    Text("Share only content you have the right and consent to share. Use Report on a message, story, post or contact to request review, and Block to stop contact. Reports are private to the service operator; relevant reported content may be reviewed.")
    Text("Liquid Chat stores account/profile details, messages, photos, voice recordings, device notification tokens and privacy settings to provide messaging. Supabase operates the core backend and protected media fallback; Google Firebase provides push delivery. Authenticated Cloudinary media may be used when configured.")
    Text("Connections use HTTPS. Messages are not end-to-end encrypted. Session tokens are encrypted with Android Keystore on this device. Recipients can save or capture shared content; deleting a message cannot erase their saved copies. Previously issued media links may work briefly until expiry.")
    Text("Delete your account in Settings → Account. Owned media and account identifiers enter the erasure workflow; sent text may remain in the recipient's history under Deleted account. Operational and abuse records may be retained for security and policy enforcement.")
    Text("Developed by Pritam Pal · User Policy version $USER_POLICY_VERSION",style=MaterialTheme.typography.bodySmall)
}

@Composable internal fun UserPolicyScreen(busy:Boolean,onAccept:()->Unit,onSignOut:()->Unit) {
    var agreed by rememberSaveable {mutableStateOf(false)}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Brand()
        GlassCard(Modifier.fillMaxWidth()) {
            Text("Before you share",style=MaterialTheme.typography.headlineSmall)
            UserPolicyText()
            Row(Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("policy-agree").toggleable(value=agreed,role=Role.Checkbox,onValueChange={agreed=it})) {
                Checkbox(agreed,onCheckedChange=null)
                Text("I agree to the User Policy",Modifier.padding(top=12.dp))
            }
            Button(onClick=onAccept,enabled=agreed&&!busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text("Agree and continue")}
            TextButton(onClick=onSignOut,enabled=!busy) {Text("Sign out")}
        }
    }
}

@Composable internal fun ReportContentDialog(title:String,explanation:String,onSubmit:(String)->Unit,onDismiss:()->Unit) {
    var reason by rememberSaveable {mutableStateOf("")}
    AlertDialog(onDismissRequest=onDismiss,title={Text(title)},text={Column(Modifier.verticalScroll(rememberScrollState())) {
        Text(explanation)
        OutlinedTextField(reason,{reason=it.take(800)},label={Text("Reason")},supportingText={Text("${reason.length} / 800")},modifier=Modifier.fillMaxWidth())
    }},confirmButton={TextButton(onClick={onSubmit(reason.trim())},enabled=reason.isNotBlank()) {Text("Submit report")}},dismissButton={TextButton(onClick=onDismiss) {Text("Cancel")}})
}
