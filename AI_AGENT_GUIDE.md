# AI Agent Integration Guide: CardDetectionLite

This guide provides technical instructions for AI agents and automated coding assistants on how to integrate, configure, and use the `CardDetectionLite` library within Android applications.

---

## 1. Library Overview & Purpose

`CardDetectionLite` is a real-time, GPU-accelerated Jetpack Compose module designed for **Stage 1 ID document detection, temporal tracking, and stabilization** (driver's licenses, passports, national IDs). 

It utilizes a custom YOLO v11 TensorFlow Lite (LiteRT) model and a multi-frame dHash perceptual similarity tracking engine. It is **not** an OCR or document classification library; its primary output is a high-resolution cropped `Bitmap` of the detected card along with metadata (`CardDetection`, sub-features like photos, barcodes, MRZ text).

---

## 2. Module Structure

When depending on or working within this repository, understand the multi-module workspace:
- **`:cardDetectionLite`**: Core UI composables (`CardDetectorLite`), CameraX adapters, tracking state machine, overlays, and simulator.
- **`:yolo`**: YOLO object detection engine, TFLite inference core, tensor buffers, validation, and NMS/IoU post-processing.
- **`:tfmodel`**: Bundled model assets (`ModelCatalog.TfLite.modelPath`), class labels (`ModelCatalog.TfLite.classes`), and card ID groupings (`cardClasses`).
- **`:coordinates`**: 2D coordinate transformation engine (`ImageSpace`, `ImageBox`, `ImagePoint`, `ImageSpaceChain`).
- **`:cryoto`**: Cryptography and key attestation utilities.
- **`:app`**: Executable sample and integration test bench.

---

## 3. Core Integration Patterns

### Dependency Setup (`app/build.gradle.kts`)
```kotlin
dependencies {
    implementation(project(":cardDetectionLite"))
    implementation(project(":yolo"))
    implementation(project(":tfmodel"))
    implementation(project(":permissionsCompose"))
}
```

### Composable Usage (`CardDetectorLite`)
To embed live camera card detection in a Compose screen, wrap it with `HandleCameraPermission`:

```kotlin
HandleCameraPermission(
    modifier = Modifier.fillMaxSize(),
    onBack = { /* handle back */ },
    onNotNow = { /* handle denial */ }
) {
    CardDetectorLite(
        instanceKey = "card-detection-session",
        modelPath = ModelCatalog.TfLite.modelPath,
        classLabels = ModelCatalog.TfLite.classes,
        cardClasses = ModelCatalog.TfLite.cardClasses,
        detectorPreset = CardDetectorPreset.HighPerformance,
        cameraPreset = CameraPreset.Default,
        isDetectionEnabled = true,
        onCardDetection = { detection, bitmap ->
            // Handle detection result & bitmap
        },
        onCapture = { detection, bitmap ->
            // Handle explicit capture
        },
        onBack = { /* handle back */ },
        controlOverlay = {
            IdCaptureOverlay()
        }
    )
}
```

---

## 4. Critical Rules & Best Practices for AI Agents

### 1. Bitmap Ownership & Lifecycle
- **Never recycle bitmaps synchronously** inside the detection or capture callback before dispatching them to a worker thread.
- The library delivers an independent `Bitmap` instance owned by the caller.
- Always process bitmaps on background dispatchers (`Dispatchers.Default` or `Dispatchers.IO`) and ensure `.use { ... }` or explicit `.recycle()` is called after processing.

```kotlin
override fun onCardDetection(detection: CardDetection, bitmap: Bitmap) {
    if (detection.lockingStatus == LockingStatus.NewCard) {
        viewModelScope.launch(Dispatchers.Default) {
            bitmap.use { processedBitmap ->
                // Perform OCR or upload
            }
        }
    } else {
        bitmap.recycle()
    }
}
```

### 2. Thread Safety & Dispatchers
- Detection and tracking callbacks run on library background worker threads. 
- Do not perform heavy blocking operations (like network uploads or heavy OCR) directly on the callback thread without switching to `Dispatchers.Default` or `Dispatchers.IO`.

### 3. Presets Selection
- Use `CardDetectorPreset.HighPerformance` for standard real-time scenarios (30 FPS, GPU enabled).
- Use `CardDetectorPreset.HighAccuracy` when full-frame analysis is required without center cropping.
- Use `CardDetectorPreset.BatterySaver` for low-power or restricted hardware (throttled to 10 FPS, CPU-only).

### 4. Testing with Simulator
- When writing UI or integration tests where physical camera streams are unavailable, use `CardTrackingSimulator` with a video resource URI (`android.source://$packageName/raw/video.mp4`).

---

## 5. Summary of Key Data Classes

- **`CardDetection`**: Contains `lockingStatus` (`LockingCard`, `NewCard`, `CardLocked`), `id`, `lockOnProgress`, `card` (`Feature`), and `features` (`List<Feature>`).
- **`Feature`**: Represents the card bounding box (`ImageBox`), confidence score, class ID, and cropped sub-feature bitmap.
- **`NumThreads`**: Configures CPU thread allocation (`NumThreads.Default`, `NumThreads.Half`, `NumThreads.CustomCount(2)`).
- **`CameraPreset`**: Configures resolution and focus (`CameraPreset.Default`, `CameraPreset.HighResolution`, `CameraPreset.FixedFocus`).
