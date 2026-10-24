package com.pritam.liquidchat.ui

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.pritam.liquidchat.data.Chat
import com.pritam.liquidchat.data.Message
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ContactMediaUiTest {
    @get:Rule val compose=createComposeRule()
    private val chat=Chat("fixture","conversation","peer","Shila","shila")

    private fun capture(name:String,node:SemanticsNodeInteraction=compose.onRoot()) {
        val resolver=ApplicationProvider.getApplicationContext<Application>().contentResolver
        val values=ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME,"$name.png")
            put(MediaStore.Images.Media.MIME_TYPE,"image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/LiquidChatUiChecks")
            put(MediaStore.Images.Media.IS_PENDING,1)
        }
        val uri=checkNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values))
        val bitmap=node.captureToImage().asAndroidBitmap()
        resolver.openOutputStream(uri)!!.use {check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))}
        values.clear();values.put(MediaStore.Images.Media.IS_PENDING,0)
        resolver.update(uri,values,null,null)
    }

    @Test fun contactTabsEmptyStatesSaveAndCloseRemainReachableAtLargeText() {
        var saved=false;var closed=false
        compose.setContent {
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,2f)) {
                LiquidTheme(false) {
                    var kind by remember {mutableStateOf("image")}
                    Surface {
                        ContactMediaContent(chat,kind,emptyList(),false,false,false,
                            onKind={kind=it},onDismiss={closed=true},onSave={saved=true},onPage={},
                            avatar={Avatar(chat.name)},media={Text(it.body)})
                    }
                }
            }
        }
        compose.onNodeWithTag("contact-media-grid").performScrollToNode(hasText("Save contact"))
        compose.onNodeWithText("Save contact").performClick()
        compose.runOnIdle {assertTrue(saved)}
        compose.onNodeWithTag("contact-media-grid").performScrollToNode(hasText("No shared photos yet"))
        compose.onNodeWithText("No shared photos yet").assertIsDisplayed()
        compose.onNodeWithTag("contact-media-grid").performScrollToNode(hasText("Voice"))
        compose.onNodeWithText("Voice").performClick()
        compose.onNodeWithTag("contact-media-grid").performScrollToNode(hasText("No shared voice messages yet"))
        compose.onNodeWithText("No shared voice messages yet").assertIsDisplayed()
        capture("21-contact-large-text")
        compose.onNodeWithTag("contact-media-grid").performScrollToNode(hasText("Starred"))
        compose.onNodeWithText("Starred").assertIsDisplayed().performClick()
        compose.onNodeWithTag("contact-media-grid").performScrollToNode(hasText("No shared starred messages yet"))
        compose.onNodeWithText("No shared starred messages yet").assertIsDisplayed()
        compose.onNodeWithContentDescription("Close contact details").performClick()
        compose.runOnIdle {assertTrue(closed)}
    }

    @Test fun galleryPaginationRetryAndDarkThemeRenderWithoutLiveMedia() {
        var requested=0
        compose.setContent {LiquidTheme(true) {Surface {
            ContactMediaContent(chat,"image",List(36) {
                Message("fixture","photo-$it","conversation","peer","Photo $it")
            },false,true,true,onKind={},onDismiss={},onSave={},onPage={requested++},
                avatar={Avatar(chat.name)},media={
                    Surface(Modifier.fillMaxWidth().aspectRatio(1f),shape=Glass.compact,
                        color=MaterialTheme.colorScheme.primaryContainer) {Text(it.body)}
                })
        }}}
        capture("22-contact-gallery-dark")
        compose.onNodeWithTag("contact-media-grid").performScrollToNode(hasText("Retry shared media"))
        compose.onNodeWithText("Retry shared media").performClick()
        compose.runOnIdle {assertEquals(1,requested)}
    }

    @Test fun actionSheetScrollsToDestructiveActionAndPreservesCallbacks() {
        var deleted=false
        compose.setContent {LiquidTheme(false) {
            CompositionLocalProvider(LocalDisplayOptions provides DisplayOptions(navBarStyle = "Liquid")) {
            LiquidActionSheet("Conversation",onDismiss={}) {
                repeat(12) {GlassActionRow("Action $it",Icons.Outlined.PhotoLibrary,onClick={})}
                GlassActionRow("Delete history for me",Icons.Outlined.DeleteOutline,
                    destructive=true,onClick={deleted=true})
            }
            }
        }}
        compose.onNodeWithText("Delete history for me").performScrollTo().performClick()
        compose.runOnIdle {assertTrue(deleted)}
        compose.onNodeWithText("Done").performScrollTo().assertIsDisplayed()
        // ModalBottomSheet has a separate Android window; target its scroll surface,
        // rather than assuming the Activity and sheet share one semantics root.
        capture("23-conversation-actions",compose.onNodeWithText("Done").onParent())
    }
}
