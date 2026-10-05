# Documentation and example coverage

Scope is main source. Public declaration inputs and internal/private classification are checked in `api-inputs.json`; the build fails on changed or newly added inputs until documentation is reviewed. Human HTML is rendered from the same canonical guide named below, while raw Markdown remains separately retrievable. Source snippets are reference excerpts, not standalone executable files.

| Feature | Human route | Canonical agent guide | Runnable example / evidence | Visual scenario |
| --- | --- | --- | --- | --- |
| Install / bundled model / requirements | installation | [IMPORT](../IMPORT.md), [model contract](agents/model-contract.md) | tfmodel + app build | Not useful |
| Live camera and runtime permission | getting-started | [quickstart](agents/quickstart.md), [components API](agents/api/components.md) | DocumentationQuickstartActivity; live Activity | Historical recording; fresh device verification required |
| Temporal tracking / callback delivery | concepts | [concepts](agents/concepts.md), [detection API](agents/api/detection.md) | Camera/simulator; CardLockStateMachineTest, LatestCallbackDispatcherTest | Recording illustrates movement, not correctness |
| Capture / image ownership / cancellation | task-recipes | [recipes](agents/recipes.md), [concepts](agents/concepts.md) | Quickstart + OwnedBitmapWork; bitmap/store tests | Not provable with stills |
| Detector/camera presets | configuration | [configuration](agents/configuration.md), [components](agents/api/components.md) | CameraPresetTest, CardDetectorPresetTest; demo overrides | Device-specific; no new baseline |
| Preprocessing / validators / custom models | model-contract, configuration | [model](agents/model-contract.md), [detection](agents/api/detection.md) | Validator/engine tests; FullImage choice via configuration | No manufactured screenshots |
| Overlay controls, split overlays | task-recipes | [recipes](agents/recipes.md), [overlays API](agents/api/overlays.md) | Live full overlay; simulator split controls | historical-card-tracking at 12 s |
| Animation / drawing / aliases | api/overlays | [overlays](agents/api/overlays.md), [migration](agents/migration.md) | AnimatedDetectionBoundsTest, ContinuousAnimationPolicyTest | Historical guide only |
| Video simulation / frame ownership | demos | [demos](agents/demos.md), [adapters](agents/api/adapters.md) | CardDetectionSimulationActivity; OwnedFrameBitmapTest | Actual recording is separate from raw video fixture |
| Low-level synchronous detector | task-recipes, api/detection | [recipes](agents/recipes.md), [detection](agents/api/detection.md) | CardDetectorThreadTest, CardDetectorConcurrencyTest, device tests | Nonvisual contract |
| dHash / bitmap helper | api/utilities | [utilities](agents/api/utilities.md) | HammingDistanceAndVisualSimilarityTest, BitmapUseTest | Nonvisual |
| Optional OCR text blocks | task-recipes, api/utilities | [recipes](agents/recipes.md), [utilities](agents/api/utilities.md) | OcrWrapperLifecycleTest, OcrLoggingTest; host supplies real OCR integration | No OCR success claimed |
| Low-level camera/video, public ViewModels/factories | api/adapters | [adapters](agents/api/adapters.md) | Built-in component wiring; camera-provider/focus tests | Hardware validation required |
| Errors and migration | troubleshooting, migration | [troubleshooting](agents/troubleshooting.md), [migration](agents/migration.md) | Failure paths in demos and unit tests | Text is authoritative |
| Build / release / media | development, releases, media | [maintenance](maintenance.md), [releases](releases.md) | Release tooling tests; docs workflow | Manifest integrity, separate candidate workflow |

The only new runnable screen fills the missing minimal, dependency-independent permission + cleanup example. Existing live/video demos remain the broader test benches. No redundant demo was added for every helper function. Hardware, inference accuracy and current screenshots require device validation; the website build does not certify them.
