# Bitmap ownership, perceptual hashes, and OCR

`Bitmap.use` recycles in finally and propagates the block result/error. Do not use it around work that escapes the block. It transfers no ownership to a launched coroutine by itself.

The dHash helpers are synchronous CPU work. Input images remain caller-owned; temporary scaled images are released. `hashSize` must be 2..8 or `IllegalArgumentException` is thrown. Region hashing clamps bounds to the bitmap and rejects an empty clamped region. Hamming distance returns differing bit count; both `isVisuallySimilar` overloads use an inclusive threshold. Similarity is not identity, authenticity, or a collision-resistant hash.

`OcrWrapper()` owns an ML Kit Latin recognizer. `run(Bitmap)` suspends and returns text blocks with nullable Rect bounds relative to its input. It neither recycles the bitmap nor parses IDs/barcodes. ML Kit failures propagate; new calls after close throw IllegalStateException. Close is idempotent and disposes the recognizer once active wrapper requests finish. Keep the input valid through processing; see the ownership recipe.

## Declarations

Exact source excerpts; implementation bodies are omitted. Imports below provide type resolution, including some implementation-only imports.

### OcrWrapper (carddetector)

[Source](../../../carddetector/src/main/java/com/apexfission/android/carddetector/domain/ocr/OcrWrapper.kt)

```kotlin
package com.apexfission.android.carddetector.domain.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

data class OcrResult(
    val text: String,
    val boundingBox: Rect?
)

class OcrWrapper internal constructor(
    private val recognizer: OcrRecognizer,
) : AutoCloseable {
    constructor() : this(MlKitOcrRecognizer())

    suspend fun run(image: Bitmap): List<OcrResult> /* body omitted */

    override fun close() /* body omitted */
}
```

### differenceHash (carddetector)

[Source](../../../carddetector/src/main/java/com/apexfission/android/carddetector/domain/tflite/image/differenceHash.kt)

```kotlin
package com.apexfission.android.carddetector.domain.tflite.image

import android.graphics.Bitmap
import androidx.core.graphics.scale
import com.apexfission.android.math.models.ImageBox

fun Bitmap.generateDHash(hashSize: Int = 8): ULong /* body omitted */

fun Bitmap.generateDHashFromRegion(
    box: ImageBox,
    hashSize: Int = 8
): ULong /* body omitted */

fun ULong.hammingDistanceTo(other: ULong): Int /* body omitted */

fun dhashDistance(a: Bitmap, b: Bitmap, hashSize: Int = 8): Int /* body omitted */

fun isVisuallySimilar(a: Bitmap, b: Bitmap, maxDistance: Int = 18, hashSize: Int = 8): Boolean /* body omitted */

fun isVisuallySimilar(a: ULong, b: ULong, differenceHashDistanceLimit: Int = 18): Boolean /* body omitted */
```

### BitmapUse (carddetector)

[Source](../../../carddetector/src/main/java/com/apexfission/android/carddetector/resource/BitmapUse.kt)

```kotlin
package com.apexfission.android.carddetector.resource

import android.graphics.Bitmap

inline fun <T> Bitmap.use(block: (Bitmap) -> T): T /* body omitted */
```
