package com.apexfission.android.carddetector.ui.overlays

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A modifier that draws a checkerboard background with shades of grey, 
 * typically used in Previews to simulate transparency/lack of video feed.
 */
internal fun Modifier.checkerboardBackground(gridSize: Dp = 16.dp): Modifier = this.drawBehind {
    val dark = Color(0xFF666666)
    val light = Color(0xFF999999)
    val sizePx = gridSize.toPx()
    val cols = (size.width / sizePx).toInt() + 1
    val rows = (size.height / sizePx).toInt() + 1
    
    for (row in 0 until rows) {
        for (col in 0 until cols) {
            val isDark = (row + col) % 2 == 0
            drawRect(
                color = if (isDark) dark else light,
                topLeft = Offset(col * sizePx, row * sizePx),
                size = Size(sizePx, sizePx)
            )
        }
    }
}
