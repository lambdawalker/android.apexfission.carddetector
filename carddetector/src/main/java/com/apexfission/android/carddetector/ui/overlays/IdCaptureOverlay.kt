package com.apexfission.android.carddetector.ui.overlays

import android.content.res.Configuration
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apexfission.android.carddetector.domain.tflite.model.CardDetection
import com.apexfission.android.carddetector.domain.tflite.model.LockingStatus
import com.apexfission.android.math.models.ImageBox
import com.apexfission.android.math.models.ImageSpaceChain
import com.apexfission.android.yolo.engine.Feature
import com.apexfission.android.carddetector.ui.camerapreview.CameraPreset
import com.apexfission.android.carddetector.ui.camerapreview.createPreviewImageSpaceChain
import com.apexfission.android.carddetector.ui.detector.CardDetectorOverlayScope
import com.apexfission.android.carddetector.ui.detector.CardDetectorPreset
import com.apexfission.android.carddetector.ui.overlays.components.CaptureControlsRow
import com.apexfission.android.carddetector.ui.overlays.components.InitialGuideOverlay
import com.apexfission.android.carddetector.ui.overlays.components.OverlayBottomGradient
import com.apexfission.android.carddetector.ui.overlays.components.OverlayHeader
import com.apexfission.android.carddetector.ui.overlays.components.OverlayInstructions
import com.apexfission.android.carddetector.ui.overlays.components.OverlayTopGradient
import com.apexfission.android.carddetector.ui.overlays.draw.lerpF

/**
 * Component.
 * Renders the animated card detection indicator / guide frame for ID capture.
 *
 * Built on top of [AnimatedDetectionCanvas].
 *
 * @param config Configuration parameters for opacity, timers, smoothing, and capture behavior.
 */
@Composable
fun CardDetectorOverlayScope.IdCaptureIndicatorOverlay(
    config: IdCaptureOverlayConfig = IdCaptureOverlayConfig()
) {
    AnimatedDetectionCanvas(
        config = DetectionAnimationConfig(
            idleOpacity = config.idleOpacity,
            detectedOpacity = config.detectedOpacity,
            trackingAnimationDurationMs = config.trackingAnimationDurationMs,
            resetAnimationDurationMs = config.resetAnimationDurationMs,
            fadeAnimationDurationMs = config.fadeAnimationDurationMs,
            enableGuideSmoothing = config.enableGuideSmoothing,
            enableContinuousAnimations = config.enableContinuousAnimations,
            resetDetectionIndicatorTime = config.resetBoundingBoxWaitTimeMs
        )
    ) {
        val left = bounds.left
        val top = bounds.top
        val right = bounds.right
        val bottom = bounds.bottom

        val progress = bounds.smoothProgress

        // Animate corner bracket thickness and length when an ID is detected
        val cornerLen = lerpF(15.dp.toPx(), 22.dp.toPx(), progress)
        val strokeWidth = lerpF(2.dp.toPx(), 6.dp.toPx(), progress)

        // Transition guide color to bright electric blue on lock-on
        val brightBlue = Color(0xFF00E5FF)
        val activeColor = lerp(config.guideColor, brightBlue, progress)
        val color = activeColor.copy(alpha = bounds.opacity)

        // Padding to offset the corner brackets from the dashed line
        val cornerPadding = 4.dp.toPx()
        val pLeft = left - cornerPadding
        val pTop = top - cornerPadding
        val pRight = right + cornerPadding
        val pBottom = bottom + cornerPadding

        // Outer dashed boundary line
        val dashedPath = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = left, top = top, right = right, bottom = bottom, cornerRadius = CornerRadius(12.dp.toPx(), 10.dp.toPx())
                )
            )
        }

        drawPath(
            path = dashedPath, color = color, style = Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f))
            )
        )

        // Top-Left corner bracket
        drawPath(
            path = Path().apply {
                moveTo(pLeft, pTop + cornerLen)
                lineTo(pLeft, pTop + 12.dp.toPx())
                quadraticBezierTo(pLeft, pTop, pLeft + 12.dp.toPx(), pTop)
                lineTo(pLeft + cornerLen, pTop)
            }, color = color, style = Stroke(width = strokeWidth)
        )

        // Top-Right corner bracket
        drawPath(
            path = Path().apply {
                moveTo(pRight - cornerLen, pTop)
                lineTo(pRight - 12.dp.toPx(), pTop)
                quadraticBezierTo(pRight, pTop, pRight, pTop + 12.dp.toPx())
                lineTo(pRight, pTop + cornerLen)
            }, color = color, style = Stroke(width = strokeWidth)
        )

        // Bottom-Right corner bracket
        drawPath(
            path = Path().apply {
                moveTo(pRight, pBottom - cornerLen)
                lineTo(pRight, pBottom - 12.dp.toPx())
                quadraticBezierTo(pRight, pBottom, pRight - 12.dp.toPx(), pBottom)
                lineTo(pRight - cornerLen, pBottom)
            }, color = color, style = Stroke(width = strokeWidth)
        )

        // Bottom-Left corner bracket
        drawPath(
            path = Path().apply {
                moveTo(pLeft + cornerLen, pBottom)
                lineTo(pLeft + 12.dp.toPx(), pBottom)
                quadraticBezierTo(pLeft, pBottom, pLeft, pBottom - 12.dp.toPx())
                lineTo(pLeft, pBottom - cornerLen)
            }, color = color, style = Stroke(width = strokeWidth)
        )
    }
}

/**
 * Component.
 * Renders the ID capture controls and text (header, title, instructions, shutter button, flashlight toggle).
 *
 * @param config Configuration parameters for titles and capture behavior.
 */
@Composable
fun CardDetectorOverlayScope.IdCaptureControlsOverlay(
    config: IdCaptureOverlayConfig = IdCaptureOverlayConfig(),
    autoCaptureEnabled: Boolean = false,
    onToggleAutoCapture: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        OverlayTopGradient(modifier = Modifier.align(Alignment.TopCenter))
        OverlayBottomGradient(modifier = Modifier.align(Alignment.BottomCenter))

        // Overlay UI layer (Header, instructions, controls)
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            // Top Bar with back button & title
            OverlayHeader(title = config.title, onBack = { goBack() })

            Spacer(modifier = Modifier.height(16.dp))

            // Instruction title and subtitle
            OverlayInstructions(
                title = config.instructionTitle,
                subtitle = config.instructionSubTitle
            )

            Spacer(modifier = Modifier.weight(1f))

            val isCaptureEnabled = if (config.requiresCardDetectionForCapture) captureEnabled else true

            CaptureControlsRow(
                isCaptureEnabled = isCaptureEnabled,
                onCapture = { capture() },
                autoCaptureEnabled = autoCaptureEnabled,
                onToggleAutoCapture = onToggleAutoCapture,
                flashlightAvailable = flashlightAvailable,
                flashlightEnabled = flashlightEnabled,
                onToggleFlashlight = { toggleFlashlight() },
                autoToggleContentDescription = "Toggle automatic capture",
                flashContentDescription = "Toggle Flashlight",
                shutterContentDescription = "Capture",
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 32.dp, vertical = 24.dp)
            )

            Text(
                text = if (autoCaptureEnabled) "Auto-capture on" else "Auto-capture off",
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Component.
 * Built-in ID capture overlay combining the card detection indicator and controls/text.
 *
 * Renders [IdCaptureIndicatorOverlay] and [IdCaptureControlsOverlay] together.
 *
 * @param config Configuration parameters for opacity, timers, smoothing, and capture behavior.
 */
@Composable
fun CardDetectorOverlayScope.IdCaptureOverlay(
    config: IdCaptureOverlayConfig = IdCaptureOverlayConfig(),
    showInitialGuide: Boolean = true,
    autoCaptureEnabled: Boolean = false,
    onToggleAutoCapture: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (showInitialGuide && detectionState == null) InitialGuideOverlay()
        IdCaptureIndicatorOverlay(config)
        IdCaptureControlsOverlay(config, autoCaptureEnabled, onToggleAutoCapture)
    }
}

@Composable
private fun IdCaptureOverlayPreviewContent(hasDetection: Boolean) {
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
                IdCaptureOverlay()
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun IdCaptureOverlayDetectedPreview() {
    IdCaptureOverlayPreviewContent(hasDetection = true)
}

@Preview(name = "Light Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun IdCaptureOverlayIdlePreview() {
    IdCaptureOverlayPreviewContent(hasDetection = false)
}
