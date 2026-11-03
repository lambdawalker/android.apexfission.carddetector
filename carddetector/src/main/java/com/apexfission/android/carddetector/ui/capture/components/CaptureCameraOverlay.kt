package com.apexfission.android.carddetector.ui.capture.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apexfission.android.carddetector.domain.tflite.model.CardDetection
import com.apexfission.android.carddetector.domain.tflite.model.LockingStatus
import com.apexfission.android.carddetector.ui.camerapreview.CameraPreset
import com.apexfission.android.carddetector.ui.camerapreview.createPreviewImageSpaceChain
import com.apexfission.android.carddetector.ui.capture.CardCaptureStrings
import com.apexfission.android.carddetector.ui.capture.CardSide
import com.apexfission.android.carddetector.ui.capture.defaultCardCaptureStrings
import com.apexfission.android.carddetector.ui.detector.CardDetectorOverlayScope
import com.apexfission.android.carddetector.ui.detector.CardDetectorPreset
import com.apexfission.android.carddetector.ui.overlays.CaptureShutterButton
import com.apexfission.android.carddetector.ui.overlays.IdCaptureIndicatorOverlay
import com.apexfission.android.carddetector.ui.overlays.IdCaptureOverlayConfig
import com.apexfission.android.carddetector.ui.overlays.checkerboardBackground
import com.apexfission.android.carddetector.ui.overlays.components.CaptureControlsRow
import com.apexfission.android.carddetector.ui.overlays.components.InitialGuideOverlay
import com.apexfission.android.carddetector.ui.overlays.components.OverlayBottomGradient
import com.apexfission.android.carddetector.ui.overlays.components.OverlayHeader
import com.apexfission.android.carddetector.ui.overlays.components.OverlayInstructions
import com.apexfission.android.carddetector.ui.overlays.components.OverlayTitle
import com.apexfission.android.carddetector.ui.overlays.components.OverlayTopGradient
import com.apexfission.android.math.models.ImageBox
import com.apexfission.android.math.models.ImageSpaceChain
import com.apexfission.android.yolo.engine.Feature

/** Component. */
@Composable
internal fun CardDetectorOverlayScope.CaptureCameraOverlay(
    side: CardSide, strings: CardCaptureStrings, autoCaptureEnabled: Boolean,
    onToggleAutoCapture: () -> Unit, showInitialGuide: Boolean,
    canCapture: Boolean, focused: Boolean, onCapture: () -> Unit,
) {
    // The initial guide yields permanently to detection for this capture attempt.
    if (showInitialGuide && detectionState == null) InitialGuideOverlay()
    IdCaptureIndicatorOverlay(IdCaptureOverlayConfig())
    Box(modifier = Modifier.fillMaxSize()) {
        OverlayTopGradient(modifier = Modifier.align(Alignment.TopCenter))
        OverlayBottomGradient(modifier = Modifier.align(Alignment.BottomCenter))

        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            OverlayHeader(title = strings.title, onBack = { goBack() }, exitContentDescription = strings.exit)
            Spacer(modifier = Modifier.height(16.dp))
            OverlayInstructions(
                title = if (side == CardSide.Front) strings.front else strings.back,
                subtitle = if (!canCapture) { if (side == CardSide.Front) strings.frontInstruction else strings.backInstruction } else if (focused) strings.ready else strings.focus
            )

            Spacer(modifier = Modifier.weight(1f))

            Spacer(modifier = Modifier.height(12.dp))
            Text(if (side == CardSide.Front) strings.stepFront else strings.stepBack, color=Color(0xFF75E8CD), fontWeight=FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            
            CaptureControlsRow(
                isCaptureEnabled = canCapture,
                onCapture = onCapture,
                autoCaptureEnabled = autoCaptureEnabled,
                onToggleAutoCapture = onToggleAutoCapture,
                flashlightAvailable = flashlightAvailable,
                flashlightEnabled = flashlightEnabled,
                onToggleFlashlight = { toggleFlashlight() },
                autoToggleContentDescription = strings.autoToggle,
                flashContentDescription = strings.flash,
                shutterContentDescription = strings.shutter,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(if (autoCaptureEnabled) strings.auto else strings.manual, color=Color.White, fontSize=13.sp)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
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
