package com.apexfission.android.carddetector.ui.capture

import com.apexfission.android.carddetector.domain.tflite.model.*
import com.apexfission.android.math.models.ImageBox
import com.apexfission.android.yolo.engine.Feature
import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureGuidanceTest {
    private val config = CardCaptureConfig()
    private val card = CardDetection(1f, 1L, Feature(ImageBox.from2P(0, 0, 200, 120), 1f, 0),
        emptyList(), LockingStatus.CardLocked, focusScore = 200f, observedAtElapsedRealtimeMs = 100)

    @Test fun distinguishesMissingBlurredUnstableAndReady() {
        assertEquals(CaptureGuidance.Looking, captureGuidance(null, 100, config, true, false))
        assertEquals(CaptureGuidance.Focusing, captureGuidance(card.copy(focusScore = 0f), 100, config, true, false))
        assertEquals(CaptureGuidance.HoldSteady, captureGuidance(card, 100, config, true, false))
        assertEquals(CaptureGuidance.Ready, captureGuidance(card, 100, config, true, true))
        assertEquals(CaptureGuidance.Ready, captureGuidance(card, 100, config, false, false))
        assertEquals(CaptureGuidance.HoldSteady, captureGuidance(card.copy(lockOnProgress = 0.5f), 100, config, false, false))
    }

    @Test fun staleFrameOverridesPreviouslyReadyGuidance() {
        assertEquals(CaptureGuidance.Looking, captureGuidance(card, 501, config, true, true))
        assertEquals(CaptureGuidance.Focusing, captureGuidance(card.copy(focusScore = Float.NaN), 100, config, true, true))
    }
}
