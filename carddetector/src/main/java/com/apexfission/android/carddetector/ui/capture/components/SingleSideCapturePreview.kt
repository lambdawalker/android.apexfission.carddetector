package com.apexfission.android.carddetector.ui.capture.components

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
import com.apexfission.android.carddetector.ui.capture.*
import com.apexfission.android.carddetector.ui.detector.CardDetectorOverlayScope
import com.apexfission.android.carddetector.ui.detector.CardDetectorPreset
import com.apexfission.android.carddetector.ui.overlays.IdCaptureOverlay
import com.apexfission.android.carddetector.ui.overlays.IdCaptureOverlayConfig
import com.apexfission.android.carddetector.ui.overlays.checkerboardBackground
import com.apexfission.android.math.models.ImageBox
import com.apexfission.android.yolo.engine.Feature

@Composable
private fun SingleCapturePreview(detected: Boolean) {
    val density = LocalDensity.current
    val chain = remember(density) {
        with(density) { createPreviewImageSpaceChain(393, 852, 393.dp.roundToPx(), 852.dp.roundToPx()) }
    }
    val scope = remember(chain, detected) {
        object : CardDetectorOverlayScope {
            override val detectionState = if (detected) CardDetection(1f, 1L,
                Feature(ImageBox.from2P(40, 328, 353, 524), 0.95f, 0), emptyList(),
                LockingStatus.CardLocked, focusScore = 150f) else null
            override val detectionSequence = if (detected) 1L else 0L
            override val latestBestDetection = detectionState
            override val imageSpaceChain = chain
            override val captureEnabled = detected
            override val detectorPreset = CardDetectorPreset.HighPerformance
            override val cameraPreset = CameraPreset.HighResolution
            override val classLabels = emptyMap<Int, String>()
            override val flashlightEnabled = false
            override val flashlightAvailable = true
            override fun goBack() = Unit
            override fun toggleFlashlight() = Unit
            override fun capture() = Unit
        }
    }
    MaterialTheme {
        Box(Modifier.fillMaxSize().checkerboardBackground()) {
            val guidance = if (detected) CaptureGuidance.HoldSteady else CaptureGuidance.Looking
            val strings = defaultCardCaptureStrings()
            scope.IdCaptureOverlay(
                config = IdCaptureOverlayConfig(
                    title = strings.title,
                    instructionTitle = strings.singleInstruction,
                    instructionSubTitle = guidance.text(strings)
                ),
                strings = strings,
                showInitialGuide = !detected,
                autoCaptureEnabled = true,
                showAutoCaptureToggle = true,
                canCapture = detected,
                onCapture = {}
            )
        }
    }
}

@Preview(name = "Single-side automatic · looking", widthDp = 393, heightDp = 852)
@Composable
private fun SingleCaptureLookingPreview() = SingleCapturePreview(false)

@Preview(name = "Single-side automatic · hold steady", widthDp = 393, heightDp = 852)
@Composable
private fun SingleCaptureHoldingPreview() = SingleCapturePreview(true)
