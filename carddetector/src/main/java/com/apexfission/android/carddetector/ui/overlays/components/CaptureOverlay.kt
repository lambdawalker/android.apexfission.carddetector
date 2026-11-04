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
        val header: @Composable () -> Unit = {
            Column(Modifier.background(MaterialTheme.colorScheme.surface), horizontalAlignment = Alignment.CenterHorizontally) {
                OverlayHeader(config.title, { goBack() }, exitContentDescription)
                Spacer(Modifier.height(16.dp))
                OverlayInstructions(config.instructionTitle, config.instructionSubTitle)
            }
        }
        val actions: @Composable () -> Unit = {
            Column(Modifier.background(MaterialTheme.colorScheme.surface), horizontalAlignment = Alignment.CenterHorizontally) {
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
