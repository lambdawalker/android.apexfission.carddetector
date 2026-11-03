package com.apexfission.android.carddetector.ui.overlays.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apexfission.android.carddetector.ui.detector.CardDetectorOverlayScope
import com.apexfission.android.carddetector.ui.overlays.IdCaptureIndicatorOverlay
import com.apexfission.android.carddetector.ui.overlays.IdCaptureOverlayConfig

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
            // The white initial guide owns the idle state. Tracking fades to transparent
            // on misses instead of adding a second, centered idle rectangle.
            IdCaptureIndicatorOverlay(config.copy(idleOpacity = 0f))
        }
        OverlayTopGradient(Modifier.align(Alignment.TopCenter))
        OverlayBottomGradient(Modifier.align(Alignment.BottomCenter))
        Column(Modifier.fillMaxSize().safeDrawingPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            OverlayHeader(config.title, { goBack() }, exitContentDescription)
            Spacer(Modifier.height(16.dp))
            OverlayInstructions(config.instructionTitle, config.instructionSubTitle)
            Spacer(Modifier.weight(1f))
            if (step != null) {
                Spacer(Modifier.height(12.dp))
                Text(step, color = Color(0xFF75E8CD), fontWeight = FontWeight.Bold)
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
                Text(modeText, color = Color.White, fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
