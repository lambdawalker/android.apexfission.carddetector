package com.apexfission.android.carddetector.ui.capture

import org.junit.Assert.*
import org.junit.Test

class SingleCaptureSessionTest {
    @Test fun replacementAndMissReleaseOwnedFrames() {
        val released = mutableListOf<String>()
        val session = SingleCaptureSession<String> { released += it }
        session.offer("first", 100)
        session.offer("second", 200)
        session.offer(null, 300)
        assertEquals(listOf("first", "second"), released)
        assertNull(session.capture(300, 400))
    }

    @Test fun transferOccursOnceAndCloseDoesNotRecycleHostImage() {
        val released = mutableListOf<String>()
        val session = SingleCaptureSession<String> { released += it }
        session.offer("host", 100)
        assertEquals("host", session.capture(200, 400))
        assertNull(session.capture(200, 400))
        session.offer("late", 300)
        session.close()
        session.close()
        assertEquals(listOf("late"), released)
    }

    @Test fun staleFutureAndUnstampedFramesCannotBeCaptured() {
        val session = SingleCaptureSession<String> {}
        session.offer("stale", 100)
        assertNull(session.capture(501, 400))
        assertNull(session.capture(99, 400))
        session.offer("unstamped", 0)
        assertNull(session.capture(100, 400))
        session.offer("fresh", 200)
        assertEquals("fresh", session.capture(600, 400))
    }

    @Test fun cancellationReleasesPendingAndLateFrames() {
        val released = mutableListOf<String>()
        val session = SingleCaptureSession<String> { released += it }
        session.offer("pending", 100)
        session.close()
        session.offer("late", 200)
        assertNull(session.capture(200, 400))
        assertEquals(listOf("pending", "late"), released)
    }
}
