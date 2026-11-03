package com.apexfission.android.carddetector.ui.overlays.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apexfission.android.carddetector.ui.overlays.CaptureShutterButton
import com.apexfission.android.carddetector.ui.overlays.checkerboardBackground

@Composable
fun CaptureControlsRow(
    isCaptureEnabled: Boolean,
    onCapture: () -> Unit,
    autoCaptureEnabled: Boolean,
    onToggleAutoCapture: () -> Unit,
    flashlightAvailable: Boolean,
    flashlightEnabled: Boolean,
    onToggleFlashlight: () -> Unit,
    autoToggleContentDescription: String,
    flashContentDescription: String,
    shutterContentDescription: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconToggleButton(
            checked = autoCaptureEnabled,
            onCheckedChange = { onToggleAutoCapture() },
            modifier = Modifier
                .size(48.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.AutoMode,
                contentDescription = autoToggleContentDescription,
                tint = if (autoCaptureEnabled) Color(0xFF75E8CD) else Color.White.copy(alpha = 0.6f)
            )
        }
        
        CaptureShutterButton(
            isCaptureEnabled = isCaptureEnabled,
            onClick = onCapture,
            modifier = Modifier.semantics { contentDescription = shutterContentDescription }
        )
        
        if (flashlightAvailable) {
            IconToggleButton(
                checked = flashlightEnabled,
                onCheckedChange = { onToggleFlashlight() },
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = if (flashlightEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = flashContentDescription,
                    tint = if (flashlightEnabled) Color.Yellow else Color.White
                )
            }
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}

@Preview(name = "Light Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CaptureControlsRowPreview() {
    Box(Modifier.checkerboardBackground()) {
        CaptureControlsRow(
            isCaptureEnabled = true,
            onCapture = {},
            autoCaptureEnabled = false,
            onToggleAutoCapture = {},
            flashlightAvailable = true,
            flashlightEnabled = false,
            onToggleFlashlight = {},
            autoToggleContentDescription = "Auto",
            flashContentDescription = "Flash",
            shutterContentDescription = "Shutter"
        )
    }
}
