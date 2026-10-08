# Modelos y límites entre dependencias

## Modelo incluido

`sentinel-card-model` exporta el núcleo compatible fijado e incluye `cdl/tflite/Y11-640E197F16.tflite`. `ModelCatalog.TfLite` es un marcador del núcleo; `modelPath`, `modelName`, `classes` y `cardClasses` son propiedades de extensión de `com.apexfission.android.carddetector.tfmodel` y requieren importaciones.

| Clase | Etiqueta | ¿Tarjeta principal? |
| --- | --- | --- |
| 0 | horizontal_card | Sí |
| 1 | vertical_card | Sí |
| 2 | horizontal_card_back | Sí |
| 3 | photo | No |
| 4 | slim-barcode | No |
| 5 | pdf417 | No |
| 6 | mrz-text | No |
| 7 | barcode | No |
| 8 | qrcode | No |

Estas son categorías de detección, no texto ni contenido de códigos de barras decodificados. El repositorio no establece una evaluación de rendimiento verificada, la procedencia del conjunto de entrenamiento ni una garantía de autenticidad. No deduzca la precisión a partir de la grabación suministrada.

## Utilizar su propio modelo

Coloque un modelo compatible en los recursos de la aplicación anfitriona y proporcione su ruta relativa a los recursos, sus etiquetas y un conjunto no vacío de clases de tarjeta principal. Descargue los recursos de Git LFS antes de compilar las demostraciones del repositorio. Tanto `core` como `sentinel-card-model` son AAR; consulte la [instalación](../../IMPORT.md).

La inferencia pertenece a la [biblioteca externa Apexfission YOLO](https://github.com/lambdawalker/android.apexfission.yolo). La integración actual espera una entrada NHWC cuadrada `[1,H,W,3]` y una salida `[1,attributes,boxes]` o `[1,boxes,attributes]`, donde la menor dimensión distinta corresponde a los atributos (al menos cinco). Las cuatro coordenadas del cuadro van seguidas de puntuaciones de clase; no se admiten disposiciones incompatibles con puntuación de presencia de objeto. La dependencia admite entrada/salida FLOAT32 e INT8, con cuantización válida para INT8. Un nombre de archivo FP16 describe los pesos, no permite usar entrada/salida FLOAT16. Consulte el contrato del modelo de la dependencia al cambiar de versión.

Algunos preajustes solicitan GPU; su funcionamiento depende del dispositivo y del modelo. No prometa una alternativa universal ante fallos de GPU ni una velocidad de inferencia. Pruebe `useGpu = false` para aislar problemas del delegado. El repositorio exporta YOLO y coordenadas como dependencias de API porque sus tipos aparecen en firmas públicas. Solo la demostración usa la biblioteca independiente de permisos; las aplicaciones anfitrionas pueden usar directamente el lanzador de permisos de Android.
