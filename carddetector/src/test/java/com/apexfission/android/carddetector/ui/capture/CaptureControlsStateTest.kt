package com.apexfission.android.carddetector.ui.capture

import com.apexfission.android.carddetector.domain.tflite.model.*
import com.apexfission.android.math.models.ImageBox
import com.apexfission.android.yolo.engine.Feature
import org.junit.Assert.*
import org.junit.Test

class CaptureControlsStateTest {
    private fun card(time: Long) = CardDetection(1f, 1L,
        Feature(ImageBox.from2P(0, 0, 200, 120), 1f, 0), emptyList(), LockingStatus.CardLocked,
        focusScore = 200f, observedAtElapsedRealtimeMs = time)

    @Test fun togglingOffRejectsQueuedReadyFramesAndOnStartsNewDwell() {
        val worker = CaptureReadiness(CardCaptureConfig())
        val on = AutoCaptureMode(true)
        assertFalse(worker.evaluate(card(1), 1, on))
        worker.evaluate(card(301), 301, on)
        assertTrue(worker.evaluate(card(601), 601, on))
        val off = on.toggled()
        assertFalse(off.accepts(on.generation, true))
        assertFalse(worker.evaluate(card(650), 650, off))
        val enabledAgain = off.toggled()
        assertFalse(enabledAgain.accepts(on.generation, true))
        assertFalse(worker.evaluate(card(701), 701, enabledAgain))
        worker.evaluate(card(1001), 1001, enabledAgain)
        assertTrue(worker.evaluate(card(1301), 1301, enabledAgain))
        assertTrue(enabledAgain.accepts(enabledAgain.generation, true))
    }

    @Test fun togglingTwiceBetweenFramesStillResetsDwell() {
        val worker = CaptureReadiness(CardCaptureConfig())
        val on = AutoCaptureMode(true)
        worker.evaluate(card(1), 1, on)
        worker.evaluate(card(301), 301, on)
        assertFalse(worker.evaluate(card(601), 601, on.toggled().toggled()))
    }

    @Test fun initialSquareStaysHiddenAfterDetectionEvenWhenCardLeaves() {
        val worker = CaptureReadiness(CardCaptureConfig())
        assertFalse(worker.hasDetected)
        worker.evaluate(null, 1)
        assertFalse(worker.hasDetected)
        worker.evaluate(card(2), 2, AutoCaptureMode(false))
        assertTrue(worker.hasDetected)
        worker.evaluate(null, 3)
        assertTrue(worker.hasDetected)
        assertFalse(CaptureReadiness(CardCaptureConfig()).hasDetected)
    }
}
