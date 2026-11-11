package com.apexfission.android.carddetector.demo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.carddetector.demo.ui.theme.CardDetectorDemoTheme
import com.apexfission.android.democommon.CardTitle
import com.apexfission.android.democommon.DemoCard
import com.apexfission.android.democommon.DemoScreen

/** Launcher catalog. Camera permissions are requested only by the selected camera demo. */
class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CardDetectorDemoTheme {
                WelcomeScreen(
                    onOpenDemo = { demo -> startActivity(demoIntent(demo)) },
                    onOpenSettings = { openAppSettings() }
                )
            }
        }
    }

    private fun demoIntent(demo: DemoDestination): Intent = when (demo) {
        DemoDestination.SingleSide -> Intent(this, DocumentationQuickstartActivity::class.java)
        DemoDestination.SingleSideAutomatic -> Intent(this, CardDetectionActivity::class.java)
            .putExtra(CardDetectionActivity.EXTRA_SINGLE_SIDE, true)
            .putExtra(CardDetectionActivity.EXTRA_AUTO_CAPTURE, true)
        DemoDestination.TwoSidesManual -> Intent(this, CardDetectionActivity::class.java)
            .putExtra(CardDetectionActivity.EXTRA_AUTO_CAPTURE, false)
        DemoDestination.TwoSidesAutomatic -> Intent(this, CardDetectionActivity::class.java)
            .putExtra(CardDetectionActivity.EXTRA_AUTO_CAPTURE, true)
        DemoDestination.BackendOcr -> Intent(this, BackendOcrActivity::class.java)
        DemoDestination.Video -> Intent(this, CardDetectionSimulationActivity::class.java)
    }

    private fun openAppSettings() {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName")
            )
        )
    }
}

internal enum class DemoDestination(
    @param:StringRes val title: Int,
    @param:StringRes val description: Int,
    @param:StringRes val action: Int,
    val icon: ImageVector,
) {
    SingleSide(R.string.welcome_single_title, R.string.welcome_single_description,
        R.string.welcome_single_action, Icons.Default.PhotoCamera),
    SingleSideAutomatic(R.string.welcome_single_auto_title, R.string.welcome_single_auto_description,
        R.string.welcome_single_auto_action, Icons.Default.AutoAwesome),
    TwoSidesManual(R.string.welcome_manual_title, R.string.welcome_manual_description,
        R.string.welcome_manual_action, Icons.Default.Flip),
    TwoSidesAutomatic(R.string.welcome_auto_title, R.string.welcome_auto_description,
        R.string.welcome_auto_action, Icons.Default.AutoAwesome),
    Video(R.string.welcome_video_title, R.string.welcome_video_description,
        R.string.welcome_video_action, Icons.Default.PlayArrow),
    BackendOcr(R.string.ocr_demo_title, R.string.ocr_demo_description,
        R.string.ocr_demo_open, Icons.Default.AutoAwesome),
}

/**
 * Screen.
 */
@Composable
internal fun WelcomeScreen(
    onOpenDemo: (DemoDestination) -> Unit,
    onOpenSettings: () -> Unit,
) {
    DemoScreen(
        title = stringResource(R.string.welcome_title),
        welcomeNote = stringResource(R.string.welcome_note),
        eyebrow = stringResource(R.string.welcome_eyebrow),
        onOpenSettings = onOpenSettings,
    ) {
        items(DemoDestination.entries, key = { it.name }) { demo ->
            DemoCard {
                CardTitle(title = stringResource(demo.title), icon = demo.icon)
                Text(
                    stringResource(demo.description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilledTonalButton(
                    onClick = { onOpenDemo(demo) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(demo.action))
                }
            }
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WelcomeScreenPreview() {
    CardDetectorDemoTheme {
        WelcomeScreen(
            onOpenDemo = {},
            onOpenSettings = {}
        )
    }
}
