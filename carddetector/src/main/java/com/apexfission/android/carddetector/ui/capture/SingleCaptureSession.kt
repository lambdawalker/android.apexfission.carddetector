package com.apexfission.android.carddetector.ui.capture

/** Main-thread ownership of one current frame, transferred at most once per attempt. */
internal class SingleCaptureSession<T>(private val release: (T) -> Unit) {
    private var latest: T? = null
    private var observedAt = 0L
    private var closed = false
    var completed = false
        private set

    fun offer(image: T?, timestamp: Long) {
        if (closed || completed) { image?.let(release); return }
        latest?.let(release)
        latest = image
        observedAt = timestamp
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

    fun close() {
        latest?.let(release)
        latest = null
        closed = true
    }
}
