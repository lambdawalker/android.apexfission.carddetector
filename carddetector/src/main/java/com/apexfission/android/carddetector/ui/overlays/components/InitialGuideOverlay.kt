package com.apexfission.android.carddetector.ui.overlays.components

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apexfission.android.carddetector.ui.overlays.checkerboardBackground

@Composable
fun InitialGuideOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val edge = minOf(size.width * 0.85f, size.height * 0.48f)
        drawRoundRect(
            color = Color.White.copy(alpha = 0.5f),
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
