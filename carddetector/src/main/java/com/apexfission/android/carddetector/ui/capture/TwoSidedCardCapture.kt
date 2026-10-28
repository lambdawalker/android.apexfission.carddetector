package com.apexfission.android.carddetector.ui.capture

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.apexfission.android.carddetector.domain.tflite.filters.AspectRatioValidator
import com.apexfission.android.carddetector.domain.tflite.filters.CardValidator
import com.apexfission.android.carddetector.domain.tflite.filters.MarginValidator
import com.apexfission.android.carddetector.domain.tflite.model.CardDetection
import com.apexfission.android.carddetector.ui.camerapreview.CameraPreset
import com.apexfission.android.carddetector.ui.detector.CardDetectorCamera
import com.apexfission.android.carddetector.ui.detector.CardDetectorOverlayScope
import com.apexfission.android.carddetector.ui.detector.CardDetectorPreset
import com.apexfission.android.carddetector.ui.overlays.IdCaptureIndicatorOverlay
import com.apexfission.android.carddetector.ui.overlays.IdCaptureOverlayConfig
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

    fun capture() {
        val image = latest ?: return
        if (receivedAt <= 0L || SystemClock.elapsedRealtime() - receivedAt !in 0..currentConfig.maxDetectionGapMs) return
        val token = session.requestCapture() ?: return
        latest = null // transfer our retained frame, not the detector's older best image
        session.accept(token, image)
        revision++
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
            if (autoCaptureMode.accepts(frame.autoGeneration, frame.autoReady)) capture()
        }
    }

    // Reading revision makes the pure state machine observable without exposing Compose in it.
    @Suppress("UNUSED_VARIABLE") val observedRevision = revision
    val fresh = latest != null && now - receivedAt in 0..config.maxDetectionGapMs
    Box(modifier.fillMaxSize().background(Color(0xFF111D27))) {
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
                        CaptureCameraOverlay(
                            side = session.side,
                            strings = strings,
                            autoCaptureEnabled = autoCaptureMode.enabled,
                            onToggleAutoCapture = { autoCaptureMode = autoCaptureMode.toggled() },
                            showInitialGuide = detectedAttempt != attempt,
                            canCapture = fresh,
                            focused = fresh && latest?.detection?.focusScore?.let { it >= config.focusThreshold } == true,
                            onCapture = { capture() },
                        )
                    }
                }
            }
            CaptureStage.TurnOver -> CaptureTurnOver(session.front!!, strings, onBack) {
                session.continueToBack()
                revision++
            }
            CaptureStage.Review -> CaptureReview(session.front!!, session.back!!, strings, onBack,
                onRetake = { session.retake(it); revision++ },
                onConfirm = {
                    session.complete()?.let { (front, back) ->
                        revision++
                        onComplete(TwoSidedCardCaptureResult(front, back))
                    }
                })
            CaptureStage.Complete -> Text(strings.done, color = Color.White, modifier = Modifier.align(Alignment.Center))
            CaptureStage.Closed -> Unit
        }
    }
}

@Composable
private fun CardDetectorOverlayScope.CaptureCameraOverlay(
    side: CardSide, strings: CardCaptureStrings, autoCaptureEnabled: Boolean,
    onToggleAutoCapture: () -> Unit, showInitialGuide: Boolean,
    canCapture: Boolean, focused: Boolean, onCapture: () -> Unit,
) {
    // The initial guide yields permanently to detection for this capture attempt.
    if (showInitialGuide && detectionState == null) Canvas(Modifier.fillMaxSize()) {
        val edge = minOf(size.width * 0.85f, size.height * 0.48f)
        drawRoundRect(Color.White.copy(alpha = 0.5f), Offset((size.width-edge)/2, (size.height-edge)/2),
            Size(edge, edge), CornerRadius(18.dp.toPx()), style = Stroke(2.dp.toPx()))
    }
    if (detectionState != null) IdCaptureIndicatorOverlay(IdCaptureOverlayConfig())
    Column(Modifier.fillMaxSize().safeDrawingPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        CaptureHeader(strings, { goBack() })
        Column(Modifier.fillMaxWidth().background(Color.Black.copy(alpha=0.65f)).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CaptureTitle(if (side == CardSide.Front) strings.front else strings.back)
            Text(if (side == CardSide.Front) strings.frontInstruction else strings.backInstruction,
                color=Color.White, textAlign=TextAlign.Center)
        }
        Spacer(Modifier.weight(1f))
        Column(Modifier.fillMaxWidth().background(Color.Black.copy(alpha=0.75f)).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (!canCapture) strings.find else if (focused) strings.ready else strings.focus, color=Color.White, textAlign=TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(if (side == CardSide.Front) strings.stepFront else strings.stepBack, color=Color.White, fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.Center) {
                IconToggleButton(checked = autoCaptureEnabled, onCheckedChange = { onToggleAutoCapture() },
                    modifier = Modifier.width(64.dp)) {
                    Icon(Icons.Default.AutoMode, contentDescription = strings.autoToggle,
                        tint = if (autoCaptureEnabled) Color(0xFF75E8CD) else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(28.dp))
                }
                Button(onClick=onCapture, enabled=canCapture, shape=CircleShape,
                    contentPadding=PaddingValues(8.dp), colors=ButtonDefaults.buttonColors(containerColor=Color.White),
                    modifier=Modifier.size(76.dp).border(3.dp, Color.White, CircleShape).semantics { contentDescription=strings.shutter }) {}
                IconToggleButton(checked = flashlightEnabled, onCheckedChange = { toggleFlashlight() },
                    enabled = flashlightAvailable, modifier = Modifier.width(64.dp)) {
                    Icon(if (flashlightEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = strings.flash,
                        tint = Color.White.copy(alpha = if (flashlightAvailable) 1f else 0.38f),
                        modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(if (autoCaptureEnabled) strings.auto else strings.manual, color=Color.White, fontSize=13.sp)
        }
    }
}

@Composable
private fun CaptureHeader(strings: CardCaptureStrings, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color.Black.copy(alpha=0.65f)), verticalAlignment=Alignment.CenterVertically) {
        TextButton(onClick=onBack) { Text(strings.exit, color=Color.White) }
        Text(strings.title, color=Color.White, fontWeight=FontWeight.Bold)
    }
}

@Composable
private fun CaptureTitle(title: String) {
    Text(title, color=Color.White, fontSize=24.sp, fontWeight=FontWeight.Bold, textAlign=TextAlign.Center)
}

@Composable
internal fun CaptureTurnOver(front: CapturedCardSide, strings: CardCaptureStrings, onBack: () -> Unit, onContinue: () -> Unit) {
    Column(Modifier.fillMaxSize().safeDrawingPadding(), horizontalAlignment=Alignment.CenterHorizontally) {
        CaptureHeader(strings, onBack)
        Spacer(Modifier.height(24.dp))
        CaptureTitle(strings.frontSaved)
        Text(strings.turnOver, color=Color.White, textAlign=TextAlign.Center, modifier=Modifier.padding(16.dp))
        Image(front.bitmap.asImageBitmap(), strings.frontSaved, Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentScale=ContentScale.Fit)
        Text(strings.stepFront, color=Color.White)
        Button(onClick=onContinue, modifier=Modifier.fillMaxWidth().padding(24.dp)) { Text(strings.continueToBack) }
    }
}

@Composable
internal fun CaptureReview(front: CapturedCardSide, back: CapturedCardSide, strings: CardCaptureStrings,
    onBack: () -> Unit, onRetake: (CardSide) -> Unit, onConfirm: () -> Unit,
) {
    Column(Modifier.fillMaxSize().safeDrawingPadding(), horizontalAlignment=Alignment.CenterHorizontally) {
        CaptureHeader(strings, onBack)
        CaptureTitle(strings.review)
        Text(strings.reviewInstruction, color=Color.White, textAlign=TextAlign.Center, modifier=Modifier.padding(16.dp))
        listOf(CardSide.Front to front, CardSide.Back to back).forEach { (side, image) ->
            Row(Modifier.weight(1f).fillMaxWidth().padding(16.dp).background(Color(0xFF243A46), RoundedCornerShape(16.dp)), verticalAlignment=Alignment.CenterVertically) {
                Image(image.bitmap.asImageBitmap(), if(side==CardSide.Front) strings.front else strings.back,
                    Modifier.weight(1f).fillMaxHeight().padding(12.dp), contentScale=ContentScale.Fit)
                TextButton(onClick={ onRetake(side) }) { Text(if(side==CardSide.Front) strings.retakeFront else strings.retakeBack, color=Color(0xFF75E8CD)) }
            }
        }
        Text(strings.complete, color=Color.White)
        Button(onClick=onConfirm, modifier=Modifier.fillMaxWidth().padding(24.dp)) { Text(strings.use) }
    }
}
