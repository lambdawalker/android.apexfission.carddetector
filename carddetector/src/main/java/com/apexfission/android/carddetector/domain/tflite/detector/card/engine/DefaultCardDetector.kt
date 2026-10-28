package com.apexfission.android.carddetector.domain.tflite.detector.card.engine

import android.graphics.Bitmap
import com.apexfission.android.math.models.ImageBox
import com.apexfission.android.carddetector.domain.tflite.image.cardSharpness
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import com.apexfission.android.carddetector.domain.tflite.detector.card.selection.CardCandidateSelector
import com.apexfission.android.carddetector.domain.tflite.detector.card.tracking.CardLockStateMachine
import com.apexfission.android.carddetector.domain.tflite.detector.card.tracking.TemporalConsistencyChecker
import com.apexfission.android.yolo.engine.Detector
import com.apexfission.android.carddetector.domain.tflite.filters.AspectRatioValidator
import com.apexfission.android.carddetector.domain.tflite.filters.CardValidator
import com.apexfission.android.carddetector.domain.tflite.filters.MarginValidator
import com.apexfission.android.yolo.image.toUprightBitmap
import com.apexfission.android.carddetector.domain.tflite.model.CardDetection

internal class DefaultCardDetector internal constructor(
    private val yoloDetector: Detector,
    private val candidateSelector: CardCandidateSelector,
    private val stateMachine: CardLockStateMachine,
    private val differenceHashDistanceLimit: Int = 25,
    private val hashBasedSearchFrameLimit: Int = 3,
    private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime,
    private val measureSharpness: (Bitmap, ImageBox) -> Float? = ::cardSharpness
) : CardDetector {

    constructor(
        yoloDetector: Detector,
        cardValidators: List<CardValidator> = listOf(
            AspectRatioValidator(),
            MarginValidator()
        ),
        cardClasses: Set<Int>,
        lockOnThreshold: Int = 5,
        memoryDetectionTimeLimit: Long = 1000L,
        validateClassIdInLockOnProcess: Boolean = true,
        noDetectionCountLimit: Int = 8,
        differenceHashDistanceLimit: Int = 25,
        allowTemporalDrift: Boolean = true,
        hashBasedSearchFrameLimit: Int = 3,
        elapsedRealtime: () -> Long = SystemClock::elapsedRealtime,
        measureSharpness: (Bitmap, ImageBox) -> Float? = ::cardSharpness
    ) : this(
        yoloDetector = yoloDetector,
        candidateSelector = CardCandidateSelector(
            cardClasses = cardClasses,
            cardValidators = cardValidators,
            validateClassIdInLockOnProcess = validateClassIdInLockOnProcess
        ),
        stateMachine = CardLockStateMachine(
            lockOnThreshold = lockOnThreshold,
            memoryDetectionTimeLimit = memoryDetectionTimeLimit,
            noDetectionCountLimit = noDetectionCountLimit,
            allowTemporalDrift = allowTemporalDrift,
            hashBasedSearchFrameLimit = hashBasedSearchFrameLimit,
            consistencyChecker = TemporalConsistencyChecker(
                validateClassIdInLockOnProcess = validateClassIdInLockOnProcess,
                differenceHashDistanceLimit = differenceHashDistanceLimit
            )
        ),
        differenceHashDistanceLimit = differenceHashDistanceLimit,
        hashBasedSearchFrameLimit = hashBasedSearchFrameLimit,
        elapsedRealtime = elapsedRealtime,
        measureSharpness = measureSharpness
    )

    override var enabled: Boolean
        get() = yoloDetector.enabled
        set(value) {
            yoloDetector.enabled = value
        }

    override fun track(imageProxy: ImageProxy): CardDetection? {
        val observedAt = elapsedRealtime()
        val bitmap = imageProxy.toUprightBitmap()

        return try {
            track(bitmap, observedAt)
        } finally {
            bitmap.recycle()
        }
    }

    override fun track(bitmap: Bitmap): CardDetection? = track(bitmap, elapsedRealtime())

    private fun track(bitmap: Bitmap, currentTime: Long): CardDetection? {
        val hashBasedCandidate = if (stateMachine.hashDetectionCount < hashBasedSearchFrameLimit) {
            stateMachine.buildCandidateFromLastKnownCoordinates(
                bitmap,
                differenceHashDistanceLimit
            )
        } else {
            stateMachine.resetHashTrackingState()
            null
        }

        if (hashBasedCandidate == null) {
            val result = yoloDetector.detect(bitmap)

            val candidate = candidateSelector.selectCandidate(
                detections = result,
                previousDetection = stateMachine.previousCardDetection,
                bitmap = bitmap
            )

            if (candidate == null) {
                stateMachine.handleMissingDetection()
                return null
            }

            return stateMachine.processDetection(
                card = candidate,
                bitmap = bitmap,
                currentTimeMs = currentTime
            ).let { it.copy(focusScore = measureSharpness(bitmap, it.card.box), observedAtElapsedRealtimeMs = currentTime) }
        } else {
            return stateMachine.giveContinuation(
                card = hashBasedCandidate,
                bitmap = bitmap,
                currentTimeMs = currentTime,
            )?.let { it.copy(focusScore = measureSharpness(bitmap, it.card.box), observedAtElapsedRealtimeMs = currentTime) }
        }
    }

    override fun close() {
        yoloDetector.close()
    }
}
