package com.apexfission.android.carddetector.ui.capture

import kotlin.math.abs

internal data class CaptureBounds(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width get() = right - left
    val height get() = bottom - top
    val valid get() = listOf(left, top, right, bottom).all { it.isFinite() } && width > 0 && height > 0
}

/** Dwell advances only on distinct evaluations, never on a timer over a frozen frame. */
internal class AutoCaptureGate(private val holdMs: Long, private val maxGapMs: Long) {
    private var sequence = -1L
    private var firstTime: Long? = null
    private var lastTime: Long? = null
    private var anchor: CaptureBounds? = null

    fun update(sequence: Long, nowMs: Long, ready: Boolean, bounds: CaptureBounds?): Boolean {
        if (sequence <= 0L || !ready || bounds == null || !bounds.valid) { reset(); return false }
        if (sequence == this.sequence) return false
        val gap = lastTime?.let { nowMs - it }
        if (sequence < this.sequence || gap != null && (gap < 0 || gap > maxGapMs)) reset()
        this.sequence = sequence
        lastTime = nowMs
        val old = anchor
        if (old == null || abs(bounds.left - old.left) > old.width * 0.04f ||
            abs(bounds.top - old.top) > old.height * 0.04f ||
            abs(bounds.right - old.right) > old.width * 0.04f ||
            abs(bounds.bottom - old.bottom) > old.height * 0.04f) {
            anchor = bounds
            firstTime = nowMs
            return false
        }
        return nowMs - (firstTime ?: nowMs) >= holdMs
    }

    fun reset() { sequence = -1L; firstTime = null; lastTime = null; anchor = null }
}
