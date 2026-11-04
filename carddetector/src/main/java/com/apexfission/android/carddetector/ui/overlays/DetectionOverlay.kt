package com.apexfission.android.carddetector.ui.overlays

import androidx.compose.material3.MaterialTheme
import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apexfission.android.carddetector.domain.tflite.model.CardDetection
import com.apexfission.android.carddetector.domain.tflite.model.LockingStatus
import com.apexfission.android.carddetector.ui.camerapreview.createPreviewImageSpaceChain
import com.apexfission.android.carddetector.ui.detector.CardDetectorOverlayScope
import com.apexfission.android.math.models.ImageBox
import com.apexfission.android.math.models.ImageSpaceChain
import com.apexfission.android.math.transformations.translate
import com.apexfission.android.yolo.engine.Feature

/**
 * Component.
 * Scoped variant of [DetectionOverlay] using [CardDetectorOverlayScope].
 *
 * @param showClassNames Whether to draw class names above bounding boxes.
 * @param classLabels Map of class IDs to human-readable labels.
 */
@Composable
fun CardDetectorOverlayScope.DetectionOverlay(
    showClassNames: Boolean = false,
    classLabels: Map<Int, String> = this.classLabels
) {
    val spaceChain = imageSpaceChain ?: return
    DetectionOverlay(
        cardDetection = detectionState,
        imageSpaceChain = spaceChain,
        showClassNames = showClassNames,
        classLabels = classLabels
    )
}

/**
 * Component.
 * Standalone variant of [DetectionOverlay] taking explicit parameters.
 *
 * @param cardDetection The [CardDetection] result from the ViewModel.
 * @param showClassNames A boolean flag. If `true`, a text label with the object's class name and
 *                       confidence score is drawn above each bounding box.
 * @param classLabels A map that translates integer class IDs from the model into human-readable
 *                    string labels for display.
 */
@Composable
fun DetectionOverlay(
    cardDetection: CardDetection?, imageSpaceChain: ImageSpaceChain, showClassNames: Boolean, classLabels: Map<Int, String>
) {
    val textMeasurer = rememberTextMeasurer()
    val colors = MaterialTheme.colorScheme
    val labelStyle = MaterialTheme.typography.labelSmall

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val features = cardDetection?.let { it.features + it.card } ?: emptyList()


        if (cardDetection == null) {
            val errorTextStyle = labelStyle.copy(color = colors.onErrorContainer, background = colors.errorContainer)
            val errorLog = "cardDetection not detected"
            val errorLogLayout = textMeasurer.measure(errorLog, errorTextStyle)
            drawText(textLayoutResult = errorLogLayout, topLeft = Offset(10f, 60f))
        } else {
            val infoTextStyle = labelStyle.copy(color = colors.onSurface, background = colors.surface)
            val infoLog =
                "lockOnProgress: ${cardDetection.lockOnProgress} | id: ${cardDetection.id} | confidence: ${(cardDetection.card.confidence * 100).toInt()}%"
            val infoLogLayout = textMeasurer.measure(infoLog, infoTextStyle)
            drawText(textLayoutResult = infoLogLayout, topLeft = Offset(10f, 60f))
        }

        features.forEach { feature ->
            val box = feature.box.translate(imageSpaceChain)

            Log.d("DetectionOverlay2", "box: $box, classId: ${feature.classId}, confidence: ${feature.confidence}")

            drawRect(
                color = colors.primary,
                topLeft = Offset(box.x.toFloat(), box.y.toFloat()),
                size = Size(box.width.toFloat(), box.height.toFloat()),
                style = Stroke(width = 2.dp.toPx())
            )

            if (showClassNames) {
                val label = classLabels[feature.classId] ?: "ID: ${feature.classId}"
                val displayString = "$label (${(feature.confidence * 100).toInt()}%)"
                val labelTextStyle = labelStyle.copy(color = colors.onSurface, background = colors.surface)
                val textLayout = textMeasurer.measure(displayString, labelTextStyle)

                val textTop = (box.y.toFloat() - textLayout.size.height).coerceAtLeast(0f)
                drawText(textLayoutResult = textLayout, topLeft = Offset(box.x.toFloat(), textTop))
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DetectionOverlayPreview() {
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

    Box(Modifier.fillMaxSize().checkerboardBackground()) {
        DetectionOverlay(
            cardDetection = sampleDetection,
            imageSpaceChain = chain,
            showClassNames = true,
            classLabels = mapOf(0 to "SAMPLE_CARD")
        )
    }
}
