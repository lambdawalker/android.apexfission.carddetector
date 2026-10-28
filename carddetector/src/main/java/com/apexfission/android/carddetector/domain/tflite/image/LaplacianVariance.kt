package com.apexfission.android.carddetector.domain.tflite.image

/** Variance of the four-neighbour Laplacian on 8-bit luminance samples. */
internal fun laplacianVariance(luma: IntArray, width: Int, height: Int): Float? {
    if (width < 3 || height < 3 || luma.size.toLong() != width.toLong() * height) return null
    var sum = 0.0
    var squared = 0.0
    var count = 0
    for (y in 1 until height - 1) for (x in 1 until width - 1) {
        val i = y * width + x
        val v = (luma[i - 1] + luma[i + 1] + luma[i - width] + luma[i + width] - 4 * luma[i]).toDouble()
        sum += v
        squared += v * v
        count++
    }
    return (squared / count - (sum / count) * (sum / count)).coerceAtLeast(0.0).toFloat()
}
