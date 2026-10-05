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
