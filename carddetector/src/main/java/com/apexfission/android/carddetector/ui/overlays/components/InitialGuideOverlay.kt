package com.apexfission.android.carddetector.ui.overlays.components

import androidx.compose.material3.MaterialTheme
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.apexfission.android.carddetector.ui.overlays.checkerboardBackground

internal const val InitialGuideFadeMillis = 250

/** Keep the canvas mounted for the fade; never restore it on a missed detector frame. */
@Composable
internal fun InitialCaptureGuide(requested: Boolean, hasDetection: Boolean) {
    val state = remember { InitialGuideState() }
    AnimatedVisibility(
        visible = state.shouldShow(requested, hasDetection),
        modifier = Modifier.fillMaxSize(),
        enter = EnterTransition.None,
        exit = fadeOut(animationSpec = tween(InitialGuideFadeMillis)),
    ) {
        InitialGuideOverlay(Modifier.testTag("initial-capture-guide"))
    }
}

@Composable
fun InitialGuideOverlay(modifier: Modifier = Modifier) {
    val guideColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier.fillMaxSize()) {
        val edge = minOf(size.width * 0.85f, size.height * 0.48f)
        drawRoundRect(
            color = guideColor.copy(alpha = 0.75f),
            topLeft = Offset((size.width - edge) / 2, (size.height - edge) / 2),
            size = Size(edge, edge),
            cornerRadius = CornerRadius(18.dp.toPx()),
            style = Stroke(2.dp.toPx())
        )
    }
}

@Preview(name = "Light Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InitialGuideOverlayPreview() {
    Box(Modifier.fillMaxSize().checkerboardBackground()) {
        InitialGuideOverlay()
    }
}
