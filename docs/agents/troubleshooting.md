# Troubleshooting

| Symptom | Likely check and correction |
| --- | --- |
| Blank camera / permission failure | Declare CAMERA and request runtime permission before composition; test camera availability and lifecycle. The quickstart includes retry/settings actions. |
| `ModelCatalog.TfLite.modelPath` unresolved | Add the model artifact and import `tfmodel.modelPath`, `classes`, and `cardClasses`. Core alone has no bundled model extensions. |
| `HandleCameraPermission` / callback interfaces unresolved | Old examples are obsolete. Use function callbacks and your host permission gate; see quickstart. |
| Model load failure | Check asset-relative path, hydrated LFS data, model tensor layout, class mapping, and device delegate support. Retry with CPU to isolate GPU failures. |
| Card never locks | Inspect score, margin/aspect filters, crop region, missing-frame limit, hash consistency, lighting and focus. Lock points are not simply number of callbacks. |
| No overlay | `controlOverlay` defaults empty; supply `IdCaptureOverlay()` or custom UI. |
| Duplicate processing / missed NewCard | Delivery can coalesce. Deduplicate non-null IDs and bound host work rather than depending on a lossless transition event. |
| Recycled bitmap crash | Do not launch work inside a `Bitmap.use` block and return before the worker finishes. Keep image valid until final consumer is done. |
| Memory growth | Supply both cleanup callbacks, bound queued work, cover cancellation before coroutine execution, and clear the scanner's ViewModel owner. |
| Shutter returns an older card | Capture copies retained best pixels, not a new frame. Add host freshness/session rules. |
| Overlay shifted | Map upright-source boxes through `imageSpaceChain`; do not interpret callback cutout coordinates as source or preview coordinates. |
| Wrong preprocessing type | Use `com.apexfission.android.yolo.image.PreProcessingImageTransformation`, not the legacy same-named carddetector type. |
| Simulator blank | Use a readable `android.resource://<applicationId>/raw/x` or other supported URI; confirm the video is real LFS data. |
| Website contains LFS pointer text | Hydrate assets (`git lfs pull`) and run media verification. Generated site media must be real bytes. |
| IMPORT verification fails | Run `python3 scripts/module_release.py generate` only from confirmed metadata and inspect the diff; do not advertise an unconfirmed version. |

For native/inference issues provide device/API, model contract, CPU/GPU choice, preset, and a synthetic reproduction. The demo's OCR methods are placeholders: no text or network result is expected from them.
