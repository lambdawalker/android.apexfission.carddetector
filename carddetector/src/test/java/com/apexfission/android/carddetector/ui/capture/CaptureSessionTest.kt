package com.apexfission.android.carddetector.ui.capture

import org.junit.Assert.*
import org.junit.Test

class CaptureSessionTest {
    @Test fun capturesFrontThenBackAndTransfersOnlyOnConfirmation() {
        val released = mutableListOf<String>()
        val s = CaptureSession<String> { released += it }
        val first = s.requestCapture()!!
        assertNull(s.requestCapture())
        assertTrue(s.accept(first, "front"))
        assertEquals(CaptureStage.TurnOver, s.stage)
        assertNull(s.requestCapture())
        assertNull(s.complete())
        s.continueToBack()
        assertEquals(CardSide.Back, s.side)
        s.accept(s.requestCapture()!!, "back")
        assertEquals(CaptureStage.Review, s.stage)
        assertEquals("front" to "back", s.complete())
        assertNull(s.complete())
        s.close()
        assertTrue(released.isEmpty())
    }
    @Test fun staleOrDisposedCallbacksAreReleased() {
        val released = mutableListOf<String>()
        val s = CaptureSession<String> { released += it }
        val token = s.requestCapture()!!
        s.close()
        assertFalse(s.accept(token, "late"))
        assertEquals(listOf("late"), released)
    }
    @Test fun retakingFrontKeepsBackAndReturnsToReview() {
        val released = mutableListOf<String>()
        val s = CaptureSession<String> { released += it }
        val old = s.requestCapture()!!
        s.accept(old, "front")
        s.continueToBack()
        s.accept(s.requestCapture()!!, "back")
        s.retake(CardSide.Front)
        assertFalse(s.accept(old, "duplicate"))
        s.accept(s.requestCapture()!!, "replacement")
        assertEquals(CaptureStage.Review, s.stage)
        assertEquals("replacement" to "back", s.complete())
        assertEquals(listOf("front", "duplicate"), released)
    }
    @Test fun failureCanRetryAndCloseReleasesUndeliveredImagesOnce() {
        val released = mutableListOf<String>()
        val s = CaptureSession<String> { released += it }
        val t = s.requestCapture()!!
        s.failed(t)
        s.accept(s.requestCapture()!!, "front")
        s.close(); s.close()
        assertEquals(listOf("front"), released)
    }
}
