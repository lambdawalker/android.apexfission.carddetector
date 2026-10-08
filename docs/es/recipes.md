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
