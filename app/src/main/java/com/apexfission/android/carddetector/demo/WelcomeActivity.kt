package com.apexfission.android.carddetector.demo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apexfission.android.carddetector.demo.ui.theme.CardDetectorDemoTheme

/** Launcher catalog. Camera permissions are requested only by the selected camera demo. */
class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CardDetectorDemoTheme {
                WelcomeScreen { demo -> startActivity(demoIntent(demo)) }
            }
        }
    }

    private fun demoIntent(demo: DemoDestination): Intent = when (demo) {
        DemoDestination.SingleSide -> Intent(this, DocumentationQuickstartActivity::class.java)
        DemoDestination.TwoSidesManual -> Intent(this, CardDetectionActivity::class.java)
            .putExtra(CardDetectionActivity.EXTRA_AUTO_CAPTURE, false)
        DemoDestination.TwoSidesAutomatic -> Intent(this, CardDetectionActivity::class.java)
            .putExtra(CardDetectionActivity.EXTRA_AUTO_CAPTURE, true)
        DemoDestination.Video -> Intent(this, CardDetectionSimulationActivity::class.java)
    }
}

internal enum class DemoDestination(
    @param:StringRes val title: Int,
    @param:StringRes val description: Int,
    @param:StringRes val action: Int,
    val component: String,
    val icon: ImageVector,
) {
    SingleSide(R.string.welcome_single_title, R.string.welcome_single_description,
        R.string.welcome_single_action, "CardDetectorLite", Icons.Default.PhotoCamera),
    TwoSidesManual(R.string.welcome_manual_title, R.string.welcome_manual_description,
        R.string.welcome_manual_action, "TwoSidedCardCapture", Icons.Default.Flip),
    TwoSidesAutomatic(R.string.welcome_auto_title, R.string.welcome_auto_description,
        R.string.welcome_auto_action, "TwoSidedCardCapture", Icons.Default.AutoAwesome),
    Video(R.string.welcome_video_title, R.string.welcome_video_description,
        R.string.welcome_video_action, "CardTrackingSimulator", Icons.Default.PlayArrow),
}

@Composable
internal fun WelcomeScreen(onOpenDemo: (DemoDestination) -> Unit) {
    Scaffold { insets ->
        Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.welcome_eyebrow),
                            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.welcome_title),
                            style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.welcome_description),
                            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                    }
                }
                items(DemoDestination.entries, key = { it.name }) { demo ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Surface(shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.primaryContainer) {
                                    Icon(demo.icon, contentDescription = null,
                                        modifier = Modifier.padding(12.dp).size(24.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(demo.title), style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold)
                                    Text(demo.component, style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Text(stringResource(demo.description), style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            FilledTonalButton(onClick = { onOpenDemo(demo) }, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(demo.action))
                            }
                        }
                    }
                }
                item {
                    Text(stringResource(R.string.welcome_footer), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
