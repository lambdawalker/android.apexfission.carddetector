package com.apexfission.android.carddetector.ui.overlays

import com.apexfission.android.carddetector.ui.overlays.components.InitialGuideState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InitialGuideStateTest {
    @Test fun firstDetectionPermanentlyHidesGuideForThisAttempt() {
        val state = InitialGuideState()
        assertTrue(state.shouldShow(requested = true, hasDetection = false))
        assertFalse(state.shouldShow(requested = true, hasDetection = true))
        repeat(10) { assertFalse(state.shouldShow(requested = true, hasDetection = false)) }
    }

    @Test fun newAttemptRestoresGuideButConfigurationDoesNotResetDetectionHistory() {
        val front = InitialGuideState()
        assertFalse(front.shouldShow(requested = false, hasDetection = true))
        assertFalse(front.shouldShow(requested = true, hasDetection = false))
        val backOrRetake = InitialGuideState()
        assertTrue(backOrRetake.shouldShow(requested = true, hasDetection = false))
    }

    @Test fun explicitlyHiddenGuideStaysHiddenBeforeDetection() {
        assertFalse(InitialGuideState().shouldShow(requested = false, hasDetection = false))
    }
}
