package com.apexfission.android.carddetector.ui.capture

import com.apexfission.android.carddetector.domain.tflite.model.CardDetection

/** Confined to the sequential detector evaluation worker, before any UI conflation. */
internal class CaptureReadiness(private val config: CardCaptureConfig) {
    private val gate = AutoCaptureGate(config.stableDurationMs, config.maxDetectionGapMs)
    private var sequence = 0L

    fun evaluate(detection: CardDetection?, nowMs: Long): Boolean {
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
