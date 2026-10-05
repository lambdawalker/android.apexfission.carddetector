# Detection, models, and validators

`CardDetection` is metadata plus tracking state. Its `card` and `features` are YOLO `Feature` objects, not images. IDs are nullable while locking; `DetectionSource` distinguishes inference from hash continuation. The callback bitmap is delivered separately. Source-frame coordinate contracts are in [concepts](../concepts.md).

`CardDetector.track` is synchronous and returns null on no candidate. Bitmap/proxy inputs remain caller-owned; close proxies after the call. Close the detector to close its YOLO dependency. Both builders use the same defaults and a thread-confined wrapper. An optional shared physical dispatcher is borrowed and must outlive borrowers. After the wrapper is closed, track returns null and enabled reads false. Do not mutate/close the input while a synchronous call is active. See [recipes](../recipes.md).

Validators receive borrowed frames and return Boolean. Margin defaults to 20 pixels; aspect ratio accepts longest/shortest within 1.28..1.7 inclusive and rejects zero dimensions. The `configurationKey` controls recomposition identity. Primary card classes must be nonempty. Keep thresholds in sensible ranges; builder construction is not a comprehensive user-input validator.

The `ModelCatalog` marker belongs to core; tfmodel extension properties require the optional model artifact and explicit imports. The legacy carddetector `PreProcessingImageTransformation` remains exported, but presets require the distinct YOLO type. Do not interchange them.

## Declarations

Exact source excerpts; implementation bodies are omitted. Imports below provide type resolution, including some implementation-only imports.

### ModelCatalog (carddetector)

[Source](../../../carddetector/src/main/java/com/apexfission/android/carddetector/domain/ModelCatalog.kt)

```kotlin
package com.apexfission.android.carddetector.domain



class ModelCatalog {
    class TfLite {
        companion object
    }
}
```

### CardDetector (carddetector)

[Source](../../../carddetector/src/main/java/com/apexfission/android/carddetector/domain/tflite/detector/card/engine/CardDetector.kt)

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

[Source](../../../carddetector/src/main/java/com/apexfission/android/carddetector/domain/tflite/detector/card/engine/ThreadConfinedCardDetector.kt)

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

[Source](../../../carddetector/src/main/java/com/apexfission/android/carddetector/domain/tflite/detector/card/engine/build.kt)

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

[Source](../../../carddetector/src/main/java/com/apexfission/android/carddetector/domain/tflite/detector/card/transformation/PreProcessingImageTransformation.kt)

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

[Source](../../../carddetector/src/main/java/com/apexfission/android/carddetector/domain/tflite/filters/AspectRatioValidator.kt)

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

[Source](../../../carddetector/src/main/java/com/apexfission/android/carddetector/domain/tflite/filters/CardValidator.kt)

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

[Source](../../../carddetector/src/main/java/com/apexfission/android/carddetector/domain/tflite/filters/MarginValidator.kt)

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

[Source](../../../carddetector/src/main/java/com/apexfission/android/carddetector/domain/tflite/model/CardDetection.kt)

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

[Source](../../../tfmodel/src/main/java/com/apexfission/android/carddetector/tfmodel/add.kt)

```kotlin
package com.apexfission.android.carddetector.tfmodel

import com.apexfission.android.carddetector.domain.ModelCatalog

val ModelCatalog.TfLite.Companion.modelPath: String

val ModelCatalog.TfLite.Companion.modelName: String

val ModelCatalog.TfLite.Companion.classes: Map<Int, String>

val ModelCatalog.TfLite.Companion.cardClasses: Set<Int>
```
