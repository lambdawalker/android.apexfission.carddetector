# Task recipes

These are partial integration snippets. The [quickstart](quickstart.md) provides the complete runnable host and setup.

## Customize controls

Inside `controlOverlay`, receiver properties/actions are supplied by `CardDetectorOverlayScope`:

```kotlin
// Imports in the surrounding Compose file:
// import androidx.compose.material3.Button
// import androidx.compose.material3.Text
// import com.apexfission.android.carddetector.ui.overlays.IdCaptureIndicatorOverlay
controlOverlay = {
    IdCaptureIndicatorOverlay()
    Button(enabled = captureEnabled, onClick = { capture() }) { Text("Use this card") }
    if (flashlightAvailable) {
        Button(onClick = { toggleFlashlight() }) { Text("Toggle light") }
    }
}
```

Provide both cleanup callbacks in the parent scanner. Use `detectionSequence` for every completed evaluation, including consecutive misses; `latestBestDetection` is retained capture data. UI text should describe the host's real processing. Never assume default overlay text means the SDK securely pre-fills fields.

## Process each observed tracking ID once

Detection callbacks can skip `NewCard`. In your serial callback, accept a non-null ID in `NewCard` or `CardLocked`, compare with your last accepted ID, and recycle rejected bitmaps. Keep that ID scoped to the detector session; do not persist it as a document identity. Reset host state on a new scan and bound work so processing cannot grow without limit.

For coroutine handoff, this helper covers cancellation before the coroutine body starts as well as after it starts. Call it with the single host-owned bitmap; `process` must not retain or recycle it. Imports are shown; this helper is included in the app source and checked against the rendered snippet.

<!-- example: ownership -->

```kotlin
package com.apexfission.android.carddetector.demo

import android.graphics.Bitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/** Transfers one bitmap into bounded host work; process must neither retain nor recycle it. */
fun CoroutineScope.consumeOwnedBitmap(
    bitmap: Bitmap,
    process: suspend (Bitmap) -> Unit,
): Job {
    val released = AtomicBoolean(false)
    fun release() {
        if (released.compareAndSet(false, true)) bitmap.recycle()
    }
    return try {
        launch(Dispatchers.Default) {
            try { process(bitmap) } finally { release() }
        }.also { job -> job.invokeOnCompletion { release() } }
    } catch (error: Throwable) {
        release()
        throw error
    }
}
```

<!-- end-example: ownership -->

The host still chooses dispatcher, task admission, and deduplication. Do not use this as an unbounded queue per camera frame.

## Change preprocessing or validation

Use `CardDetectorPreset.HighPerformance.copy(...)` as shown in [configuration](configuration.md). A remembered `CardValidator` can reject small boxes, glare, or a shape outside your use case; keep its computation fast and its borrowed bitmap valid. Supply `cardFilters = emptyList()` only when intentionally removing both default geometric checks.

## Replay a video

Pass a readable URI to `CardTrackingSimulator`, using the model extensions and callbacks from the quickstart. Use `onCaptureRequested` / `onBackRequested` rather than the camera names. The [simulation Activity](../../app/src/main/java/com/apexfission/android/carddetector/demo/CardDetectionSimulationActivity.kt) is runnable with `raw/x.mp4`. Simulator flashlight is unavailable; it does not validate real camera focus or permission behavior.

## Use a low-level detector

Use `buildCardDetector(yoloDetector = existingDetector, cardClasses = setOf(0, 1, 2))` with a compatible YOLO detector. Both builder names return thread-confined synchronous detectors. `track(Bitmap)` and `track(ImageProxy)` return nullable metadata; they do not return the Compose callback cutout. Keep the input valid, close proxies yourself, and close the card detector when done. It closes the supplied YOLO detector, so do not share that detector with unrelated owners. For a GPU stack, construct/operate/close all layers on a compatible shared physical dispatcher. The [engine reference](api/detection.md) lists all builder defaults.

## Read text after capture

```kotlin
import android.graphics.Bitmap
import com.apexfission.android.carddetector.domain.ocr.OcrResult
import com.apexfission.android.carddetector.domain.ocr.OcrWrapper

suspend fun recognizeOwnedCrop(bitmap: Bitmap): List<OcrResult> {
    val ocr = OcrWrapper()
    try {
        return ocr.run(bitmap)
    } finally {
        ocr.close()
        bitmap.recycle()
    }
}
```

Invoke only after your coroutine begins with valid ownership, or use the handoff helper and omit this function's bitmap recycling to keep one owner. OCR returns Latin text blocks and nullable bounding rectangles relative to the input crop, not structured ID fields. For repeated requests reuse a lifecycle-owned wrapper and close it after use.

## Capture both sides

`TwoSidedCardCapture` provides front → explicit “Continue to back” → back → review. It shows the side on the camera feed, a square aiming area with an adaptive document outline, and `1 / 2` or `2 / 2` above the shutter. Review allows independent retakes. Crops retain the detected document rectangle; portrait and landscape documents use the same aiming area. The existing single-side `CardDetectorLite` remains available.

Obtain camera permission first. Use the model imports from the [quickstart](quickstart.md), then compose:

```kotlin
import com.apexfission.android.carddetector.ui.capture.CardCaptureConfig
import com.apexfission.android.carddetector.ui.capture.TwoSidedCardCapture
import com.apexfission.android.carddetector.ui.capture.defaultCardCaptureStrings

TwoSidedCardCapture(
    instanceKey = "identity-document",
    modelPath = ModelCatalog.TfLite.modelPath,
    classLabels = ModelCatalog.TfLite.classes,
    cardClasses = ModelCatalog.TfLite.cardClasses,
    config = CardCaptureConfig(autoCaptureEnabled = true),
    strings = defaultCardCaptureStrings(),
    onBack = onBack,
    onComplete = { result ->
        // Example consumes synchronously; transfer ownership safely for async processing.
        try {
            // Consume result.front and result.back here.
        } finally {
            result.front.bitmap.recycle()
            result.back.bitmap.recycle()
        }
    },
)
```

`onComplete` runs once on the **main thread** and transfers both bitmaps to the host. Until confirmation the flow owns them and recycles discarded frames, retaken images, and cancelled sessions. Use a cancellation-safe ownership handoff for asynchronous OCR/upload. Changing `instanceKey`, model metadata, detector/camera settings, validator configuration, or capture configuration restarts the whole flow and releases its previous images. Each side/retake uses a fresh, disposable detector owner; activity recreation starts again. The turn-over screen displays the captured front while the camera is stopped.

Automatic capture is off by default. Enable `CardCaptureConfig(autoCaptureEnabled = true)` to require current lock, card-region focus score of at least `focusThreshold` (default 100), and 600 ms of stable bounds. Gaps above 400 ms, misses, unfocused frames and movement reset readiness. Every detector evaluation updates readiness before UI delivery can conflate frames. The manual shutter stays available for a fresh detected crop, even below the automatic focus threshold. Both modes consume the current crop, not the older best image retained by the single-side API.

`CardDetection.focusScore` is Laplacian variance of luminance on an inset card region, scaled to a longest edge of at most 256 px. `focusState` is Unknown, Focused or Unfocused; `isFocused` uses the default threshold of 100. `observedAtElapsedRealtimeMs` is a monotonic evaluation timestamp, not wall-clock time. These values are also reported by low-level/video detection. This heuristic is **not camera autofocus completion, OCR readability, glare assessment, or side classification**. Noise, lighting and texture affect it; calibrate `focusThreshold` on target devices/documents. An unavailable/invalid score never authorizes automatic capture. A custom capture threshold does not change the detection's default `isFocused` threshold.

English copy is the fallback; Spanish follows the Android locale. Override individual strings with `defaultCardCaptureStrings().copy(frontInstruction = "…", backInstruction = "…")`, or supply a complete `CardCaptureStrings`. The side labels guide users; showing the same side twice is not automatically rejected.
