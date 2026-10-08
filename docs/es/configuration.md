# Configurar la detección y la presentación

## Preajustes del detector

Estos son valores de inicialización, no descripciones antiguas de KDoc. Los intervalos indican la separación mínima entre admisiones, no FPS medidos ni garantizados.

| Preajuste | Puntuación | Puntos de fijación | Evaluaciones sin detección | Intervalo | GPU solicitada | Recorte | Hilos de CPU |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `HighPerformance` | 0.50 | 5 | 8 | 33 ms | Sí | `SquareCrop()` | `NumThreads.Default` |
| `HighAccuracy` | 0.80 | 7 | 6 | 33 ms | Sí | `FullImage` | `NumThreads.Default` |
| `BatterySaver` | 0.50 | 4 | 10 | 100 ms | No | `SquareCrop()` | `CustomCount(2)` |

Todos usan de forma predeterminada IoU 0.45, límite de memoria de 1000 ms, validación de clases activada, límite de distancia dHash de 25 y deriva temporal activada. La deriva compara con el fotograma anterior; desactivarla fija la coherencia de forma más estricta. Aumentar los umbrales de puntuación o fijación puede rechazar más candidatos e incrementar el tiempo de adquisición. El tiempo de espera de memoria y el límite de evaluaciones sin detección restablecen la continuidad del seguimiento; no borran las imágenes conservadas para captura.

Ejemplo parcial de configuración (páselo a `CardDetectorLite` o al simulador):

```kotlin
import com.apexfission.android.carddetector.ui.detector.CardDetectorPreset
import com.apexfission.android.yolo.image.PreProcessingImageTransformation

val preset = CardDetectorPreset.HighPerformance.copy(
    scoreThreshold = 0.6f,
    lockOnThreshold = 6,
    preProcessingImageTransformation = PreProcessingImageTransformation.FullImage,
)
```

`copy` denomina al campo `preProcessingImageTransformation`; `change` denomina al mismo argumento `imageMode`. Use puntuaciones e IoU finitos y razonables en 0..1, puntos de fijación positivos, intervalos no negativos y una distancia dHash en 0..64; no todas las configuraciones se validan al construirlas.

## Cámara

| Preajuste | Tamaño de análisis solicitado | Enfoque al tocar | Enfoque en la tarjeta |
| --- | --- | --- | --- |
| `Default` | 2048 × 1080 | Activado | Activado |
| `HighResolution` | 3840 × 2160 | Activado | Activado |
| `FixedFocus` | 2048 × 1080 | Desactivado | Desactivado |

`CardDetectorLite` en vivo usa **HighResolution** de forma predeterminada; el simulador usa **Default** (sin cámara física). La negociación entre CameraX y el dispositivo determina el tamaño real. Un mayor tamaño de análisis no garantiza recortes legibles; la iluminación, el enfoque, la distancia y la región del modelo siguen siendo relevantes. `AutoFocusPolicy` tiene un intervalo mínimo de 500 ms, un umbral de posición de 50 px y un umbral de cambio de área del 10 %. Produce una decisión de enfoque, no una garantía de que la cámara pueda enfocar.

## Recorte y validadores

Importe `com.apexfission.android.yolo.image.PreProcessingImageTransformation`. Las opciones son `FullImage`, `CenterSquareCrop`, `SquareCrop(top)`, `CenterVisibleImage`, `VisibleImage(top)`, `CenterVisibleImageSquareCrop` y `VisibleImageSquareCrop(top)`. Las formas con desplazamiento usan `0.dp` de forma predeterminada; las variantes explícitamente centradas son distintas. El recorte cambia la región de detección, no el tamaño del tensor del modelo. Use la imagen completa para documentos descentrados; asegúrese de que las transformaciones de región visible dispongan de un área de visualización medida.

`MarginValidator(margin = 20u)` rechaza cuadros próximos a los bordes de la imagen. `AspectRatioValidator(minAspectRatio = 1.28f, maxAspectRatio = 1.7f)` compara el lado más largo con el más corto y rechaza cuadros degenerados. Los valores predeterminados pueden rechazar documentos que de otro modo serían plausibles. Sobrescriba `cardFilters` deliberadamente para otras formas.

Los validadores personalizados implementan `isValid(Detection, Detection?, Bitmap): Boolean`. Trate el mapa de bits como un préstamo y no lo recicle. Sobrescriba `configurationKey` con un valor estable cuando se recreen instancias equivalentes, o conserve el validador con `remember`; la identidad predeterminada usa la instancia del objeto.

## Superposiciones

El `controlOverlay` predeterminado está vacío. Proporcione `IdCaptureOverlay()` para usar el indicador y los controles integrados, o componga `IdCaptureIndicatorOverlay()` e `IdCaptureControlsOverlay()` por separado. `DetectionOverlay` muestra cuadros de detección; `DebugOverlay` expone diagnósticos; `CardLockOnOverlay` utiliza una guía animada.

`IdCaptureOverlayConfig` controla texto/color, opacidades (0.35 en reposo / 0.85 con detección), suavizado (activado; factor 0.2), seguimiento de 200 ms, restablecimiento de 300 ms, espera de desvanecimiento/restablecimiento de 1000 ms y condición de habilitación de captura (true). Los efectos continuos están desactivados de forma predeterminada. Establecer `requiresCardDetectionForCapture = false` cambia la condición del botón; no crea una API de captura de imagen nueva cuando no existe un resultado conservado. Sustituya el texto predeterminado, como «rellenar previamente su información», para que corresponda a lo que implementa realmente su aplicación.

Consulte [la referencia exacta de superposiciones](api/overlays.md) para ver todos los parámetros y sobrecargas de animación, y las [recetas](recipes.md) para los controles personalizados.
