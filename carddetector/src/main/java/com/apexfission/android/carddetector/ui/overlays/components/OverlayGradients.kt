package com.apexfission.android.carddetector.ui.overlays.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.apexfission.android.carddetector.ui.overlays.checkerboardBackground

@Composable
fun OverlayTopGradient(modifier: Modifier = Modifier, height: Dp = 180.dp) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black,
                        Color.Black.copy(alpha = 0.75f),
                        Color.Transparent
                    )
                )
            )
    )
}

@Composable
fun OverlayBottomGradient(modifier: Modifier = Modifier, height: Dp = 280.dp) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.75f),
                        Color.Black
                    )
                )
            )
    )
}

@Preview(name = "Light Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OverlayTopGradientPreview() {
    Box(Modifier.checkerboardBackground()) {
        OverlayTopGradient()
    }
}

@Preview(name = "Light Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OverlayBottomGradientPreview() {
    Box(Modifier.checkerboardBackground()) {
        OverlayBottomGradient()
    }
}
