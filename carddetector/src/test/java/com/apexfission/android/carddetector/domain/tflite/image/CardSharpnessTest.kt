package com.apexfission.android.carddetector.domain.tflite.image

import org.junit.Assert.*
import org.junit.Test

class CardSharpnessTest {
    @Test fun flatImageHasNoFocus() {
        assertEquals(0f, laplacianVariance(IntArray(64) { 127 }, 8, 8)!!, 0f)
    }
    @Test fun detailScoresHigherThanBlurredRamp() {
        val detail = IntArray(64) { if ((it % 8 + it / 8) % 2 == 0) 0 else 255 }
        val ramp = IntArray(64) { (it % 8) * 30 }
        assertTrue(laplacianVariance(detail, 8, 8)!! > 100f)
        assertEquals(0f, laplacianVariance(ramp, 8, 8)!!, 0f)
    }
    @Test fun tinyRegionIsUnknown() {
        assertNull(laplacianVariance(intArrayOf(0), 1, 1))
    }
}
