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

Both complete capture flows call the public `IdCaptureOverlay()` directly, sharing its layout with `IdCaptureControlsOverlay()`. The low-level `IdCaptureOverlay` hides the side/step row and hides the automatic-capture toggle by default. Set `showAutoCaptureToggle = true` only when the host implements that policy and supplies `autoCaptureEnabled` plus `onToggleAutoCapture`; the overlay itself never starts automatic capture. Two-sided capture owns that policy and displays the step and mode text. Flash controls are omitted when unavailable, with balanced spacing around the shutter.

The theme-colored initial square fades to transparent over **250 ms** after the first observed detection (including a retained detection). It stays hidden through missed frames for the mounted capture attempt. Front, back and each retake start a fresh attempt and restore the square; changing a single-side detector's `instanceKey` resets its overlay state. `showInitialGuide = false` suppresses it entirely. The colored tracking indicator is separate: the complete capture overlays give it zero idle opacity to avoid a second static rectangle, while standalone `IdCaptureIndicatorOverlay(config)` retains the configurable idle opacity described above. Still screenshots show the start/end states; the emulator transition tests advance the Compose clock and inspect intermediate pixels to verify the actual fade.

For built-in single-side automatic capture, use `SingleSideCardCapture`; its own policy drives the toggle and guidance. Both complete flows accept `CardCaptureConfig`, including success haptics enabled by default; set `hapticFeedbackEnabled = false` to opt out. See [the single-side recipe](recipes.md#single-side-automatic-capture). Low-level `IdCaptureOverlay` still only renders controls and requires the host to implement automatic capture.

`IdCaptureOverlay` and `IdCaptureControlsOverlay` accept `step`, `modeText`, `canCapture`, `onCapture` and `strings`. Leave the defaults for `CardDetectorLite` retained-image capture. A host implementing its own fresh-frame flow must override both availability and the capture callback together. `step = null` hides side progress; `modeText = null` hides the mode label. Supplying `strings` customizes control accessibility labels; pass a matching `modeText` when overriding localized mode copy. These render-only composables do not manage review, bitmap ownership or automatic-capture policy.

The shared review screen supports one or two images, per-side retake controls and a fixed confirmation area below scrollable image content. `CardCaptureStrings.privacyTerms` supplies the privacy notice for both review variants; `defaultCardCaptureStrings()` loads its Android-localized resource. Override it with `defaultCardCaptureStrings().copy(privacyTerms = "Your application's privacy notice")`. It remains plain text without a built-in link or upload action. Review cards and the elevated confirmation footer use `surfaceContainerLow` from the host's Material theme.

### Host theme and overlay defaults

Built-in capture controls and review surfaces inherit the host `MaterialTheme` colors, typography, and shapes. Opaque theme surfaces behind camera instructions and controls preserve text contrast against changing preview content. Camera guide geometry, animation timings, touch targets, and image sizing remain intrinsic to the capture UI. `IdCaptureOverlayConfig.guideColor = Color.Unspecified` resolves to the host primary color during composition; explicit colors remain supported. Use `defaultIdCaptureOverlayConfig()` for localized built-in overlay text and `.copy(...)` to override individual fields. The plain configuration constructor retains its English text defaults for compatibility.
