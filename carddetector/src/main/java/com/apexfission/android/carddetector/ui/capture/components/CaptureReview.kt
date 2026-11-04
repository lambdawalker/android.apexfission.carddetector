package com.apexfission.android.carddetector.ui.capture.components

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.carddetector.domain.tflite.model.CardDetection
import com.apexfission.android.carddetector.domain.tflite.model.LockingStatus
import com.apexfission.android.math.models.ImageBox
import com.apexfission.android.yolo.engine.Feature
import com.apexfission.android.carddetector.ui.capture.CapturedCardSide
import com.apexfission.android.carddetector.ui.capture.CardCaptureStrings
import com.apexfission.android.carddetector.ui.capture.CardSide
import com.apexfission.android.carddetector.ui.overlays.components.OverlayHeader
import com.apexfission.android.carddetector.ui.overlays.components.OverlayTitle
import androidx.core.graphics.createBitmap

/** Component. */
@Composable
internal fun CaptureReview(
    front: CapturedCardSide, back: CapturedCardSide? = null, strings: CardCaptureStrings,
    onBack: () -> Unit, onRetake: (CardSide) -> Unit, onConfirm: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OverlayHeader(title = strings.title, onBack = onBack, exitContentDescription = strings.exit, contentColor = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(16.dp))
            OverlayTitle(if (back != null) strings.review else strings.captured, contentColor = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(
                strings.reviewInstruction,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(16.dp))
            val sidesToReview = listOfNotNull(CardSide.Front to front, back?.let { CardSide.Back to it })
            sidesToReview.forEach { (side, image) ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.large)
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
                ) {
                    FlowRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            if (side == CardSide.Front) strings.front else strings.back,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        TextButton(
                            onClick = { onRetake(side) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (side == CardSide.Front) strings.retakeFront else strings.retakeBack,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Image(
                        image.bitmap.asImageBitmap(),
                        if (side == CardSide.Front) strings.front else strings.back,
                        Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Column(
            Modifier
                .fillMaxWidth()
                .shadow(elevation = 4.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onConfirm,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(strings.use)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = strings.privacyTerms,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(name = "Light Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CaptureReviewPreview() {
    val front = CapturedCardSide(
        CardDetection(1f, 1L, Feature(ImageBox.from2P(0, 0, 100, 60), 1f, 0), emptyList(), LockingStatus.CardLocked),
        createBitmap(100, 60)
    )
    val back = CapturedCardSide(
        CardDetection(1f, 1L, Feature(ImageBox.from2P(0, 0, 100, 60), 1f, 0), emptyList(), LockingStatus.CardLocked),
        createBitmap(100, 60)
    )
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
        Surface(color = MaterialTheme.colorScheme.background) {
            CaptureReview(
                front = front,
                back = back,
                strings = CardCaptureStrings(
                    title = "Scan your document",
                    front = "Front of your card",
                    back = "Back of your card",
                    frontInstruction = "Place the front of your card inside the square.",
                    backInstruction = "Place the back of your card inside the square.",
                    frontSaved = "Front captured",
                    turnOver = "Turn your card over to capture the back.",
                    continueToBack = "Continue to back",
                    review = "Both sides captured",
                    reviewInstruction = "Check that both images are clear and all details are readable.",
                    use = "Use these photos",
                    retakeFront = "Retake front",
                    retakeBack = "Retake back",
                    find = "Looking for a card",
                    focus = "Hold steady · finding focus",
                    ready = "In focus · hold steady",
                    manual = "Tap to capture",
                    auto = "Auto-capture on",
                    shutter = "Capture this side",
                    exit = "Back",
                    flash = "Toggle flashlight",
                    stepFront = "1 / 2 · Front",
                    stepBack = "2 / 2 · Back",
                    complete = "2 / 2 · Complete",
                    done = "Photos ready",
                    privacyTerms = "By continuing, you agree to the Attestra Privacy Terms."
                ),
                onBack = {},
                onRetake = {},
                onConfirm = {}
            )
        }
    }
}
