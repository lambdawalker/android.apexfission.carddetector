package com.apexfission.android.carddetector.ui.capture

import org.junit.Assert.*
import org.junit.Test

class CardCaptureConfigTest {
    @Test fun autoCaptureIsOptIn() { assertFalse(CardCaptureConfig().autoCaptureEnabled) }
    @Test fun invalidThresholdsAreRejected() {
        for (score in listOf(Float.NaN, Float.POSITIVE_INFINITY, 0f, -1f)) {
            assertThrows(IllegalArgumentException::class.java) { CardCaptureConfig(focusThreshold = score) }
        }
        assertThrows(IllegalArgumentException::class.java) { CardCaptureConfig(stableDurationMs = 0) }
        assertThrows(IllegalArgumentException::class.java) { CardCaptureConfig(maxDetectionGapMs = 0) }
    }
}
