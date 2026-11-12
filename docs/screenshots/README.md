# Recording and screenshot provenance

The primary showcase is the repository's supplied `../demo.mp4`. It is approximately 31 seconds, H.264, 1080 × 2340, with no narrated audio track. `manifest.json` records its content hash and the repository commit that supplied it. The original capture commit/device/OS/density/font-scale configuration is unknown; these fields remain explicitly null.

`tracking.png` is a reviewed still at 12 seconds, extracted with FFmpeg at width 540. It shows a cyan outline around a sample card. It is an illustrative recording frame, **not** a new device screenshot or a screenshot regression baseline. The recording and still do not establish current-release functionality or performance. Textual API contracts remain authoritative for integrations.

## Reproduce and review

Run from the repository root with Python 3 and FFmpeg (the accepting tool version is recorded in the manifest):

```bash
python3 scripts/documentation/media.py render   # Candidate under build/docs-media; never overwrites accepted media
# Open and inspect build/docs-media/tracking.png before accepting.
python3 scripts/documentation/media.py compare # Re-extract and compare to the accepted file; fails on differences
python3 scripts/documentation/media.py accept  # Intentionally accept the reviewed candidate + provenance
python3 scripts/documentation/media.py verify  # Integrity/required-scenario check; no rendering or acceptance
```

Keep accepted originals here. Small curated stills use ordinary Git storage; the original recording remains in Git LFS. Site copies are generated and ignored. CI verifies input/output hashes and required scenarios; the manual media workflow only renders/uploads candidates. A decoder/toolchain change may alter pixels or PNG encoding: investigate and intentionally review it, never silently accept differences. Use the recorded FFmpeg version for exact reproduction.

A different repository commit alone does not invalidate the historical still; it depends on the recording hash and render configuration. Verification of unchanged inputs lives in CI logs, separately from capture provenance. This historical asset can pass integrity verification without claiming fresh device behavior.

## Capture a current demonstration when needed

Use a device running the exact source you want to demonstrate, synthetic/sample cards, fixed orientation and locale. Record model, app/source SHA, Android/device version, viewport/density, theme/font scale, lighting, animation settings and any external inputs. For the live demo, obtain camera permission, frame the sample card, try capture/back and denial/recovery. Use `adb shell screenrecord /sdcard/card-demo.mp4`, then `adb pull /sdcard/card-demo.mp4` to collect it; inspect the entire recording before replacing canonical media. Device interaction is manual and not claimed deterministic.

A release that changes UI or camera behavior needs current device verification; the existing recording is never reused as that evidence. Static frames alone cannot validate motion, permissions, focus or lifecycle. The present documentation change adds no new current-device capture claim.

## Render current UI states on the host

The website overview groups the reviewed previews into two-sided capture, single-side capture and overlays, and the demo catalog. `ui-manifest.json` records the 23 curated images and their hashes, dimensions, render commits, and workflow runs. Per-image provenance overrides the top-level render when a later run supplied the image. English and Spanish overviews select the matching capture and welcome screens; overlay examples are shared. These development previews are illustrative on every version's overview; they do not assert that an archived release provides the displayed UI.

The **Render documentation screens** workflow renders Compose previews for `:carddetector` and `:app`, then compares a second render with those generated images. It runs for PRs and pushes to main/previews and can be dispatched manually. Download `compose-previews-carddetector` and `compose-previews-app` from the run. Each archive includes PNGs, reports, and `render-manifest.json` with the exact source SHA and image hashes. Missing, duplicate or unexpected scenarios fail the job; `scripts/screenshots/screenshot-scenarios.json` lists the required scenario names, rather than only an image count. Run `python3 scripts/screenshots/screenshot_manifest.py --module carddetector` (or `--module app`) after rendering to validate them locally and write the candidate manifest.

```bash
./gradlew :carddetector:updateDebugScreenshotTest :carddetector:validateDebugScreenshotTest
./gradlew :app:updateDebugScreenshotTest :app:validateDebugScreenshotTest
```

The reference PNGs are generated under each module's `src/screenshotTestDebug/reference/`; HTML reports are under `build/reports/screenshotTest/`. These transient images are not approved regression baselines. Review candidates before adding selected images to documentation. The workflow never commits or silently accepts baselines. It uses the standalone screenshot plugin compatible with this repository's AGP 9.4 toolchain.

The suite renders **62 library scenarios and 5 app scenarios**: the welcome catalog, single-side overlays, idle and ready front/back guides, focusing/manual-capture states, light/dark turn-over and review, large-font review, enabled/disabled shutter buttons, and detection/debug/lock-on overlays. Additional snapshots cover the optional single-side auto control, unavailable flash, and frozen post-detection miss states for single-side/front/back capture. Capture and welcome screens include English/Spanish variants at 393 × 852 dp and explicit static themes. The review accessibility scenario uses font scale 1.3; the explicit layout stress cases use 1.6; remaining scenarios use 1.0. Shutter component captures are 160 × 160 dp. The card fixture is drawn from synthetic data, with visibly different front/back images. Coordinate mappings and focused metadata are explicit. Camera overlay fixtures reuse the same grey `checkerboardBackground()` modifier as the editor previews, underneath synthetic cards and controls. It stands in for the absent camera feed; turn-over and review use the actual theme background instead. Back-ready captures show the back fixture, and focus metadata matches the requested guidance. The lock-on fixture disables continuous animations to stabilize its render.

Previews invoke UI components directly; they do not start camera, inference, playback or network services.

Host renders illustrate layout and copy. They do not establish device camera correctness, permission interactions, model quality, focus accuracy or successful release publication. Keep these images' source and renderer provenance separate from the historical recording above.

The reviewed `capture-*-en.png` / `capture-*-es.png` files illustrate idle/ready front/back, turn-over and review in the demo guide. `ui-manifest.json` records their exact render source, workflow run, renderer, viewport, original PNG dimensions and SHA-256. These accepted copies retain the original renderer bytes. The site verifies their hashes/dimensions before copying them; they are distinct from the video-frame manifest. Their render commit is preserved in the merged branch history.

The existing workflow generates candidates and compares another render from the same source. This checks render repeatability, not regression against approved images from an earlier commit. Curated website images and their recorded provenance remain unchanged until the new candidates have been visually reviewed and intentionally accepted.

## Initial-guide transition verification

`CaptureOverlayTest` runs on an API 35 emulator in the **Capture guide and layout tests** job. It exercises the actual single-side/front/back wrappers without starting a camera or model. With the Compose clock paused, it samples pixels on the initial white square before detection and midway through the 250 ms fade, verifies removal after the fade, verifies that a missed detection does not restore it, and verifies that a fresh composition/capture attempt restores it. JVM tests also cover the one-way guide-state policy. Run just these UI tests with:

```bash
./gradlew :carddetector:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=com.apexfission.android.carddetector.ui
```

Static after-miss snapshots deliberately supply the final guide-hidden state; they are not animation evidence. The current UI also uses theme-aware text on turn-over/review screens while camera headers retain their white text over gradients. Refresh curated images only from the reviewed render artifacts and retain their actual source SHA and hashes.

Layout coverage includes 320 × 640 dp narrow screens, 852 × 393 dp landscape, font scale 1.6, and intentionally long Spanish copy for both camera flows. The welcome catalog has narrow/landscape/large-font variants, and review has a landscape variant. `CaptureLayoutTest` checks scrolling to the shutter/toggle and visibility in actual landscape orientation. Persistent screenshot baselines remain deferred: candidate generation and same-source comparison continue to verify repeatability only.

Single-side review now has English/Spanish, narrow, landscape, large-font and dark variants. Turn-over has narrow, landscape and large-font renders, including its new front-retake action. German, Japanese, Ukrainian and Simplified Chinese camera previews sample the expanded localization resources. `CaptureReviewTest` exercises one-image review, independent two-image retakes, confirmation, and explicit turn-over/retake actions; CI requires these five tests in addition to the seven guide/layout cases.

The landscape turn-over interaction case scrolls to the captured image and checks that continue and retake remain visible. Large-font single-image review also asserts that the retake label remains readable instead of wrapping vertically.

Review footer coverage includes localized German, Japanese and Simplified Chinese notices, plus an explicit host-supplied privacy notice. The emulator asserts that the host override replaces the default notice. English/Spanish review images show the current localized footer and theme surface styling. Haptics are enabled by default in the capture configuration; still previews cannot establish physical haptic feedback.
