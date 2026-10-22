package com.pritam.liquidchat.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

internal data class MbTouchConfig(
    val enabled:Boolean=false,
    val glyph:String="❤️",
    val color:String="#FF477E"
)

internal object MbTouchEffects {
    private const val NAME="liquid_touch_visuals_v1"
    private fun prefs(context:Context):SharedPreferences=
        context.applicationContext.getSharedPreferences(NAME,Context.MODE_PRIVATE)
    private fun glyph(raw:String):String {
        val value=raw.trim().take(12)
        return value.takeIf {it.isNotEmpty() && it.none(Char::isISOControl)}
            ?: "❤️"
    }
    fun read(context:Context):MbTouchConfig {
        val p=prefs(context)
        return MbTouchConfig(
            enabled=p.getBoolean("enabled",false),
            glyph=glyph(p.getString("glyph","❤️").orEmpty()),
            color=normalizedAccentHex(p.getString("color","#FF477E").orEmpty())
                ?: "#FF477E")
    }
    fun save(context:Context,config:MbTouchConfig) {
        val p=prefs(context)
        p.edit().putBoolean("enabled",config.enabled)
            .putString("glyph",glyph(config.glyph))
            .putString("color",normalizedAccentHex(config.color)?:"#FF477E")
            .apply()
    }
    fun listen(context:Context,listener:SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs(context).registerOnSharedPreferenceChangeListener(listener)
    }
    fun unlisten(context:Context,listener:SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs(context).unregisterOnSharedPreferenceChangeListener(listener)
    }
}

@Composable internal fun rememberMbTouchConfig():MbTouchConfig {
    val context=LocalContext.current.applicationContext
    var config by remember(context) {mutableStateOf(MbTouchEffects.read(context))}
    DisposableEffect(context) {
        val listener=SharedPreferences.OnSharedPreferenceChangeListener {_,_->
            config=MbTouchEffects.read(context)
        }
        MbTouchEffects.listen(context,listener)
        onDispose {MbTouchEffects.unlisten(context,listener)}
    }
    return config
}

private data class MbTouchParticle(val id:Long,val point:Offset)

@Composable internal fun MbTouchHost(content:@Composable BoxScope.()->Unit) {
    val config=rememberMbTouchConfig()
    val reduced=LocalDisplayOptions.current.reducedMotion
    val enabled=config.enabled&&!reduced
    val particles=remember {mutableStateListOf<MbTouchParticle>()}
    var nextId by remember {mutableLongStateOf(0L)}
    LaunchedEffect(enabled) {if(!enabled)particles.clear()}
    val listener=if(enabled)Modifier.pointerInput(config.enabled,reduced) {
        // Parent's Initial pass observes taps; neither consumes nor blocks
        // the child button, scrolling, reply, calling or text field gesture.
        awaitPointerEventScope {
            while(true) {
                val event=awaitPointerEvent(PointerEventPass.Initial)
                event.changes.forEach {change->
                    if(change.changedToDownIgnoreConsumed()) {
                        if(particles.size>=8)particles.removeAt(0)
                        particles.add(MbTouchParticle(++nextId,change.position))
                    }
                }
            }
        }
    } else Modifier
    Box(Modifier.fillMaxSize().then(listener)) {
        content()
        if(enabled)particles.forEach {particle->
            key(particle.id) {
                var started by remember {mutableStateOf(false)}
                val rise by animateFloatAsState(if(started)1f else 0f,
                    animationSpec=tween(durationMillis=650),
                    label="reference touch animation")
                LaunchedEffect(particle.id) {
                    started=true
                    delay(700)
                    particles.remove(particle)
                }
                Text(config.glyph,
                    modifier=Modifier.offset {
                        IntOffset(particle.point.x.toInt()-18,
                            particle.point.y.toInt()-24)
                    }.graphicsLayer {
                        alpha=(1f-rise).coerceIn(0f,1f)
                        translationY=-52f*rise
                        scaleX=1f+rise*.25f
                        scaleY=1f+rise*.25f
                    },
                    color=Color(android.graphics.Color.parseColor(config.color)),
                    fontSize=28.sp,maxLines=1)
            }
        }
    }
}

@Composable internal fun MbTouchEffectsSettingsCard() {
    val context=LocalContext.current
    val config=rememberMbTouchConfig()
    val reduced=LocalDisplayOptions.current.reducedMotion
    GlassCard(Modifier.fillMaxWidth(),Glass.hero) {
        Text("Touch animation",style=MaterialTheme.typography.titleMedium)
        Text("Optional visual touch feedback. No touch is intercepted and no chat data is collected.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(LocalDisplayOptions.current.navBarStyle=="Reference")
            MbReferenceSwitchRow(
                title="Touch animation",
                subtitle="Show floating character when the screen is tapped",
                checked=config.enabled,
                onCheckedChange={MbTouchEffects.save(context,config.copy(enabled=it))})
        else Row(Modifier.fillMaxWidth(),
            verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            Text("Show floating character on touch",Modifier.weight(1f))
            Switch(checked=config.enabled,onCheckedChange={
                MbTouchEffects.save(context,config.copy(enabled=it))
            })
        }
        MbReferenceEditTextRow(
            title="Touch character",
            description="Choose one visible character or emoji.",
            value=config.glyph,maxLength=12,
            valid={it.isNotBlank() && it.none(Char::isISOControl)},
            onCommit={glyph->
                MbTouchEffects.save(context,config.copy(glyph=glyph))
            })
        MbColorPickerButton("Touch animation color",config.color) {
            MbTouchEffects.save(context,config.copy(color=it))
        }
        if(reduced)Text("Reduce Motion is on, so touch animation is disabled.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
