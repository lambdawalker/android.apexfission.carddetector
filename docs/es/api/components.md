# Componentes de cámara y vídeo

Llame a los composables desde una aplicación Compose que gestione el ciclo de vida. Obtenga primero el permiso de cámara. `instanceKey` debe ser no vacío, estable y único entre detectores hermanos. Los modelos, clases y validadores configuran el detector; los preajustes controlan la admisión y el seguimiento. Consulte [configuración](../configuration.md) para conocer el significado de los parámetros y los valores reales de los preajustes.

`CardDetectorLite` devuelve Unit y conecta CameraX con la detección en un hilo de trabajo; su valor predeterminado de cámara es HighResolution. Ambas lambdas de callback reciben un recorte propiedad de la aplicación anfitriona; consúmalos o recíclelos siempre. `onBack` es una acción del usuario en la superposición. La superposición predeterminada está vacía. El simulador usa una `videoUri` legible, requiere `onCardDetection` y denomina sus acciones `onCaptureRequested` / `onBackRequested`. No tiene linterna. Consulte [propiedad y duración](../concepts.md).

Las propiedades de ámbito `detectionState`/`cardDetection` describen la evaluación actual, `detectionSequence` cambia también cuando no hay detección, y `latestBestDetection`/`captureEnabled` se refieren a la captura conservada. `capture()` copia los datos conservados, `goBack()` invoca la acción de la aplicación anfitriona y `toggleFlashlight()` solicita el estado de la linterna cuando está disponible. `imageSpaceChain` puede ser nulo antes de medir la vista previa.

## Declaraciones

Fragmentos exactos del código fuente; se omiten los cuerpos de implementación. Las importaciones siguientes permiten resolver los tipos e incluyen algunas que solo usa la implementación.

### CameraPreset (carddetector)

[Código fuente](../../../carddetector/src/main/java/com/apexfission/android/carddetector/ui/camerapreview/CameraPreset.kt)

```kotlin
package com.apexfission.android.carddetector.ui.camerapreview

import android.util.Size
import androidx.compose.runtime.Immutable

data class CameraPreset(
    val analysisTargetResolution: Size = Size(2048, 1080),
    val tapToFocusEnabled: Boolean = true,
    val focusOnCardEnabled: Boolean = true
) {
    fun change(
        analysisTargetResolution: Size = this.analysisTargetResolution,
        tapToFocusEnabled: Boolean = this.tapToFocusEnabled,
        focusOnCardEnabled: Boolean = this.focusOnCardEnabled
    ): CameraPreset /* expression body omitted */

    companion object {
        val Default = CameraPreset(
            analysisTargetResolution = Size(2048, 1080),
            tapToFocusEnabled = true,
            focusOnCardEnabled = true
        )

        val HighResolution = CameraPreset(
            analysisTargetResolution = Size(3840, 2160),
            tapToFocusEnabled = true,
            focusOnCardEnabled = true
        )

        val FixedFocus = CameraPreset(
            analysisTargetResolution = Size(2048, 1080),
            tapToFocusEnabled = false,
            focusOnCardEnabled = false
        )
    }
}
```

### CardDetectorLite (carddetector)

[Código fuente](../../../carddetector/src/main/java/com/apexfission/android/carddetector/ui/detector/CardDetectorLite.kt)

```kotlin
package com.apexfission.android.carddetector.ui.detector

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apexfission.android.math.models.ImageSpaceChain
import com.apexfission.android.carddetector.domain.tflite.filters.AspectRatioValidator
import com.apexfission.android.carddetector.domain.tflite.filters.CardValidator
import com.apexfission.android.carddetector.domain.tflite.filters.MarginValidator
import com.apexfission.android.carddetector.domain.tflite.model.CardDetection
import com.apexfission.android.carddetector.ui.camerapreview.CameraPreset
import com.apexfission.android.carddetector.ui.camerapreview.CameraPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@Composable
fun CardDetectorLite(
    modifier: Modifier = Modifier,
    instanceKey: String,
    modelPath: String,
    classLabels: Map<Int, String>,
    cardClasses: Set<Int>,
    isDetectionEnabled: Boolean = true,
    detectorPreset: CardDetectorPreset = CardDetectorPreset.HighPerformance,
    cameraPreset: CameraPreset = CameraPreset.HighResolution,
    cardFilters: List<CardValidator> = listOf(
        MarginValidator(), AspectRatioValidator()
    ),
    onCardDetection: (CardDetection, Bitmap) -> Unit = { _, _ -> },
    onBack: () -> Unit = {},
    onCapture: (CardDetection, Bitmap) -> Unit = { _, _ -> },
    controlOverlay: @Composable CardDetectorOverlayScope.() -> Unit = {}
) /* body omitted */
```

### CardDetectorOverlayScope (carddetector)

[Código fuente](../../../carddetector/src/main/java/com/apexfission/android/carddetector/ui/detector/CardDetectorOverlayScope.kt)

```kotlin
package com.apexfission.android.carddetector.ui.detector

import androidx.compose.runtime.Stable
import com.apexfission.android.math.models.ImageSpaceChain
import com.apexfission.android.carddetector.domain.tflite.model.CardDetection
import com.apexfission.android.carddetector.ui.camerapreview.CameraPreset

interface CardDetectorOverlayScope {
    val detectionState: CardDetection?

    val detectionSequence: Long get() = 0L

    val cardDetection: CardDetection? get() = detectionState

    val latestBestDetection: CardDetection?

    val imageSpaceChain: ImageSpaceChain?

    val captureEnabled: Boolean

    val flashlightAvailable: Boolean

    val flashlightEnabled: Boolean

    val detectorPreset: CardDetectorPreset

    val cameraPreset: CameraPreset

    val classLabels: Map<Int, String>

    fun capture()

    fun goBack()

    fun toggleFlashlight()
}
```

### CardDetectorPreset (carddetector)

[Código fuente](../../../carddetector/src/main/java/com/apexfission/android/carddetector/ui/detector/CardDetectorPreset.kt)

```kotlin
package com.apexfission.android.carddetector.ui.detector

import androidx.compose.runtime.Immutable
import com.apexfission.android.yolo.image.PreProcessingImageTransformation
import com.apexfission.android.yolo.engine.NumThreads

data class CardDetectorPreset(
    val scoreThreshold: Float,
    val iouThreshold: Float = 0.45f,
    val lockOnThreshold: Int,
    val noDetectionCountLimit: Int,
    val memoryDetectionTimeLimit: Long = 1000L,
    val validateClassIdInLockOnProcess: Boolean = true,
    val differenceHashDistanceLimit: Int = 25,
    val allowTemporalDrift: Boolean = true,
    val inferenceIntervalMs: Long,
    val useGpu: Boolean,
    val preProcessingImageTransformation: PreProcessingImageTransformation,
    val numThreads: NumThreads
) {
    fun change(
        scoreThreshold: Float = this.scoreThreshold,
        iouThreshold: Float = this.iouThreshold,
        lockOnThreshold: Int = this.lockOnThreshold,
        noDetectionCountLimit: Int = this.noDetectionCountLimit,
        memoryDetectionTimeLimit: Long = this.memoryDetectionTimeLimit,
        validateClassIdInLockOnProcess: Boolean = this.validateClassIdInLockOnProcess,
        differenceHashDistanceLimit: Int = this.differenceHashDistanceLimit,
        allowTemporalDrift: Boolean = this.allowTemporalDrift,
        inferenceIntervalMs: Long = this.inferenceIntervalMs,
        useGpu: Boolean = this.useGpu,
        imageMode: PreProcessingImageTransformation = this.preProcessingImageTransformation,
        numThreads: NumThreads = this.numThreads
    ): CardDetectorPreset /* expression body omitted */

    companion object {
        val HighAccuracy = CardDetectorPreset(
            scoreThreshold = 0.80f,
            iouThreshold = 0.45f,
            lockOnThreshold = 7,
            noDetectionCountLimit = 6,
            inferenceIntervalMs = 33L,
            useGpu = true,
            preProcessingImageTransformation = PreProcessingImageTransformation.FullImage,
            numThreads = NumThreads.Default
        )

        val HighPerformance = CardDetectorPreset(
            scoreThreshold = 0.50f,
            iouThreshold = 0.45f,
            lockOnThreshold = 5,
            noDetectionCountLimit = 8,
            inferenceIntervalMs = 33L,
            useGpu = true,
            preProcessingImageTransformation = PreProcessingImageTransformation.SquareCrop(),
            numThreads = NumThreads.Default
        )

        val BatterySaver = CardDetectorPreset(
            scoreThreshold = 0.50f,
            iouThreshold = 0.45f,
            lockOnThreshold = 4,
            noDetectionCountLimit = 10,
            inferenceIntervalMs = 100L,
            useGpu = false,
            preProcessingImageTransformation = PreProcessingImageTransformation.SquareCrop(),
            numThreads = NumThreads.CustomCount(2)
        )
    }
}
```

### CardTrackingSimulator (carddetector)

[Código fuente](../../../carddetector/src/main/java/com/apexfission/android/carddetector/ui/simulation/CardTrackingSimulator.kt)

```kotlin
package com.apexfission.android.carddetector.ui.simulation

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apexfission.android.math.models.ImageSpaceChain
import com.apexfission.android.carddetector.domain.tflite.filters.AspectRatioValidator
import com.apexfission.android.carddetector.domain.tflite.filters.CardValidator
import com.apexfission.android.carddetector.domain.tflite.filters.MarginValidator
import com.apexfission.android.carddetector.domain.tflite.model.CardDetection
import com.apexfission.android.carddetector.ui.camerapreview.CameraPreset
import com.apexfission.android.carddetector.ui.detector.CardDetectorOverlayScope
import com.apexfission.android.carddetector.ui.detector.CardDetectorOverlayScopeImpl
import com.apexfission.android.carddetector.ui.detector.CardDetectorPreset
import com.apexfission.android.carddetector.ui.detector.DetectorComponent
import com.apexfission.android.carddetector.ui.detector.detectorViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@Composable
fun CardTrackingSimulator(
    modifier: Modifier = Modifier,
    instanceKey: String,
    videoUri: Uri,
    modelPath: String,
    classLabels: Map<Int, String>,
    cardClasses: Set<Int>,
    isDetectionEnabled: Boolean = true,
    detectorPreset: CardDetectorPreset = CardDetectorPreset.HighPerformance,
    cameraPreset: CameraPreset = CameraPreset.Default,
    cardFilters: List<CardValidator> = listOf(
        MarginValidator(), AspectRatioValidator()
    ),
    onCardDetection: (CardDetection, Bitmap) -> Unit,
    onCaptureRequested: (CardDetection, Bitmap) -> Unit = { _, _ -> },
    onBackRequested: () -> Unit = {},

    controlOverlay: @Composable CardDetectorOverlayScope.() -> Unit = {}
) /* body omitted */
```
