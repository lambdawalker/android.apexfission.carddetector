# Recording and screenshot provenance

The primary showcase is the repository's supplied `../demo.mp4`. It is approximately 31 seconds, H.264, 1080 × 2340, with no narrated audio track. `manifest.json` records its content hash and the repository commit that supplied it. The original capture commit/device/OS/density/font-scale configuration is unknown; these fields remain explicitly null.

`tracking.png` is a reviewed still at 12 seconds, extracted with FFmpeg at width 540. It shows a cyan outline around a sample card. It is an illustrative recording frame, **not** a new device screenshot or a screenshot regression baseline. The recording and still do not establish current-release functionality or performance. Textual API contracts remain authoritative for integrations.

## Reproduce and review

Run from the repository root with Python 3 and FFmpeg (the accepting tool version is recorded in the manifest):

```bash
python3 scripts/media.py render   # Candidate under build/docs-media; never overwrites accepted media
# Open and inspect build/docs-media/tracking.png before accepting.
python3 scripts/media.py compare # Re-extract and compare to the accepted file; fails on differences
python3 scripts/media.py accept  # Intentionally accept the reviewed candidate + provenance
python3 scripts/media.py verify  # Integrity/required-scenario check; no rendering or acceptance
```

Keep accepted originals here. Small curated stills use ordinary Git storage; the original recording remains in Git LFS. Site copies are generated and ignored. CI verifies input/output hashes and required scenarios; the manual media workflow only renders/uploads candidates. A decoder/toolchain change may alter pixels or PNG encoding: investigate and intentionally review it, never silently accept differences. Use the recorded FFmpeg version for exact reproduction.

A different repository commit alone does not invalidate the historical still; it depends on the recording hash and render configuration. Verification of unchanged inputs lives in CI logs, separately from capture provenance. This historical asset can pass integrity verification without claiming fresh device behavior.

## Capture a current demonstration when needed

Use a device running the exact source you want to demonstrate, synthetic/sample cards, fixed orientation and locale. Record model, app/source SHA, Android/device version, viewport/density, theme/font scale, lighting, animation settings and any external inputs. For the live demo, obtain camera permission, frame the sample card, try capture/back and denial/recovery. Use `adb shell screenrecord /sdcard/card-demo.mp4`, then `adb pull /sdcard/card-demo.mp4` to collect it; inspect the entire recording before replacing canonical media. Device interaction is manual and not claimed deterministic.

A release that changes UI or camera behavior needs current device verification; the existing recording is never reused as that evidence. Static frames alone cannot validate motion, permissions, focus or lifecycle. The present documentation change adds no new current-device capture claim.

## Render current UI states on the host

The website overview groups the reviewed previews into two-sided capture, single-side capture and overlays, and the demo catalog. `ui-manifest.json` records the 15 curated images and their hashes, dimensions, render commits, and workflow runs. Per-image provenance overrides the top-level render when a later run supplied the image. English and Spanish overviews select the matching capture and welcome screens; overlay examples are shared. These development previews are illustrative on every version's overview; they do not assert that an archived release provides the displayed UI.

The **Render documentation screens** workflow renders Compose previews for `:carddetector` and `:app`, then compares a second render with those generated images. It runs for PRs and pushes to main/previews and can be dispatched manually. Download `compose-previews-carddetector` and `compose-previews-app` from the run. Each archive includes PNGs, reports, and `render-manifest.json` with the exact source SHA and image hashes. Missing previews fail the job.

```bash
./gradlew :carddetector:updateDebugScreenshotTest :carddetector:validateDebugScreenshotTest
./gradlew :app:updateDebugScreenshotTest :app:validateDebugScreenshotTest
```

The reference PNGs are generated under each module's `src/screenshotTestDebug/reference/`; HTML reports are under `build/reports/screenshotTest/`. These transient images are not approved regression baselines. Review candidates before adding selected images to documentation. The workflow never commits or silently accepts baselines. It uses the standalone screenshot plugin compatible with this repository's AGP 9.4 toolchain.

The suite renders the welcome catalog, single-side overlays, front/back guides, turn-over and review screens, plus detection/debug/lock-on overlays. Capture and welcome screens include English/Spanish variants at 393 × 852 dp, fixed font scale and static theme. The card fixture is drawn from synthetic data, with visibly different front/back images. Coordinate mappings and focused metadata are explicit. Previews invoke UI components directly; they do not start camera, inference, playback or network services.

Host renders illustrate layout and copy. They do not establish device camera correctness, permission interactions, model quality, focus accuracy or successful release publication. Keep these images' source and renderer provenance separate from the historical recording above.

The reviewed `capture-*-en.png` / `capture-*-es.png` files illustrate front/back, turn-over and review in the demo guide. `ui-manifest.json` records their exact render source, workflow run, renderer, viewport, original PNG dimensions and SHA-256. These accepted copies retain the original renderer bytes. The site verifies their hashes/dimensions before copying them; they are distinct from the video-frame manifest. Their render commit is preserved in the merged branch history.
