# Migration and version scope

The site describes main development source, pinned to a commit when built. [IMPORT.md](../../IMPORT.md) describes the last confirmed public dependency and its source. No package release is created by documentation deployment.

## Earlier monorepo examples

Use `com.apexfission.android.carddetector` imports and current Maven coordinates. This workspace contains only `:carddetector`, `:tfmodel`, and `:app`. YOLO, coordinates, and permissions are separate published dependencies; do not add obsolete `:yolo`, `:coordinates`, `:cryoto`, or `:permissionsCompose` modules.

Replace `CardDetectionCallback`/`CardCaptureCallback` examples with `(CardDetection, Bitmap) -> Unit` function callbacks. The live API names are `onCardDetection`, `onCapture`, and `onBack`; the simulator uses `onCardDetection`, `onCaptureRequested`, and `onBackRequested`.

Replace `HandleCameraPermission` from old examples with your runtime permission gate. Supply a stable `instanceKey`. Read [ownership](concepts.md): immediate recycling of unused callbacks is valid, and asynchronous handoff must handle cancellation.

`Feature` comes from YOLO and contains metadata, not `image: Bitmap`. The delivered callback bitmap is separate. Use the YOLO preprocessing type in presets; the old similarly named carddetector type remains exposed but is not assignment-compatible.

Overlay compatibility aliases remain in `ui.overlays`; the canonical animation types live in `ui.overlays.animation`. Prefer canonical imports in new custom drawing. `AI_AGENT_GUIDE.md` now redirects to the focused integration guides. Existing module docs link to those canonical guides.
