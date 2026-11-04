package com.apexfission.android.carddetector.ui.capture

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import com.apexfission.android.carddetector.domain.tflite.model.CardDetection
import com.apexfission.android.carddetector.domain.tflite.model.LockingStatus
import com.apexfission.android.carddetector.ui.capture.components.CaptureReview
import com.apexfission.android.carddetector.ui.capture.components.CaptureTurnOver
import com.apexfission.android.math.models.ImageBox
import com.apexfission.android.yolo.engine.Feature
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CaptureReviewTest {
    @get:Rule val compose = createComposeRule()
    private fun card() = CapturedCardSide(
        CardDetection(1f, 1L, Feature(ImageBox.from2P(0, 0, 100, 60), 1f, 0), emptyList(), LockingStatus.CardLocked),
        Bitmap.createBitmap(100, 60, Bitmap.Config.ARGB_8888),
    )

    @Test fun reviewExposesSeparateRetakesAndConfirmation() {
        val front = card(); val back = card()
        var retaken: CardSide? = null
        var confirmed = false
        lateinit var copy: CardCaptureStrings
        compose.setContent {
            copy = defaultCardCaptureStrings()
            MaterialTheme { CaptureReview(front, back, copy, {}, { retaken = it }, { confirmed = true }) }
        }
        compose.onNodeWithText(copy.review).assertIsDisplayed()
        compose.onNodeWithText(copy.retakeFront).performClick()
        compose.runOnIdle { assertEquals(CardSide.Front, retaken) }
        compose.onNodeWithText(copy.retakeBack).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(CardSide.Back, retaken) }
        compose.onNodeWithText(copy.use).performClick()
        compose.runOnIdle { assertTrue(confirmed) }
    }

    @Test fun singleReviewExposesOnlyFrontRetakeAndConfirmation() {
        val front = card()
        var retaken: CardSide? = null
        var confirmed = false
        lateinit var copy: CardCaptureStrings
        compose.setContent {
            copy = defaultCardCaptureStrings()
            MaterialTheme { CaptureReview(front, strings = copy, onBack = {},
                onRetake = { retaken = it }, onConfirm = { confirmed = true }) }
        }
        compose.onNodeWithText(copy.captured).assertIsDisplayed()
        compose.onNodeWithText(copy.retakeBack).assertDoesNotExist()
        compose.onNodeWithText(copy.retakeFront).performClick()
        compose.runOnIdle { assertEquals(CardSide.Front, retaken); assertEquals(false, confirmed) }
        compose.onNodeWithText(copy.use).performClick()
        compose.runOnIdle { assertTrue(confirmed) }
    }

    @Test fun largeFontSingleReviewKeepsRetakeReadable() {
        val front = card()
        var retaken = false
        lateinit var copy: CardCaptureStrings
        compose.setContent {
            val density = LocalDensity.current
            copy = defaultCardCaptureStrings().copy(front = "Frente de tu identificación", retakeFront = "Repetir frente")
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f)) {
                MaterialTheme { CaptureReview(front, strings = copy, onBack = {},
                    onRetake = { retaken = true }, onConfirm = {}) }
            }
        }
        val retake = compose.onNodeWithText(copy.retakeFront).performScrollTo()
        val bounds = retake.fetchSemanticsNode().boundsInRoot
        assertTrue("Retake text should not wrap into a vertical letter column", bounds.width > bounds.height)
        retake.performClick()
        compose.runOnIdle { assertTrue(retaken) }
    }

    @Test fun turnOverWaitsForExplicitContinue() {
        val front = card()
        var continued = false
        var retaken = false
        lateinit var copy: CardCaptureStrings
        compose.setContent {
            copy = defaultCardCaptureStrings()
            MaterialTheme { CaptureTurnOver(front, copy, {}, { retaken = true }, { continued = true }) }
        }
        compose.onNodeWithText(copy.stepFront).assertIsDisplayed()
        compose.runOnIdle { assertEquals(false, continued) }
        compose.onNodeWithText(copy.retakeFront).performClick()
        compose.runOnIdle { assertTrue(retaken); assertEquals(false, continued) }
        compose.onNodeWithText(copy.continueToBack).performClick()
        compose.runOnIdle { assertTrue(continued) }
    }

    @Test fun reviewDisplaysHostPrivacyNotice() {
        val front = card()
        val notice = "Host privacy notice for this capture"
        lateinit var original: CardCaptureStrings
        compose.setContent {
            original = defaultCardCaptureStrings()
            MaterialTheme { CaptureReview(front, strings = original.copy(privacyTerms = notice),
                onBack = {}, onRetake = {}, onConfirm = {}) }
        }
        compose.onNodeWithText(notice).assertIsDisplayed()
        compose.onNodeWithText(original.privacyTerms).assertDoesNotExist()
    }
}
