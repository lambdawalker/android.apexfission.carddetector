# Camera and video components

Call composables from a lifecycle-aware Compose host. Obtain camera permission first. `instanceKey` must be nonblank, stable, and unique among sibling detectors. Models/classes/validators configure the detector; presets control admission and tracking. See [configuration](../configuration.md) for parameter meanings and actual preset values.

`CardDetectorLite` returns Unit and wires CameraX to worker detection; its camera default is HighResolution. Both callback lambdas receive a host-owned cutout; always consume/recycle them. `onBack` is an overlay user action. The default overlay is empty. Simulator uses a readable `videoUri`, requires `onCardDetection`, and names its actions `onCaptureRequested` / `onBackRequested`. It has no torch. See [ownership and lifetime](../concepts.md).

`SingleSideCardCapture` adds manual/optional-automatic capture, one-image review and retake. Its `onCapture` delivers a host-owned crop on the main thread only after confirmation; unconfirmed images remain session-owned. See [single-side capture](../recipes.md#single-side-automatic-capture).

`TwoSidedCardCapture` adds an opt-in front/back flow with a square guide, explicit turn-over (including front retake), review and independent retakes. Its `onComplete` transfers two owned bitmaps on the main thread. `CardCaptureConfig` controls optional automatic capture; `CardCaptureStrings` supports localized instruction overrides. See [both-side capture](../recipes.md#capture-both-sides).

Scope `detectionState`/`cardDetection` describe the current evaluation, `detectionSequence` changes on misses too, and `latestBestDetection`/`captureEnabled` refer to retained capture. `capture()` copies retained data, `goBack()` invokes the host action, and `toggleFlashlight()` requests torch state where available. `imageSpaceChain` can be null before preview measurement.
