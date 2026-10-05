# Your first camera integration

## Requirements and dependencies

Start with a Compose-enabled Android app, `google()` and `mavenCentral()` repositories, and the [confirmed bundled-model dependency](../../IMPORT.md). The model artifact exports the pinned compatible core. Use the Android/Kotlin/JVM requirements in that generated installation document; building this repository additionally uses the checked-in Gradle wrapper and JDK 21 daemon / JDK 17 compilation toolchain.

For the exact demo dependency/plugin versions see [the app build](../../app/build.gradle.kts) and [version catalog](../../gradle/libs.versions.toml). The minimal Activity below uses Activity Compose, Compose foundation/runtime/Material3, and lifecycle APIs. The separate Apexfission permissions dependency is not required for this example.

Declare the permission and Activity in your host manifest (adjust the Activity package if you move the example):

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="false" />
<!-- Inside application: -->
<activity android:name="com.apexfission.android.carddetector.demo.DocumentationQuickstartActivity"
    android:exported="false" />
```

## Complete Activity

This exact file is part of `:app` and is compiled by existing Android CI. It requests permission, observes permission changes after Settings, renders the scanner, and explicitly recycles both kinds of delivered bitmap. It shows capture dimensions and a transient tracking ID. It intentionally does not perform OCR or retain images.

<!-- example: quickstart -->

```kotlin
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
```

<!-- end-example: quickstart -->

## Try it

```bash
./gradlew :app:installDebug
adb shell am start -n com.apexfission.android.carddetector.demo/.DocumentationQuickstartActivity
```

Grant permission, point at a synthetic/sample card, hold it within the guide, and press the shutter once enabled. The status shows crop size and tracker ID. Denial keeps the permission screen; retry or use Settings and return. The retained crop can be older than the latest visible frame: see [capture semantics](concepts.md).

In a production host, replace recycling-only callbacks with bounded processing and explicit cleanup. Do not copy the demo Activity's exported setting into a sensitive production scanner; the repository exports demos so they can be launched with adb. See [recipes](recipes.md) and [the demo catalog](demos.md).
