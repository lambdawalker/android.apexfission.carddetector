package com.apexfission.android.carddetector.ui.capture

enum class CardSide { Front, Back }
internal enum class CaptureStage { Camera, TurnOver, Review, Complete, Closed }

/** Single-threaded ownership state machine. UI dispatches all events on the main thread. */
internal class CaptureSession<T>(private val release: (T) -> Unit) {
    var stage = CaptureStage.Camera
        private set
    var side = CardSide.Front
        private set
    var attempt = 0L
        private set
    var pending = false
        private set
    var front: T? = null
        private set
    var back: T? = null
        private set

    fun requestCapture(): Long? {
        if (stage != CaptureStage.Camera || pending) return null
        pending = true
        return attempt
    }

    fun accept(token: Long, image: T): Boolean {
        if (stage != CaptureStage.Camera || !pending || token != attempt) {
            release(image)
            return false
        }
        pending = false
        if (side == CardSide.Front) front = image else back = image
        stage = if (front != null && back != null) CaptureStage.Review else CaptureStage.TurnOver
        return true
    }

    fun failed(token: Long) {
        if (stage == CaptureStage.Camera && token == attempt) pending = false
    }

    fun continueToBack() {
        if (stage != CaptureStage.TurnOver) return
        side = CardSide.Back
        attempt++
        stage = CaptureStage.Camera
    }

    fun retake(target: CardSide) {
        if (stage != CaptureStage.Review) return
        if (target == CardSide.Front) { front?.let(release); front = null }
        else { back?.let(release); back = null }
        side = target
        attempt++
        pending = false
        stage = CaptureStage.Camera
    }

    fun complete(): Pair<T, T>? {
        if (stage != CaptureStage.Review) return null
        val result = (front ?: return null) to (back ?: return null)
        front = null
        back = null
        stage = CaptureStage.Complete
        return result
    }

    fun close() {
        front?.let(release)
        back?.let(release)
        front = null
        back = null
        pending = false
        stage = CaptureStage.Closed
    }
}
