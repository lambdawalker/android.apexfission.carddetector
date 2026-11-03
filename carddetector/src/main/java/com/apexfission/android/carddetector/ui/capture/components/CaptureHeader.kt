package com.apexfission.android.carddetector.ui.capture.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.carddetector.ui.capture.CardCaptureStrings
import com.apexfission.android.carddetector.ui.capture.defaultCardCaptureStrings

/** Component. */
@Composable
internal fun CaptureHeader(strings: CardCaptureStrings, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color.Black.copy(alpha=0.65f)), verticalAlignment=Alignment.CenterVertically) {
        IconButton(onClick=onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription=strings.exit, tint=Color.White) }
        Text(strings.title, color=Color.White, fontWeight=FontWeight.Bold)
    }
}

/** Component. */
@Composable
internal fun CaptureTitle(title: String) {
    Text(title, color=Color(0xFF75E8CD), fontSize=24.sp, fontWeight=FontWeight.Bold, textAlign=TextAlign.Center)
}

@Preview(name = "Light Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CaptureHeaderPreview() {
    CaptureHeader(
        strings = defaultCardCaptureStrings(),
        onBack = {}
    )
}

@Preview(name = "Light Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CaptureTitlePreview() {
    CaptureTitle("Title")
}
