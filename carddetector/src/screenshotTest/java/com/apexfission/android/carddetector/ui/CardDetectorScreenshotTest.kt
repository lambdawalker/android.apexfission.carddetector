package com.apexfission.android.carddetector.ui

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.apexfission.android.carddetector.domain.tflite.model.CardDetection
import com.apexfission.android.carddetector.domain.tflite.model.LockingStatus
import com.apexfission.android.carddetector.ui.camerapreview.CameraPreset
import com.apexfission.android.carddetector.ui.camerapreview.createPreviewImageSpaceChain
import com.apexfission.android.carddetector.ui.capture.components.CaptureReview
import com.apexfission.android.carddetector.ui.capture.components.CaptureTurnOver
import com.apexfission.android.carddetector.ui.capture.CapturedCardSide
import com.apexfission.android.carddetector.ui.capture.CardCaptureStrings
import com.apexfission.android.carddetector.ui.capture.CaptureGuidance
import com.apexfission.android.carddetector.ui.capture.text
import com.apexfission.android.carddetector.ui.capture.SingleCaptureComplete
import com.apexfission.android.carddetector.ui.capture.CardSide
import com.apexfission.android.carddetector.ui.capture.defaultCardCaptureStrings
import com.apexfission.android.carddetector.ui.detector.CardDetectorOverlayScope
import com.apexfission.android.carddetector.ui.detector.CardDetectorPreset
import com.apexfission.android.carddetector.ui.overlays.CardLockOnOverlay
import com.apexfission.android.carddetector.ui.overlays.CaptureShutterButton
import com.apexfission.android.carddetector.ui.overlays.DetectionAnimationConfig
import com.apexfission.android.carddetector.ui.overlays.checkerboardBackground
import com.apexfission.android.carddetector.ui.overlays.DebugOverlay
import com.apexfission.android.carddetector.ui.overlays.DetectionOverlay
import com.apexfission.android.carddetector.ui.overlays.IdCaptureOverlay
import com.apexfission.android.carddetector.ui.overlays.IdCaptureOverlayConfig
import com.apexfission.android.math.models.ImageBox
import com.apexfission.android.math.models.ImageSpaceChain
import com.apexfission.android.yolo.engine.Feature

// Synthetic UI fixtures only: no camera, video decoder, model, permission or network.
private val sampleDetection = CardDetection(
    lockOnProgress = 1f,
    id = 12345L,
    card = Feature(ImageBox.from2P(40, 328, 353, 524), 0.95f, 0),
    features = emptyList(),
    lockingStatus = LockingStatus.CardLocked,
    focusScore = 150f,
)

private fun sampleCard(back: Boolean = false): Bitmap {
    val bitmap = Bitmap.createBitmap(800, 500, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    canvas.drawColor(android.graphics.Color.rgb(218, 235, 238))
    paint.color = android.graphics.Color.rgb(17, 52, 68)
    canvas.drawRect(0f, 0f, 800f, 85f, paint)
    paint.color = android.graphics.Color.WHITE
    paint.textSize = 32f
    canvas.drawText(if (back) "SAMPLE DOCUMENT / BACK" else "SAMPLE DOCUMENT / FRONT", 32f, 56f, paint)
    paint.color = android.graphics.Color.rgb(17, 52, 68)
    paint.textSize = 29f
    canvas.drawText("DEMO - NOT VALID", 32f, 150f, paint)
    paint.textSize = 24f
    canvas.drawText("Synthetic screenshot fixture", 32f, 205f, paint)
    if (back) {
        for (x in 40..740 step 12) canvas.drawRect(x.toFloat(), 270f, x + 5f, 380f, paint)
    } else {
        paint.color = android.graphics.Color.rgb(127, 169, 176)
        canvas.drawRoundRect(32f, 250f, 190f, 420f, 12f, 12f, paint)
        paint.color = android.graphics.Color.rgb(17, 52, 68)
        canvas.drawText("NAME: SAMPLE", 230f, 290f, paint)
        canvas.drawText("NUMBER: 000000", 230f, 345f, paint)
    }
    return bitmap
}

@Composable
private fun rememberScope(detected: Boolean, focused: Boolean = true, torchAvailable: Boolean = true): CardDetectorOverlayScope {
    val density = LocalDensity.current
    val chain = remember(density) {
        with(density) { createPreviewImageSpaceChain(393, 852, 393.dp.roundToPx(), 852.dp.roundToPx()) }
    }
    return remember(detected, focused, torchAvailable, chain) {
        object : CardDetectorOverlayScope {
            override val detectionState = sampleDetection.copy(focusScore = if (focused) 150f else 0f).takeIf { detected }
            override val detectionSequence = if (detected) 1L else 0L
            override val latestBestDetection = detectionState
            override val imageSpaceChain: ImageSpaceChain = chain
            override val captureEnabled = detected
            override val detectorPreset = CardDetectorPreset.HighPerformance
            override val cameraPreset = CameraPreset.HighResolution
            override val classLabels = mapOf(0 to "SAMPLE_CARD")
            override val flashlightEnabled = false
            override val flashlightAvailable = torchAvailable
            override fun goBack() = Unit
            override fun toggleFlashlight() = Unit
            override fun capture() = Unit
        }
    }
}

@Composable
private fun Screen(
    showCard: Boolean = true,
    camera: Boolean = true,
    backCard: Boolean = false,
    darkTheme: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    MaterialTheme(colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()) {
        // Match the editor previews: checkerboard stands in for the missing camera feed.
        // Non-camera screens use the same theme background as TwoSidedCardCapture.
        val background = if (camera) Modifier.checkerboardBackground()
            else Modifier.background(MaterialTheme.colorScheme.background)
        Box(Modifier.fillMaxSize().then(background)) {
            if (showCard) {
                val bitmap = remember(backCard) { sampleCard(back = backCard) }
                Image(bitmap.asImageBitmap(), "Synthetic sample card",
                    Modifier.align(Alignment.Center).padding(horizontal = 40.dp).fillMaxWidth())
            }
            content()
        }
    }
}

@PreviewTest
@Preview(name = "single-side-idle-en", widthDp = 393, heightDp = 852, locale = "en", fontScale = 1f)
@Preview(name = "single-side-idle-es", widthDp = 393, heightDp = 852, locale = "es", fontScale = 1f)
@Composable
fun SingleSideIdleScreenshot() = Screen(showCard = false) {
    rememberScope(false).IdCaptureOverlay(IdCaptureOverlayConfig(title = "Card Detector demo"))
}

@PreviewTest
@Preview(name = "single-side-detected-en", widthDp = 393, heightDp = 852, locale = "en", fontScale = 1f)
@Preview(name = "single-side-detected-es", widthDp = 393, heightDp = 852, locale = "es", fontScale = 1f)
@Composable
fun SingleSideDetectedScreenshot() = Screen {
    rememberScope(true).IdCaptureOverlay(IdCaptureOverlayConfig(title = "Card Detector demo"))
}

@Composable
private fun CameraScreen(
    side: CardSide,
    detected: Boolean = false,
    focused: Boolean = false,
    autoCapture: Boolean = true,
    showInitialGuide: Boolean = !detected,
    strings: CardCaptureStrings = defaultCardCaptureStrings(),
) = Screen(showCard = detected, backCard = side == CardSide.Back) {
    val guidance = if (!detected) CaptureGuidance.Looking else if (!focused) CaptureGuidance.Focusing else CaptureGuidance.Ready
    rememberScope(detected, focused).IdCaptureOverlay(
        config = IdCaptureOverlayConfig(
            title = strings.title,
            instructionTitle = if (side == CardSide.Front) strings.front else strings.back,
            instructionSubTitle = guidance.text(strings)
        ),
        strings = strings,
        autoCaptureEnabled = autoCapture,
        onToggleAutoCapture = {},
        showInitialGuide = showInitialGuide,
        canCapture = detected,
        onCapture = {},
        showAutoCaptureToggle = true,
        step = if (side == CardSide.Front) strings.stepFront else strings.stepBack,
    )
}

@PreviewTest
@Preview(name = "capture-front-en", widthDp = 393, heightDp = 852, locale = "en", fontScale = 1f)
@Preview(name = "capture-front-es", widthDp = 393, heightDp = 852, locale = "es", fontScale = 1f)
@Composable
fun CaptureFrontScreenshot() = CameraScreen(CardSide.Front)

@PreviewTest
@Preview(name = "capture-back-en", widthDp = 393, heightDp = 852, locale = "en", fontScale = 1f)
@Preview(name = "capture-back-es", widthDp = 393, heightDp = 852, locale = "es", fontScale = 1f)
@Composable
fun CaptureBackScreenshot() = CameraScreen(CardSide.Back)

@PreviewTest
@Preview(name = "capture-turn-over-en", widthDp = 393, heightDp = 852, locale = "en", fontScale = 1f)
@Preview(name = "capture-turn-over-es", widthDp = 393, heightDp = 852, locale = "es", fontScale = 1f)
@Composable
fun CaptureTurnOverScreenshot() = TurnOverScreen()

@Composable
private fun TurnOverScreen(darkTheme: Boolean = false) = Screen(showCard = false, camera = false, darkTheme = darkTheme) {
    val front = remember { CapturedCardSide(sampleDetection, sampleCard()) }
    CaptureTurnOver(front, defaultCardCaptureStrings(), {}, {}, {})
}

@PreviewTest
@Preview(name = "capture-review-en", widthDp = 393, heightDp = 852, locale = "en", fontScale = 1f)
@Preview(name = "capture-review-es", widthDp = 393, heightDp = 852, locale = "es", fontScale = 1f)
@Composable
fun CaptureReviewScreenshot() = ReviewScreen()

@Composable
private fun ReviewScreen(darkTheme: Boolean = false) = Screen(showCard = false, camera = false, darkTheme = darkTheme) {
    val front = remember { CapturedCardSide(sampleDetection, sampleCard()) }
    val back = remember { CapturedCardSide(sampleDetection, sampleCard(back = true)) }
    CaptureReview(front, back, defaultCardCaptureStrings(), {}, {}, {})
}

@PreviewTest
@Preview(name = "debug-overlay", widthDp = 393, heightDp = 852, fontScale = 1f)
@Composable
fun DebugOverlayScreenshot() = Screen { rememberScope(true).DebugOverlay() }

@PreviewTest
@Preview(name = "detection-overlay", widthDp = 393, heightDp = 852, fontScale = 1f)
@Composable
fun DetectionOverlayScreenshot() = Screen { rememberScope(true).DetectionOverlay(showClassNames = true) }

@PreviewTest
@Preview(name = "lock-on-overlay", widthDp = 393, heightDp = 852, fontScale = 1f)
@Composable
fun LockOnOverlayScreenshot() = Screen {
    rememberScope(true).CardLockOnOverlay(DetectionAnimationConfig(
        idleOpacity = 0f, detectedOpacity = 1f, resetPositionOnMissing = false,
        enableContinuousAnimations = false,
    ))
}


@PreviewTest
@Preview(name = "capture-front-ready-en", widthDp = 393, heightDp = 852, locale = "en", fontScale = 1f)
@Preview(name = "capture-front-ready-es", widthDp = 393, heightDp = 852, locale = "es", fontScale = 1f)
@Composable
fun CaptureFrontReadyScreenshot() = CameraScreen(CardSide.Front, detected = true, focused = true)

@PreviewTest
@Preview(name = "capture-back-ready-en", widthDp = 393, heightDp = 852, locale = "en", fontScale = 1f)
@Preview(name = "capture-back-ready-es", widthDp = 393, heightDp = 852, locale = "es", fontScale = 1f)
@Composable
fun CaptureBackReadyScreenshot() = CameraScreen(CardSide.Back, detected = true, focused = true)

@PreviewTest
@Preview(name = "capture-front-focusing-en", widthDp = 393, heightDp = 852, locale = "en", fontScale = 1f)
@Composable
fun CaptureFrontFocusingScreenshot() = CameraScreen(CardSide.Front, detected = true, focused = false)

@PreviewTest
@Preview(name = "capture-front-manual-en", widthDp = 393, heightDp = 852, locale = "en", fontScale = 1f)
@Composable
fun CaptureFrontManualScreenshot() = CameraScreen(CardSide.Front, detected = true, focused = true, autoCapture = false)

@PreviewTest
@Preview(name = "capture-turn-over-dark-en", widthDp = 393, heightDp = 852, locale = "en", fontScale = 1f, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "capture-turn-over-dark-es", widthDp = 393, heightDp = 852, locale = "es", fontScale = 1f, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CaptureTurnOverDarkScreenshot() = TurnOverScreen(darkTheme = true)

@PreviewTest
@Preview(name = "capture-review-dark-en", widthDp = 393, heightDp = 852, locale = "en", fontScale = 1f, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "capture-review-dark-es", widthDp = 393, heightDp = 852, locale = "es", fontScale = 1f, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CaptureReviewDarkScreenshot() = ReviewScreen(darkTheme = true)

@PreviewTest
@Preview(name = "capture-review-large-font-en", widthDp = 393, heightDp = 852, locale = "en", fontScale = 1.3f)
@Composable
fun CaptureReviewLargeFontScreenshot() = ReviewScreen()

@PreviewTest
@Preview(name = "shutter-enabled", widthDp = 160, heightDp = 160, fontScale = 1f)
@Composable
fun ShutterEnabledScreenshot() = Screen(showCard = false) {
    CaptureShutterButton(true, {}, Modifier.align(Alignment.Center))
}

@PreviewTest
@Preview(name = "shutter-disabled", widthDp = 160, heightDp = 160, fontScale = 1f)
@Composable
fun ShutterDisabledScreenshot() = Screen(showCard = false) {
    CaptureShutterButton(false, {}, Modifier.align(Alignment.Center))
}


@PreviewTest
@Preview(name = "single-side-auto-en", widthDp = 393, heightDp = 852, locale = "en", fontScale = 1f)
@Preview(name = "single-side-auto-es", widthDp = 393, heightDp = 852, locale = "es", fontScale = 1f)
@Composable
fun SingleSideAutoScreenshot() = AutoSingleScreen()

// Frozen post-detection miss states; live fade timing and latch/reset behavior are
// tested separately by CaptureOverlayTest on the emulator.
@PreviewTest
@Preview(name = "single-side-after-miss-en", widthDp = 393, heightDp = 852, locale = "en", fontScale = 1f)
@Composable
fun SingleSideAfterMissScreenshot() = Screen(showCard = false) {
    rememberScope(false).IdCaptureOverlay(showInitialGuide = false)
}

@PreviewTest
@Preview(name = "capture-front-after-miss-en", widthDp = 393, heightDp = 852, locale = "en", fontScale = 1f)
@Composable
fun CaptureFrontAfterMissScreenshot() = CameraScreen(CardSide.Front, showInitialGuide = false)

@PreviewTest
@Preview(name = "capture-back-after-miss-en", widthDp = 393, heightDp = 852, locale = "en", fontScale = 1f)
@Composable
fun CaptureBackAfterMissScreenshot() = CameraScreen(CardSide.Back, showInitialGuide = false)

@PreviewTest
@Preview(name = "single-side-no-flash-en", widthDp = 393, heightDp = 852, locale = "en", fontScale = 1f)
@Composable
fun SingleSideNoFlashScreenshot() = Screen {
    rememberScope(true, torchAvailable = false).IdCaptureOverlay()
}


@Composable
private fun AutoSingleScreen(
    guidance: CaptureGuidance = CaptureGuidance.HoldSteady,
    strings: CardCaptureStrings = defaultCardCaptureStrings(),
) = Screen(showCard = guidance != CaptureGuidance.Looking) {
    rememberScope(guidance != CaptureGuidance.Looking).IdCaptureOverlay(
        config = IdCaptureOverlayConfig(
            title = strings.title,
            instructionTitle = strings.singleInstruction,
            instructionSubTitle = guidance.text(strings)
        ),
        strings = strings,
        showInitialGuide = guidance == CaptureGuidance.Looking,
        autoCaptureEnabled = true,
        showAutoCaptureToggle = true,
        canCapture = guidance != CaptureGuidance.Looking,
        onCapture = {}
    )
}

@Composable
private fun longSpanishStrings() = defaultCardCaptureStrings().copy(
    title = "Captura de una identificación para completar el registro de la familia",
    singleInstruction = "Coloca la identificación completa dentro de la guía y asegúrate de que todos sus bordes sean visibles",
    find = "Buscando una tarjeta: evita los reflejos y mantén la identificación estable mientras comprobamos el enfoque antes de capturar la imagen automáticamente",
    frontInstruction = "Coloca el frente de la identificación dentro del recuadro y mantén visibles todos los bordes, sin reflejos y con buena iluminación",
    auto = "La captura automática está activada; también puedes tocar el botón para capturar manualmente",
)

@PreviewTest
@Preview(name = "single-auto-narrow-es", widthDp = 320, heightDp = 640, locale = "es", fontScale = 1f)
@Preview(name = "single-auto-landscape-en", widthDp = 852, heightDp = 393, locale = "en", fontScale = 1f)
@Preview(name = "single-auto-large-font-es", widthDp = 393, heightDp = 852, locale = "es", fontScale = 1.6f)
@Composable
fun SingleAutoLayoutScreenshot() = AutoSingleScreen(CaptureGuidance.Looking)

@PreviewTest
@Preview(name = "single-auto-long-spanish", widthDp = 320, heightDp = 640, locale = "es", fontScale = 1f)
@Composable
fun SingleAutoLongSpanishScreenshot() = AutoSingleScreen(CaptureGuidance.Looking, longSpanishStrings())

@PreviewTest
@Preview(name = "two-side-narrow-es", widthDp = 320, heightDp = 640, locale = "es", fontScale = 1f)
@Preview(name = "two-side-landscape-en", widthDp = 852, heightDp = 393, locale = "en", fontScale = 1f)
@Preview(name = "two-side-large-font-es", widthDp = 393, heightDp = 852, locale = "es", fontScale = 1.6f)
@Composable
fun TwoSideLayoutScreenshot() = CameraScreen(CardSide.Front)

@PreviewTest
@Preview(name = "two-side-long-spanish", widthDp = 320, heightDp = 640, locale = "es", fontScale = 1f)
@Composable
fun TwoSideLongSpanishScreenshot() = CameraScreen(CardSide.Front, strings = longSpanishStrings())

@PreviewTest
@Preview(name = "review-landscape-es", widthDp = 852, heightDp = 393, locale = "es", fontScale = 1f)
@Composable
fun ReviewLandscapeScreenshot() = ReviewScreen()

@PreviewTest
@Preview(name = "single-auto-ready-en", widthDp = 393, heightDp = 852, locale = "en", fontScale = 1f)
@Composable
fun SingleAutoReadyScreenshot() = AutoSingleScreen(CaptureGuidance.Ready)

@PreviewTest
@Preview(name = "single-auto-complete-en", widthDp = 393, heightDp = 852, locale = "en", fontScale = 1f)
@Composable
fun SingleAutoCompleteScreenshot() = Screen(showCard = false, camera = false) {
    SingleCaptureComplete(defaultCardCaptureStrings())
}
