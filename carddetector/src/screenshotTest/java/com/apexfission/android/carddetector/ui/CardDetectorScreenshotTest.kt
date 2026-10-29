package com.apexfission.android.carddetector.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.apexfission.android.carddetector.domain.tflite.model.CardDetection
import com.apexfission.android.carddetector.domain.tflite.model.LockingStatus
import com.apexfission.android.carddetector.domain.tflite.model.DetectionSource
import com.apexfission.android.carddetector.ui.capture.CaptureReview
import com.apexfission.android.carddetector.ui.capture.CaptureTurnOver
import com.apexfission.android.carddetector.ui.capture.CapturedCardSide
import com.apexfission.android.carddetector.ui.capture.defaultCardCaptureStrings
import com.apexfission.android.carddetector.ui.detector.CardDetectorOverlayScope
import com.apexfission.android.carddetector.ui.detector.CardDetectorPreset
import com.apexfission.android.carddetector.ui.camerapreview.CameraPreset
import com.apexfission.android.carddetector.ui.overlays.CardLockOnOverlay
import com.apexfission.android.carddetector.ui.overlays.DebugOverlay
import com.apexfission.android.carddetector.ui.overlays.DetectionOverlay
import com.apexfission.android.carddetector.ui.overlays.IdCaptureOverlay
import com.apexfission.android.math.models.ImageBox
import com.apexfission.android.math.models.ImageSpaceChain
import com.apexfission.android.yolo.engine.Feature

/**
 * Screenshot test suite to generate images of the screens and composables.
 * This is primarily for documentation purposes, using the screenshotTest source set. 
 */
class CardDetectorScreenshotTest {

    private val dummyBitmap = Bitmap.createBitmap(400, 250, Bitmap.Config.ARGB_8888)

    private val dummyFeature = Feature(
        box = ImageBox(100u, 100u, 900u, 600u, 800u, 500u),
        confidence = 0.95f,
        classId = 0
    )

    private val dummyDetection = CardDetection(
        lockOnProgress = 1.0f,
        id = 12345L,
        card = dummyFeature,
        features = emptyList(),
        lockingStatus = LockingStatus.CardLocked,
        detectionSource = DetectionSource.Yolo,
        focusScore = 0.8f,
        observedAtElapsedRealtimeMs = 0L
    )

    private val mockScope = object : CardDetectorOverlayScope {
        override val detectionState: CardDetection? = dummyDetection
        override val latestBestDetection: CardDetection? = dummyDetection
        override val imageSpaceChain: ImageSpaceChain? = null
        override val captureEnabled: Boolean = true
        override val detectorPreset: CardDetectorPreset = CardDetectorPreset.HighPerformance
        override val cameraPreset: CameraPreset = CameraPreset.HighResolution
        override val classLabels: Map<Int, String> = mapOf(0 to "ID_CARD")
        override val flashlightEnabled: Boolean = false
        override val flashlightAvailable: Boolean = true
        override fun goBack() {}
        override fun toggleFlashlight() {}
        override fun capture() {}
    }

    private val emptyScope = object : CardDetectorOverlayScope {
        override val detectionState: CardDetection? = null
        override val latestBestDetection: CardDetection? = null
        override val imageSpaceChain: ImageSpaceChain? = null
        override val captureEnabled: Boolean = false
        override val detectorPreset: CardDetectorPreset = CardDetectorPreset.HighPerformance
        override val cameraPreset: CameraPreset = CameraPreset.HighResolution
        override val classLabels: Map<Int, String> = emptyMap()
        override val flashlightEnabled: Boolean = false
        override val flashlightAvailable: Boolean = true
        override fun goBack() {}
        override fun toggleFlashlight() {}
        override fun capture() {}
    }

    @Composable
    private fun SimulatedCameraFeed(content: @Composable () -> Unit) {
        Box(modifier = Modifier.fillMaxSize().background(Color.DarkGray)) {
            content()
        }
    }

    @PreviewTest
    @Preview(name = "IdCaptureOverlay - Detected", widthDp = 393, heightDp = 852, showBackground = true)
    @Composable
    fun IdCaptureOverlayPreview() {
        MaterialTheme {
            SimulatedCameraFeed {
                mockScope.IdCaptureOverlay()
            }
        }
    }

    @PreviewTest
    @Preview(name = "IdCaptureOverlay - Idle", widthDp = 393, heightDp = 852, showBackground = true)
    @Composable
    fun IdCaptureOverlayIdlePreview() {
        MaterialTheme {
            SimulatedCameraFeed {
                emptyScope.IdCaptureOverlay()
            }
        }
    }

    @PreviewTest
    @Preview(name = "DebugOverlay", widthDp = 393, heightDp = 852, showBackground = true)
    @Composable
    fun DebugOverlayPreview() {
        MaterialTheme {
            SimulatedCameraFeed {
                mockScope.DebugOverlay()
            }
        }
    }

    @PreviewTest
    @Preview(name = "DetectionOverlay", widthDp = 393, heightDp = 852, showBackground = true)
    @Composable
    fun DetectionOverlayPreview() {
        MaterialTheme {
            SimulatedCameraFeed {
                mockScope.DetectionOverlay(showClassNames = true, classLabels = mapOf(0 to "ID_CARD"))
            }
        }
    }

    @PreviewTest
    @Preview(name = "CardLockOnOverlay", widthDp = 393, heightDp = 852, showBackground = true)
    @Composable
    fun CardLockOnOverlayPreview() {
        MaterialTheme {
            SimulatedCameraFeed {
                mockScope.CardLockOnOverlay()
            }
        }
    }

    @PreviewTest
    @Preview(name = "CaptureTurnOver Screen", widthDp = 393, heightDp = 852, showBackground = true)
    @Composable
    fun CaptureTurnOverPreview() {
        MaterialTheme {
            Box(Modifier.fillMaxSize().background(Color(0xFF111D27))) {
                CaptureTurnOver(
                    front = CapturedCardSide(dummyDetection, dummyBitmap),
                    strings = defaultCardCaptureStrings(),
                    onBack = {},
                    onContinue = {}
                )
            }
        }
    }

    @PreviewTest
    @Preview(name = "CaptureReview Screen", widthDp = 393, heightDp = 852, showBackground = true)
    @Composable
    fun CaptureReviewPreview() {
        MaterialTheme {
            Box(Modifier.fillMaxSize().background(Color(0xFF111D27))) {
                CaptureReview(
                    front = CapturedCardSide(dummyDetection, dummyBitmap),
                    back = CapturedCardSide(dummyDetection, dummyBitmap),
                    strings = defaultCardCaptureStrings(),
                    onBack = {},
                    onRetake = {},
                    onConfirm = {}
                )
            }
        }
    }
}
