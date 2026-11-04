package com.apexfission.android.carddetector.ui.overlays

import android.graphics.Bitmap
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.apexfission.android.carddetector.domain.tflite.model.CardDetection
import com.apexfission.android.carddetector.ui.camerapreview.CameraPreset
import com.apexfission.android.carddetector.ui.capture.*
import com.apexfission.android.carddetector.ui.capture.components.CaptureTurnOver
import com.apexfission.android.carddetector.domain.tflite.model.LockingStatus
import com.apexfission.android.math.models.ImageBox
import com.apexfission.android.yolo.engine.Feature
import com.apexfission.android.carddetector.ui.detector.CardDetectorOverlayScope
import com.apexfission.android.carddetector.ui.detector.CardDetectorPreset
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CaptureLayoutTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val scope = object : CardDetectorOverlayScope {
        override val detectionState: CardDetection? = null
        override val latestBestDetection: CardDetection? = null
        override val imageSpaceChain = null
        override val captureEnabled = true
        override val detectorPreset = CardDetectorPreset.HighPerformance
        override val cameraPreset = CameraPreset.HighResolution
        override val classLabels = emptyMap<Int, String>()
        override val flashlightEnabled = false
        override val flashlightAvailable = false
        override fun goBack() = Unit
        override fun toggleFlashlight() = Unit
        override fun capture() = Unit
    }

    @Test fun narrowLongTextKeepsSingleSideToggleAndShutterReachable() {
        var captures = 0
        val auto = mutableStateOf(true)
        lateinit var strings: CardCaptureStrings
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(240.dp, 480.dp)) {
                    strings = defaultCardCaptureStrings().copy(
                        singleInstruction = "Coloca la identificación completa dentro de la guía y mantén visibles todos sus bordes sin reflejos",
                        find = "Buscando una identificación para completar el registro de la familia con buena iluminación",
                    )
                    scope.IdCaptureOverlay(
                        config = IdCaptureOverlayConfig(title = strings.title,
                            instructionTitle = strings.singleInstruction, instructionSubTitle = strings.find),
                        strings = strings, canCapture = true, autoCaptureEnabled = auto.value,
                        showInitialGuide = true, showAutoCaptureToggle = true,
                        onToggleAutoCapture = { auto.value = !auto.value }, onCapture = { captures++ })
                }
            }
        }
        compose.onNodeWithContentDescription(strings.autoToggle).performScrollTo().performClick()
        compose.runOnIdle { assertFalse(auto.value) }
        compose.onNodeWithContentDescription(strings.shutter).performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, captures) }
    }

    @Test fun largeFontTwoSideCaptureCanScrollToShutter() {
        var captures = 0
        lateinit var strings: CardCaptureStrings
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f)) {
                MaterialTheme {
                    Box(Modifier.size(280.dp, 480.dp)) {
                        strings = defaultCardCaptureStrings().copy(frontInstruction = "Coloca el frente de tu identificación completo dentro del recuadro con buena iluminación y sin reflejos")
                        scope.IdCaptureOverlay(
                            config = IdCaptureOverlayConfig(
                                title = strings.title,
                                instructionTitle = strings.front,
                                instructionSubTitle = strings.find + " · " + strings.frontInstruction
                            ),
                            autoCaptureEnabled = true,
                            showInitialGuide = true,
                            canCapture = true,
                            showAutoCaptureToggle = true,
                            step = strings.stepFront,
                            strings = strings,
                            onCapture = { captures++ }
                        )
                    }
                }
            }
        }
        compose.onNodeWithContentDescription(strings.shutter).performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, captures) }
    }

    @Test fun landscapeKeepsSingleSideShutterVisible() {
        compose.runOnUiThread { compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        compose.waitUntil(10_000) { compose.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE }
        lateinit var strings: CardCaptureStrings
        compose.setContent {
            MaterialTheme {
                Box(Modifier.fillMaxSize()) {
                    strings = defaultCardCaptureStrings()
                    scope.IdCaptureOverlay(
                        config = IdCaptureOverlayConfig(title = strings.title,
                            instructionTitle = strings.singleInstruction, instructionSubTitle = strings.readyToCapture),
                        strings = strings, canCapture = true, autoCaptureEnabled = true,
                        showInitialGuide = false, showAutoCaptureToggle = true, onCapture = {})
                }
            }
        }
        compose.onNodeWithContentDescription(strings.shutter).assertIsDisplayed()
    }

    @Test fun landscapeTurnOverCanScrollToFullImageAndKeepActionsVisible() {
        compose.runOnUiThread { compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        compose.waitUntil(10_000) { compose.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE }
        val front = CapturedCardSide(
            CardDetection(1f, 1L, Feature(ImageBox.from2P(0, 0, 100, 60), 1f, 0), emptyList(), LockingStatus.CardLocked),
            Bitmap.createBitmap(100, 60, Bitmap.Config.ARGB_8888),
        )
        lateinit var strings: CardCaptureStrings
        compose.setContent {
            strings = defaultCardCaptureStrings()
            MaterialTheme { CaptureTurnOver(front, strings, {}, {}, {}) }
        }
        compose.onNodeWithContentDescription(strings.frontSaved).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(strings.continueToBack).assertIsDisplayed()
        compose.onNodeWithText(strings.retakeFront).assertIsDisplayed()
        compose.runOnIdle { front.bitmap.recycle() }
    }
}
