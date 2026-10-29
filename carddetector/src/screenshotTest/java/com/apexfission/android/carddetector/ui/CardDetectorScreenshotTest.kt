package com.apexfission.android.carddetector.ui

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.apexfission.android.carddetector.domain.tflite.model.CardDetection
import com.apexfission.android.carddetector.domain.tflite.model.LockingStatus
import com.apexfission.android.carddetector.ui.camerapreview.CameraPreset
import com.apexfission.android.carddetector.ui.camerapreview.createPreviewImageSpaceChain
import com.apexfission.android.carddetector.ui.capture.CaptureCameraOverlay
import com.apexfission.android.carddetector.ui.capture.CaptureReview
import com.apexfission.android.carddetector.ui.capture.CaptureTurnOver
import com.apexfission.android.carddetector.ui.capture.CapturedCardSide
import com.apexfission.android.carddetector.ui.capture.CardSide
import com.apexfission.android.carddetector.ui.capture.defaultCardCaptureStrings
import com.apexfission.android.carddetector.ui.detector.CardDetectorOverlayScope
import com.apexfission.android.carddetector.ui.detector.CardDetectorPreset
import com.apexfission.android.carddetector.ui.overlays.CardLockOnOverlay
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
private fun rememberScope(detected: Boolean): CardDetectorOverlayScope {
    val density = LocalDensity.current
    val chain = remember(density) {
        with(density) { createPreviewImageSpaceChain(393, 852, 393.dp.roundToPx(), 852.dp.roundToPx()) }
    }
    return remember(detected, chain) {
        object : CardDetectorOverlayScope {
            override val detectionState = sampleDetection.takeIf { detected }
            override val detectionSequence = if (detected) 1L else 0L
            override val latestBestDetection = detectionState
            override val imageSpaceChain: ImageSpaceChain = chain
            override val captureEnabled = detected
            override val detectorPreset = CardDetectorPreset.HighPerformance
            override val cameraPreset = CameraPreset.HighResolution
            override val classLabels = mapOf(0 to "SAMPLE_CARD")
            override val flashlightEnabled = false
            override val flashlightAvailable = true
            override fun goBack() = Unit
            override fun toggleFlashlight() = Unit
            override fun capture() = Unit
        }
    }
}

@Composable
private fun Screen(showCard: Boolean = true, content: @Composable BoxScope.() -> Unit) {
    MaterialTheme {
        Box(Modifier.fillMaxSize().background(Color(0xFF111D27))) {
            if (showCard) {
                val bitmap = remember { sampleCard() }
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
@Composable
fun SingleSideDetectedScreenshot() = Screen {
    rememberScope(true).IdCaptureOverlay(IdCaptureOverlayConfig(title = "Card Detector demo"))
}

@Composable
private fun CameraScreen(side: CardSide) = Screen(showCard = false) {
    rememberScope(false).CaptureCameraOverlay(
        side = side, strings = defaultCardCaptureStrings(), autoCaptureEnabled = true,
        onToggleAutoCapture = {}, showInitialGuide = true,
        canCapture = false, focused = false, onCapture = {},
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
fun CaptureTurnOverScreenshot() = Screen(showCard = false) {
    val front = remember { CapturedCardSide(sampleDetection, sampleCard()) }
    CaptureTurnOver(front, defaultCardCaptureStrings(), {}, {})
}

@PreviewTest
@Preview(name = "capture-review-en", widthDp = 393, heightDp = 852, locale = "en", fontScale = 1f)
@Preview(name = "capture-review-es", widthDp = 393, heightDp = 852, locale = "es", fontScale = 1f)
@Composable
fun CaptureReviewScreenshot() = Screen(showCard = false) {
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
fun LockOnOverlayScreenshot() = Screen { rememberScope(true).CardLockOnOverlay() }
