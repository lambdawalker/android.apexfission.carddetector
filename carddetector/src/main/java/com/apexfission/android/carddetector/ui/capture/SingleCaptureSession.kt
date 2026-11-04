package com.apexfission.android.carddetector.ui.capture

/** Main-thread ownership of one current frame, transferred at most once per attempt. */
internal class SingleCaptureSession<T>(private val release: (T) -> Unit) {
    private var latest: T? = null
    private var observedAt = 0L
    private var closed = false
    var attempt = 0L
        private set
    var completed = false
        private set

    fun offer(image: T?, timestamp: Long, attempt: Long = this.attempt): Boolean {
        if (closed || completed || attempt != this.attempt) { image?.let(release); return false }
        latest?.let(release)
        latest = image
        observedAt = timestamp
        return true
    }

    fun isFresh(now: Long, maxGap: Long) = !closed && !completed && latest != null &&
        observedAt > 0 && now - observedAt in 0..maxGap

    fun capture(now: Long, maxGap: Long): T? {
        if (!isFresh(now, maxGap)) return null
        val image = latest ?: return null
        latest = null
        completed = true
        return image
    }

    fun reset() {
        if (closed) return
        attempt++
        completed = false
        latest?.let(release)
        latest = null
        observedAt = 0L
    }

    fun close() {
        latest?.let(release)
        latest = null
        closed = true
    }
}
