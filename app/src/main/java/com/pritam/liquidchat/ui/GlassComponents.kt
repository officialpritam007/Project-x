package com.pritam.liquidchat.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.request.ImageRequest
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.onFocusChanged

/** Dialog windows and bottom sheets cannot reliably capture the activity's
 * Haze source. Follow the active Material surface (including Reference's
 * white/green palette and user customizations) instead of using a fixed
 * ice-blue popup. Near-opaque chrome keeps chat text from bleeding through
 * long action lists. Reduced transparency is always completely opaque.
 */
internal fun popupSurfaceColor(surface:Color,reducedTransparency:Boolean):Color =
    surface.copy(alpha=if(reducedTransparency)1f else .985f)

@Composable internal fun glassPopupColor():Color = popupSurfaceColor(
    MaterialTheme.colorScheme.surface,
    LocalDisplayOptions.current.reducedTransparency
)

@Composable internal fun LiquidDropdownMenu(
    expanded:Boolean,onDismissRequest:()->Unit,content:@Composable ColumnScope.()->Unit
) {
    MaterialTheme(shapes=MaterialTheme.shapes.copy(extraSmall=Glass.card)) {
        DropdownMenu(expanded=expanded,onDismissRequest=onDismissRequest,
            containerColor=glassPopupColor(),tonalElevation=0.dp,
            modifier=Modifier.widthIn(max=320.dp),content=content)
    }
}

/** Compact icon + label target for the conversation long-press glass palette. */
@Composable internal fun CompactMessageAction(
    label:String,icon:ImageVector,onClick:()->Unit,
    modifier:Modifier=Modifier,enabled:Boolean=true,destructive:Boolean=false
) {
    val ink=if(destructive)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Column(modifier.heightIn(min=64.dp).clip(Glass.compact)
        .background(MaterialTheme.colorScheme.surface.copy(alpha=.94f))
        .clickable(enabled=enabled,role=Role.Button,onClick=onClick)
        .padding(horizontal=2.dp,vertical=7.dp),
        horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=Arrangement.spacedBy(3.dp,Alignment.CenterVertically)) {
        Icon(icon,contentDescription=null,modifier=Modifier.size(21.dp),
            tint=ink.copy(alpha=if(enabled)1f else .4f))
        Text(label,maxLines=2,overflow=TextOverflow.Ellipsis,
            textAlign=androidx.compose.ui.text.style.TextAlign.Center,
            style=MaterialTheme.typography.labelSmall,
            color=ink.copy(alpha=if(enabled)1f else .4f))
    }
}

/** Full-width targets wrap at large font scales; destructive actions stay distinct. */
@Composable internal fun GlassActionRow(
    label:String,icon:ImageVector,onClick:()->Unit,
    destructive:Boolean=false,enabled:Boolean=true
) {
    val tint=if(destructive)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val reference=LocalDisplayOptions.current.navBarStyle=="Reference"
    Row(Modifier.fillMaxWidth().heightIn(min=if(reference)48.dp else 52.dp)
        .clip(Glass.compact)
        .clickable(enabled=enabled,role=Role.Button,onClick=onClick)
        .padding(start=if(reference)15.dp else 12.dp,
            end=if(reference)10.dp else 12.dp,
            top=if(reference)8.dp else 12.dp,
            bottom=if(reference)8.dp else 12.dp),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(if(reference)16.dp else 14.dp)) {
        Icon(icon,null,Modifier.size(if(reference)24.dp else 23.dp),
            tint=tint.copy(alpha=if(enabled)1f else .38f))
        Text(label,Modifier.weight(1f),style=MaterialTheme.typography.bodyLarge,
            color=tint.copy(alpha=if(enabled)1f else .38f))
    }
}

/** Action lists scroll as a whole on short screens instead of clipping their final action. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun LiquidActionSheet(
    title:String,onDismiss:()->Unit,content:@Composable ColumnScope.()->Unit
) {
    val reference=LocalDisplayOptions.current.navBarStyle=="Reference"
    ModalBottomSheet(onDismissRequest=onDismiss,
        sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),
        shape=androidx.compose.foundation.shape.RoundedCornerShape(
            topStart=if(reference)22.dp else 30.dp,
            topEnd=if(reference)22.dp else 30.dp),
        dragHandle={BottomSheetDefaults.DragHandle()},
        containerColor=glassPopupColor(),tonalElevation=0.dp) {
        Column(Modifier.fillMaxWidth()
            .padding(horizontal=if(reference)16.dp else 20.dp)
            .padding(bottom=16.dp)) {
            if(reference) {
                // Owner-supplied mb_new_dialog_bottom.xml has a centered 17sp
                // title, 16dp outer padding, a close action and 12dp gap.
                Row(Modifier.fillMaxWidth().padding(bottom=12.dp),
                    verticalAlignment=Alignment.CenterVertically) {
                    Spacer(Modifier.size(48.dp))
                    Text(title,modifier=Modifier.weight(1f),
                        fontSize=17.sp,fontWeight=FontWeight.Bold,
                        textAlign=androidx.compose.ui.text.style.TextAlign.Center,
                        color=MaterialTheme.colorScheme.onSurface)
                    IconButton(onClick=onDismiss,modifier=Modifier.size(48.dp)) {
                        Icon(androidx.compose.material.icons.Icons.Outlined.Close,
                            contentDescription="Close $title")
                    }
                }
            } else Text(title,style=MaterialTheme.typography.titleLarge,
                modifier=Modifier.padding(horizontal=12.dp,vertical=8.dp))
            Column(Modifier.fillMaxWidth().weight(1f,fill=false)
                .verticalScroll(rememberScrollState()),
                verticalArrangement=Arrangement.spacedBy(if(reference)0.dp else 4.dp)) {
                content()
                if(!reference)TextButton(onClick=onDismiss,
                    modifier=Modifier.align(Alignment.End)) {Text("Done")}
            }
        }
    }
}

@Composable fun DesignIcon(asset:String,description:String?,size:Int=24) {
    val context=LocalContext.current
    val request=remember(context,asset) {ImageRequest.Builder(context).data("file:///android_asset/figma/$asset.svg").decoderFactory(SvgDecoder.Factory()).build()}
    AsyncImage(model=request,contentDescription=description,modifier=Modifier.size(size.dp),
        colorFilter=LocalAppChromeInk.current?.let {androidx.compose.ui.graphics.ColorFilter.tint(it)})
}
/** Authentic, owner-supplied APK navigation art, stored without any foreign branding.
 * Only used for matching visible Liquid Chat destinations in Reference mode.
 */
@Composable internal fun LiquidReferenceIcon(
    asset:String,description:String?,size:Int,tint:Color
) {
    val context=LocalContext.current
    val request=remember(context,asset) {
        ImageRequest.Builder(context)
            .data("file:///android_asset/liquid_reference/${asset}.png")
            .build()
    }
    AsyncImage(model=request,contentDescription=description,
        modifier=Modifier.size(size.dp),
        colorFilter=androidx.compose.ui.graphics.ColorFilter.tint(tint))
}
@Composable fun GlassSearch(
    value:String,onValue:(String)->Unit,hint:String,modifier:Modifier=Modifier
) {
    val reference=LocalDisplayOptions.current.navBarStyle=="Reference"
    val reduced=LocalDisplayOptions.current.reducedMotion
    var focused by remember {mutableStateOf(false)}
    val searchTint by animateColorAsState(
        if(focused)MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec=tween(durationMillis=if(reduced)0 else 160),
        label="reference search focus")
    GlassSurface(modifier.fillMaxWidth()
        .then(if(reference)Modifier.padding(start=12.dp,bottom=4.dp) else Modifier),
        if(reference)Glass.compact else Glass.capsule) {
        Row(Modifier.fillMaxWidth().heightIn(min=if(reference)35.dp else 48.dp)
            .then(if(reference)Modifier.height(35.dp) else Modifier)
            .padding(start=if(reference)14.dp else 14.dp,
                end=if(value.isNotEmpty())4.dp else 14.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Icon(androidx.compose.material.icons.Icons.Outlined.Search,
                contentDescription=null,modifier=Modifier.size(if(reference)24.dp else 21.dp),
                tint=searchTint)
            BasicTextField(value,onValue,
                modifier=Modifier.weight(1f).onFocusChanged {focused=it.isFocused}
                    .semantics {contentDescription=hint},
                singleLine=true,
                textStyle=MaterialTheme.typography.bodyLarge.copy(
                    color=MaterialTheme.colorScheme.onSurface,fontWeight=FontWeight.Medium),
                cursorBrush=SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox={inner->Box {
                    if(value.isEmpty())Text(hint,
                        color=MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize=16.sp,fontWeight=FontWeight.Medium)
                    inner()
                }})
            if(value.isNotEmpty()) IconButton(onClick={onValue("")},
                modifier=Modifier.size(if(reference)35.dp else 40.dp)) {
                Icon(androidx.compose.material.icons.Icons.Outlined.Close,
                    contentDescription="Clear $hint",modifier=Modifier.size(20.dp),
                    tint=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
/** Reference mode uses flat conversation/contact/call lists, preserving actions and accessibility. */
internal fun referenceListUsesFlatRows(display:DisplayOptions):Boolean =
    display.navBarStyle=="Reference"

@Composable internal fun ReferenceListRow(
    modifier:Modifier=Modifier,
    content:@Composable ()->Unit
) {
    val reference=referenceListUsesFlatRows(LocalDisplayOptions.current)
    GlassSurface(modifier,if(reference)Glass.compact else Glass.card,content)
}

@Composable fun GlassHeader(title:String,brand:Boolean=false,trailing:@Composable ()->Unit={}) {
    val display=LocalDisplayOptions.current
    // Reference res/layout-v1/mb_home_toolbar.xml specifies a 47dp home
    // toolbar, a 17sp title and 28dp action icons. Preserve the explicit
    // typography adjustment when the owner has customized headerTitleSize.
    val referenceHeader=display.navBarStyle=="Reference"
    val titleSize=if(referenceHeader&&display.headerTitleSize==25)17
        else display.headerTitleSize
    GlassChromeSurface(Modifier.fillMaxWidth(),if(referenceHeader)Glass.compact else Glass.hero) {
        Row(Modifier.fillMaxWidth().heightIn(min=if(referenceHeader)47.dp else 56.dp)
            .padding(start=if(referenceHeader)16.dp else 20.dp,
                end=if(referenceHeader)12.dp else 8.dp),
            verticalAlignment=Alignment.CenterVertically) {
            if(brand) {
                Icon(painter=androidx.compose.ui.res.painterResource(com.pritam.liquidchat.R.drawable.ic_liquid),
                    contentDescription="Liquid Chat",modifier=Modifier.size(if(referenceHeader)28.dp else 22.dp),tint=Color.Unspecified)
                Spacer(Modifier.width(if(referenceHeader)8.dp else 10.dp))
            }
            Text(title,Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis,
                fontSize=titleSize.sp,fontWeight=FontWeight.SemiBold,
                color=if(brand)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            trailing()
        }
    }
}

/**
 * A single visual language for modal actions in Chats, Settings and Calls.
 * Keep Material's dialog focus/scrim semantics and existing action callbacks.
 * A highly translucent fallback is intentional for separate Android windows,
 * where Haze cannot reliably capture the underlying Activity.
 */
@Composable fun LiquidAlertDialog(
    onDismissRequest:()->Unit,
    confirmButton:@Composable ()->Unit,
    modifier:Modifier=Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null
) {
    val dark=LocalGlassDark.current
    val display=LocalDisplayOptions.current
    val reduced=display.reducedTransparency
    val reference=display.navBarStyle=="Reference"
    val shape=if(reference)androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
        else Glass.dialog
    var shown by remember {mutableStateOf(false)}
    LaunchedEffect(Unit) {shown=true}
    val entranceScale by animateFloatAsState(
        targetValue=if(shown)1f else .94f,
        animationSpec=if(display.reducedMotion) snap() else spring(
            dampingRatio=Spring.DampingRatioNoBouncy,
            stiffness=Spring.StiffnessMedium),
        label="reference alert entrance")
    val entranceAlpha by animateFloatAsState(
        targetValue=if(shown)1f else 0f,
        animationSpec=tween(durationMillis=if(display.reducedMotion)0 else 180),
        label="reference alert fade")
    AlertDialog(
        onDismissRequest=onDismissRequest,
        confirmButton=confirmButton,
        dismissButton=dismissButton,
        title=title,
        text=text,
        modifier=modifier
            .graphicsLayer {
                scaleX=entranceScale
                scaleY=entranceScale
                alpha=entranceAlpha
            }
            .shadow(18.dp,shape)
            .clip(shape)
            .then(Modifier.background(
                Brush.linearGradient(listOf(
                    Color.White.copy(alpha=if(dark).10f else .28f),
                    Color.Transparent,
                    Color.White.copy(alpha=if(dark).05f else .12f)
                )),shape
            ))
            .then(Modifier),
        shape=shape,
        containerColor=popupSurfaceColor(MaterialTheme.colorScheme.surface,reduced),
        tonalElevation=0.dp,
        titleContentColor=MaterialTheme.colorScheme.onSurface,
        textContentColor=MaterialTheme.colorScheme.onSurfaceVariant
    )
}
