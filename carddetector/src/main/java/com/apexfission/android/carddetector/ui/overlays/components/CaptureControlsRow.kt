package com.apexfission.android.carddetector.ui.overlays.components

import androidx.compose.material3.MaterialTheme
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    modifier: Modifier = Modifier,
    showAutoCaptureToggle: Boolean = true,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (showAutoCaptureToggle) IconToggleButton(
            checked = autoCaptureEnabled,
            onCheckedChange = { onToggleAutoCapture() },
            modifier = Modifier
                .size(48.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
        ) {
            Icon(
                imageVector = Icons.Default.AutoMode,
                contentDescription = autoToggleContentDescription,
                tint = if (autoCaptureEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else Spacer(Modifier.size(48.dp))
        
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
                .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
            ) {
                Icon(
                    imageVector = if (flashlightEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = flashContentDescription,
                    tint = if (flashlightEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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
