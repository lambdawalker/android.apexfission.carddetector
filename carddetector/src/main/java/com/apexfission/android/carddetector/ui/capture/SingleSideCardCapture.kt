package com.apexfission.android.carddetector.ui.capture

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.apexfission.android.carddetector.domain.tflite.filters.*
import com.apexfission.android.carddetector.domain.tflite.model.CardDetection
import com.apexfission.android.carddetector.ui.camerapreview.CameraPreset
import com.apexfission.android.carddetector.ui.detector.CardDetectorCamera
import com.apexfission.android.carddetector.ui.detector.CardDetectorOverlayScope
import com.apexfission.android.carddetector.ui.detector.CardDetectorPreset
import com.apexfission.android.carddetector.ui.overlays.IdCaptureOverlayConfig
import com.apexfission.android.carddetector.ui.overlays.components.CaptureOverlay
import kotlinx.coroutines.channels.Channel

private data class SingleCaptureFrame(val card: CapturedCardSide?, val autoReady: Boolean, val generation: Long, val hasDetected: Boolean)
private class SingleCaptureOwner : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
}

/**
 * One-shot single-side capture with an optional automatic mode and a manual shutter.
 * Requires the host's camera permission. Reuses the two-sided focus/stability/freshness gate.
 * [onCapture] runs once per attempt on the main thread, transferring one current crop to
 * the host; recycle it after its final use. Replaced, cancelled and undelivered images are
 * recycled internally. Change [instanceKey] to capture again. Detector/configuration changes
 * also restart the attempt. Unlike CardDetectorLite, this does not capture an older best image.
 */
@Composable
fun SingleSideCardCapture(
    instanceKey: String,
    modelPath: String,
    classLabels: Map<Int, String>,
    cardClasses: Set<Int>,
    onCapture: (CardDetection, Bitmap) -> Unit,
    modifier: Modifier = Modifier,
    config: CardCaptureConfig = CardCaptureConfig(),
    strings: CardCaptureStrings = defaultCardCaptureStrings(),
    detectorPreset: CardDetectorPreset = CardDetectorPreset.HighPerformance,
    cameraPreset: CameraPreset = CameraPreset.HighResolution,
    cardFilters: List<CardValidator> = listOf(MarginValidator(), AspectRatioValidator()),
    onBack: () -> Unit = {},
) = key(instanceKey, modelPath, classLabels.toMap(), cardClasses.toSet(),
    detectorPreset, cameraPreset, cardFilters.map { it.configurationKey }, config) {
    val session = remember { SingleCaptureSession<CapturedCardSide> { it.bitmap.recycle() } }
    val frames = remember {
        Channel<SingleCaptureFrame>(Channel.CONFLATED, onUndeliveredElement = { it.card?.bitmap?.recycle() })
    }
    var mode by remember { mutableStateOf(AutoCaptureMode(config.autoCaptureEnabled)) }
    var current by remember { mutableStateOf<CapturedCardSide?>(null) }
    var autoReady by remember { mutableStateOf(false) }
    var hasDetected by remember { mutableStateOf(false) }
    var completed by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val callback by rememberUpdatedState(onCapture)
    val haptics = LocalHapticFeedback.current

    fun capture() {
        val card = session.capture(SystemClock.elapsedRealtime(), config.maxDetectionGapMs) ?: return
        current = null
        completed = true
        if (config.hapticFeedbackEnabled) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        callback(card.detection, card.bitmap)
    }

    DisposableEffect(session) {
        onDispose { frames.cancel(); session.close() }
    }
    LaunchedEffect(session) {
        while (!session.completed) { kotlinx.coroutines.delay(100); now = SystemClock.elapsedRealtime() }
    }
    LaunchedEffect(frames) {
        for (frame in frames) {
            session.offer(frame.card, frame.card?.detection?.observedAtElapsedRealtimeMs ?: 0)
            if (session.completed) continue
            current = frame.card
            autoReady = mode.accepts(frame.generation, frame.autoReady)
            hasDetected = hasDetected || frame.hasDetected
            now = SystemClock.elapsedRealtime()
            if (autoReady) capture()
        }
    }
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (completed) SingleCaptureComplete(strings) else {
            val owner = remember { SingleCaptureOwner() }
            val readiness = remember { CaptureReadiness(config) }
            DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
            CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                CardDetectorCamera(
                    instanceKey = instanceKey, modelPath = modelPath, classLabels = classLabels,
                    cardClasses = cardClasses, detectorPreset = detectorPreset, cameraPreset = cameraPreset,
                    cardFilters = cardFilters, onBack = onBack,
                    onFrameEvaluation = { detection, bitmap ->
                        val evaluatedMode = mode
                        val ready = readiness.evaluate(detection, SystemClock.elapsedRealtime(), evaluatedMode)
                        val card = if (detection != null && bitmap != null) CapturedCardSide(detection, bitmap) else null
                        if (card == null) bitmap?.recycle()
                        val frame = SingleCaptureFrame(card, ready, evaluatedMode.generation, readiness.hasDetected)
                        if (frames.trySend(frame).isFailure) card?.bitmap?.recycle()
                    },
                ) {
                    SingleSideCaptureOverlay(
                        strings = strings,
                        guidance = captureGuidance(current?.detection, now, config, mode.enabled, autoReady),
                        canCapture = session.isFresh(now, config.maxDetectionGapMs),
                        autoCaptureEnabled = mode.enabled,
                        showInitialGuide = !hasDetected,
                        onToggleAutoCapture = { mode = mode.toggled(); autoReady = false },
                        onCapture = { capture() },
                    )
                }
            }
        }
    }
}

/** Render-only adapter used by the live flow, editor previews and screenshot tests. */
@Composable
internal fun CardDetectorOverlayScope.SingleSideCaptureOverlay(
    strings: CardCaptureStrings,
    guidance: CaptureGuidance,
    canCapture: Boolean,
    autoCaptureEnabled: Boolean,
    showInitialGuide: Boolean,
    onToggleAutoCapture: () -> Unit,
    onCapture: () -> Unit,
) = CaptureOverlay(
    config = IdCaptureOverlayConfig(title = strings.title, instructionTitle = strings.singleInstruction,
        instructionSubTitle = guidance.text(strings)),
    showInitialGuide = showInitialGuide, canCapture = canCapture, onCapture = onCapture,
    autoCaptureEnabled = autoCaptureEnabled, onToggleAutoCapture = onToggleAutoCapture,
    showAutoCaptureToggle = true, exitContentDescription = strings.exit,
    autoToggleContentDescription = strings.autoToggle, flashContentDescription = strings.flash,
    shutterContentDescription = strings.shutter,
    modeText = if (autoCaptureEnabled) strings.auto else strings.manual,
)


@Composable
internal fun SingleCaptureComplete(strings: CardCaptureStrings) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(strings.captured, color = MaterialTheme.colorScheme.onSurface)
    }
}
