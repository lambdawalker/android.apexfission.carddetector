# Mapa de integración de Card Detector

Ámbito: **código fuente de desarrollo actual en main**. El sitio web indica el commit exacto de la compilación y fija los enlaces al código fuente. [IMPORT.md](../../IMPORT.md) es el documento autorizado de instalación de la versión publicada confirmada; esta guía no afirma que todas las API de main estén disponibles en esa versión.

La biblioteca detecta y sigue una tarjeta principal, publica metadatos y mapas de bits recortados, y ofrece adaptadores de cámara y vídeo para Compose. No autentica documentos, comprueba la presencia de una persona viva, interpreta códigos de barras ni constituye un sistema completo de verificación de identidad. Un contenedor de OCR opcional devuelve únicamente bloques de texto.

| Tarea | API | Siguiente lectura |
| --- | --- | --- |
| Primera integración y permisos | `CardDetectorLite` | [Inicio rápido](quickstart.md) |
| Reproducir un vídeo | `CardTrackingSimulator` | [Demostraciones](demos.md) |
| Ajustar la detección y el recorte | `CardDetectorPreset`, `CardValidator` | [Configuración](configuration.md) |
| Personalizar la interfaz o la captura | `CardDetectorOverlayScope`, `IdCaptureOverlayConfig` | [Recetas](recipes.md) |
| Gestionar imágenes y callbacks | `Bitmap.use`, lambdas de detección/captura | [Conceptos](concepts.md) |
| Detectar sin Compose / OCR | `buildCardDetector`, `OcrWrapper` | [Recetas](recipes.md) |
| Importaciones y declaraciones exactas | Referencia pública por subsistema | [API](api.md) |
| Diagnosticar y migrar | Errores, importaciones antiguas y limitaciones | [Solución de problemas](troubleshooting.md), [migración](migration.md) |

Invariantes fundamentales:

- Obtenga el permiso de cámara antes de componer la cámara. No hay una solicitud de permiso integrada.
- Proporcione valores de `instanceKey` estables, distintos y no vacíos dentro de un propietario de ViewModel.
- La aplicación anfitriona es propietaria de ambos mapas de bits de los callbacks; recíclelos tras su último uso, también en caso de error o transferencia cancelada. Proporcione siempre callbacks que consuman o reciclen las imágenes; los predeterminados no liberan las imágenes entregadas.
- Los callbacks de detección se ejecutan en serie, en un hilo de trabajo, y entregan el último resultado pendiente; no forman un flujo de fotogramas sin pérdidas. No dependa de observar todas las transiciones `NewCard`.
- La captura copia los mejores píxeles conservados, que pueden sobrevivir a un fotograma perdido o a una pausa de detección. No realiza una nueva captura fotográfica.
- Los cuadros de los metadatos describen el fotograma de origen con la orientación corregida, no coordenadas locales del recorte ni de la pantalla.
- Use `PreProcessingImageTransformation` de YOLO con `CardDetectorPreset`; el tipo antiguo de carddetector con un nombre similar es distinto.

[Limitaciones](limitations.md) · [contrato del modelo](model-contract.md) · [mantenimiento](../maintenance.md) · [cobertura de funciones](../coverage.md)
