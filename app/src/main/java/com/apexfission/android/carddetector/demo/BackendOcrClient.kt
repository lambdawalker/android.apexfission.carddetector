package com.apexfission.android.carddetector.demo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URL
import kotlin.math.roundToInt

internal data class BackendOcrResult(
    val totalMs: Long,
    val inferenceMs: Long,
    val extraction: String?,
    val error: String?,
)

internal fun runBackendOcr(front: Bitmap, back: Bitmap, code: String): BackendOcrResult {
    val jpeg = stitchCards(front, back)
    try {
        val connection = URL("http://127.0.0.1:8765/ocr").openConnection(Proxy.NO_PROXY) as HttpURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 5_000
            connection.readTimeout = 150_000
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $code")
            connection.setRequestProperty("Content-Type", "image/jpeg")
            connection.setFixedLengthStreamingMode(jpeg.size)
            connection.outputStream.use { it.write(jpeg) }
            check(connection.responseCode == HttpURLConnection.HTTP_OK)
            val json = connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
            return BackendOcrResult(
                totalMs = json.optLong("total_ms"),
                inferenceMs = json.optLong("inference_ms"),
                extraction = json.optJSONObject("extraction")?.toString(2),
                error = if (json.getBoolean("success")) null else json.optString("error", "extraction_failed"),
            )
        } finally {
            connection.disconnect()
        }
    } finally {
        jpeg.fill(0)
    }
}

private fun stitchCards(front: Bitmap, back: Bitmap): ByteArray {
    val scale = minOf(1.0, 1200.0 / maxOf(front.width, back.width), 1200.0 / (front.height + back.height))
    val frontWidth = (front.width * scale).roundToInt().coerceAtLeast(1)
    val backWidth = (back.width * scale).roundToInt().coerceAtLeast(1)
    val frontHeight = (front.height * scale).toInt().coerceIn(1, 1199)
    val backHeight = (back.height * scale).toInt().coerceIn(1, 1200 - frontHeight)
    val stitched = Bitmap.createBitmap(maxOf(frontWidth, backWidth), frontHeight + backHeight, Bitmap.Config.ARGB_8888)
    try {
        val canvas = Canvas(stitched)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        val frontX = (stitched.width - frontWidth) / 2
        val backX = (stitched.width - backWidth) / 2
        canvas.drawBitmap(front, null, Rect(frontX, 0, frontX + frontWidth, frontHeight), paint)
        canvas.drawBitmap(back, null, Rect(backX, frontHeight, backX + backWidth, stitched.height), paint)
        val encoded = ByteArrayOutputStream()
        for (quality in listOf(85, 75, 65, 55, 45, 35)) {
            encoded.reset()
            check(stitched.compress(Bitmap.CompressFormat.JPEG, quality, encoded))
            if (encoded.size() <= 500 * 1024) return encoded.toByteArray()
        }
        error("The stitched image exceeds the bench limit")
    } finally {
        stitched.recycle()
    }
}
