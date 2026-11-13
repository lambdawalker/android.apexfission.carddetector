# Detection, models, and validators

`CardDetection` is metadata plus tracking state. Its `card` and `features` are YOLO `Feature` objects, not images. IDs are nullable while locking; `DetectionSource` distinguishes inference from hash continuation. `focusScore`, `focusState` and `isFocused` report card-region sharpness independently of tracking; `observedAtElapsedRealtimeMs` records monotonic evaluation time. Focus is a heuristic, not camera AF completion or OCR validation. The callback bitmap is delivered separately. Source-frame coordinate contracts are in [concepts](../concepts.md).

`CardDetector.track` is synchronous and returns null on no candidate. Bitmap/proxy inputs remain caller-owned; close proxies after the call. Close the detector to close its YOLO dependency. Both builders use the same defaults and a thread-confined wrapper. An optional shared physical dispatcher is borrowed and must outlive borrowers. After the wrapper is closed, track returns null and enabled reads false. Do not mutate/close the input while a synchronous call is active. See [recipes](../recipes.md).

Validators receive borrowed frames and return Boolean. Margin defaults to 20 pixels; aspect ratio accepts longest/shortest within 1.28..1.7 inclusive and rejects zero dimensions. The `configurationKey` controls recomposition identity. Primary card classes must be nonempty. Keep thresholds in sensible ranges; builder construction is not a comprehensive user-input validator.

The `ModelCatalog` marker belongs to core; tfmodel extension properties require the optional model artifact and explicit imports. The legacy carddetector `PreProcessingImageTransformation` remains exported, but presets require the distinct YOLO type. Do not interchange them.
