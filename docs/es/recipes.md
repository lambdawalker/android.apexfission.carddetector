# Recetas por tarea

Estos son fragmentos parciales de integración. El [inicio rápido](quickstart.md) proporciona la aplicación anfitriona completa ejecutable y su configuración.

## Personalizar los controles

Dentro de `controlOverlay`, `CardDetectorOverlayScope` proporciona las propiedades y acciones del receptor:

```kotlin
// Imports in the surrounding Compose file:
// import androidx.compose.material3.Button
// import androidx.compose.material3.Text
// import com.apexfission.android.carddetector.ui.overlays.IdCaptureIndicatorOverlay
controlOverlay = {
    IdCaptureIndicatorOverlay()
    Button(enabled = captureEnabled, onClick = { capture() }) { Text("Use this card") }
    if (flashlightAvailable) {
        Button(onClick = { toggleFlashlight() }) { Text("Toggle light") }
    }
}
```

Proporcione ambos callbacks de liberación en el escáner padre. Use `detectionSequence` para cada evaluación completada, incluidas las faltas de detección consecutivas; `latestBestDetection` contiene los datos conservados para captura. El texto de la interfaz debe describir el procesamiento real de la aplicación anfitriona. Nunca suponga que el texto predeterminado de la superposición significa que el SDK rellena campos previamente de forma segura.

## Procesar una sola vez cada ID de seguimiento observado

Los callbacks de detección pueden omitir `NewCard`. En su callback en serie, acepte un ID no nulo en `NewCard` o `CardLocked`, compárelo con el último ID aceptado y recicle los mapas de bits rechazados. Limite ese ID a la sesión del detector; no lo guarde como identidad de un documento. Restablezca el estado de la aplicación al iniciar un nuevo escaneo y limite el trabajo para que el procesamiento no crezca sin límite.

Para transferir a una corrutina, esta función auxiliar contempla la cancelación tanto antes como después de comenzar el cuerpo de la corrutina. Llámela con el único mapa de bits propiedad de la aplicación; `process` no debe conservarlo ni reciclarlo. Se muestran las importaciones; esta función auxiliar está incluida en el código fuente de la aplicación y se coteja con el fragmento renderizado.

<!-- example: ownership -->

```kotlin
package com.apexfission.android.carddetector.demo

import android.graphics.Bitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/** Transfers one bitmap into bounded host work; process must neither retain nor recycle it. */
fun CoroutineScope.consumeOwnedBitmap(
    bitmap: Bitmap,
    process: suspend (Bitmap) -> Unit,
): Job {
    val released = AtomicBoolean(false)
    fun release() {
        if (released.compareAndSet(false, true)) bitmap.recycle()
    }
    return try {
        launch(Dispatchers.Default) {
            try { process(bitmap) } finally { release() }
        }.also { job -> job.invokeOnCompletion { release() } }
    } catch (error: Throwable) {
        release()
        throw error
    }
}
```

<!-- end-example: ownership -->

La aplicación anfitriona sigue eligiendo el dispatcher, la admisión de tareas y la eliminación de duplicados. No use esto como una cola ilimitada por fotograma de cámara.

## Cambiar el preprocesamiento o la validación

Use `CardDetectorPreset.HighPerformance.copy(...)` como se muestra en [configuración](configuration.md). Un `CardValidator` recordado puede rechazar cuadros pequeños, reflejos o formas ajenas a su caso de uso; mantenga rápido su cálculo y válido el mapa de bits prestado. Proporcione `cardFilters = emptyList()` solo si desea eliminar deliberadamente ambas comprobaciones geométricas predeterminadas.

## Reproducir un vídeo

Pase una URI legible a `CardTrackingSimulator`, usando las extensiones del modelo y los callbacks del inicio rápido. Use `onCaptureRequested` / `onBackRequested` en lugar de los nombres de la cámara. La [Activity de simulación](../../app/src/main/java/com/apexfission/android/carddetector/demo/CardDetectionSimulationActivity.kt) se puede ejecutar con `raw/x.mp4`. El simulador no dispone de linterna; no valida el enfoque real de la cámara ni el comportamiento de los permisos.

## Usar un detector de bajo nivel

Use `buildCardDetector(yoloDetector = existingDetector, cardClasses = setOf(0, 1, 2))` con un detector YOLO compatible. Ambos nombres de constructor devuelven detectores síncronos confinados a un hilo. `track(Bitmap)` y `track(ImageProxy)` devuelven metadatos que pueden ser nulos; no devuelven el recorte del callback de Compose. Mantenga válida la entrada, cierre usted mismo los proxies y cierre el detector de tarjetas al terminar. Este cierra el detector YOLO suministrado, por lo que no debe compartir ese detector con propietarios ajenos. Para una pila con GPU, construya, opere y cierre todas las capas en un dispatcher físico compartido compatible. La [referencia del motor](api/detection.md) enumera todos los valores predeterminados de los constructores.

## Leer texto después de capturar

```kotlin
import android.graphics.Bitmap
import com.apexfission.android.carddetector.domain.ocr.OcrResult
import com.apexfission.android.carddetector.domain.ocr.OcrWrapper

suspend fun recognizeOwnedCrop(bitmap: Bitmap): List<OcrResult> {
    val ocr = OcrWrapper()
    try {
        return ocr.run(bitmap)
    } finally {
        ocr.close()
        bitmap.recycle()
    }
}
```

Invoque la función solo después de que su corrutina haya comenzado con propiedad válida, o use la función auxiliar de transferencia y omita el reciclado del mapa de bits de esta función para mantener un único propietario. OCR devuelve bloques de texto latino y rectángulos delimitadores que pueden ser nulos, relativos al recorte de entrada, no campos estructurados de identidad. Para solicitudes repetidas, reutilice un contenedor cuyo propietario gestione su ciclo de vida y ciérrelo después de usarlo.

## Capturar ambos lados

`TwoSidedCardCapture` ofrece frente → “Continuar al reverso” explícito → reverso → revisión. Muestra el lado sobre la cámara, un recuadro cuadrado inicial que desaparece tras la primera detección de cada lado o repetición, con un contorno que sigue el documento y `1 / 2` o `2 / 2` encima del obturador. La revisión permite repetir cada lado por separado. Los recortes conservan el rectángulo detectado; los documentos verticales y horizontales usan la misma zona. `CardDetectorLite` sigue disponible para un solo lado.

Obtenga primero el permiso de cámara. Use las importaciones del modelo de la [guía rápida](quickstart.md) y componga:

```kotlin
import com.apexfission.android.carddetector.ui.capture.CardCaptureConfig
import com.apexfission.android.carddetector.ui.capture.TwoSidedCardCapture
import com.apexfission.android.carddetector.ui.capture.defaultCardCaptureStrings

TwoSidedCardCapture(
    instanceKey = "identity-document",
    modelPath = ModelCatalog.TfLite.modelPath,
    classLabels = ModelCatalog.TfLite.classes,
    cardClasses = ModelCatalog.TfLite.cardClasses,
    config = CardCaptureConfig(autoCaptureEnabled = true),
    strings = defaultCardCaptureStrings(),
    onBack = onBack,
    onComplete = { result ->
        // Example consumes synchronously; transfer ownership safely for async processing.
        try {
            // Consume result.front and result.back here.
        } finally {
            result.front.bitmap.recycle()
            result.back.bitmap.recycle()
        }
    },
)
```

`onComplete` se ejecuta una vez en el **hilo principal** y transfiere ambos mapas de bits a la aplicación. Hasta confirmar, el flujo los posee y recicla los fotogramas descartados, las imágenes sustituidas y las sesiones canceladas. Para OCR o cargas asíncronas, transfiera la propiedad con protección ante cancelaciones. Cambiar `instanceKey`, metadatos del modelo, ajustes del detector/cámara, configuración de validadores o captura reinicia todo el flujo y libera sus imágenes. Cada lado o repetición usa un propietario del detector nuevo y desechable; recrear la actividad empieza de nuevo. La pantalla de volteo muestra el frente capturado con la cámara detenida.

El icono de captura automática junto al obturador permite activarla o desactivarla sin perder los lados guardados. Al activarla empieza un nuevo intervalo de estabilidad; al desactivarla se rechazan las capturas automáticas pendientes. El icono de flash controla la linterna en cámaras compatibles.

La captura automática está inicialmente desactivada de forma predeterminada. `CardCaptureConfig(autoCaptureEnabled = true)` exige fijación actual, nitidez de la región de la tarjeta de al menos `focusThreshold` (100 por defecto) y límites estables durante 600 ms. Intervalos superiores a 400 ms, ausencias, desenfoque y movimiento reinician la espera. Cada evaluación actualiza la preparación antes de que la entrega a la interfaz pueda descartar fotogramas. El obturador manual sigue disponible con un recorte detectado reciente, incluso por debajo del umbral automático. Ambos modos consumen el recorte actual, no la mejor imagen antigua conservada por la API de un lado.

`CardDetection.focusScore` es la varianza del laplaciano de luminancia en una región interior de la tarjeta, escalada a un lado máximo de 256 px. `focusState` es Unknown, Focused o Unfocused; `isFocused` usa el umbral predeterminado de 100. `observedAtElapsedRealtimeMs` es una marca temporal monótona de evaluación, no la hora civil. También se reportan en detección de bajo nivel y vídeo. La heurística **no representa finalización del autoenfoque, legibilidad OCR, evaluación de reflejos ni clasificación del lado**. El ruido, la iluminación y la textura influyen; calibre `focusThreshold` en los dispositivos y documentos objetivo. Una puntuación ausente o inválida nunca autoriza captura automática. Un umbral personalizado no modifica el umbral predeterminado de `isFocused`.

El inglés es el idioma de respaldo; el español sigue la configuración regional de Android. Personalice cadenas con `defaultCardCaptureStrings().copy(frontInstruction = "…", backInstruction = "…")` o proporcione un `CardCaptureStrings` completo. Las etiquetas guían al usuario; mostrar el mismo lado dos veces no se rechaza automáticamente.
