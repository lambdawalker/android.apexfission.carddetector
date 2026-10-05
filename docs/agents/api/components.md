# Camera and video components

Call composables from a lifecycle-aware Compose host. Obtain camera permission first. `instanceKey` must be nonblank, stable, and unique among sibling detectors. Models/classes/validators configure the detector; presets control admission and tracking. See [configuration](../configuration.md) for parameter meanings and actual preset values.

`CardDetectorLite` returns Unit and wires CameraX to worker detection; its camera default is HighResolution. Both callback lambdas receive a host-owned cutout; always consume/recycle them. `onBack` is an overlay user action. The default overlay is empty. Simulator uses a readable `videoUri`, requires `onCardDetection`, and names its actions `onCaptureRequested` / `onBackRequested`. It has no torch. See [ownership and lifetime](../concepts.md).

Scope `detectionState`/`cardDetection` describe the current evaluation, `detectionSequence` changes on misses too, and `latestBestDetection`/`captureEnabled` refer to retained capture. `capture()` copies retained data, `goBack()` invokes the host action, and `toggleFlashlight()` requests torch state where available. `imageSpaceChain` can be null before preview measurement.

## Declarations

Exact source excerpts; implementation bodies are omitted. Imports below provide type resolution, including some implementation-only imports.

### CameraPreset (carddetector)

[Source](../../../carddetector/src/main/java/com/apexfission/android/carddetector/ui/camerapreview/CameraPreset.kt)

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

[Source](../../../carddetector/src/main/java/com/apexfission/android/carddetector/ui/detector/CardDetectorLite.kt)

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

[Source](../../../carddetector/src/main/java/com/apexfission/android/carddetector/ui/detector/CardDetectorOverlayScope.kt)

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

[Source](../../../carddetector/src/main/java/com/apexfission/android/carddetector/ui/detector/CardDetectorPreset.kt)

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

[Source](../../../carddetector/src/main/java/com/apexfission/android/carddetector/ui/simulation/CardTrackingSimulator.kt)

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
