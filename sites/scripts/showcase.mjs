
/** Shared presentation, with links into the selected documentation scope. */
export function showcase(scope, base) {
 const es=scope.language==='es',lang=scope.language;
 const link=(slug)=>scope.root+slug+'/';
 const gallery=(items)=>items.map(([file,title,description])=>`### ${title}\n\n![${title}](${base}/screenshots/${file}.png)\n\n${description}\n`).join('\n');
 return `# ${es?'Detecta. Sigue. Captura.':'Detect. Track. Capture.'}

${es?'Integra detección de tarjetas en Android con seguimiento local y recortes para tu propio flujo de procesamiento. CameraX y Jetpack Compose proporcionan la vista de cámara y los controles.':'Build an Android card-scanning experience with on-device detection, temporal tracking, and cropped images for your own processing pipeline. CameraX and Jetpack Compose provide the preview and capture controls.'}

**[${es?'Instalar':'Install'}](${link('installation')})** · **[${es?'Guías del módulo':'Module guides'}](${scope.root})** · **[${es?'Versiones y módulos':'Versions and modules'}](${base}/${lang}/)**

> ${es?'Las imágenes siguientes ilustran la interfaz de desarrollo mediante vistas sintéticas renderizadas en el host. No utilizan cámara ni inferencia y no verifican funciones de esta versión. Consulta las guías seleccionadas para conocer su API y disponibilidad.':'The images below illustrate the development UI using synthetic host-rendered previews. They do not run a camera or inference and do not verify features in this version. Check the selected guides for API availability.'} [${es?'Procedencia de las imágenes':'Image provenance'}](${scope.root}project/media/).

## ${es?'Captura ambas caras':'Capture both sides'}

${es?'El flujo de dos caras guía al usuario desde el frente hasta la revisión final. La aplicación recibe las imágenes confirmadas para procesarlas.':'The two-sided flow guides the user from the front of a card through final review. The host application receives the confirmed images for downstream processing.'}

${gallery([
 [`capture-front-${lang}`,es?'1. Frente':'1. Front',es?'Coloca la tarjeta dentro de la guía.':'Place the card inside the guide.'],
 [`capture-front-ready-${lang}`,es?'2. Frente detectado':'2. Front detected',es?'El cuadrado blanco deja paso al indicador de detección.':'The white square yields to the detection indicator.'],
 [`capture-turn-over-${lang}`,es?'3. Girar':'3. Turn over',es?'Revisa el frente, repítelo si hace falta y continúa al reverso.':'Inspect or retake the front before continuing to the back.'],
 [`capture-back-${lang}`,es?'4. Reverso':'4. Back',es?'Captura la segunda cara.':'Capture the second side.'],
 [`capture-back-ready-${lang}`,es?'5. Reverso detectado':'5. Back detected',es?'La guía sigue la segunda cara detectada.':'The guide follows the detected second side.'],
 [`capture-review-${lang}`,es?'6. Revisar':'6. Review',es?'Confirma el par o repite una captura.':'Confirm the pair or retake a side.'],
])}

## ${es?'Una cara y capas visuales':'Single-side capture and overlays'}

${es?'Usa una captura de una sola cara o construye tus propios controles sobre la detección. Las capas muestran el resultado y el estado del seguimiento.':'Use a single-side capture or build your own controls around detection. Overlays expose the result and tracking state.'}

${gallery([
 ['single-side-idle',es?'Guía inicial':'Initial guide',es?'Orientación antes de detectar una tarjeta.':'Guidance before a card is detected.'],
 ['single-side-detected',es?'Tarjeta detectada':'Card detected',es?'Controles y estado después de la detección.':'Controls and state after detection.'],
 [`single-side-auto-${lang}`,es?'Captura automática de un lado':'Single-side automatic capture',es?'Espera enfoque y estabilidad; permite cambiar a captura manual.':'Waits for focus and stability; supports switching to manual capture.'],
 [`single-review-${lang}`,es?'Revisar una cara':'Review one side',es?'Repite la foto o confirma antes de entregarla a la aplicación.':'Retake or confirm before the image is delivered to the host app.'],
 ['detection-overlay',es?'Detección':'Detection',es?'Geometría del resultado detectado.':'Geometry of the detection result.'],
 ['lock-on-overlay',es?'Seguimiento':'Lock-on',es?'Guía alineada con la tarjeta.':'Tracking guide aligned with the card.'],
 ['debug-overlay',es?'Diagnóstico':'Debug',es?'Información adicional para desarrollo.':'Additional information for development.'],
])}

## ${es?'Explora la aplicación de demostración':'Explore the demo app'}

${es?'La pantalla de bienvenida reúne los componentes para probar los distintos flujos. Las guías del módulo explican los requisitos de permisos, el ciclo de vida y la propiedad de las imágenes.':'The welcome screen groups the components so you can try the different flows. The module guides explain permission requirements, lifecycle, and bitmap ownership.'}

${gallery([[`welcome-${lang}`,es?'Catálogo de demos':'Demo catalog',es?'Elige el componente que deseas explorar.':'Choose the component you want to explore.']])}

${es?'La detección es la primera etapa de un flujo de documentos. La aplicación decide cómo procesar, enviar y descartar las imágenes.':'Detection is the first stage of a document pipeline. Your application decides how to process, transmit, and discard images.'}
`;
}
