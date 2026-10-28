package com.apexfission.android.carddetector.ui.capture

import org.junit.Assert.*
import org.junit.Test

class AutoCaptureGateTest {
    private val box = CaptureBounds(0f, 0f, 200f, 120f)
    @Test fun requiresNewFramesAndContinuousStability() {
        val gate = AutoCaptureGate(600, 400)
        assertFalse(gate.update(1, 0, true, box))
        assertFalse(gate.update(1, 600, true, box)) // duplicate does not advance dwell
        assertFalse(gate.update(2, 300, true, box))
        assertTrue(gate.update(3, 600, true, box))
    }
    @Test fun missesBlurAndStaleFramesRestartDwell() {
        for (failure in listOf("blur", "missing", "stale")) {
            val g = AutoCaptureGate(600, 400)
            g.update(1, 0, true, box)
            if (failure == "stale") g.update(2, 600, true, box)
            else g.update(2, 300, failure != "blur", if (failure == "missing") null else box)
            assertFalse(g.update(3, 650, true, box))
        }
    }
    @Test fun movementAndCumulativeDriftRestartDwell() {
        val g = AutoCaptureGate(600, 400)
        g.update(1, 0, true, box)
        assertFalse(g.update(2, 300, true, box.copy(left=20f,right=220f)))
        assertFalse(g.update(3, 600, true, box.copy(left=40f,right=240f)))
    }
}
