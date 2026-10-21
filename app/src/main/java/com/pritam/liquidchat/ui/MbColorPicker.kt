package com.pritam.liquidchat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
    val valid=normalizedAccentHex(input)
    fun update(value:MbHsvColor) {hsv=value;input=value.toHex()}
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
