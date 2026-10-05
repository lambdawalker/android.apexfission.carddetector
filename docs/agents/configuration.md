# Configure detection and presentation

## Detector presets

These are initializer values, not older KDoc descriptions. Intervals are minimum admission spacing, not measured or guaranteed FPS.

| Preset | Score | Lock points | Missing evaluations | Interval | GPU requested | Crop | CPU threads |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `HighPerformance` | 0.50 | 5 | 8 | 33 ms | Yes | `SquareCrop()` | `NumThreads.Default` |
| `HighAccuracy` | 0.80 | 7 | 6 | 33 ms | Yes | `FullImage` | `NumThreads.Default` |
| `BatterySaver` | 0.50 | 4 | 10 | 100 ms | No | `SquareCrop()` | `CustomCount(2)` |

All default to IoU 0.45, memory limit 1000 ms, class validation true, dHash distance limit 25, and temporal drift true. Drift compares against the previous frame; disabling it anchors consistency more strictly. Increasing score/lock thresholds can reject more candidates and increase acquisition time. Memory timeout and missing-evaluation limit reset tracking continuity; they do not clear retained capture images.

Partial configuration example (pass to `CardDetectorLite` or the simulator):

```kotlin
import com.apexfission.android.carddetector.ui.detector.CardDetectorPreset
import com.apexfission.android.yolo.image.PreProcessingImageTransformation

val preset = CardDetectorPreset.HighPerformance.copy(
    scoreThreshold = 0.6f,
    lockOnThreshold = 6,
    preProcessingImageTransformation = PreProcessingImageTransformation.FullImage,
)
```

`copy` names the field `preProcessingImageTransformation`; `change` names the same argument `imageMode`. Use finite sensible scores/IoU in 0..1, positive lock points, nonnegative intervals, and a dHash distance in 0..64; not every configuration is validated at construction.

## Camera

| Preset | Requested analysis size | Tap focus | Card focus |
| --- | --- | --- | --- |
| `Default` | 2048 × 1080 | On | On |
| `HighResolution` | 3840 × 2160 | On | On |
| `FixedFocus` | 2048 × 1080 | Off | Off |

Live `CardDetectorLite` defaults to **HighResolution**; simulator defaults to **Default** (no physical camera). CameraX/device negotiation determines actual size. Higher analysis size does not guarantee readable crops; lighting, focus, distance, and model region still matter. `AutoFocusPolicy` has a 500 ms cooldown, 50 px position threshold, and 10% area-change threshold. It produces a focus decision, not a guarantee the camera can focus.

## Cropping and validators

Import `com.apexfission.android.yolo.image.PreProcessingImageTransformation`. Options are `FullImage`, `CenterSquareCrop`, `SquareCrop(top)`, `CenterVisibleImage`, `VisibleImage(top)`, `CenterVisibleImageSquareCrop`, and `VisibleImageSquareCrop(top)`. Offset forms default to `0.dp`; explicit centered variants are distinct. Cropping changes the detection region, not the model tensor size. Use full image for off-center documents; ensure visible-region transforms have a measured viewport.

`MarginValidator(margin = 20u)` rejects boxes near image edges. `AspectRatioValidator(minAspectRatio = 1.28f, maxAspectRatio = 1.7f)` compares longest/shortest side and rejects degenerate boxes. Defaults can reject otherwise plausible documents. Override `cardFilters` intentionally for other shapes.

Custom validators implement `isValid(Detection, Detection?, Bitmap): Boolean`. Treat the bitmap as borrowed and do not recycle it. Override stable `configurationKey` when equivalent instances are recreated, or retain the validator with `remember`; default identity uses the object instance.

## Overlays

The default `controlOverlay` is empty. Supply `IdCaptureOverlay()` for the built-in indicator and controls, or compose `IdCaptureIndicatorOverlay()` and `IdCaptureControlsOverlay()` separately. `DetectionOverlay` shows detection boxes; `DebugOverlay` exposes diagnostics; `CardLockOnOverlay` uses animated guidance.

`IdCaptureOverlayConfig` controls text/color, opacities (0.35 idle / 0.85 detected), smoothing (enabled; factor 0.2), 200 ms tracking, 300 ms reset, 1000 ms fade/reset wait, and capture gating (true). Continuous effects default off. Setting `requiresCardDetectionForCapture = false` changes the button gate; it does not create a fresh-image capture API when no retained result exists. Override default text such as “pre-fill your information” to match what your host actually implements.

See [the exact overlay reference](api/overlays.md) for all parameters and animation overloads, and [recipes](recipes.md) for custom controls.
