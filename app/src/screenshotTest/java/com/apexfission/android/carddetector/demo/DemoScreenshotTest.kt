package com.apexfission.android.carddetector.demo

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.apexfission.android.carddetector.demo.ui.theme.CardDetectorDemoTheme

@PreviewTest
@Preview(name = "welcome-en", widthDp = 393, heightDp = 852, locale = "en", fontScale = 1f)
@Preview(name = "welcome-es", widthDp = 393, heightDp = 852, locale = "es", fontScale = 1f)
@Composable
fun WelcomeScreenshot() {
    CardDetectorDemoTheme(darkTheme = false, dynamicColor = false) {
        WelcomeScreen(onOpenDemo = {}, onOpenSettings = {})
    }
}
