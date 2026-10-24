package com.pritam.liquidchat.calls

import android.content.Context
import android.graphics.SurfaceTexture
import android.view.TextureView
import org.webrtc.*
import java.util.concurrent.CountDownLatch

/** A composited video surface: rounded clipping applies to pixels, not just the border. */
class CallTextureRenderer(context:Context):TextureView(context),TextureView.SurfaceTextureListener,VideoSink {
    private val renderer=EglRenderer("LiquidCallTexture")
    private var firstFrame:(()->Unit)?=null
    fun init(shared:EglBase.Context,onFirstFrame:()->Unit) {
        firstFrame=onFirstFrame
        renderer.init(shared,EglBase.CONFIG_PLAIN,GlRectDrawer())
        surfaceTextureListener=this
        if(isAvailable)surfaceTexture?.let(renderer::createEglSurface)
    }
    fun setMirror(mirror:Boolean)=renderer.setMirror(mirror)
    override fun onFrame(frame:VideoFrame) {
        firstFrame?.invoke()
        renderer.onFrame(frame)
    }
    override fun onSurfaceTextureAvailable(surface:SurfaceTexture,width:Int,height:Int) {
        renderer.createEglSurface(surface);renderer.setLayoutAspectRatio(width.toFloat()/height)
    }
    override fun onSurfaceTextureSizeChanged(surface:SurfaceTexture,width:Int,height:Int) {
        renderer.setLayoutAspectRatio(width.toFloat()/height)
    }
    override fun onSurfaceTextureDestroyed(surface:SurfaceTexture):Boolean {
        val done=CountDownLatch(1)
        renderer.releaseEglSurface {done.countDown()}
        ThreadUtils.awaitUninterruptibly(done)
        return true
    }
    override fun onSurfaceTextureUpdated(surface:SurfaceTexture) {}
    fun release(){firstFrame=null;renderer.release()}
}
