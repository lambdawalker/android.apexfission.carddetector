package com.apexfission.android.carddetector.ui.overlays.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apexfission.android.carddetector.ui.detector.CardDetectorOverlayScope
import com.apexfission.android.carddetector.ui.overlays.IdCaptureIndicatorOverlay
import com.apexfission.android.carddetector.ui.overlays.IdCaptureOverlayConfig
import com.apexfission.android.carddetector.ui.overlays.checkerboardBackground
import android.content.res.Configuration
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.carddetector.domain.tflite.model.CardDetection
import com.apexfission.android.carddetector.domain.tflite.model.LockingStatus
import com.apexfission.android.carddetector.ui.camerapreview.CameraPreset
import com.apexfission.android.carddetector.ui.camerapreview.createPreviewImageSpaceChain
import com.apexfission.android.carddetector.ui.detector.CardDetectorPreset
import com.apexfission.android.math.models.ImageBox
import com.apexfission.android.yolo.engine.Feature

/** One camera overlay for single-side and two-sided capture; absent slots consume no space. */
@Composable
internal fun CardDetectorOverlayScope.CaptureOverlay(
    config: IdCaptureOverlayConfig,
    showInitialGuide: Boolean,
    canCapture: Boolean,
    onCapture: () -> Unit,
    autoCaptureEnabled: Boolean,
    onToggleAutoCapture: () -> Unit,
    showAutoCaptureToggle: Boolean,
    exitContentDescription: String,
    autoToggleContentDescription: String,
    flashContentDescription: String,
    shutterContentDescription: String,
    step: String? = null,
    modeText: String? = null,
    showIndicator: Boolean = true,
) {
    Box(Modifier.fillMaxSize()) {
        if (showIndicator) {
            InitialCaptureGuide(showInitialGuide, detectionState != null || latestBestDetection != null)
            // The initial guide owns the idle state. Tracking fades to transparent
            // on misses instead of adding a second, centered idle rectangle.
            IdCaptureIndicatorOverlay(config.copy(idleOpacity = 0f))
        }
        OverlayTopGradient(Modifier.align(Alignment.TopCenter))
        OverlayBottomGradient(Modifier.align(Alignment.BottomCenter))
        val header: @Composable () -> Unit = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OverlayHeader(config.title, { goBack() }, exitContentDescription)
                Spacer(Modifier.height(16.dp))
                OverlayInstructions(config.instructionTitle, config.instructionSubTitle)
            }
        }
        val actions: @Composable () -> Unit = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (step != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(step, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(12.dp))
                }
                CaptureControlsRow(
                    isCaptureEnabled = canCapture,
                    onCapture = onCapture,
                    autoCaptureEnabled = autoCaptureEnabled,
                    onToggleAutoCapture = onToggleAutoCapture,
                    flashlightAvailable = flashlightAvailable,
                    flashlightEnabled = flashlightEnabled,
                    onToggleFlashlight = { toggleFlashlight() },
                    autoToggleContentDescription = autoToggleContentDescription,
                    flashContentDescription = flashContentDescription,
                    shutterContentDescription = shutterContentDescription,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp),
                    showAutoCaptureToggle = showAutoCaptureToggle,
                )
                if (modeText != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(modeText, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            if (maxWidth > maxHeight && maxWidth >= 600.dp) {
                Row(Modifier.fillMaxSize()) {
                    Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) { header() }
                    Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Bottom) { actions() }
                }
            } else {
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = maxHeight),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    header()
                    actions()
                }
            }
        }
    }
}

@Composable
private fun CaptureOverlayPreviewContent(hasDetection: Boolean) {
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
            override val imageSpaceChain = chain
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
                CaptureOverlay(
                    config = IdCaptureOverlayConfig(),
                    showInitialGuide = !hasDetection,
                    canCapture = hasDetection,
                    onCapture = {},
                    autoCaptureEnabled = true,
                    onToggleAutoCapture = {},
                    showAutoCaptureToggle = true,
                    exitContentDescription = "Exit",
                    autoToggleContentDescription = "Auto",
                    flashContentDescription = "Flash",
                    shutterContentDescription = "Shutter",
                    step = "Step 1 of 2",
                    modeText = "Capture ID",
                    showIndicator = true,
                )
            }
        }
    }
}

@Preview(name = "Light Mode Detected", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark Mode Detected", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CaptureOverlayDetectedPreview() {
    CaptureOverlayPreviewContent(hasDetection = true)
}

@Preview(name = "Light Mode Idle", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark Mode Idle", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CaptureOverlayIdlePreview() {
    CaptureOverlayPreviewContent(hasDetection = false)
}

