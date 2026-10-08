# Migración y ámbito de versiones

El sitio describe el código fuente de desarrollo en main, fijado a un commit al compilarse. [IMPORT.md](../../IMPORT.md) describe la última dependencia pública confirmada y su código fuente. Desplegar la documentación no crea ninguna versión publicada del paquete.

## Ejemplos anteriores del monorepositorio

Use las importaciones de `com.apexfission.android.carddetector` y las coordenadas Maven actuales. Este espacio de trabajo contiene únicamente `:carddetector`, `:tfmodel` y `:app`. YOLO, coordenadas y permisos son dependencias publicadas por separado; no añada los módulos obsoletos `:yolo`, `:coordinates`, `:cryoto` ni `:permissionsCompose`.

Sustituya los ejemplos de `CardDetectionCallback`/`CardCaptureCallback` por callbacks de función `(CardDetection, Bitmap) -> Unit`. Los nombres de la API en vivo son `onCardDetection`, `onCapture` y `onBack`; el simulador usa `onCardDetection`, `onCaptureRequested` y `onBackRequested`.

Sustituya `HandleCameraPermission` de los ejemplos antiguos por su control de permisos en tiempo de ejecución. Proporcione un `instanceKey` estable. Lea sobre la [propiedad](concepts.md): es válido reciclar inmediatamente las imágenes no utilizadas de los callbacks, y la transferencia asíncrona debe gestionar la cancelación.

`Feature` procede de YOLO y contiene metadatos, no `image: Bitmap`. El mapa de bits entregado por el callback es independiente. Use el tipo de preprocesamiento de YOLO en los preajustes; el tipo antiguo de carddetector con un nombre similar sigue expuesto, pero no es compatible para asignación.

Los alias de compatibilidad de superposiciones permanecen en `ui.overlays`; los tipos de animación canónicos están en `ui.overlays.animation`. Prefiera las importaciones canónicas para nuevo dibujo personalizado. `AI_AGENT_GUIDE.md` ahora redirige a las guías de integración específicas. La documentación existente de los módulos enlaza a esas guías canónicas.
