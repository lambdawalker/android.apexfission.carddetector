# Referencia de la API pública

Estas páginas contienen fragmentos exactos de declaraciones derivados del código fuente, importaciones y contratos de subsistemas. En la referencia se omiten los cuerpos de las funciones; se conservan los valores predeterminados. El manifiesto de entradas revisadas hace fallar la compilación de la documentación cuando cambian las entradas Kotlin. Es una protección contra la desactualización de la documentación, no un comprobador de compatibilidad binaria.

| Subsistema | Referencia |
| --- | --- |
| Composables de cámara en vivo y vídeo, preajustes y ámbito de superposición | [Componentes](api/components.md) |
| Metadatos de detección, validadores, motores que se pueden cerrar y catálogo de modelos | [Detección](api/detection.md) |
| Funciones de superposición, tipos de animación y funciones auxiliares de dibujo | [Superposiciones](api/overlays.md) |
| OCR, dHash y función auxiliar de propiedad de mapas de bits | [Utilidades](api/utilities.md) |
| Adaptadores de cámara/vídeo y ViewModels públicos orientados a la implementación | [Adaptadores avanzados](api/adapters.md) |

Las declaraciones Kotlin `internal`/`private` se excluyen de los fragmentos para consumidores. Algunas clases públicas están orientadas a la implementación, especialmente ViewModels, fábricas y funciones auxiliares de estado de animación; prefiera los composables de alto nivel salvo que gestione deliberadamente su ciclo de vida. Las declaraciones de YOLO y coordenadas pertenecen a los repositorios de esas dependencias y no se copian aquí.

Lea [conceptos](concepts.md) antes de llamar a estas API. El núcleo y el modelo son artefactos independientes; las extensiones del modelo requieren el artefacto del modelo. Todos los ejemplos describen main, mientras que [instalación](../../IMPORT.md) informa de la publicación confirmada.
