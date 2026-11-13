# Advanced camera and video adapters

These public declarations expose lower-level integration points. Prefer `CardDetectorLite` / `CardTrackingSimulator`: custom adapters must reproduce ownership, lifecycle, dispatching, and cleanup themselves. Public ViewModels and factories are implementation-facing; their signatures do not imply safe independent native-resource sharing.

`CameraPreview` binds preview and analysis to the supplied lifecycle owner. Its `onFrame` receiver must close every ImageProxy. Frame callbacks follow CameraX dispatch, not a main-thread UI guarantee. `createPreviewImageSpaceChain` constructs rotation/preview mapping; use actual measured sizes, not guessed screen dimensions. `AutoFocusPolicy` is stateful; serialize access, pass a consistent clock, and reset for a new session. It only recommends a point/action.

`VideoPreviewWithFullFrameCapture` owns/releases its Media3 player; its frame consumer owns delivered bitmaps. `BitmapFrameProcessor` invokes callbacks on the supplied executor, transfers ownership at delivery, and clamps capture intervals. `release()` is currently a no-op; queued callbacks can outlive it. These APIs use Media3 unstable interfaces and may need OptIn in host code.

ViewModel process methods admit work with a single-flight gate; rejected frames are released. `captureLatest` delivers an independent owned copy on a worker. `setDetectionEnabled` controls processing; it does not cancel already-delivered host tasks. Native resources and retained/callback buffers are released by ViewModel clearing. Factories are intended for the Android ViewModel system, not repeated manual construction. All callbacks and retained-image caveats in [concepts](../concepts.md) apply.
