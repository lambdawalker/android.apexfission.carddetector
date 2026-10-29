# Su primera integración de cámara

## Requisitos y dependencias

Empiece con una aplicación Android que admita Compose, los repositorios `google()` y `mavenCentral()`, y la [dependencia confirmada con modelo incluido](../../IMPORT.md). El artefacto del modelo exporta la versión compatible fijada del núcleo. Use los requisitos de Android/Kotlin/JVM de ese documento de instalación generado; para compilar este repositorio también se usan el wrapper de Gradle incluido, un daemon con JDK 21 y la cadena de compilación JDK 17.

Consulte las versiones exactas de las dependencias y los plugins de la demostración en [la compilación de la aplicación](../../app/build.gradle.kts) y el [catálogo de versiones](../../gradle/libs.versions.toml). La Activity mínima siguiente usa Activity Compose, foundation/runtime/Material3 de Compose y las API de ciclo de vida. Esta demostración usa la interfaz de permisos de Apexfission desde JitPack; consulte la dependencia y los repositorios en la compilación de la aplicación y el catálogo enlazados.

Declare el permiso y la Activity en el manifiesto de la aplicación anfitriona (ajuste el paquete de la Activity si mueve el ejemplo):

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="false" />
<!-- Inside application: -->
<activity android:name="com.apexfission.android.carddetector.demo.DocumentationQuickstartActivity"
    android:exported="false" />
```

## Activity completa

Este mismo archivo forma parte de `:app` y se compila en la CI de Android existente. Solicita permiso, observa los cambios de permisos al volver de Ajustes, muestra el escáner y recicla explícitamente ambos tipos de mapas de bits entregados. Muestra las dimensiones de captura y un ID de seguimiento transitorio. Deliberadamente no realiza OCR ni conserva imágenes.

<!-- example: quickstart -->

```kotlin
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
```

<!-- end-example: quickstart -->

## Pruébelo

```bash
./gradlew :app:installDebug
adb shell am start -n com.apexfission.android.carddetector.demo/.DocumentationQuickstartActivity
```

Conceda permiso, apunte a una tarjeta sintética o de ejemplo, manténgala dentro de la guía y pulse el disparador cuando se active. El estado muestra el tamaño del recorte y el ID de seguimiento. Si se deniega el permiso, se mantiene la pantalla de permisos; vuelva a intentarlo o abra Ajustes y regrese. El recorte conservado puede ser anterior al último fotograma visible: consulte la [semántica de captura](concepts.md).

En una aplicación de producción, sustituya los callbacks que solo reciclan por procesamiento limitado y liberación explícita. No copie la configuración de exportación de la Activity de demostración en un escáner de producción sensible; el repositorio exporta las demostraciones para poder iniciarlas con adb. Consulte las [recetas](recipes.md) y el [catálogo de demostraciones](demos.md).
