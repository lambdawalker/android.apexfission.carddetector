package com.apexfission.android.carddetector.ui.capture.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apexfission.android.carddetector.domain.tflite.model.CardDetection
import com.apexfission.android.carddetector.domain.tflite.model.LockingStatus
import com.apexfission.android.carddetector.ui.camerapreview.CameraPreset
import com.apexfission.android.carddetector.ui.camerapreview.createPreviewImageSpaceChain
import com.apexfission.android.carddetector.ui.capture.CardCaptureStrings
import com.apexfission.android.carddetector.ui.capture.CaptureGuidance
import com.apexfission.android.carddetector.ui.capture.text
import com.apexfission.android.carddetector.ui.capture.CardSide
import com.apexfission.android.carddetector.ui.capture.defaultCardCaptureStrings
import com.apexfission.android.carddetector.ui.detector.CardDetectorOverlayScope
import com.apexfission.android.carddetector.ui.detector.CardDetectorPreset
import com.apexfission.android.carddetector.ui.overlays.IdCaptureOverlayConfig
import com.apexfission.android.carddetector.ui.overlays.checkerboardBackground
import com.apexfission.android.carddetector.ui.overlays.components.CaptureOverlay
import com.apexfission.android.math.models.ImageBox
import com.apexfission.android.math.models.ImageSpaceChain
import com.apexfission.android.yolo.engine.Feature

/** Two-sided copy and readiness policy adapted to the shared capture overlay. */
@Composable
internal fun CardDetectorOverlayScope.CaptureCameraOverlay(
    side: CardSide, strings: CardCaptureStrings, autoCaptureEnabled: Boolean,
    onToggleAutoCapture: () -> Unit, showInitialGuide: Boolean,
    canCapture: Boolean, focused: Boolean, onCapture: () -> Unit,
    guidance: CaptureGuidance? = null,
) {
    CaptureOverlay(
        config = IdCaptureOverlayConfig(
            title = strings.title,
            instructionTitle = if (side == CardSide.Front) strings.front else strings.back,
            instructionSubTitle = when {
                guidance == CaptureGuidance.Looking -> strings.find + " · " + if (side == CardSide.Front) strings.frontInstruction else strings.backInstruction
                guidance != null -> guidance.text(strings)
                !canCapture -> if (side == CardSide.Front) strings.frontInstruction else strings.backInstruction
                focused -> strings.ready
                else -> strings.focus
            },
        ),
        showInitialGuide = showInitialGuide,
        canCapture = canCapture,
        onCapture = onCapture,
        autoCaptureEnabled = autoCaptureEnabled,
        onToggleAutoCapture = onToggleAutoCapture,
        showAutoCaptureToggle = true,
        exitContentDescription = strings.exit,
        autoToggleContentDescription = strings.autoToggle,
        flashContentDescription = strings.flash,
        shutterContentDescription = strings.shutter,
        step = if (side == CardSide.Front) strings.stepFront else strings.stepBack,
        modeText = if (autoCaptureEnabled) strings.auto else strings.manual,
    )
}

@Composable
private fun CaptureCameraOverlayPreviewContent(side: CardSide, hasDetection: Boolean) {
    val density = LocalDensity.current
    val chain = remember(density) {
        with(density) { createPreviewImageSpaceChain(393, 852, 393.dp.roundToPx(), 852.dp.roundToPx()) }
    }
    val sampleDetection = if (hasDetection) {
        CardDetection(
            lockOnProgress = 0.5f,
            id = 12345L,
            card = Feature(ImageBox.from2P(40, 328, 353, 524), 0.95f, 0),
            features = emptyList(),
            lockingStatus = LockingStatus.LockingCard,
            focusScore = 150f,
        )
    } else null
    val scope = remember(chain, sampleDetection) {
        object : CardDetectorOverlayScope {
            override val detectionState = sampleDetection
            override val detectionSequence = if (hasDetection) 1L else 0L
            override val latestBestDetection = sampleDetection
            override val imageSpaceChain: ImageSpaceChain = chain
            override val captureEnabled = hasDetection
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
    MaterialTheme {
        Box(Modifier.fillMaxSize().checkerboardBackground()) {
            with(scope) {
                CaptureCameraOverlay(
                    side = side,
                    strings = defaultCardCaptureStrings(),
                    autoCaptureEnabled = true,
                    onToggleAutoCapture = {},
                    showInitialGuide = !hasDetection,
                    canCapture = hasDetection,
                    focused = hasDetection,
                    onCapture = {},
                )
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CaptureCameraOverlayFrontPreview() {
    CaptureCameraOverlayPreviewContent(side = CardSide.Front, hasDetection = false)
}

@Preview(name = "Light Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CaptureCameraOverlayBackPreview() {
    CaptureCameraOverlayPreviewContent(side = CardSide.Back, hasDetection = false)
}

@Preview(name = "Light Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CaptureCameraOverlayFrontDetectedPreview() {
    CaptureCameraOverlayPreviewContent(side = CardSide.Front, hasDetection = true)
}

@Preview(name = "Light Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CaptureCameraOverlayBackDetectedPreview() {
    CaptureCameraOverlayPreviewContent(side = CardSide.Back, hasDetection = true)
}
