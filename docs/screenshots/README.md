# Recording and screenshot provenance

The primary showcase is the repository's supplied `docs/Screen_recording_20260917_121435.mp4`. It is approximately 31 seconds, H.264, 1080 × 2340, with no narrated audio track. `manifest.json` records its content hash and the repository commit that supplied it. The original capture commit/device/OS/density/font-scale configuration is unknown; these fields remain explicitly null.

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
