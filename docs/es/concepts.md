# Ciclo de vida, callbacks y coordenadas

## Flujo de procesamiento

`ImageProxy` de CameraX o mapa de bits de vídeo de Media3 → admisión de un único procesamiento simultáneo y limitación de frecuencia → fotograma con orientación corregida y preprocesado → continuación por hash o inferencia YOLO → validadores y selección de candidatos → estado de fijación temporal → metadatos e imagen conservada → callback en hilo de trabajo y superposición de Compose.

`LockingCard` evalúa la coherencia; `NewCard` asigna un ID de seguimiento; `CardLocked` mantiene esa identidad. `DetectionSource.Yolo` indica inferencia; `Hash` indica continuación visual. La continuación por hash aporta puntos fraccionarios de fijación. Un ID es una identidad transitoria del rastreador, no el número de un documento de identidad. La fijación no demuestra autenticidad ni legibilidad del texto.

## Tabla de propiedad

| Límite | Propiedad y liberación |
| --- | --- |
| `CameraPreview.onFrame` | El receptor debe cerrar el `ImageProxy`. El ViewModel integrado lo hace tanto para los fotogramas procesados como para los rechazados. |
| `track(Bitmap)` de bajo nivel | Quien llama conserva la imagen y la mantiene válida hasta que termina la llamada síncrona. |
| `track(ImageProxy)` de bajo nivel | Quien llama cierra el proxy; los mapas de bits temporales convertidos se liberan internamente. |
| Callback de detección | La aplicación anfitriona es propietaria del recorte de la tarjeta desde la entrada al callback; recíclelo tras su último uso. |
| Callback de captura | La aplicación anfitriona es propietaria de una copia independiente del mejor recorte conservado. |
| Mejor imagen conservada | Pertenece al SDK; no la recicle a través de los metadatos ni del estado de la interfaz. |
| `OcrWrapper.run` | La entrada pertenece a quien llama y debe mantenerse válida mientras se ejecuta OCR; OCR no la recicla. |
| `Bitmap.use` | Recicla en `finally`; complete todo el trabajo con la imagen dentro de su bloque. |

Use `import com.apexfission.android.carddetector.resource.use`. Es válido reciclar inmediatamente un mapa de bits entregado que no se necesite. No es válido iniciar trabajo asíncrono dentro de `bitmap.use` y regresar: el hilo de trabajo recibiría píxeles reciclados. Una corrutina que nunca comienza tampoco entra en su `finally`; una transferencia asíncrona debe contemplar la cancelación antes de la ejecución. El inicio rápido evita este problema liberando las imágenes de forma síncrona, y las [recetas](recipes.md) muestran una transferencia que contempla la cancelación.

## Planificación de callbacks

La entrega de detecciones se ejecuta en serie en un dispatcher de trabajo separado de la inferencia. Como máximo se ejecuta un callback y espera un resultado; un resultado pendiente más reciente sustituye a la imagen anterior no entregada, que la biblioteca recicla. La aplicación anfitriona es propietaria de cualquier imagen ya entregada, incluso si su callback lanza una excepción. No lo trate como un flujo sin pérdidas, no cuente fotogramas mediante callbacks ni exija un único evento `NewCard` para iniciar todo el trabajo. Cuando corresponda, elimine los ID de seguimiento no nulos duplicados entre `NewCard` y `CardLocked`.

La captura también llama al código de la aplicación en un hilo de trabajo. Envíe las acciones de interfaz al hilo principal. Mueva el trabajo de CPU o red a un dispatcher controlado por la aplicación anfitriona y limite el trabajo posterior. La pausa mediante `isDetectionEnabled` detiene la admisión al procesamiento, no el trabajo ya entregado a la aplicación.

## Captura y duración

El primer candidato, uno con mayor confianza o un ID diferente recién fijado pueden sustituir la mejor imagen conservada. `captureEnabled` refleja los datos conservados, no necesariamente una tarjeta visible en ese momento. La ausencia de detecciones y la pausa de detección no vacían por sí mismas el almacén. La captura puede devolver datos conservados anteriores; rechácelos en la aplicación cuando sea importante que sean recientes. No se entrega ningún callback de captura si no hay una imagen conservada.

Los ViewModels de Compose viven en el `ViewModelStoreOwner` actual. Las claves incluyen el tipo de componente, un `instanceKey` estable, los metadatos del modelo, el preajuste del detector y la configuración de validadores. Se excluyen los callbacks y los ajustes exclusivos de cámara. Cambiar la configuración selecciona un ViewModel nuevo; el anterior puede permanecer hasta que se libere su propietario. Limite el ámbito de los propietarios a las sesiones del escáner y evite cambiar las claves continuamente. Retirarlo de la composición no implica por sí solo la liberación del detector nativo.

La vinculación de la cámara coordina la finalización asíncrona del proveedor con la liberación. La vista previa de vídeo libera su reproductor al desecharse. El `BitmapFrameProcessor.release()` de bajo nivel no realiza ninguna operación y no suprime los callbacks ya encolados en su ejecutor.

## Espacios de coordenadas

YOLO revierte el relleno de bandas para obtener coordenadas de su imagen de entrada. Los ViewModels de interfaz restauran los desplazamientos del recorte de preprocesamiento al fotograma de origen con orientación corregida. El mapa de bits del callback es un recorte de tarjeta, pero `card.box` y los cuadros de características siguen usando coordenadas del fotograma de origen. Asígnelos mediante el `ImageSpaceChain` proporcionado para las superposiciones en pantalla, o reste explícitamente el origen del recorte de tarjeta para trabajar con coordenadas locales del recorte. `Feature` contiene metadatos de YOLO; no incluye una propiedad `image` recortada.

## Propiedad de recursos de bajo nivel

`buildCardDetector` y `buildThreadConfinedCardDetector` devuelven detectores síncronos que se pueden cerrar. Restringen las llamadas a un hilo de trabajo físico, pero siguen bloqueando a quien llama. El detector de tarjetas cierra el detector YOLO que se le proporciona. Use un `EngineThreadDispatcher` compartido compatible para una pila con GPU; cierre los componentes que lo toman prestado antes de cerrar el dispatcher. Cambiar arbitrariamente de hilo de trabajo mediante corrutinas no establece afinidad de hilo para la GPU.

`OcrWrapper.close()` es idempotente, rechaza llamadas nuevas con `IllegalStateException` y aplaza la liberación del reconocedor hasta que terminen las solicitudes activas del contenedor. `run` suspende; los errores de ML Kit se propagan. `CardDetectorLite` no realiza OCR ni cargas automáticas.
