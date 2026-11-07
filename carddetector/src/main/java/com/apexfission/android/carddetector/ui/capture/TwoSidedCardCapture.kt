package com.apexfission.android.carddetector.ui.capture

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.apexfission.android.carddetector.domain.tflite.filters.AspectRatioValidator
import com.apexfission.android.carddetector.domain.tflite.filters.CardValidator
import com.apexfission.android.carddetector.domain.tflite.filters.MarginValidator
import com.apexfission.android.carddetector.domain.tflite.model.CardDetection
import com.apexfission.android.carddetector.ui.camerapreview.CameraPreset
import com.apexfission.android.carddetector.ui.detector.CardDetectorCamera
import com.apexfission.android.carddetector.ui.detector.CardDetectorPreset
import com.apexfission.android.carddetector.ui.overlays.IdCaptureOverlay
import com.apexfission.android.carddetector.ui.overlays.IdCaptureOverlayConfig
import com.apexfission.android.carddetector.ui.capture.components.CaptureReview
import com.apexfission.android.carddetector.ui.capture.components.CaptureTurnOver
import kotlinx.coroutines.channels.Channel

/** An owned, rectangular card crop and its corresponding detection. */
data class CapturedCardSide(val detection: CardDetection, val bitmap: Bitmap)

/** Host owns both bitmaps after onComplete; recycle each after its final use. */
data class TwoSidedCardCaptureResult(val front: CapturedCardSide, val back: CapturedCardSide)

private data class CaptureFrame(val attempt: Long, val card: CapturedCardSide?, val autoReady: Boolean, val autoGeneration: Long, val hasDetected: Boolean)
private class AttemptOwner : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
}

/**
 * Screen.
 * Front → explicit turn-over → back → review, with independent retakes.
 * Requires camera permission from the host, just like CardDetectorLite.
 * [onComplete] runs once on the main thread and transfers ownership of both images.
 * Undelivered/replaced images are recycled by the flow. Removing this composable or changing
 * [instanceKey] cancels the session. Activity recreation starts a new session.
 * Side labels express user intent; this component does not classify front versus back.
 */
@Composable
fun TwoSidedCardCapture(
    instanceKey: String,
    modelPath: String,
    classLabels: Map<Int, String>,
    cardClasses: Set<Int>,
    onComplete: (TwoSidedCardCaptureResult) -> Unit,
    modifier: Modifier = Modifier,
    config: CardCaptureConfig = CardCaptureConfig(),
    strings: CardCaptureStrings = defaultCardCaptureStrings(),
    detectorPreset: CardDetectorPreset = CardDetectorPreset.HighPerformance,
    cameraPreset: CameraPreset = CameraPreset.HighResolution,
    cardFilters: List<CardValidator> = listOf(MarginValidator(), AspectRatioValidator()),
    onBack: () -> Unit = {},
) = TwoSidedCardCapture(
    instanceKey = instanceKey,
    modelPath = modelPath,
    classLabels = classLabels,
    cardClasses = cardClasses,
    onComplete = onComplete,
    onSideChanged = { _, _ -> },
    modifier = modifier,
    config = config,
    strings = strings,
    detectorPreset = detectorPreset,
    cameraPreset = cameraPreset,
    cardFilters = cardFilters,
    onBack = onBack,
)

/**
 * [onSideReady] delivers each captured crop immediately, including replacements.
 * [onSideInvalidated] identifies the side discarded by a retake.
 * [onSideAccepted] delivers each side of the final confirmed pair, front then back.
 *
 * All side callbacks borrow the bitmap on the main thread. Copy it before
 * returning if background work needs it. Only [onComplete] transfers ownership.
 */
@Composable
fun TwoSidedCardCapture(
    instanceKey: String,
    modelPath: String,
    classLabels: Map<Int, String>,
    cardClasses: Set<Int>,
    onComplete: (TwoSidedCardCaptureResult) -> Unit,
    onSideReady: (CardSide, CapturedCardSide) -> Unit,
    onSideAccepted: (CardSide, CapturedCardSide) -> Unit,
    onSideInvalidated: (CardSide) -> Unit = {},
    onImagesReady: ((TwoSidedCardCaptureResult) -> Unit)? = null,
    modifier: Modifier = Modifier,
    config: CardCaptureConfig = CardCaptureConfig(),
    strings: CardCaptureStrings = defaultCardCaptureStrings(),
    detectorPreset: CardDetectorPreset = CardDetectorPreset.HighPerformance,
    cameraPreset: CameraPreset = CameraPreset.HighResolution,
    cardFilters: List<CardValidator> = listOf(MarginValidator(), AspectRatioValidator()),
    onBack: () -> Unit = {},
) = TwoSidedCardCapture(
    instanceKey = instanceKey,
    modelPath = modelPath,
    classLabels = classLabels,
    cardClasses = cardClasses,
    onComplete = { result ->
        try {
            onSideAccepted(CardSide.Front, result.front)
            onSideAccepted(CardSide.Back, result.back)
        } catch (error: Throwable) {
            result.front.bitmap.recycle()
            result.back.bitmap.recycle()
            throw error
        }
        onComplete(result)
    },
    onSideChanged = { side, image ->
        if (image == null) onSideInvalidated(side) else onSideReady(side, image)
    },
    onImagesReady = onImagesReady,
    modifier = modifier,
    config = config,
    strings = strings,
    detectorPreset = detectorPreset,
    cameraPreset = cameraPreset,
    cardFilters = cardFilters,
    onBack = onBack,
)

/**
 * [onSideChanged] reports accepted crops immediately and sends null when a side is retaken.
 * [onImagesReady] reports each complete pair before confirmation, including after a retake.
 * These bitmaps are borrowed for the synchronous callback on the main thread. Copy its pixels
 * before returning if needed later. The session owns it until [onComplete] transfers the pair.
 */
@Composable
fun TwoSidedCardCapture(
    instanceKey: String,
    modelPath: String,
    classLabels: Map<Int, String>,
    cardClasses: Set<Int>,
    onComplete: (TwoSidedCardCaptureResult) -> Unit,
    onSideChanged: (CardSide, CapturedCardSide?) -> Unit,
    onImagesReady: ((TwoSidedCardCaptureResult) -> Unit)? = null,
    modifier: Modifier = Modifier,
    config: CardCaptureConfig = CardCaptureConfig(),
    strings: CardCaptureStrings = defaultCardCaptureStrings(),
    detectorPreset: CardDetectorPreset = CardDetectorPreset.HighPerformance,
    cameraPreset: CameraPreset = CameraPreset.HighResolution,
    cardFilters: List<CardValidator> = listOf(MarginValidator(), AspectRatioValidator()),
    onBack: () -> Unit = {},
) = key(instanceKey, modelPath, classLabels.toMap(), cardClasses.toSet(),
    detectorPreset, cameraPreset, cardFilters.map { it.configurationKey }, config) {
    val session = remember { CaptureSession<CapturedCardSide> { it.bitmap.recycle() } }
    val frames = remember {
        Channel<CaptureFrame>(Channel.CONFLATED, onUndeliveredElement = { it.card?.bitmap?.recycle() })
    }
    var revision by remember { mutableIntStateOf(0) }
    var latest by remember { mutableStateOf<CapturedCardSide?>(null) }
    var receivedAt by remember { mutableLongStateOf(0L) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val currentConfig by rememberUpdatedState(config)
    var autoCaptureMode by remember { mutableStateOf(AutoCaptureMode(config.autoCaptureEnabled)) }
    var detectedAttempt by remember { mutableLongStateOf(-1L) }
    var lastAutoReady by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    val currentOnSideChanged by rememberUpdatedState(onSideChanged)
    val currentOnImagesReady by rememberUpdatedState(onImagesReady)

    fun capture() {
        val image = latest ?: return
        if (receivedAt <= 0L || SystemClock.elapsedRealtime() - receivedAt !in 0..currentConfig.maxDetectionGapMs) return
        val token = session.requestCapture() ?: return
        latest = null // transfer our retained frame, not the detector's older best image
        val side = session.side
        val accepted = session.accept(token, image)
        if (accepted && config.hapticFeedbackEnabled) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        revision++
        if (accepted) {
            currentOnSideChanged(side, image)
            val front = session.front
            val back = session.back
            if (front != null && back != null) {
                currentOnImagesReady?.invoke(TwoSidedCardCaptureResult(front, back))
            }
        }
    }

    fun retake(side: CardSide) {
        if (!session.retake(side)) return
        revision++
        currentOnSideChanged(side, null)
    }

    DisposableEffect(session) {
        onDispose {
            frames.cancel()
            latest?.bitmap?.recycle()
            latest = null
            session.close()
        }
    }
    LaunchedEffect(session) {
        while (true) {
            kotlinx.coroutines.delay(100)
            now = SystemClock.elapsedRealtime()
        }
    }
    LaunchedEffect(frames) {
        for (frame in frames) {
            if (frame.attempt != session.attempt || session.stage != CaptureStage.Camera) {
                frame.card?.bitmap?.recycle()
                continue
            }
            latest?.bitmap?.recycle()
            latest = frame.card
            receivedAt = frame.card?.detection?.observedAtElapsedRealtimeMs ?: 0L
            now = SystemClock.elapsedRealtime()
            if (frame.hasDetected) detectedAttempt = frame.attempt
            lastAutoReady = autoCaptureMode.accepts(frame.autoGeneration, frame.autoReady)
            if (lastAutoReady) capture()
        }
    }

    // Reading revision makes the pure state machine observable without exposing Compose in it.
    @Suppress("UNUSED_VARIABLE") val observedRevision = revision
    val fresh = latest != null && now - receivedAt in 0..config.maxDetectionGapMs
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (session.stage) {
            CaptureStage.Camera -> key(session.attempt) {
                val attempt = session.attempt
                val owner = remember { AttemptOwner() }
                // Only the detector worker touches this gate. Every evaluated frame arrives here,
                // before the bounded UI channel can drop intermediate results.
                val readiness = remember { CaptureReadiness(config) }
                DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
                CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                    CardDetectorCamera(
                        instanceKey = "$instanceKey:$attempt",
                        modelPath = modelPath,
                        classLabels = classLabels,
                        cardClasses = cardClasses,
                        detectorPreset = detectorPreset,
                        cameraPreset = cameraPreset,
                        cardFilters = cardFilters,
                        onFrameEvaluation = { d, bitmap ->
                            val mode = autoCaptureMode
                            val ready = readiness.evaluate(d, SystemClock.elapsedRealtime(), mode)
                            val card = if (d != null && bitmap != null) CapturedCardSide(d, bitmap) else null
                            val frame = CaptureFrame(attempt, card, ready, mode.generation, readiness.hasDetected)
                            if (frames.trySend(frame).isFailure) bitmap?.recycle()
                        },
                        onBack = onBack,
                    ) {
                        val guidance = captureGuidance(latest?.detection, now, config, autoCaptureMode.enabled, lastAutoReady)
                        IdCaptureOverlay(
                            config = IdCaptureOverlayConfig(
                                title = strings.title,
                                instructionTitle = if (session.side == CardSide.Front) strings.front else strings.back,
                                instructionSubTitle = when {
                                    guidance == CaptureGuidance.Looking -> strings.find + " · " + if (session.side == CardSide.Front) strings.frontInstruction else strings.backInstruction
                                    else -> guidance.text(strings)
                                }
                            ),
                            strings = strings,
                            autoCaptureEnabled = autoCaptureMode.enabled,
                            onToggleAutoCapture = { autoCaptureMode = autoCaptureMode.toggled(); lastAutoReady = false },
                            showInitialGuide = detectedAttempt != attempt,
                            canCapture = fresh,
                            onCapture = { capture() },
                            showAutoCaptureToggle = true,
                            step = if (session.side == CardSide.Front) strings.stepFront else strings.stepBack,
                        )
                    }
                }
            }
            CaptureStage.TurnOver -> CaptureTurnOver(session.front!!, strings, onBack, onRetake = {
                retake(CardSide.Front)
            }) {
                session.continueToBack()
                revision++
            }
            CaptureStage.Review -> CaptureReview(session.front!!, session.back!!, strings, onBack,
                onRetake = ::retake,
                onConfirm = {
                    session.complete()?.let { (front, back) ->
                        revision++
                        onComplete(TwoSidedCardCaptureResult(front, back))
                    }
                })
            CaptureStage.Complete -> Text(strings.done, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.align(Alignment.Center))
            CaptureStage.Closed -> Unit
        }
    }
}
