# Adaptadores avanzados de cámara y vídeo

Estas declaraciones públicas exponen puntos de integración de nivel inferior. Prefiera `CardDetectorLite` / `CardTrackingSimulator`: los adaptadores personalizados deben reproducir por sí mismos la propiedad, el ciclo de vida, la distribución de trabajo y la liberación. Los ViewModels y las fábricas públicos están orientados a la implementación; sus firmas no implican que sea seguro compartir recursos nativos de forma independiente.

`CameraPreview` vincula la vista previa y el análisis al propietario de ciclo de vida suministrado. Su receptor `onFrame` debe cerrar cada ImageProxy. Los callbacks de fotogramas siguen la distribución de CameraX, sin garantía de ejecutarse en el hilo principal de la interfaz. `createPreviewImageSpaceChain` construye la transformación de rotación y vista previa; use tamaños medidos reales, no dimensiones de pantalla estimadas. `AutoFocusPolicy` tiene estado; serialice el acceso, proporcione un reloj coherente y restablézcalo para una nueva sesión. Solo recomienda un punto o una acción.

`VideoPreviewWithFullFrameCapture` es propietario de su reproductor Media3 y lo libera; el consumidor de fotogramas es propietario de los mapas de bits entregados. `BitmapFrameProcessor` invoca callbacks en el ejecutor suministrado, transfiere la propiedad al entregar y limita los intervalos de captura. Actualmente `release()` no realiza ninguna operación; los callbacks encolados pueden ejecutarse después. Estas API usan interfaces inestables de Media3 y pueden requerir OptIn en el código de la aplicación anfitriona.

Los métodos de procesamiento del ViewModel admiten trabajo mediante un control que permite un único procesamiento simultáneo; los fotogramas rechazados se liberan. `captureLatest` entrega una copia independiente de propiedad transferida en un hilo de trabajo. `setDetectionEnabled` controla el procesamiento; no cancela tareas ya entregadas a la aplicación. Los recursos nativos y los búferes conservados o de callbacks se liberan al limpiar el ViewModel. Las fábricas están destinadas al sistema ViewModel de Android, no a construcciones manuales repetidas. Se aplican todas las salvedades sobre callbacks e imágenes conservadas de [conceptos](../concepts.md).

## Declaraciones

Fragmentos exactos del código fuente; se omiten los cuerpos de implementación. Las importaciones siguientes permiten resolver los tipos e incluyen algunas que solo usa la implementación.

### AutoFocusPolicy (carddetector)

[Código fuente](../../../carddetector/src/main/java/com/apexfission/android/carddetector/ui/camerapreview/AutoFocusPolicy.kt)

```kotlin
package com.apexfission.android.carddetector.ui.camerapreview

import androidx.compose.runtime.Immutable
import com.apexfission.android.math.models.ImagePoint
import com.apexfission.android.math.models.ImageSpaceChain
import com.apexfission.android.math.transformations.toChildSpace
import com.apexfission.android.carddetector.domain.tflite.model.CardDetection
import kotlin.math.abs

data class FocusPoint(
    val x: Float,
    val y: Float
)

data class AutoFocusResult(
    val shouldFocus: Boolean,
    val focusPoint: FocusPoint? = null
)

class AutoFocusPolicy(
    private val cooldownMs: Long = 500L,
    private val positionThresholdPx: Float = 50f,
    private val areaChangeThreshold: Float = 0.10f
) {
    fun shouldTriggerFocus(
        detection: CardDetection?,
        currentTimeMs: Long = System.currentTimeMillis()
    ): AutoFocusResult /* body omitted */

    fun reset() /* body omitted */
}
```

### CameraPreview (carddetector)

[Código fuente](../../../carddetector/src/main/java/com/apexfission/android/carddetector/ui/camerapreview/CameraPreview.kt)

```kotlin
package com.apexfission.android.carddetector.ui.camerapreview

import android.util.Log
import android.util.Size
import android.view.MotionEvent
import android.view.OrientationEventListener
import android.view.Surface
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.MeteringPoint
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.apexfission.android.math.models.ImageSpaceChain
import com.apexfission.android.carddetector.domain.tflite.model.CardDetection
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

@Composable
fun CameraPreview(
    onFrame: (ImageProxy, ImageSpaceChain) -> Unit,
    onFocusEvent: (CameraControl, MeteringPoint) -> Unit,
    lifecycleOwner: LifecycleOwner,
    flashlightEnabled: Boolean,
    onFlashlightAvailabilityChanged: (Boolean) -> Unit = {},
    analysisTargetResolution: Size = Size(2048, 1080),
    focusOn: CardDetection?,
    focusImageSpaceChain: ImageSpaceChain? = null,
    tapToFocusEnabled: Boolean = true,
    focusOnCardEnabled: Boolean = true,
    showFocusIndicator: Boolean = true,
) /* body omitted */
```

### utils (carddetector)

[Código fuente](../../../carddetector/src/main/java/com/apexfission/android/carddetector/ui/camerapreview/utils.kt)

```kotlin
package com.apexfission.android.carddetector.ui.camerapreview

import com.apexfission.android.math.models.ImageSpace
import com.apexfission.android.math.models.ImageSpaceChain
import com.apexfission.android.math.models.SpaceRelationship
import com.apexfission.android.math.models.chain
import com.apexfission.android.math.models.cropAtCenter
import com.apexfission.android.math.models.scale

fun createPreviewImageSpaceChain(
    videoWidth: Int,
    videoHeight: Int,
    viewWidth: Int,
    viewHeight: Int
): ImageSpaceChain /* body omitted */
```

### CardDetectorLiteViewModel (carddetector)

[Código fuente](../../../carddetector/src/main/java/com/apexfission/android/carddetector/ui/detector/CardDetectorLiteViewModel.kt)

```kotlin
package com.apexfission.android.carddetector.ui.detector

import android.app.Application
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.CameraControl
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageProxy
import androidx.camera.core.MeteringPoint
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apexfission.android.carddetector.domain.tflite.detector.card.engine.buildCardDetector
import com.apexfission.android.yolo.image.PreProcessingImageTransformation
import com.apexfission.android.yolo.engine.NumThreads
import com.apexfission.android.yolo.engine.YoloDetector
import com.apexfission.android.carddetector.domain.tflite.filters.CardValidator
import com.apexfission.android.yolo.image.crop
import com.apexfission.android.yolo.image.cropWithOffset
import com.apexfission.android.yolo.image.toUprightBitmap
import com.apexfission.android.carddetector.domain.tflite.model.CardDetection
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CardDetectorLiteViewModel(
    application: Application,
    modelPath: String,
    val classLabels: Map<Int, String> = emptyMap(),
    cardClasses: Set<Int>,
    useGpu: Boolean,
    scoreThreshold: Float,
    iouThreshold: Float,
    cardFilters: List<CardValidator>,
    private val inferenceIntervalMs: Long,
    lockOnThreshold: Int,
    noDetectionCountLimit: Int,
    memoryDetectionTimeLimit: Long,
    validateClassIdInLockOnProcess: Boolean,
    differenceHashDistanceLimit: Int,
    allowTemporalDrift: Boolean,
    private val preProcessingImageTransformation: PreProcessingImageTransformation,
    numThreads: NumThreads,
) : AndroidViewModel(application) {
    val latestBestDetection: StateFlow<CardDetection?> = _latestBestDetection.asStateFlow()

    val flashlightEnabled: StateFlow<Boolean> = _flashlightEnabled.asStateFlow()

    val flashlightAvailable: StateFlow<Boolean> = _flashlightAvailable.asStateFlow()

    fun setDetectionEnabled(enabled: Boolean) /* body omitted */

    fun setFlashlightAvailable(available: Boolean) /* body omitted */

    fun toggleFlashlight() /* body omitted */

    fun onFocusEvent(cameraControl: CameraControl, meteringPoint: MeteringPoint) /* body omitted */

    fun processImage(
        imageProxy: ImageProxy,
        canvasSize: IntSize = IntSize.Zero,
        onDetection: (CardDetection, Bitmap) -> Unit,
    ) /* body omitted */

    suspend fun captureLatest(
        onCapture: (CardDetection, Bitmap) -> Unit,
    ): Boolean /* expression body omitted */

    override fun onCleared() /* body omitted */
}
```

### CardDetectorLiteViewModelFactory (carddetector)

[Código fuente](../../../carddetector/src/main/java/com/apexfission/android/carddetector/ui/detector/CardDetectorLiteViewModelFactory.kt)

```kotlin
package com.apexfission.android.carddetector.ui.detector

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.apexfission.android.yolo.image.PreProcessingImageTransformation
import com.apexfission.android.yolo.engine.NumThreads
import com.apexfission.android.carddetector.domain.tflite.filters.CardValidator

class CardDetectorLiteViewModelFactory(
    private val application: Application,
    private val modelPath: String,
    private val classLabels: Map<Int, String> = emptyMap(),
    private val useGpu: Boolean,
    private val scoreThreshold: Float,
    private val iouThreshold: Float,
    private val cardFilters: List<CardValidator>,
    private val cardClasses: Set<Int>,
    private val inferenceIntervalMs: Long,
    private val lockOnThreshold: Int,
    private val noDetectionCountLimit: Int,
    private val memoryDetectionTimeLimit: Long,
    private val validateClassIdInLockOnProcess: Boolean,
    private val differenceHashDistanceLimit: Int,
    private val allowTemporalDrift: Boolean,
    private val preProcessingImageTransformation: PreProcessingImageTransformation,
    private val numThreads: NumThreads,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T /* body omitted */
}
```

### BitmapFrameProcessor (carddetector)

[Código fuente](../../../carddetector/src/main/java/com/apexfission/android/carddetector/ui/simulation/BitmapFrameProcessor.kt)

```kotlin
package com.apexfission.android.carddetector.ui.simulation

import android.graphics.Bitmap
import androidx.media3.common.GlTextureInfo
import androidx.media3.common.util.GlRect
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.ByteBufferGlEffect
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor

class BitmapFrameProcessor(
    captureIntervalMs: Long,
    private val callbackExecutor: Executor,
    private val onConfigured: (width: Int, height: Int) -> Unit,
    private val onBitmap: (bitmap: Bitmap, presentationTimeUs: Long) -> Unit,
) : ByteBufferGlEffect.Processor<Unit> {
    override fun configure(inputWidth: Int, inputHeight: Int): Size /* body omitted */

    override fun getScaledRegion(presentationTimeUs: Long): GlRect /* expression body omitted */

    override fun processImage(
        image: ByteBufferGlEffect.Image,
        presentationTimeUs: Long
    ): ListenableFuture<Unit> /* body omitted */

    override fun finishProcessingAndBlend(
        outputFrame: GlTextureInfo,
        presentationTimeUs: Long,
        result: Unit
    ) /* expression body omitted */

    override fun release() /* expression body omitted */
}
```

### CardTrackingSimulatorViewModel (carddetector)

[Código fuente](../../../carddetector/src/main/java/com/apexfission/android/carddetector/ui/simulation/CardTrackingSimulatorViewModel.kt)

```kotlin
package com.apexfission.android.carddetector.ui.simulation

import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apexfission.android.carddetector.domain.tflite.detector.card.engine.buildCardDetector
import com.apexfission.android.yolo.image.PreProcessingImageTransformation
import com.apexfission.android.yolo.engine.YoloDetector
import com.apexfission.android.carddetector.domain.tflite.filters.CardValidator
import com.apexfission.android.yolo.image.crop
import com.apexfission.android.yolo.image.cropWithOffset
import com.apexfission.android.carddetector.domain.tflite.model.CardDetection
import com.apexfission.android.carddetector.ui.detector.DetectionFrame
import com.apexfission.android.carddetector.ui.detector.DetectionFrameSequencer
import com.apexfission.android.carddetector.ui.detector.LatestBestDetectionStore
import com.apexfission.android.carddetector.ui.detector.LatestCallbackDispatcher
import com.apexfission.android.yolo.engine.NumThreads
import com.apexfission.android.carddetector.ui.detector.SingleFlightGate
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CardTrackingSimulatorViewModel(
    application: android.app.Application,
    modelPath: String,
    val classLabels: Map<Int, String> = emptyMap(),
    cardClasses: Set<Int>,
    useGpu: Boolean,
    scoreThreshold: Float,
    iouThreshold: Float,
    cardFilters: List<CardValidator>,
    private val inferenceIntervalMs: Long,
    lockOnThreshold: Int,
    noDetectionCountLimit: Int,
    memoryDetectionTimeLimit: Long,
    validateClassIdInLockOnProcess: Boolean,
    differenceHashDistanceLimit: Int,
    allowTemporalDrift: Boolean,
    private val preProcessingImageTransformation: PreProcessingImageTransformation,
    numThreads: NumThreads,
) : AndroidViewModel(application) {
    val latestBestDetection = _latestBestDetection.asStateFlow()

    fun setDetectionEnabled(enabled: Boolean) /* body omitted */

    fun processBitmap(
        bitmap: Bitmap,
        canvasSize: IntSize = IntSize.Zero,
        onDetection: (CardDetection, Bitmap) -> Unit,
    ) /* body omitted */

    suspend fun captureLatest(
        onCapture: (CardDetection, Bitmap) -> Unit,
    ): Boolean /* expression body omitted */

    override fun onCleared() /* body omitted */
}
```

### CardTrackingSimulatorViewModelFactory (carddetector)

[Código fuente](../../../carddetector/src/main/java/com/apexfission/android/carddetector/ui/simulation/CardTrackingSimulatorViewModelFactory.kt)

```kotlin
package com.apexfission.android.carddetector.ui.simulation

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.apexfission.android.yolo.image.PreProcessingImageTransformation
import com.apexfission.android.carddetector.domain.tflite.filters.CardValidator
import com.apexfission.android.yolo.engine.NumThreads

class CardTrackingSimulatorViewModelFactory(
    private val application: Application,
    private val modelPath: String,
    private val classLabels: Map<Int, String> = emptyMap(),
    private val cardClasses: Set<Int>,
    private val useGpu: Boolean,
    private val scoreThreshold: Float,
    private val iouThreshold: Float,
    private val cardFilters: List<CardValidator>,
    private val inferenceIntervalMs: Long,
    private val lockOnThreshold: Int,
    private val noDetectionCountLimit: Int,
    private val memoryDetectionTimeLimit: Long,
    private val validateClassIdInLockOnProcess: Boolean,
    private val differenceHashDistanceLimit: Int,
    private val allowTemporalDrift: Boolean,
    private val preProcessingImageTransformation: PreProcessingImageTransformation,
    private val numThreads: NumThreads,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T /* body omitted */
}
```

### VideoPreviewWithFullFrameCapture (carddetector)

[Código fuente](../../../carddetector/src/main/java/com/apexfission/android/carddetector/ui/simulation/VideoPreviewWithFullFrameCapture.kt)

```kotlin
package com.apexfission.android.carddetector.ui.simulation

import android.graphics.Bitmap
import android.net.Uri
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.ByteBufferGlEffect
import androidx.media3.effect.Presentation
import androidx.media3.exoplayer.ExoPlayer
import com.apexfission.android.math.models.ImageSpaceChain
import com.apexfission.android.carddetector.ui.camerapreview.createPreviewImageSpaceChain
import java.util.concurrent.atomic.AtomicReference

@Composable
fun VideoPreviewWithFullFrameCapture(
    videoUri: Uri, modifier: Modifier = Modifier, captureIntervalMs: Long = 33L, onFrame: (Bitmap, ImageSpaceChain) -> Unit
) /* body omitted */
```
