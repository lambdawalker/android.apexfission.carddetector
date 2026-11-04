package com.apexfission.android.carddetector.ui.capture.components

import android.content.res.Configuration
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.carddetector.domain.tflite.model.CardDetection
import com.apexfission.android.carddetector.domain.tflite.model.LockingStatus
import com.apexfission.android.math.models.ImageBox
import com.apexfission.android.yolo.engine.Feature
import com.apexfission.android.carddetector.ui.capture.CapturedCardSide
import com.apexfission.android.carddetector.ui.capture.CardCaptureStrings
import com.apexfission.android.carddetector.ui.capture.defaultCardCaptureStrings
import com.apexfission.android.carddetector.ui.overlays.components.OverlayHeader
import com.apexfission.android.carddetector.ui.overlays.components.OverlayTitle
import androidx.core.graphics.createBitmap

/** Component. */
@Composable
internal fun CaptureTurnOver(
    front: CapturedCardSide, strings: CardCaptureStrings,
    onBack: () -> Unit, onRetake: () -> Unit, onContinue: () -> Unit
) {
    Column(Modifier.fillMaxSize().safeDrawingPadding(), horizontalAlignment=Alignment.CenterHorizontally) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OverlayHeader(title = strings.title, onBack = onBack, exitContentDescription = strings.exit, contentColor = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(24.dp))
            OverlayTitle(strings.frontSaved, contentColor = MaterialTheme.colorScheme.primary)
            Text(strings.turnOver, style = MaterialTheme.typography.bodyLarge, color=MaterialTheme.colorScheme.onSurface, textAlign=TextAlign.Center, modifier=Modifier.padding(16.dp))

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.large)
                    .padding(16.dp)
            ) {
                Image(
                    front.bitmap.asImageBitmap(),
                    strings.frontSaved,
                    Modifier.fillMaxWidth().height(180.dp),
                    contentScale=ContentScale.Fit
                )
            }

        }
        Text(strings.stepFront, color=MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(16.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(shape = MaterialTheme.shapes.small, onClick=onContinue, modifier=Modifier.fillMaxWidth()) {
                Text(strings.continueToBack)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(shape = MaterialTheme.shapes.small, onClick = onRetake) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(strings.retakeFront, color = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Preview(name = "Light Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CaptureTurnOverPreview() {
    val front = CapturedCardSide(
        CardDetection(1f, 1L, Feature(ImageBox.from2P(0, 0, 100, 60), 1f, 0), emptyList(), LockingStatus.CardLocked),
        createBitmap(100, 60)
    )
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
        Surface(color = MaterialTheme.colorScheme.background) {
            CaptureTurnOver(
                front = front,
                strings = defaultCardCaptureStrings(),
                onBack = {},
                onRetake = {},
                onContinue = {}
            )
        }
    }
}
