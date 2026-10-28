# Card Detector

Detect, track, and crop cards in an Android camera or video feed with Jetpack Compose, CameraX, and on-device YOLO inference. Use the resulting bitmap in your own OCR or document-processing pipeline. Temporal lock-on means visual stability; it does not establish document authenticity or identity.

**[Documentation website](https://lambdawalker.github.io/android.apexfission.carddetector/)** · **[Installation](IMPORT.md)** · **[AI integration](docs/agents/index.md)** · **[Demo catalog](docs/agents/demos.md)**

**Versioned guides:** [English](https://lambdawalker.github.io/android.apexfission.carddetector/en/) · [Español](https://lambdawalker.github.io/android.apexfission.carddetector/es/). Select the module and release independently; installation pages show the confirmed destinations for that exact version.

## See it in motion

[![Card detection around a sample card at 12 seconds](docs/screenshots/tracking.png)](https://lambdawalker.github.io/android.apexfission.carddetector/demos/)

Watch the [screen recording with playback controls](https://lambdawalker.github.io/android.apexfission.carddetector/demos/), or [download the original recording](https://github.com/lambdawalker/android.apexfission.carddetector/raw/main/docs/demo.mp4). This September 17, 2026 recording illustrates the moving guide; its capture commit/device configuration is unknown. It is not a benchmark or current-release test.

## Integrate

Use the confirmed coordinates generated in [IMPORT.md](IMPORT.md). Choose the `carddetector` module for your own compatible model or `tfmodel` for bundled assets and its pinned compatible detector. Artifact names and repository requirements vary by destination; copy the generated installation example rather than combining old and new coordinates. The [complete quickstart](docs/agents/quickstart.md) includes imports, permission handling, and explicit bitmap cleanup.

- Single-side live camera: `CardDetectorLite`.
- Front/back capture with review, retakes, and optional automatic capture: [`TwoSidedCardCapture`](docs/agents/recipes.md#capture-both-sides).
- Recorded video: `CardTrackingSimulator`.
- Presentation: `IdCaptureOverlay`, custom scoped controls, and animation helpers.
- Configuration: detector/camera presets, preprocessing, and `CardValidator`.
- Without Compose: `buildCardDetector` and synchronous `track` overloads.

Every delivered callback bitmap belongs to the host. Recycle after final use. Single-side detection/capture callbacks run on worker threads; detection delivery can skip pending results and capture uses a retained best image. Two-sided capture uses fresh crops and transfers the confirmed front/back pair on the main thread. Read [ownership and lifecycle](docs/agents/concepts.md).

## Repository

| Module | Purpose |
| --- | --- |
| `:carddetector` | Core, camera/video adapters, tracking, overlays, optional OCR wrapper |
| `:tfmodel` | Sentinel model assets and `ModelCatalog.TfLite` extensions |
| `:app` | Welcome catalog, manual/automatic two-sided capture, video simulation, and minimal quickstart |

YOLO and coordinates come from Maven Central; the demo permissions dependency comes from JitPack. No sibling checkouts or submodules are required. Use JDK 21 for the Gradle daemon, JDK 17 for compilation, and Android SDK 37. Hydrate models and media with `git lfs pull`. See [testing prerequisites and dependency compatibility](docs/maintenance.md#android-verification).

```bash
./gradlew :carddetector:testDebugUnitTest :app:assembleDebug
python3 -m pip install -r scripts/requirements-setup.txt
cd sites
npm ci
npm run check
```

The existing unversioned guides describe **main development source**. The versioned site preserves release-specific guides and source links in English and Spanish. Published installation facts come independently from confirmed release metadata. See [maintenance](docs/maintenance.md), [coverage](docs/coverage.md), and the [release runbook](docs/releases.md). Apache-2.0; see [LICENSE](LICENSE).

## Independent releases

Use **Publish card-detector** or **Publish card-detector-model**, then select the destination repository. Each module/destination has its own version and release history; model releases pin a compatible published core. Configure destinations in `publishing/repositories.yml` and use the [Textual environment setup wizard](docs/releases.md#interactive-environment-setup) to create or update their GitHub settings. See [the release runbook](docs/releases.md).
