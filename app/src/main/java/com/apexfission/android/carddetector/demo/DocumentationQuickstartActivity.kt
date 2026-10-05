package com.apexfission.android.carddetector.demo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import com.apexfission.android.carddetector.domain.ModelCatalog
import com.apexfission.android.carddetector.tfmodel.cardClasses
import com.apexfission.android.carddetector.tfmodel.classes
import com.apexfission.android.carddetector.tfmodel.modelPath
import com.apexfission.android.carddetector.ui.camerapreview.CameraPreset
import com.apexfission.android.carddetector.ui.detector.CardDetectorLite
import com.apexfission.android.carddetector.ui.overlays.IdCaptureOverlay
import com.apexfission.android.carddetector.ui.overlays.IdCaptureOverlayConfig

/** Minimal permission + detection + explicit bitmap cleanup example; no OCR or upload. */
class DocumentationQuickstartActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var allowed by remember { mutableStateOf(hasCameraPermission()) }
                var status by remember { mutableStateOf("Ready to scan a sample card") }
                val request = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { allowed = it }
                DisposableEffect(this@DocumentationQuickstartActivity) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) allowed = hasCameraPermission()
                    }
                    lifecycle.addObserver(observer)
                    onDispose { lifecycle.removeObserver(observer) }
                }
                if (!allowed) {
                    Column {
                        Text("Camera access is needed to scan. You can retry or enable it in Settings.")
                        Button(onClick = { request.launch(Manifest.permission.CAMERA) }) {
                            Text("Allow camera")
                        }
                        Button(onClick = {
                            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:$packageName")))
                        }) { Text("Open app settings") }
                        Button(onClick = { finish() }) { Text("Back") }
                    }
                } else {
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

    private fun hasCameraPermission(): Boolean =
        checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
}
