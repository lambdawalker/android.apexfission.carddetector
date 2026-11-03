package com.apexfission.android.carddetector.ui.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.apexfission.android.carddetector.domain.tflite.model.CardDetection
import com.apexfission.android.carddetector.domain.tflite.model.LockingStatus
import com.apexfission.android.carddetector.ui.camerapreview.CameraPreset
import com.apexfission.android.carddetector.ui.capture.CardSide
import com.apexfission.android.carddetector.ui.capture.defaultCardCaptureStrings
import com.apexfission.android.carddetector.ui.detector.CardDetectorOverlayScope
import com.apexfission.android.carddetector.ui.detector.CardDetectorPreset
import com.apexfission.android.math.models.ImageBox
import com.apexfission.android.yolo.engine.Feature
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.roundToInt

class CaptureOverlayTest {
    @get:Rule val compose = createComposeRule()

    @Test fun singleSideGuideFadesAndDoesNotReturnOnMiss() = verifyGuide(null)
    @Test fun frontGuideFadesAndResetsForANewAttempt() = verifyGuide(CardSide.Front)
    @Test fun backGuideFadesAndResetsForANewAttempt() = verifyGuide(CardSide.Back)

    private fun verifyGuide(side: CardSide?) {
        val detected = mutableStateOf(false)
        val attempt = mutableIntStateOf(0)
        val sample = CardDetection(1f, 1L, Feature(ImageBox.from2P(30, 120, 200, 230), 1f, 0),
            emptyList(), LockingStatus.CardLocked)
        val scope = object : CardDetectorOverlayScope {
            override val detectionState get() = sample.takeIf { detected.value }
            override val detectionSequence get() = if (detected.value) 1L else 2L
            override val latestBestDetection get() = detectionState
            override val imageSpaceChain = null
            override val captureEnabled get() = detected.value
            override val detectorPreset = CardDetectorPreset.HighPerformance
            override val cameraPreset = CameraPreset.HighResolution
            override val classLabels = emptyMap<Int, String>()
            override val flashlightEnabled = false
            override val flashlightAvailable = false
            override fun goBack() = Unit
            override fun toggleFlashlight() = Unit
            override fun capture() = Unit
        }
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(240.dp, 480.dp).background(Color.Black)) {
                    key(attempt.intValue) {
                        with(scope) {
                            if (side == null) IdCaptureOverlay()
                            else {
                                val strings = defaultCardCaptureStrings()
                                IdCaptureOverlay(
                                    config = IdCaptureOverlayConfig(
                                        title = strings.title,
                                        instructionTitle = if (side == CardSide.Front) strings.front else strings.back,
                                        instructionSubTitle = if (side == CardSide.Front) strings.frontInstruction else strings.backInstruction
                                    ),
                                    autoCaptureEnabled = true,
                                    showInitialGuide = true,
                                    canCapture = detected.value,
                                    showAutoCaptureToggle = true,
                                    step = if (side == CardSide.Front) strings.stepFront else strings.stepBack
                                )
                            }
                        }
                    }
                }
            }
        }
        val guide = compose.onNodeWithTag("initial-capture-guide")
        val before = borderBrightness(guide.assertExists().captureToImage())
        assertTrue("The initial white guide must be visible", before > 0.1f)
        compose.runOnIdle { detected.value = true }
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(125)
        val halfway = borderBrightness(guide.assertExists().captureToImage())
        assertTrue("Guide must fade, not disappear immediately: $before -> $halfway", halfway > 0.01f && halfway < before * 0.9f)
        compose.mainClock.advanceTimeBy(300)
        guide.assertDoesNotExist()
        compose.runOnIdle { detected.value = false }
        compose.mainClock.advanceTimeByFrame()
        guide.assertDoesNotExist()
        compose.runOnIdle { attempt.intValue++ }
        compose.mainClock.advanceTimeByFrame()
        guide.assertExists()
    }

    private fun borderBrightness(image: ImageBitmap): Float {
        val pixels = image.toPixelMap()
        val x = (image.width * 0.075f).roundToInt()
        return (x - 4..x + 4).maxOf { pixels[it, image.height / 2].red }
    }
}
