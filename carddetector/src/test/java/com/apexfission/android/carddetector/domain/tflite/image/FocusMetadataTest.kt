package com.apexfission.android.carddetector.domain.tflite.image

import com.apexfission.android.carddetector.domain.tflite.model.*
import com.apexfission.android.yolo.engine.Feature
import com.apexfission.android.math.models.ImageBox
import org.junit.Assert.*
import org.junit.Test

class FocusMetadataTest {
    private fun detection(score: Float?) = CardDetection(1f, 1L,
        Feature(ImageBox.from2P(0, 0, 100, 60), 0.99f, 0), emptyList(), LockingStatus.CardLocked,
        focusScore = score)
    @Test fun unknownAndInvalidScoresNeverReportFocused() {
        for (score in listOf(null, Float.NaN, Float.POSITIVE_INFINITY, -1f)) {
            assertEquals(FocusState.Unknown, detection(score).focusState)
            assertFalse(detection(score).isFocused)
        }
    }
    @Test fun thresholdBoundaryIsExplicitAndIndependentOfLock() {
        assertEquals(FocusState.Unfocused, detection(99f).focusState)
        assertTrue(detection(100f).isFocused)
        assertFalse(detection(0f).isFocused)
    }
}
