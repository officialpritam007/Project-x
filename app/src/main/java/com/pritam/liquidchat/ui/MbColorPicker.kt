package com.pritam.liquidchat.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

/** Opaque RGB only. Arbitrary alpha would bypass existing bubble contrast rules. */
internal data class MbHsvColor(val hue:Float,val saturation:Float,val brightness:Float) {
    fun toHex():String {
        val h=(hue.takeIf {it.isFinite()}?:0f).coerceIn(0f,360f)%360f
        val s=(saturation.takeIf {it.isFinite()}?:0f).coerceIn(0f,1f)
        val v=(brightness.takeIf {it.isFinite()}?:0f).coerceIn(0f,1f)
        val chroma=v*s
        val x=chroma*(1f-abs((h/60f)%2f-1f))
        val (r,g,b)=when(h.toInt()/60) {
            0->Triple(chroma,x,0f)
            1->Triple(x,chroma,0f)
            2->Triple(0f,chroma,x)
            3->Triple(0f,x,chroma)
            4->Triple(x,0f,chroma)
            else->Triple(chroma,0f,x)
        }
        val m=v-chroma
        return "#%02X%02X%02X".format(java.util.Locale.ROOT,
            ((r+m)*255).roundToInt().coerceIn(0,255),
            ((g+m)*255).roundToInt().coerceIn(0,255),
            ((b+m)*255).roundToInt().coerceIn(0,255))
    }
    companion object {
        fun fromHex(input:String):MbHsvColor? {
            val hex=normalizedAccentHex(input)?:return null
            val rgb=hex.substring(1).toInt(16)
            val r=((rgb shr 16)and 255)/255f
            val g=((rgb shr 8)and 255)/255f
            val b=(rgb and 255)/255f
            val max=maxOf(r,g,b);val min=minOf(r,g,b);val delta=max-min
            val hue=when {
                delta==0f->0f
                max==r->60f*((g-b)/delta%6f)
                max==g->60f*((b-r)/delta+2f)
                else->60f*((r-g)/delta+4f)
            }
            return MbHsvColor((hue+360f)%360f,if(max==0f)0f else delta/max,max)
        }
    }
}

@Composable internal fun MbColorPickerDialog(
    title:String,
    initialHex:String,
    onDismiss:()->Unit,
    onApply:(String)->Unit
) {
    val initial=normalizedAccentHex(initialHex)?:"#2563EB"
    var hsv by remember(initial) {mutableStateOf(MbHsvColor.fromHex(initial)!!)}
    var input by remember(initial) {mutableStateOf(initial)}
    var photoError by remember(initial) {mutableStateOf<String?>(null)}
    var photoLoading by remember(initial) {mutableStateOf(false)}
    val scope=rememberCoroutineScope()
    val context=LocalContext.current
    val photoPicker=rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()) {uri->
        if(uri!=null && !photoLoading) {
            photoLoading=true
            scope.launch {
                try {
                    val color=PhotoAccentSampler.extract(context,uri)
                    MbHsvColor.fromHex(color)?.let {hsv=it;input=it.toHex()}
                    photoError=null
                } catch(e:kotlinx.coroutines.CancellationException) {throw e}
                catch(e:Exception) {
                    photoError=e.message ?: "Could not identify a usable color."
                } finally {photoLoading=false}
            }
        }
    }
    val valid=normalizedAccentHex(input)
    fun update(value:MbHsvColor) {hsv=value;input=value.toHex();photoError=null}
    LiquidAlertDialog(
        onDismissRequest=onDismiss,
        title={Text(title)},
        text={Column(Modifier.fillMaxWidth().heightIn(max=420.dp)
            .verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                listOf("Current" to initial,"Preview" to hsv.toHex()).forEach {(label,hex)->
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        Text(label,style=MaterialTheme.typography.labelMedium)
                        Box(Modifier.fillMaxWidth().height(56.dp)
                            .background(Color(hex.substring(1).toLong(16) or 0xFF000000L),RoundedCornerShape(12.dp))
                            .semantics {contentDescription="$label color $hex"})
                    }
                }
            }
            Text("Quick palette",style=MaterialTheme.typography.titleSmall)
            val swatches=listOf(
                "#0B86AE","#3182CE","#2F5FB4","#6346BF","#A3439A","#CC467B",
                "#D65353","#D18332","#B4992C","#438E56","#249C8F","#334155",
                "#E5A9C9","#C5B2EA","#9BC2F5","#9ED8C2","#EDCDA4","#E6E9EF"
            )
            swatches.chunked(6).forEach {group->
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.SpaceEvenly) {
                    group.forEach {hex->
                        val color=Color(hex.substring(1).toLong(16) or 0xFF000000L)
                        Box(Modifier.size(38.dp)
                            .border(if(input.equals(hex,true))3.dp else 1.dp,
                                if(input.equals(hex,true))
                                    MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.outlineVariant,CircleShape)
                            .padding(3.dp)
                            .background(color,CircleShape)
                            .clickable {MbHsvColor.fromHex(hex)?.let(::update)}
                            .semantics {contentDescription="Choose palette color $hex"})
                    }
                }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
                verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                Text("Sample from a photo",style=MaterialTheme.typography.bodySmall)
                TextButton(enabled=!photoLoading,onClick={photoPicker.launch("image/*")}) {
                    Text(if(photoLoading)"Reading photo…" else "Choose photo")
                }
            }
            photoError?.let {
                Text(it,color=MaterialTheme.colorScheme.error,
                    style=MaterialTheme.typography.bodySmall)
            }
            Text("Hue · ${hsv.hue.roundToInt()}°",style=MaterialTheme.typography.labelLarge)
            Slider(hsv.hue,{update(hsv.copy(hue=it))},valueRange=0f..360f,
                modifier=Modifier.fillMaxWidth().semantics {contentDescription="Hue"})
            Text("Saturation · ${(hsv.saturation*100).roundToInt()}%",style=MaterialTheme.typography.labelLarge)
            Slider(hsv.saturation,{update(hsv.copy(saturation=it))},valueRange=0f..1f,
                modifier=Modifier.fillMaxWidth().semantics {contentDescription="Saturation"})
            Text("Brightness · ${(hsv.brightness*100).roundToInt()}%",style=MaterialTheme.typography.labelLarge)
            Slider(hsv.brightness,{update(hsv.copy(brightness=it))},valueRange=0f..1f,
                modifier=Modifier.fillMaxWidth().semantics {contentDescription="Brightness"})
            OutlinedTextField(value=input,onValueChange={value->
                input=value.take(7)
                MbHsvColor.fromHex(input)?.let {hsv=it}
            },label={Text("Color #RRGGBB")},singleLine=true,isError=valid==null,
                modifier=Modifier.fillMaxWidth(),supportingText={Text(
                    if(valid==null)"Enter # followed by six hexadecimal digits." else "Changes apply only when you tap Apply.")})
        }},
        confirmButton={TextButton(onClick={valid?.let(onApply)},enabled=valid!=null) {Text("Apply")}},
        dismissButton={TextButton(onClick=onDismiss) {Text("Cancel")}}
    )
}

/** Draft changes are not saved until the user chooses Apply. */
@Composable internal fun MbColorPickerButton(title:String,currentHex:String,onApply:(String)->Unit) {
    var open by remember {mutableStateOf(false)}
    TextButton(onClick={open=true},modifier=Modifier.semantics {contentDescription="Pick color for $title"}) {
        Text("Pick color")
    }
    if(open)MbColorPickerDialog(title,currentHex,onDismiss={open=false},onApply={hex->
        onApply(hex);open=false
    })
}


/**
 * MB 3.3.4 mb_colorpicker_layout.xml is a 45dp settings row with
 * 15sp / 13sp labels and a 30dp value swatch. Keep the actual validated
 * Liquid Chat RGB editor and its photo sampler as the action behind the row.
 */
@Composable internal fun MbReferenceColorRow(
    title:String,subtitle:String,value:String,
    onApply:(String)->Unit,modifier:Modifier=Modifier
) {
    val safe=normalizedAccentHex(value) ?: "#2563EB"
    val swatch=Color(safe.substring(1).toLong(16) or 0xFF000000L)
    var open by remember(title) {mutableStateOf(false)}
    Row(modifier.fillMaxWidth().heightIn(min=45.dp)
        .clip(Glass.compact)
        .clickable {open=true}
        .padding(horizontal=6.dp,vertical=6.dp),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f).padding(start=6.dp)) {
            Text(title,fontSize=15.sp,maxLines=1,
                color=MaterialTheme.colorScheme.onSurface)
            Text(subtitle,fontSize=13.sp,maxLines=1,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(Modifier.size(30.dp)
            .border(1.dp,MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(6.dp))
            .padding(2.dp)
            .background(swatch,RoundedCornerShape(4.dp))
            .semantics {contentDescription="Edit $title color: $safe"})
    }
    if(open) MbColorPickerDialog(title,safe,onDismiss={open=false},onApply={hex->
        onApply(hex)
        open=false
    })
}
