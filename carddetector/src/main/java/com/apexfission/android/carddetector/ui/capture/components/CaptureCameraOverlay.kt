package com.apexfission.android.carddetector.ui.capture.components

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
import com.apexfission.android.carddetector.ui.camerapreview.CameraPreset
import com.apexfission.android.carddetector.ui.camerapreview.createPreviewImageSpaceChain
import com.apexfission.android.carddetector.ui.capture.CardCaptureStrings
import com.apexfission.android.carddetector.ui.capture.CardSide
import com.apexfission.android.carddetector.ui.capture.defaultCardCaptureStrings
import com.apexfission.android.carddetector.ui.detector.CardDetectorOverlayScope
import com.apexfission.android.carddetector.ui.detector.CardDetectorPreset
import com.apexfission.android.carddetector.ui.overlays.IdCaptureIndicatorOverlay
import com.apexfission.android.carddetector.ui.overlays.IdCaptureOverlayConfig
import com.apexfission.android.math.models.ImageSpaceChain

/** Component. */
@Composable
internal fun CardDetectorOverlayScope.CaptureCameraOverlay(
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
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface.copy(alpha=0.65f)).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CaptureTitle(if (side == CardSide.Front) strings.front else strings.back)
        }
        Spacer(Modifier.weight(1f))
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface.copy(alpha=0.75f)).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (!canCapture) { if (side == CardSide.Front) strings.frontInstruction else strings.backInstruction } else if (focused) strings.ready else strings.focus, color=MaterialTheme.colorScheme.onSurface, textAlign=TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(if (side == CardSide.Front) strings.stepFront else strings.stepBack, color=MaterialTheme.colorScheme.primary, fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.Center) {
                IconToggleButton(checked = autoCaptureEnabled, onCheckedChange = { onToggleAutoCapture() },
                    modifier = Modifier.width(64.dp)) {
                    Icon(Icons.Default.AutoMode, contentDescription = strings.autoToggle,
                        tint = if (autoCaptureEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(28.dp))
                }
                Button(onClick=onCapture, enabled=canCapture, shape=CircleShape,
                    contentPadding=PaddingValues(8.dp), colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.colorScheme.onSurface),
                    modifier=Modifier.size(76.dp).border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape).semantics { contentDescription=strings.shutter }) {}
                IconToggleButton(checked = flashlightEnabled, onCheckedChange = { toggleFlashlight() },
                    enabled = flashlightAvailable, modifier = Modifier.width(64.dp)) {
                    Icon(if (flashlightEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = strings.flash,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (flashlightAvailable) 1f else 0.38f),
                        modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(if (autoCaptureEnabled) strings.auto else strings.manual, color=MaterialTheme.colorScheme.onSurface, fontSize=13.sp)
        }
    }
}

@Preview(name = "Light Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CaptureCameraOverlayPreview() {
    val density = LocalDensity.current
    val chain = remember(density) {
        with(density) { createPreviewImageSpaceChain(393, 852, 393.dp.roundToPx(), 852.dp.roundToPx()) }
    }
    val scope = remember(chain) {
        object : CardDetectorOverlayScope {
            override val detectionState = null
            override val detectionSequence = 0L
            override val latestBestDetection = null
            override val imageSpaceChain: ImageSpaceChain = chain
            override val captureEnabled = false
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
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            with(scope) {
                CaptureCameraOverlay(
                    side = CardSide.Front,
                    strings = defaultCardCaptureStrings(),
                    autoCaptureEnabled = true,
                    onToggleAutoCapture = {},
                    showInitialGuide = true,
                    canCapture = false,
                    focused = false,
                    onCapture = {},
                )
            }
        }
    }
}
