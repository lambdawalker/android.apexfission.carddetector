package com.apexfission.android.carddetector.ui.capture

import com.apexfission.android.carddetector.domain.tflite.model.CardDetection

internal enum class CaptureGuidance { Looking, Focusing, HoldSteady, Ready }

internal fun captureGuidance(
    detection: CardDetection?, nowMs: Long, config: CardCaptureConfig,
    automatic: Boolean, autoReady: Boolean,
): CaptureGuidance = when {
    detection == null || detection.observedAtElapsedRealtimeMs <= 0 ||
        nowMs - detection.observedAtElapsedRealtimeMs !in 0..config.maxDetectionGapMs -> CaptureGuidance.Looking
    detection.focusScore?.let { it.isFinite() && it >= config.focusThreshold } != true -> CaptureGuidance.Focusing
    !detection.lockOnProgress.isFinite() || detection.lockOnProgress < 1f || automatic && !autoReady -> CaptureGuidance.HoldSteady
    else -> CaptureGuidance.Ready
}

internal fun CaptureGuidance.text(strings: CardCaptureStrings): String = when (this) {
    CaptureGuidance.Looking -> strings.find
    CaptureGuidance.Focusing -> strings.focus
    CaptureGuidance.HoldSteady -> strings.ready
    CaptureGuidance.Ready -> strings.readyToCapture
}
