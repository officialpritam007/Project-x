package com.pritam.liquidchat.ui

/**
 * Scales the original contrast veil over a personal photo wallpaper.
 * At 50%, all pre-existing light/dark overlay alphas remain byte-for-byte
 * identical to the previous Liquid Chat design.
 * Zero exposes the full photo; 100 stays below a fully opaque veil.
 */
internal fun wallpaperDimAlpha(originalAlpha:Float,intensity:Int):Float =
    (originalAlpha * (intensity.coerceIn(0,100)/50f)).coerceIn(0f,0.90f)
