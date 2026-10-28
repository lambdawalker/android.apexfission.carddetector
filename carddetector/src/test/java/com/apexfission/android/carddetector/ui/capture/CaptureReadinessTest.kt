package com.apexfission.android.carddetector.ui.capture

import com.apexfission.android.carddetector.domain.tflite.model.*
import com.apexfission.android.math.models.ImageBox
import com.apexfission.android.yolo.engine.Feature
import org.junit.Assert.*
import org.junit.Test

class CaptureReadinessTest {
    private fun card(time: Long, score: Float = 200f, left: Int = 0) = CardDetection(
        1f, 1L, Feature(ImageBox.from2P(left, 0, left+200, 120), 1f, 0), emptyList(), LockingStatus.CardLocked,
        focusScore=score, observedAtElapsedRealtimeMs=time)
    @Test fun intermediateInvalidEvaluationsResetEvenWhenUiDropsTheirResults() {
        for (invalid in listOf<CardDetection?>(null, card(590, 0f), card(590, left=30))) {
            val worker = CaptureReadiness(CardCaptureConfig())
            worker.evaluate(card(1), 1)
            worker.evaluate(card(300), 300)
            worker.evaluate(invalid, 590) // UI can discard this result; worker already reset.
            assertFalse(worker.evaluate(card(610), 610))
        }
    }
    @Test fun staleFrameAndUnlockedCardCannotTriggerCapture() {
        val worker = CaptureReadiness(CardCaptureConfig())
        assertFalse(worker.evaluate(card(1), 1))
        assertFalse(worker.evaluate(card(1), 700))
        assertFalse(worker.evaluate(card(710).copy(lockOnProgress=0.8f), 710))
    }
}
