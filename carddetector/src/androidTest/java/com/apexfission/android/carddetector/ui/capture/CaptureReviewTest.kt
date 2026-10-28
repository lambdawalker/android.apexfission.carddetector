package com.apexfission.android.carddetector.ui.capture

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.apexfission.android.carddetector.domain.tflite.model.CardDetection
import com.apexfission.android.carddetector.domain.tflite.model.LockingStatus
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
        compose.onNodeWithText(copy.complete).assertIsDisplayed()
        compose.onNodeWithText(copy.retakeFront).performClick()
        compose.runOnIdle { assertEquals(CardSide.Front, retaken) }
        compose.onNodeWithText(copy.retakeBack).performClick()
        compose.runOnIdle { assertEquals(CardSide.Back, retaken) }
        compose.onNodeWithText(copy.use).performClick()
        compose.runOnIdle { assertTrue(confirmed) }
    }

    @Test fun turnOverWaitsForExplicitContinue() {
        val front = card()
        var continued = false
        lateinit var copy: CardCaptureStrings
        compose.setContent {
            copy = defaultCardCaptureStrings()
            MaterialTheme { CaptureTurnOver(front, copy, {}, { continued = true }) }
        }
        compose.onNodeWithText(copy.stepFront).assertIsDisplayed()
        compose.runOnIdle { assertEquals(false, continued) }
        compose.onNodeWithText(copy.continueToBack).performClick()
        compose.runOnIdle { assertTrue(continued) }
    }
}
