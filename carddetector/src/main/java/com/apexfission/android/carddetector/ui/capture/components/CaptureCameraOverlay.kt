package com.apexfission.android.carddetector.ui.capture.components

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
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
    if (showInitialGuide && detectionState == null) Canvas(Modifier.fillMaxSize()) {
        val edge = minOf(size.width * 0.85f, size.height * 0.48f)
        drawRoundRect(Color.White.copy(alpha = 0.5f), Offset((size.width-edge)/2, (size.height-edge)/2),
            Size(edge, edge), CornerRadius(18.dp.toPx()), style = Stroke(2.dp.toPx()))
    }
    if (detectionState != null) IdCaptureIndicatorOverlay(IdCaptureOverlayConfig())
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black,
                            Color.Black.copy(alpha = 0.75f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.75f),
                            Color.Black
                        )
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            CaptureHeader(strings, { goBack() })
            Spacer(modifier = Modifier.height(16.dp))
            CaptureTitle(if (side == CardSide.Front) strings.front else strings.back)
            Spacer(modifier = Modifier.height(6.dp))
            Text(if (!canCapture) { if (side == CardSide.Front) strings.frontInstruction else strings.backInstruction } else if (focused) strings.ready else strings.focus, color=Color.White, textAlign=TextAlign.Center)

            Spacer(modifier = Modifier.weight(1f))

            Spacer(modifier = Modifier.height(12.dp))
            Text(if (side == CardSide.Front) strings.stepFront else strings.stepBack, color=Color(0xFF75E8CD), fontWeight=FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 24.dp)
            ) {
                CaptureShutterButton(
                    isCaptureEnabled = canCapture,
                    onClick = onCapture,
                    modifier = Modifier.align(Alignment.Center).semantics { contentDescription=strings.shutter }
                )
                
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconToggleButton(checked = autoCaptureEnabled, onCheckedChange = { onToggleAutoCapture() },
                        modifier = Modifier.size(48.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)) {
                        Icon(Icons.Default.AutoMode, contentDescription = strings.autoToggle,
                            tint = if (autoCaptureEnabled) Color(0xFF75E8CD) else Color.White.copy(alpha = 0.6f))
                    }
                    if (flashlightAvailable) {
                        IconToggleButton(checked = flashlightEnabled, onCheckedChange = { toggleFlashlight() },
                            modifier = Modifier.size(48.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)) {
                            Icon(if (flashlightEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = strings.flash,
                                tint = if (flashlightEnabled) Color.Yellow else Color.White)
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }
            }
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
