package com.apexfission.android.carddetector.ui.overlays

import androidx.compose.material3.MaterialTheme
import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apexfission.android.carddetector.domain.tflite.model.CardDetection
import com.apexfission.android.carddetector.domain.tflite.model.LockingStatus
import com.apexfission.android.math.models.ImageBox
import com.apexfission.android.yolo.engine.Feature
import com.apexfission.android.carddetector.ui.camerapreview.CameraPreset
import com.apexfission.android.carddetector.ui.camerapreview.createPreviewImageSpaceChain
import com.apexfission.android.carddetector.ui.detector.CardDetectorPreset
import com.apexfission.android.carddetector.ui.detector.CardDetectorOverlayScope
import com.apexfission.android.carddetector.ui.overlays.draw.buildConnectors
import com.apexfission.android.carddetector.ui.overlays.draw.buildCorners
import com.apexfission.android.carddetector.ui.overlays.draw.drawGlowPath
import com.apexfission.android.carddetector.ui.overlays.draw.lerpF
import kotlin.math.min

/**
 * Component.
 * Lock-on overlay using AnimatedDetectionCanvas.
 *
 * Progress, breathe, and sweepPhase are automatically animated and exposed by AnimatedDetectionCanvas.
 * When detection disappears, the last progress is retained while the overlay fades out.
 */
@Composable
fun CardDetectorOverlayScope.CardLockOnOverlay(
    config: DetectionAnimationConfig = DetectionAnimationConfig(
        idleOpacity = 0f,
        detectedOpacity = 1f,
        resetPositionOnMissing = false,
        fadeAnimationDurationMs = 300
    )
) {
    val colors = MaterialTheme.colorScheme
    AnimatedDetectionCanvas(config = config) {
        if (bounds.opacity <= 0f) return@AnimatedDetectionCanvas

        val progress = bounds.smoothProgress
        val breathe = bounds.breathe
        val sweepPhase = bounds.sweepPhase

        val idlePadding = 28.dp.toPx()
        val lockedPadding = 4.dp.toPx()
        val padding = lerpF(idlePadding, lockedPadding, progress)

        val left = bounds.left - padding
        val top = bounds.top - padding
        val right = bounds.right + padding
        val bottom = bounds.bottom + padding

        val frameWidth = (right - left).coerceAtLeast(1f)
        val frameHeight = (bottom - top).coerceAtLeast(1f)
        if (frameWidth < 2f || frameHeight < 2f) return@AnimatedDetectionCanvas

        val radius = 22.dp.toPx()
        val strokeWidth = 0.8.dp.toPx()

        val idleColor = colors.onSurface.copy(alpha = 0.42f * bounds.opacity)
        val trackingColor = colors.primary.copy(alpha = 0.95f * bounds.opacity)
        val lockedColor = colors.tertiary.copy(alpha = bounds.opacity)

        val frameColor = when {
            progress < 0.55f ->
                lerp(idleColor, trackingColor, progress / 0.55f)

            else ->
                lerp(trackingColor, lockedColor, (progress - 0.55f) / 0.45f)
        }

        val glowColor = frameColor.copy(
            alpha = lerpF(
                0.45f * bounds.opacity,
                0.85f * bounds.opacity,
                progress
            )
        )

        val pulse = if (progress < 0.15f) breathe else 1f
        val blurRadius = lerpF(6.dp.toPx(), 12.dp.toPx(), progress) * pulse
        val cornerLen = lerpF(18.dp.toPx(), 24.dp.toPx(), progress)

        val inset = radius * 0.45f

        val frameSegments =
            buildCorners(
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                cornerLength = cornerLen,
                cornerRadius = cornerLen / 2
            ) + buildConnectors(
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                cornerLength = cornerLen,
                gap = strokeWidth
            )

        frameSegments.forEach { segment ->
            val segmentStroke = if (segment.isCorner) strokeWidth * 6f else strokeWidth
            val cornerRadius = min(segmentStroke * 1.6f, 10.dp.toPx())

            drawGlowPath(
                points = segment.points,
                blurRadius = blurRadius,
                color = glowColor,
                strokeWidth = segmentStroke,
                rounded = segment.rounded,
                cornerRadius = cornerRadius
            )
        }

        if (progress > 0.72f) {
            val sweepAlpha =
                ((progress - 0.72f) / 0.28f).coerceIn(0f, 1f) * bounds.opacity

            val sweepWidth = frameWidth * 0.005f
            val sweepX =
                left + (frameWidth + sweepWidth) * sweepPhase - sweepWidth

            val startX = sweepX.coerceAtLeast(left + inset + cornerLen)
            val endX = (sweepX + sweepWidth).coerceAtMost(
                right - inset - cornerLen
            )

            if (endX > startX) {
                val sweepPoints = listOf(startX to top, endX to top)
                val sweepColor = colors.onSurface.copy(alpha = 0.85f * sweepAlpha)

                listOf(2, 2, 4).forEach {
                    drawGlowPath(
                        points = sweepPoints,
                        blurRadius = blurRadius,
                        color = sweepColor,
                        strokeWidth = strokeWidth * it
                    )
                }
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CardLockOnOverlayPreview() {
    val density = LocalDensity.current
    val chain = remember(density) {
        with(density) { createPreviewImageSpaceChain(393, 852, 393.dp.roundToPx(), 852.dp.roundToPx()) }
    }
    val sampleDetection = CardDetection(
        lockOnProgress = 0.5f,
        id = 12345L,
        card = Feature(ImageBox.from2P(40, 328, 353, 524), 0.95f, 0),
        features = emptyList(),
        lockingStatus = LockingStatus.LockingCard,
        focusScore = 150f,
    )
    val scope = remember(chain) {
        object : CardDetectorOverlayScope {
            override val detectionState = sampleDetection
            override val detectionSequence = 1L
            override val latestBestDetection = sampleDetection
            override val imageSpaceChain = chain
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
    with(scope) {
        Box(Modifier.fillMaxSize().checkerboardBackground()) {
            CardLockOnOverlay()
        }
    }
}
