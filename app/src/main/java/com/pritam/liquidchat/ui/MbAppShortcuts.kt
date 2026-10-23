package com.pritam.liquidchat.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pritam.liquidchat.MainActivity
import com.pritam.liquidchat.R

/** App-owned, non-account-bearing launcher targets. No private chat IDs or names. */
internal object MbAppShortcuts {
    const val EXTRA_TAB="com.pritam.liquidchat.extra.SHORTCUT_TAB"

    fun supported(context:Context):Boolean =
        runCatching {
            context.getSystemService(ShortcutManager::class.java)
                ?.isRequestPinShortcutSupported==true
        }.getOrDefault(false)

    /** True means the launcher accepted the placement request, not that it was pinned. */
    fun requestPin(context:Context,tab:Int):Boolean {
        require(tab in MbNavigationOrder.titles.indices) {"Unknown shortcut target"}
        val manager=context.getSystemService(ShortcutManager::class.java) ?: return false
        if(!manager.isRequestPinShortcutSupported)return false
        val title=MbNavigationOrder.titles[tab]
        val open=Intent(context,MainActivity::class.java).apply {
            action=Intent.ACTION_VIEW
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(EXTRA_TAB,tab)
        }
        val info=ShortcutInfo.Builder(context,"liquid-tab-$tab")
            .setShortLabel(title)
            .setLongLabel("Open Liquid Chat $title")
            .setIcon(Icon.createWithResource(context,R.mipmap.ic_launcher))
            .setIntent(open)
            .build()
        return manager.requestPinShortcut(info,null)
    }
}

@Composable internal fun MbAppShortcutsSettingsCard() {
    val context=LocalContext.current
    val available=remember(context) {MbAppShortcuts.supported(context)}
    var feedback by remember {mutableStateOf<String?>(null)}
    GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
        Text("Home screen shortcuts",style=MaterialTheme.typography.titleMedium)
        Text("Add a launcher shortcut to Chats, Calls, Contacts, Settings, or Profile. " +
            "Shortcuts follow normal sign-in and privacy controls and do not reveal contact names.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        MbNavigationOrder.titles.forEachIndexed {tab,title->
            Row(Modifier.fillMaxWidth().heightIn(min=50.dp),
                verticalAlignment=Alignment.CenterVertically) {
                Text(title,modifier=Modifier.weight(1f),
                    style=MaterialTheme.typography.bodyMedium)
                TextButton(enabled=available,onClick={
                    feedback=runCatching {
                        if(MbAppShortcuts.requestPin(context,tab))
                            "Shortcut request sent for $title. Approve it on your launcher if prompted."
                        else "Your launcher did not accept the $title shortcut request."
                    }.getOrElse {
                        "Could not request the shortcut. Your launcher may not support it."
                    }
                }) {Text("Add shortcut")}
            }
        }
        if(!available)Text("This launcher does not support pinning app shortcuts.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        feedback?.let {Text(it,style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)}
    }
}
