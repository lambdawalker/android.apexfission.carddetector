package com.apexfission.android.carddetector.ui.capture.components

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.apexfission.android.carddetector.ui.capture.defaultCardCaptureStrings
import androidx.core.graphics.createBitmap

/** Component. */
@Composable
internal fun CaptureReview(
    front: CapturedCardSide, back: CapturedCardSide, strings: CardCaptureStrings,
    onBack: () -> Unit, onRetake: (CardSide) -> Unit, onConfirm: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CaptureHeader(strings, onBack)
        Spacer(Modifier.height(16.dp))
        CaptureTitle(strings.review)
        Spacer(Modifier.height(8.dp))
        Text(
            strings.reviewInstruction,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(16.dp))
        listOf(CardSide.Front to front, CardSide.Back to back).forEach { (side, image) ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (side == CardSide.Front) strings.front else strings.back,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    TextButton(onClick = { onRetake(side) }) {
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
        Text(strings.complete, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(strings.use, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
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
                strings = defaultCardCaptureStrings(),
                onBack = {},
                onRetake = {},
                onConfirm = {}
            )
        }
    }
}
