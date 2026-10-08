# Propiedad de mapas de bits, hashes perceptuales y OCR

`Bitmap.use` recicla en finally y propaga el resultado o error del bloque. No lo use con trabajo que se ejecute fuera del bloque. Por sí solo no transfiere la propiedad a una corrutina iniciada.

Las funciones auxiliares de dHash realizan trabajo síncrono de CPU. Las imágenes de entrada siguen perteneciendo a quien llama; las imágenes temporales escaladas se liberan. `hashSize` debe estar en 2..8 o se lanza `IllegalArgumentException`. El cálculo de hash de regiones limita los bordes al mapa de bits y rechaza una región que quede vacía tras ese ajuste. La distancia de Hamming devuelve el número de bits distintos; ambas sobrecargas de `isVisuallySimilar` usan un umbral inclusivo. La similitud no demuestra identidad ni autenticidad, y no es un hash resistente a colisiones.

`OcrWrapper()` es propietario de un reconocedor de texto latino de ML Kit. `run(Bitmap)` suspende y devuelve bloques de texto con límites Rect que pueden ser nulos, relativos a su entrada. No recicla el mapa de bits ni interpreta documentos de identidad o códigos de barras. Los errores de ML Kit se propagan; las llamadas nuevas después del cierre lanzan IllegalStateException. El cierre es idempotente y libera el reconocedor cuando terminan las solicitudes activas del contenedor. Mantenga válida la entrada durante el procesamiento; consulte la receta de propiedad.

## Declaraciones

Fragmentos exactos del código fuente; se omiten los cuerpos de implementación. Las importaciones siguientes permiten resolver los tipos e incluyen algunas que solo usa la implementación.

### OcrWrapper (carddetector)

[Código fuente](../../../carddetector/src/main/java/com/apexfission/android/carddetector/domain/ocr/OcrWrapper.kt)

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

[Código fuente](../../../carddetector/src/main/java/com/apexfission/android/carddetector/domain/tflite/image/differenceHash.kt)

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

[Código fuente](../../../carddetector/src/main/java/com/apexfission/android/carddetector/resource/BitmapUse.kt)

```kotlin
package com.apexfission.android.carddetector.resource

import android.graphics.Bitmap

inline fun <T> Bitmap.use(block: (Bitmap) -> T): T /* body omitted */
```
