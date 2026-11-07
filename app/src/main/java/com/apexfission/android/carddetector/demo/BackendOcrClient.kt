package com.apexfission.android.carddetector.demo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

internal data class BackendOcrResult(
    val totalMs: Long,
    val inferenceMs: Long,
    val extraction: String?,
    val error: String?,
)

internal const val DEMO_ORIGIN = "https://demo.card-detector.apexfission.com"

internal fun demoJson(path: String, token: String? = null, body: JSONObject? = null): JSONObject {
    val connection = URL(DEMO_ORIGIN + path).openConnection() as HttpURLConnection
    try {
        connection.instanceFollowRedirects = false
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        token?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
        if (body != null) {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        }
        if (connection.responseCode !in 200..299) {
            val message = when (connection.responseCode) {
                401, 403 -> "Your session expired. Sign in again."
                429 -> "Please wait one minute before starting another scan."
                else -> "The demo service returned HTTP ${connection.responseCode}."
            }
            throw java.io.IOException(message)
        }
        return connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
    } finally {
        connection.disconnect()
    }
}

internal suspend fun runBackendOcr(front: Bitmap, back: Bitmap, token: suspend () -> String): BackendOcrResult {
    val jpeg = stitchCards(front, back)
    try {
        val checksum = android.util.Base64.encodeToString(
            java.security.MessageDigest.getInstance("SHA-256").digest(jpeg), android.util.Base64.NO_WRAP)
        val job = demoJson("/api/ocr/jobs", token(), JSONObject().put("bytes", jpeg.size).put("sha256", checksum))
        val upload = URL(job.getString("uploadUrl"))
        require(upload.protocol == "https" && upload.userInfo == null) { "Invalid upload URL" }
        val connection = upload.openConnection() as HttpURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 10_000
            connection.readTimeout = 30_000
            connection.requestMethod = "PUT"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "image/jpeg")
            connection.setRequestProperty("x-amz-checksum-sha256", checksum)
            connection.setRequestProperty("If-None-Match", "*")
            connection.setFixedLengthStreamingMode(jpeg.size)
            connection.outputStream.use { it.write(jpeg) }
            check(connection.responseCode in 200..299) { "Image upload failed (HTTP ${connection.responseCode})." }
        } finally {
            connection.disconnect()
        }
        val id = job.getString("id")
        require(id.matches(Regex("[0-9a-f]{32}")))
        return kotlinx.coroutines.withTimeout(310_000) {
            var completed: BackendOcrResult? = null
            while (completed == null) {
                kotlinx.coroutines.delay(1_000)
                val status = demoJson("/api/ocr/jobs/$id", token())
                if (status.getString("state") in listOf("complete", "failed")) {
                    val json = status.optJSONObject("result") ?: JSONObject()
                    completed = BackendOcrResult(
                        totalMs = json.optLong("total_ms"),
                        inferenceMs = json.optLong("inference_ms"),
                        extraction = json.optJSONObject("extraction")?.toString(2),
                        error = if (json.optBoolean("success")) null else
                            json.optString("error", status.optString("error", "extraction_failed")),
                    )
                }
            }
            checkNotNull(completed)
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
