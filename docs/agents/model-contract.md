# Models and dependency boundaries

## Bundled model

`sentinel-card-model` exports the matching core and packages `cdl/tflite/Y11-640E197F16.tflite`. `ModelCatalog.TfLite` is a marker in core; `modelPath`, `modelName`, `classes`, and `cardClasses` are extension properties from `com.apexfission.android.carddetector.tfmodel` and require imports.

| Class | Label | Primary card? |
| --- | --- | --- |
| 0 | horizontal_card | Yes |
| 1 | vertical_card | Yes |
| 2 | horizontal_card_back | Yes |
| 3 | photo | No |
| 4 | slim-barcode | No |
| 5 | pdf417 | No |
| 6 | mrz-text | No |
| 7 | barcode | No |
| 8 | qrcode | No |

These are detection categories, not decoded text or barcode contents. The repository does not establish a verified benchmark, training-set provenance, or authenticity guarantee. Do not infer accuracy from the supplied recording.

## Bring your own model

Place a compatible model in the host's assets and supply its asset-relative path, labels, and nonempty primary-card class set. Hydrate Git LFS assets before building repository demos. Both `core` and `sentinel-card-model` are AARs; see [installation](../../IMPORT.md).

Inference belongs to the external [Apexfission YOLO library](https://github.com/lambdawalker/android.apexfission.yolo). The current integration expects one square NHWC input `[1,H,W,3]` and one output `[1,attributes,boxes]` or `[1,boxes,attributes]`, where the smaller distinct dimension is attributes (at least five). The four box coordinates are followed by class scores; incompatible objectness layouts are unsupported. FLOAT32 and INT8 input/output are supported by the dependency, with valid quantization for INT8. An FP16 filename describes weights, not permission to use FLOAT16 I/O. Consult the dependency's model contract when changing versions.

GPU is requested by some presets; it is device/model dependent. Do not promise universal GPU fallback or inference speed. Try `useGpu = false` to isolate delegate issues. The repository exports YOLO and coordinates as API dependencies because their types appear in public signatures. The demo alone uses the separate permissions library; host apps may use Android's permission launcher directly.
