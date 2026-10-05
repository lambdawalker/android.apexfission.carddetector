# Card Detector integration map

Scope: **main development source**. The website stamps the exact build commit and pins source links. [IMPORT.md](../../IMPORT.md) is the authoritative confirmed-release installation document; this guide does not claim every main API is available in that release.

The library detects and tracks a primary card, publishes metadata and cropped bitmaps, and provides camera/video Compose adapters. It is not document authentication, liveness, a barcode parser, or a complete identity verification system. An optional OCR wrapper returns text blocks only.

| Task | API | Read next |
| --- | --- | --- |
| First integration, permissions | `CardDetectorLite` | [Quickstart](quickstart.md) |
| Replay a video | `CardTrackingSimulator` | [Demos](demos.md) |
| Tune detection and crop | `CardDetectorPreset`, `CardValidator` | [Configuration](configuration.md) |
| Customize UI or capture | `CardDetectorOverlayScope`, `IdCaptureOverlayConfig` | [Recipes](recipes.md) |
| Own images / handle callbacks | `Bitmap.use`, detection/capture lambdas | [Concepts](concepts.md) |
| Detect without Compose / OCR | `buildCardDetector`, `OcrWrapper` | [Recipes](recipes.md) |
| Exact imports and declarations | Public reference by subsystem | [API](api.md) |
| Diagnose / migrate | Errors, old imports, limitations | [Troubleshooting](troubleshooting.md), [migration](migration.md) |

Critical invariants:

- Obtain camera permission before composing the camera. There is no built-in permission request.
- Provide stable, distinct, nonblank `instanceKey` values within a ViewModel owner.
- Both callback bitmaps are host-owned; recycle after final use, including failures and cancelled handoffs. Always provide consuming/recycling callbacks; defaults do not clean up delivered images.
- Detection callbacks are serial, worker-thread, latest-pending delivery, not a lossless frame stream. Do not rely on observing every `NewCard` transition.
- Capture copies retained best pixels, which can outlive a missed frame or detection pause. It is not a new still capture.
- Metadata boxes describe the upright source frame, not cutout-local or screen coordinates.
- Use YOLO's `PreProcessingImageTransformation` with `CardDetectorPreset`; the similarly named legacy carddetector type is distinct.

[Limitations](limitations.md) · [model contract](model-contract.md) · [maintenance](../maintenance.md) · [feature coverage](../coverage.md)
