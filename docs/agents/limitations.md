# Limitations and unsupported assumptions

- Android API 28+, Compose/CameraX integration; this is not a desktop or multiplatform detector.
- One primary card is tracked. Other detected features are metadata; no feature bitmap, barcode decoding, MRZ parsing, authenticity check, or identity decision is supplied.
- Preset names express intent. No measured FPS, accuracy, or universal GPU/device compatibility claim is established here.
- Default geometric filters target card-like rectangles and may reject passports, strong perspective distortion, clipped cards, and other shapes.
- Callback delivery is latest-pending. A `NewCard` event can be replaced before delivery. Use tracking-ID deduplication when designing automatic processing.
- Capture may return a previous retained image after a miss/pause; it is not a fresh still-camera request. Turning off overlay capture gating does not change this.
- The caller owns callback images even if callbacks throw. Default no-op callback arguments do not recycle images.
- ViewModel cleanup follows its owner, not merely composition removal. Configuration changes can leave old keyed ViewModels alive until owner cleanup.
- The low-level video processor can deliver queued callbacks after `release()`. The simulator is not a deterministic rendering baseline or a camera hardware test.
- Camera/engine initialization errors do not have a unified public `onError` callback. Inspect Logcat and host lifecycle/permission setup; do not invent an error API.
- The historical recording's capture source, OS, density, locale, and device are unknown. It illustrates interaction, not release evidence. Still frames cannot prove camera lifecycle or motion behavior.
- Main documentation and confirmed release metadata have different scopes. Compare source against the confirmed release before depending on new APIs.
