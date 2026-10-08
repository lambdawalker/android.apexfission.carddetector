# Solución de problemas

| Síntoma | Comprobación y corrección probables |
| --- | --- |
| Cámara en blanco / fallo de permisos | Declare CAMERA y solicite permiso en tiempo de ejecución antes de la composición; compruebe la disponibilidad de la cámara y su ciclo de vida. El inicio rápido incluye acciones para reintentar y abrir Ajustes. |
| No se resuelve `ModelCatalog.TfLite.modelPath` | Añada el artefacto del modelo e importe `tfmodel.modelPath`, `classes` y `cardClasses`. El núcleo por sí solo no tiene extensiones de modelo incluido. |
| No se resuelven `HandleCameraPermission` o las interfaces de callbacks | Los ejemplos antiguos están obsoletos. Use callbacks de función y el control de permisos de su aplicación; consulte el inicio rápido. |
| Fallo al cargar el modelo | Compruebe la ruta relativa a los recursos, los datos LFS descargados, la disposición de tensores del modelo, la asignación de clases y la compatibilidad del delegado del dispositivo. Reintente con CPU para aislar fallos de GPU. |
| La tarjeta nunca se fija | Inspeccione la puntuación, los filtros de margen y proporción, la región de recorte, el límite de fotogramas sin detección, la coherencia del hash, la iluminación y el enfoque. Los puntos de fijación no son simplemente el número de callbacks. |
| No hay superposición | `controlOverlay` está vacío de forma predeterminada; proporcione `IdCaptureOverlay()` o una interfaz personalizada. |
| Procesamiento duplicado / pérdida de NewCard | La entrega puede agrupar resultados. Elimine los ID no nulos duplicados y limite el trabajo de la aplicación en lugar de depender de un evento de transición sin pérdidas. |
| Fallo por mapa de bits reciclado | No inicie trabajo dentro de un bloque `Bitmap.use` y regrese antes de que termine el hilo de trabajo. Mantenga válida la imagen hasta que termine su último consumidor. |
| Aumento de memoria | Proporcione ambos callbacks de liberación, limite el trabajo encolado, contemple la cancelación antes de la ejecución de la corrutina y libere el propietario del ViewModel del escáner. |
| El disparador devuelve una tarjeta anterior | La captura copia los mejores píxeles conservados, no un fotograma nuevo. Añada reglas de antigüedad y sesión en la aplicación. |
| Superposición desplazada | Transforme los cuadros del origen con orientación corregida mediante `imageSpaceChain`; no interprete las coordenadas del recorte del callback como coordenadas del origen o la vista previa. |
| Tipo de preprocesamiento incorrecto | Use `com.apexfission.android.yolo.image.PreProcessingImageTransformation`, no el tipo antiguo de carddetector con el mismo nombre. |
| Simulador en blanco | Use una URI legible `android.resource://<applicationId>/raw/x` u otra URI admitida; confirme que el vídeo contiene datos LFS reales. |
| El sitio web contiene texto de punteros LFS | Descargue los recursos (`git lfs pull`) y ejecute la verificación multimedia. El material multimedia generado del sitio debe contener bytes reales. |
| Falla la verificación de IMPORT | Ejecute `python3 scripts/module_release.py generate` solo a partir de metadatos confirmados e inspeccione las diferencias; no anuncie una versión no confirmada. |

Para problemas nativos o de inferencia, proporcione dispositivo/API, contrato del modelo, elección CPU/GPU, preajuste y una reproducción sintética. Los métodos OCR de la demostración son ejemplos provisionales: no se espera que produzcan texto ni resultados de red.
