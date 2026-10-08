# Detección, modelos y validadores

`CardDetection` contiene metadatos y estado de seguimiento. Sus propiedades `card` y `features` son objetos `Feature` de YOLO, no imágenes. Los ID pueden ser nulos durante la fijación; `DetectionSource` distingue la inferencia de la continuación por hash. El mapa de bits del callback se entrega por separado. Los contratos de coordenadas del fotograma de origen están en [conceptos](../concepts.md).

`CardDetector.track` es síncrono y devuelve null cuando no hay candidato. Las entradas de mapa de bits o proxy siguen perteneciendo a quien llama; cierre los proxies después de la llamada. Cierre el detector para cerrar su dependencia YOLO. Ambos constructores usan los mismos valores predeterminados y un contenedor confinado a un hilo. Un dispatcher físico compartido opcional se toma prestado y debe sobrevivir a los componentes que lo usan. Una vez cerrado el contenedor, track devuelve null y enabled devuelve false. No modifique ni cierre la entrada mientras haya una llamada síncrona activa. Consulte las [recetas](../recipes.md).

Los validadores reciben fotogramas prestados y devuelven Boolean. El margen predeterminado es de 20 píxeles; la proporción acepta el cociente entre el lado más largo y el más corto en 1.28..1.7, ambos inclusive, y rechaza dimensiones nulas. `configurationKey` controla la identidad en la recomposición. Las clases de tarjeta principal no deben estar vacías. Mantenga los umbrales en rangos razonables; la construcción mediante el constructor no es una validación exhaustiva de entradas de usuario.

El marcador `ModelCatalog` pertenece al núcleo; las propiedades de extensión de tfmodel requieren el artefacto opcional del modelo e importaciones explícitas. El `PreProcessingImageTransformation` antiguo de carddetector sigue exportándose, pero los preajustes requieren el tipo distinto de YOLO. No los intercambie.

## Declaraciones

Fragmentos exactos del código fuente; se omiten los cuerpos de implementación. Las importaciones siguientes permiten resolver los tipos e incluyen algunas que solo usa la implementación.

### ModelCatalog (carddetector)

[Código fuente](../../../carddetector/src/main/java/com/apexfission/android/carddetector/domain/ModelCatalog.kt)

```kotlin
package com.apexfission.android.carddetector.domain



class ModelCatalog {
    class TfLite {
        companion object
    }
}
```

### CardDetector (carddetector)

[Código fuente](../../../carddetector/src/main/java/com/apexfission/android/carddetector/domain/tflite/detector/card/engine/CardDetector.kt)

```kotlin
package com.apexfission.android.carddetector.domain.tflite.detector.card.engine

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.apexfission.android.yolo.engine.YoloDetector
import com.apexfission.android.carddetector.domain.tflite.model.CardDetection
import java.io.Closeable

interface CardDetector : Closeable {
    var enabled: Boolean

    fun track(imageProxy: ImageProxy): CardDetection?

    fun track(bitmap: Bitmap): CardDetection?
}
```

### ThreadConfinedCardDetector (carddetector)

[Código fuente](../../../carddetector/src/main/java/com/apexfission/android/carddetector/domain/tflite/detector/card/engine/ThreadConfinedCardDetector.kt)

```kotlin
package com.apexfission.android.carddetector.domain.tflite.detector.card.engine

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.apexfission.android.yolo.tflite.engine.EngineThreadDispatcher
import com.apexfission.android.yolo.tflite.engine.ThreadConfinedResource
import com.apexfission.android.carddetector.domain.tflite.model.CardDetection

class ThreadConfinedCardDetector private constructor(
    private val resource: ThreadConfinedResource<CardDetector>,
) : CardDetector by resource.value {
    constructor(
        sharedDispatcher: EngineThreadDispatcher? = null,
        detectorFactory: () -> CardDetector,
    ) : this(ThreadConfinedResource(sharedDispatcher, detectorFactory))

    override fun track(bitmap: Bitmap): CardDetection? /* expression body omitted */

    override fun track(imageProxy: ImageProxy): CardDetection? /* expression body omitted */

    override var enabled: Boolean

    override fun close() /* body omitted */
}
```

### build (carddetector)

[Código fuente](../../../carddetector/src/main/java/com/apexfission/android/carddetector/domain/tflite/detector/card/engine/build.kt)

```kotlin
package com.apexfission.android.carddetector.domain.tflite.detector.card.engine

import com.apexfission.android.yolo.tflite.engine.EngineThreadDispatcher
import com.apexfission.android.yolo.engine.Detector
import com.apexfission.android.carddetector.domain.tflite.filters.AspectRatioValidator
import com.apexfission.android.carddetector.domain.tflite.filters.CardValidator
import com.apexfission.android.carddetector.domain.tflite.filters.MarginValidator

fun buildCardDetector(
    yoloDetector: Detector,
    cardValidators: List<CardValidator> = listOf(
        AspectRatioValidator(),
        MarginValidator()
    ),
    cardClasses: Set<Int>,
    lockOnThreshold: Int = 5,
    memoryDetectionTimeLimit: Long = 1000L,
    validateClassIdInLockOnProcess: Boolean = true,
    noDetectionCountLimit: Int = 8,
    differenceHashDistanceLimit: Int = 25,
    allowTemporalDrift: Boolean = true,
    hashBasedSearchFrameLimit: Int = 3,
    sharedDispatcher: EngineThreadDispatcher? = null
): CardDetector /* expression body omitted */

fun buildThreadConfinedCardDetector(
    yoloDetector: Detector,
    cardValidators: List<CardValidator> = listOf(
        AspectRatioValidator(),
        MarginValidator()
    ),
    cardClasses: Set<Int>,
    lockOnThreshold: Int = 5,
    memoryDetectionTimeLimit: Long = 1000L,
    validateClassIdInLockOnProcess: Boolean = true,
    noDetectionCountLimit: Int = 8,
    differenceHashDistanceLimit: Int = 25,
    allowTemporalDrift: Boolean = true,
    hashBasedSearchFrameLimit: Int = 3,
    sharedDispatcher: EngineThreadDispatcher? = null
): CardDetector /* body omitted */
```

### PreProcessingImageTransformation (carddetector)

[Código fuente](../../../carddetector/src/main/java/com/apexfission/android/carddetector/domain/tflite/detector/card/transformation/PreProcessingImageTransformation.kt)

```kotlin
package com.apexfission.android.carddetector.domain.tflite.detector.card.transformation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

sealed class PreProcessingImageTransformation {
    data object FullImage : PreProcessingImageTransformation()

    data object CenterSquareCrop : PreProcessingImageTransformation()

    data class SquareCrop(val top: Dp = 0.dp) : PreProcessingImageTransformation()

    data object CenterVisibleImage : PreProcessingImageTransformation()

    data class VisibleImage(val top: Dp = 0.dp) : PreProcessingImageTransformation()

    data object CenterVisibleImageSquareCrop : PreProcessingImageTransformation()

    data class VisibleImageSquareCrop(val top: Dp = 0.dp) : PreProcessingImageTransformation()
}
```

### AspectRatioValidator (carddetector)

[Código fuente](../../../carddetector/src/main/java/com/apexfission/android/carddetector/domain/tflite/filters/AspectRatioValidator.kt)

```kotlin
package com.apexfission.android.carddetector.domain.tflite.filters

import android.graphics.Bitmap
import com.apexfission.android.yolo.engine.Detection
import kotlin.math.max
import kotlin.math.min

class AspectRatioValidator(
    private val minAspectRatio: Float = 1.28f,
    private val maxAspectRatio: Float = 1.7f
) : CardValidator {
    override val configurationKey: String =
        "aspect-ratio:${minAspectRatio.toRawBits()}:${maxAspectRatio.toRawBits()}"

    override fun isValid(
        detection: Detection, previousCardDetection: Detection?, bitmap: Bitmap
    ): Boolean /* body omitted */
}
```

### CardValidator (carddetector)

[Código fuente](../../../carddetector/src/main/java/com/apexfission/android/carddetector/domain/tflite/filters/CardValidator.kt)

```kotlin
package com.apexfission.android.carddetector.domain.tflite.filters

import android.graphics.Bitmap
import com.apexfission.android.yolo.engine.Detection

fun interface CardValidator {
    val configurationKey: String

    fun isValid(detection: Detection, previousCardDetection: Detection?, bitmap: Bitmap): Boolean
}
```

### MarginValidator (carddetector)

[Código fuente](../../../carddetector/src/main/java/com/apexfission/android/carddetector/domain/tflite/filters/MarginValidator.kt)

```kotlin
package com.apexfission.android.carddetector.domain.tflite.filters

import android.graphics.Bitmap
import com.apexfission.android.yolo.engine.Detection

class MarginValidator(private val margin: UInt = 20u) : CardValidator {
    override val configurationKey: String = "margin:$margin"

    override fun isValid(
        detection: Detection, previousCardDetection: Detection?, bitmap: Bitmap
    ): Boolean /* body omitted */
}
```

### CardDetection (carddetector)

[Código fuente](../../../carddetector/src/main/java/com/apexfission/android/carddetector/domain/tflite/model/CardDetection.kt)

```kotlin
package com.apexfission.android.carddetector.domain.tflite.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.apexfission.android.yolo.engine.Feature

data class CardDetection(
    val lockOnProgress: Float,
    val id: Long?,
    val card: Feature,
    val features: List<Feature>,
    val lockingStatus: LockingStatus,
    val detectionSource: DetectionSource = DetectionSource.Yolo,
)

enum class DetectionSource {
    /** The card was detected via YOLO object detector inference. */
    Yolo,

    /** The card was tracked via difference hash (dHash) visual similarity matching. */
    Hash
}

enum class LockingStatus {
    /** The detector is evaluating a candidate card over consecutive frames to confirm stability. */
    LockingCard,

    /** The card has achieved temporal lock-on for the first time; a new card ID has been assigned. */
    NewCard,

    /** The card remains locked on across subsequent frames under the same assigned card ID. */
    CardLocked
}
```

### add (tfmodel)

[Código fuente](../../../tfmodel/src/main/java/com/apexfission/android/carddetector/tfmodel/add.kt)

```kotlin
package com.apexfission.android.carddetector.tfmodel

import com.apexfission.android.carddetector.domain.ModelCatalog

val ModelCatalog.TfLite.Companion.modelPath: String

val ModelCatalog.TfLite.Companion.modelName: String

val ModelCatalog.TfLite.Companion.classes: Map<Int, String>

val ModelCatalog.TfLite.Companion.cardClasses: Set<Int>
```
