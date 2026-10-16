package com.pritam.liquidchat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable fun LiquidApp() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    LiquidTheme {
        Scaffold(containerColor=MaterialTheme.colorScheme.background, bottomBar={
            NavigationBar(containerColor=MaterialTheme.colorScheme.surface, modifier=Modifier.navigationBarsPadding()) {
                listOf("Chats", "Contacts", "Settings").forEachIndexed { i,title ->
                    NavigationBarItem(selected=tab==i, onClick={tab=i}, icon={ Icon(listOf(Icons.Outlined.ChatBubbleOutline,Icons.Outlined.PeopleOutline,Icons.Outlined.Settings)[i], title) }, label={Text(title)})
                }
            }
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement=Arrangement.spacedBy(24.dp)) {
                Text(if(tab==0) "Liquid Chat" else listOf("Chats","Contacts","Settings")[tab], style=MaterialTheme.typography.headlineLarge)
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(listOf("Your conversations start here", "Find your people", "Made for your everyday")[tab], style=MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(12.dp))
                    Text(listOf("Sign-in and messaging arrive in the next implementation steps.", "Username discovery will appear here after account setup.", "Developed by Pritam Pal · No calls or group chats")[tab])
                }
            }
        }
    }
}
