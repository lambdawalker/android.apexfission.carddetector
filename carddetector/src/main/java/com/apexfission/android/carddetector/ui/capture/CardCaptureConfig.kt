package com.apexfission.android.carddetector.ui.capture

/** Auto-capture is opt-in. Thresholds are heuristics requiring target-device validation. */
data class CardCaptureConfig(
    val autoCaptureEnabled: Boolean = false,
    val focusThreshold: Float = 100f,
    val stableDurationMs: Long = 600L,
    val maxDetectionGapMs: Long = 400L,
) {
    init {
        require(focusThreshold.isFinite() && focusThreshold > 0f)
        require(stableDurationMs > 0L)
        require(maxDetectionGapMs > 0L)
    }
}
