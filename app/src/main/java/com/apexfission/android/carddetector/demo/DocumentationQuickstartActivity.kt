package com.apexfission.android.carddetector.demo

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.apexfission.android.carddetector.demo.ui.theme.CardDetectorDemoTheme
import com.apexfission.android.carddetector.domain.ModelCatalog
import com.apexfission.android.carddetector.tfmodel.cardClasses
import com.apexfission.android.carddetector.tfmodel.classes
import com.apexfission.android.carddetector.tfmodel.modelPath
import com.apexfission.android.carddetector.ui.camerapreview.CameraPreset
import com.apexfission.android.carddetector.ui.detector.CardDetectorLite
import com.apexfission.android.carddetector.ui.overlays.IdCaptureOverlay
import com.apexfission.android.carddetector.ui.overlays.IdCaptureOverlayConfig
import com.apexfission.android.permission.requester.HandlePermissions
import com.apexfission.android.permission.ui.DefaultPermissionPage
import com.apexfission.android.permission.ui.PermissionDescription
import com.apexfission.android.permission.ui.ReadingPace
import com.apexfission.android.permission.ui.estimateReadingDelayMillis

private const val CAMERA_TITLE = "Scan sample card"
private const val CAMERA_BODY = "Allow camera access to run the single-side card detector demo."

/** Minimal permission + detection + explicit bitmap cleanup example; no OCR or upload. */
class DocumentationQuickstartActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CardDetectorDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding)) {
                        HandlePermissions(
                            permissions = listOf(
                                PermissionDescription(
                                    permission = Manifest.permission.CAMERA,
                                    autoAdvanceDelayMillis = estimateReadingDelayMillis(
                                        "$CAMERA_TITLE $CAMERA_BODY", ReadingPace.Slow
                                    )
                                ) {
                                    DefaultPermissionPage(
                                        label = "Camera",
                                        heroImage = Icons.Default.PhotoCamera,
                                        title = CAMERA_TITLE,
                                        body = CAMERA_BODY,
                                    )
                                }
                            ),
                            onBack = { finish() },
                            onNotNow = { finish() },
                        ) {
                            var status by remember { mutableStateOf("Ready to scan a sample card") }
                            Column(Modifier.fillMaxSize()) {
                                Text(status)
                                CardDetectorLite(
                                    modifier = Modifier.weight(1f),
                                    instanceKey = "documentation-camera",
                                    modelPath = ModelCatalog.TfLite.modelPath,
                                    classLabels = ModelCatalog.TfLite.classes,
                                    cardClasses = ModelCatalog.TfLite.cardClasses,
                                    cameraPreset = CameraPreset.Default,
                                    onCardDetection = { _, bitmap -> bitmap.recycle() },
                                    onCapture = { card, bitmap ->
                                        try {
                                            val message = "Captured ${bitmap.width} × ${bitmap.height}; tracking ID ${card.id}"
                                            runOnUiThread { status = message }
                                        } finally {
                                            bitmap.recycle()
                                        }
                                    },
                                    onBack = { finish() },
                                    controlOverlay = {
                                        IdCaptureOverlay(config = IdCaptureOverlayConfig(
                                            title = "Card Detector demo",
                                            instructionSubTitle = "Scan a sample card; this demo does not upload images",
                                        ))
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
