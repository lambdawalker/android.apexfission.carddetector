# Lifecycle, callbacks, and coordinates

## Pipeline

CameraX `ImageProxy` or Media3 video bitmap → single-flight admission and throttle → upright/preprocessed frame → hash continuation or YOLO inference → validators and candidate selection → temporal lock state → metadata and retained image → worker callback and Compose overlay.

`LockingCard` evaluates consistency; `NewCard` assigns a tracking ID; `CardLocked` continues that identity. `DetectionSource.Yolo` means inference; `Hash` means visual continuation. Hash continuation contributes fractional lock points. An ID is a transient tracker identity, not an identity-document number. Locking proves neither authenticity nor readable text.

## Ownership table

| Boundary | Ownership and cleanup |
| --- | --- |
| `CameraPreview.onFrame` | Receiver must close the `ImageProxy`. The built-in ViewModel does so for processed and rejected frames. |
| Low-level `track(Bitmap)` | Caller retains image and keeps it valid until the synchronous call returns. |
| Low-level `track(ImageProxy)` | Caller closes proxy; temporary converted bitmaps are released internally. |
| Detection callback | Host owns delivered card cutout from callback entry; recycle after final use. |
| Capture callback | Host owns an independent copy of the retained best cutout. |
| Retained best image | SDK-owned; do not recycle it through metadata or UI state. |
| `OcrWrapper.run` | Caller-owned input, kept valid while OCR runs; OCR does not recycle it. |
| `Bitmap.use` | Recycles in `finally`; complete all image work inside its block. |

Use `import com.apexfission.android.carddetector.resource.use`. Immediately recycling an unwanted delivered bitmap is valid. Launching asynchronous work inside `bitmap.use` and returning is invalid: the worker would receive recycled pixels. A coroutine that never begins also never enters its `finally`; an asynchronous handoff must cover cancellation before execution. The quickstart avoids this by releasing images synchronously, and [recipes](recipes.md) demonstrates cancellation-aware handoff.

## Callback scheduling

Detection delivery runs serially on a worker dispatcher separate from inference. At most one callback runs and one result waits; a newer pending result replaces the older undelivered image, which the library recycles. The host owns any already-delivered image, even when its callback throws. Do not treat this as a lossless stream, count frames using callbacks, or require a single `NewCard` event to trigger all work. Deduplicate non-null tracking IDs across `NewCard` and `CardLocked` when appropriate.

Capture also calls application code on a worker. Dispatch UI actions to the main thread. Move CPU or network work to a host-controlled dispatcher and bound downstream work. Pausing via `isDetectionEnabled` stops processing admission, not already-delivered host work.

## Capture and lifetime

The first candidate, a higher-confidence candidate, or a newly locked different ID can replace the retained best image. `captureEnabled` reflects retained data, not necessarily a currently visible card. Missing detections and detection pause do not themselves clear the store. Capture can return older retained data; reject it in the host when freshness matters. No capture callback is delivered when there is no retained image.

Compose ViewModels live in the current `ViewModelStoreOwner`. Keys include component kind, stable `instanceKey`, model metadata, detector preset, and validator configuration. Callbacks and camera-only settings are excluded. Changing configuration selects a new ViewModel; the old one can remain until its owner is cleared. Scope owners to scanner sessions and avoid continuously changing keys. Removal from composition alone does not imply native detector cleanup.

Camera binding coordinates asynchronous provider completion with disposal. Video preview releases its player on disposal. Low-level `BitmapFrameProcessor.release()` is a no-op and does not suppress callbacks already queued to its executor.

## Coordinate spaces

YOLO reverses letterboxing into its input image. UI ViewModels restore preprocessing crop offsets to the upright source frame. The callback bitmap is a card cutout, but `card.box` and feature boxes remain source-frame coordinates. Map them using the supplied `ImageSpaceChain` for screen overlays, or explicitly subtract the card crop origin for cutout-local work. `Feature` is YOLO metadata; it does not contain a cropped `image` property.

## Low-level resource ownership

`buildCardDetector` and `buildThreadConfinedCardDetector` return synchronous closeable detectors. They confine calls to a physical worker but still block the caller. The card detector closes its supplied YOLO detector. Use one compatible shared `EngineThreadDispatcher` for a GPU-backed stack; close borrowers before the dispatcher. Arbitrary coroutine worker switching does not establish GPU thread affinity.

`OcrWrapper.close()` is idempotent, rejects new calls with `IllegalStateException`, and defers recognizer disposal until active wrapper requests complete. `run` suspends; ML Kit failures propagate. There is no automatic OCR or upload in `CardDetectorLite`.
