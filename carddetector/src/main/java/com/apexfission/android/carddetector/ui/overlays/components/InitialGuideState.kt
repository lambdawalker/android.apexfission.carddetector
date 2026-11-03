package com.apexfission.android.carddetector.ui.overlays.components

/** One initial guide per mounted capture attempt; frame misses never start a new attempt. */
internal class InitialGuideState {
    private var hasDetected = false

    fun shouldShow(requested: Boolean, hasDetection: Boolean): Boolean {
        hasDetected = hasDetected || hasDetection
        return requested && !hasDetected
    }
}
