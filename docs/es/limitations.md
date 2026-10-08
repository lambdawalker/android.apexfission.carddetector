# Limitaciones y supuestos no admitidos

- Android API 28+ e integración con Compose/CameraX; no es un detector de escritorio ni multiplataforma.
- Se sigue una tarjeta principal. Las demás características detectadas son metadatos; no se proporcionan mapas de bits de características, decodificación de códigos de barras, interpretación de MRZ, comprobación de autenticidad ni decisiones de identidad.
- Los nombres de los preajustes expresan su intención. Aquí no se establecen FPS ni precisión medidos, ni compatibilidad universal con GPU o dispositivos.
- Los filtros geométricos predeterminados están orientados a rectángulos con forma de tarjeta y pueden rechazar pasaportes, distorsiones fuertes de perspectiva, tarjetas cortadas y otras formas.
- La entrega de callbacks conserva el último resultado pendiente. Un evento `NewCard` puede sustituirse antes de entregarse. Al diseñar procesamiento automático, elimine duplicados por ID de seguimiento.
- La captura puede devolver una imagen conservada anterior tras una falta de detección o una pausa; no es una solicitud de nueva fotografía a la cámara. Desactivar la condición de captura de la superposición no cambia esto.
- Quien llama es propietario de las imágenes de los callbacks incluso si estos lanzan excepciones. Los argumentos de callback predeterminados sin operación no reciclan imágenes.
- La liberación del ViewModel sigue a su propietario, no simplemente su retirada de la composición. Los cambios de configuración pueden dejar activos ViewModels con claves antiguas hasta que se libere el propietario.
- El procesador de vídeo de bajo nivel puede entregar callbacks encolados después de `release()`. El simulador no es una referencia de renderizado determinista ni una prueba del hardware de cámara.
- Los errores de inicialización de cámara o motor no disponen de un callback público `onError` unificado. Inspeccione Logcat y la configuración del ciclo de vida y permisos de la aplicación; no invente una API de errores.
- Se desconocen el origen de captura, el sistema operativo, la densidad, la configuración regional y el dispositivo de la grabación histórica. Ilustra la interacción, no demuestra una versión publicada. Los fotogramas fijos no pueden demostrar el ciclo de vida de la cámara ni el comportamiento en movimiento.
- La documentación de main y los metadatos de publicación confirmada tienen ámbitos distintos. Compare el código fuente con la versión publicada confirmada antes de depender de nuevas API.
