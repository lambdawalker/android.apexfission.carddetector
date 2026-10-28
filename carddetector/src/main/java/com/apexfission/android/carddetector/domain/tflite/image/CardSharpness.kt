package com.apexfission.android.carddetector.domain.tflite.image

import android.graphics.Bitmap
import com.apexfission.android.math.models.ImageBox
import kotlin.math.roundToInt

/** Normalize the card's longest edge to at most 256px before assessing sharpness.
 * Borders are inset to avoid scoring background contrast as card detail. */
internal fun cardSharpness(bitmap: Bitmap, box: ImageBox): Float? {
    if (bitmap.width < 3 || bitmap.height < 3 || bitmap.isRecycled) return null
    val left = box.x.toInt().coerceIn(0, bitmap.width)
    val top = box.y.toInt().coerceIn(0, bitmap.height)
    val right = box.x2.toInt().coerceIn(left, bitmap.width)
    val bottom = box.y2.toInt().coerceIn(top, bitmap.height)
    val insetX = ((right - left) * 0.05f).toInt()
    val insetY = ((bottom - top) * 0.05f).toInt()
    val width = right - left - 2 * insetX
    val height = bottom - top - 2 * insetY
    if (width < 16 || height < 16) return null
    val region = Bitmap.createBitmap(bitmap, left + insetX, top + insetY, width, height)
    var scaled: Bitmap? = null
    try {
        val scale = minOf(1f, 256f / maxOf(width, height))
        scaled = Bitmap.createScaledBitmap(region, (width * scale).roundToInt(), (height * scale).roundToInt(), true)
        val pixels = IntArray(scaled.width * scaled.height)
        scaled.getPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)
        for (i in pixels.indices) {
            val p = pixels[i]
            pixels[i] = (77 * (p shr 16 and 255) + 150 * (p shr 8 and 255) + 29 * (p and 255)) shr 8
        }
        return laplacianVariance(pixels, scaled.width, scaled.height)
    } finally {
        if (scaled != null && scaled !== region && scaled !== bitmap) scaled.recycle()
        if (region !== bitmap) region.recycle()
    }
}
