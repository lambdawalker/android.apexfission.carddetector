package com.apexfission.android.carddetector.ui.capture

import com.apexfission.android.carddetector.domain.tflite.model.CardDetection

/** Versioned mode prevents queued frames from capturing after a toggle. */
internal data class AutoCaptureMode(val enabled: Boolean, val generation: Long = 0L) {
    fun toggled() = copy(enabled = !enabled, generation = generation + 1)
    fun accepts(frameGeneration: Long, ready: Boolean) = enabled && ready && generation == frameGeneration
}

/** Confined to the sequential detector evaluation worker, before any UI conflation. */
internal class CaptureReadiness(private val config: CardCaptureConfig) {
    private val gate = AutoCaptureGate(config.stableDurationMs, config.maxDetectionGapMs)
    private var sequence = 0L
    private var generation = 0L
    var hasDetected = false
        private set

    fun evaluate(detection: CardDetection?, nowMs: Long, mode: AutoCaptureMode = AutoCaptureMode(true)): Boolean {
        if (detection != null) hasDetected = true
        if (generation != mode.generation) {
            gate.reset()
            generation = mode.generation
        }
        if (!mode.enabled) {
            gate.reset()
            return false
        }
        val box = detection?.card?.box
        val fresh = detection != null && detection.observedAtElapsedRealtimeMs > 0 &&
            nowMs - detection.observedAtElapsedRealtimeMs in 0..config.maxDetectionGapMs
        val ready = fresh && detection != null && detection.lockOnProgress >= 1f &&
            detection.focusScore?.let { it.isFinite() && it >= config.focusThreshold } == true
        return gate.update(++sequence, nowMs, ready, box?.let {
            CaptureBounds(it.x.toFloat(), it.y.toFloat(), it.x2.toFloat(), it.y2.toFloat())
        })
    }
}
