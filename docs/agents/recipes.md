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

`TwoSidedCardCapture` provides front → explicit “Continue to back” → back → review. It shows the side on the camera feed, an initial square aiming area that disappears after the first detection for each side or retake, with an adaptive document outline, and `1 / 2` or `2 / 2` above the shutter. The turn-over screen also offers **Retake front** before continuing. Review allows independent retakes and keeps confirmation below the scrollable images. Crops retain the detected document rectangle; portrait and landscape documents use the same aiming area. The existing single-side `CardDetectorLite` remains available.

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

The auto-capture icon beside the shutter toggles automatic capture without discarding saved sides. Switching it on starts a fresh stability interval; switching it off rejects queued automatic captures. The flashlight icon toggles the torch on supported cameras.

Automatic capture is initially off by default. Set `CardCaptureConfig(autoCaptureEnabled = true)` to start enabled. Automatic capture requires current lock, card-region focus score of at least `focusThreshold` (default 100), and 600 ms of stable bounds. Gaps above 400 ms, misses, unfocused frames and movement reset readiness. Every detector evaluation updates readiness before UI delivery can conflate frames. The manual shutter stays available for a fresh detected crop, even below the automatic focus threshold. Both modes consume the current crop, not the older best image retained by `CardDetectorLite`.

`CardDetection.focusScore` is Laplacian variance of luminance on an inset card region, scaled to a longest edge of at most 256 px. `focusState` is Unknown, Focused or Unfocused; `isFocused` uses the default threshold of 100. `observedAtElapsedRealtimeMs` is a monotonic evaluation timestamp, not wall-clock time. These values are also reported by low-level/video detection. This heuristic is **not camera autofocus completion, OCR readability, glare assessment, or side classification**. Noise, lighting and texture affect it; calibrate `focusThreshold` on target devices/documents. An unavailable/invalid score never authorizes automatic capture. A custom capture threshold does not change the detection's default `isFocused` threshold.

English copy is the fallback. Android resources also provide Spanish, German, French, Italian, Japanese, Korean, Polish, Portuguese, Russian, Ukrainian, Simplified Chinese and Traditional Chinese. Override individual strings with `defaultCardCaptureStrings().copy(frontInstruction = "…", backInstruction = "…")`, or supply a complete `CardCaptureStrings`. The side labels guide users; showing the same side twice is not automatically rejected.

## Single-side automatic capture

Use `SingleSideCardCapture` for a complete single-side flow: capture → review → confirm, with retake available before confirmation. The host must obtain camera permission first and supply the same model metadata as the quickstart. Automatic capture is opt-in; the demo enables it. The on-screen toggle switches modes without starting a new attempt. Manual capture remains available for a fresh crop even while automatic focus/stability checks are pending.

```kotlin
// Imports: ui.capture.SingleSideCardCapture and ui.capture.CardCaptureConfig
SingleSideCardCapture(
    instanceKey = "single-document",
    modelPath = modelPath,
    classLabels = classLabels,
    cardClasses = cardClasses,
    config = CardCaptureConfig(autoCaptureEnabled = true, hapticFeedbackEnabled = true),
    onBack = onBack,
    onCapture = { detection, bitmap ->
        try {
            // Consume synchronously, or transfer ownership safely to your own work queue.
        } finally {
            bitmap.recycle()
        }
    },
)
```

`onCapture` runs **after review confirmation, once per attempt on the main thread**, and transfers the captured crop to the host. Taking a photo pauses the camera/detector and opens the shared review screen with one image. **Retake front** or the review back arrow discards that image and starts a fresh camera attempt; neither calls the host callback. The confirmation button transfers ownership, and disposal must not recycle the confirmed image. The flow stops its camera/detector owner during review and after completion; change `instanceKey` to start another attempt. Unused/replaced/late frames are recycled internally. Removing the composable cancels the attempt and releases undelivered images. Model, detector/camera, validator, or capture-configuration changes restart the attempt. Heavy image work must leave the main thread through a cancellation-safe ownership handoff. This differs from `CardDetectorLite.onCapture`, which runs on a worker and copies a retained best image.

Both complete capture flows share `CaptureReadiness`: 600 ms of stable, locked, focused detections, a default focus threshold of 100, and a maximum gap/age of 400 ms. Every evaluation, including misses, reaches this gate before UI conflation. Turning automatic capture off rejects queued auto-ready frames; turning it on resets the dwell interval. A duplicate, stale, blurred, moving, or missed frame cannot advance readiness by a timer alone. Successful capture produces haptic confirmation by default (`hapticFeedbackEnabled = true`). Set `hapticFeedbackEnabled = false` to disable it. Automatic capture remains opt-in.

Guidance distinguishes **looking for a card**, **finding focus**, **hold steady**, **ready to capture**, and **photo captured**. Manual readiness text never implies the document was validated or OCR succeeded. `readyToCapture`, `captured`, `singleInstruction`, and the existing strings are customizable through `defaultCardCaptureStrings().copy(...)`. Status text uses a polite live region. Narrow/large-text layouts can scroll to controls; sufficiently wide landscape layouts place instructions and controls in separate columns. These are same-source rendering checks, not persistent visual baselines.
