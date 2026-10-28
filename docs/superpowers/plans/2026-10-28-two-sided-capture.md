# Two-sided Card Capture Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans. Steps use checkbox syntax for tracking.

**Goal:** Capture and review both sides with optional focus-gated automatic capture.
**Architecture:** Add image sharpness metadata to core detections and a separate Compose session component around the existing detector. Scope each attempt to a disposable ViewModelStore; a tested ownership state machine handles front/back, retakes, and final delivery.
**Tech Stack:** Kotlin, Compose, CameraX, JUnit, Android string resources.
**Spec:** docs/superpowers/specs/2026-10-28-two-sided-capture.md

## Global Constraints
- Existing single-side call sites keep working; auto-capture defaults off.
- Bitmap ownership transfers only on final confirmation; release abandoned images.
- English defaults, Spanish translations, explicit turn-over, square guide and rectangular crop.
- Focus means measured image sharpness; no claim of side classification.

## Review Focus
- A callback arriving after disposal is released, not accepted by another side.
- Repeated shutter taps and sustained lock produce at most one capture per attempt.
- Retaking front preserves back and returns to review.
- Missing, stale, unfocused or moving frames reset auto-capture readiness.
- Camera recreation releases the old detector and retained bitmap.

### Task 1: Focus metadata and retention
Files: domain/tflite/image/CardSharpness.kt, domain/tflite/model/CardDetection.kt, engine/DefaultCardDetector.kt, ui/detector/LatestBestDetectionStore.kt and unit tests.
Interface: FocusState, focusScore: Float?, isFocused: Boolean on CardDetection; card-region Laplacian variance with documented threshold.
- [ ] Write and run tests for blurred/flat/sharp inputs, unknown values and focus-aware retention.
- [ ] Implement assessment for both YOLO and hash results; prefer focused over unfocused images.
- [ ] Run :carddetector:testDebugUnitTest and commit runtime/tests.

### Task 2: Capture session and readiness
Files: ui/capture/CaptureSession.kt, AutoCaptureGate.kt, CardCaptureConfig.kt and tests.
Interfaces: CardSide, CaptureStage, generic CaptureSession<T> with release callback, attempt token, capture request, acceptance, continue, retake, complete and close; AutoCaptureGate on fresh sequence/time/box/focus.
- [ ] Test duplicate/stale completion, disposal, retakes and ownership transfer; test readiness with stale/missing/moving/blurred detections.
- [ ] Implement pure state and readiness logic and validate configuration.
- [ ] Run unit suite and commit.

### Task 3: Compose flow, strings and demo
Files: ui/capture/TwoSidedCardCapture.kt, CardCaptureStrings.kt, values/strings.xml, values-es/strings.xml; built-in overlay config and controls; demo CardDetectionActivity.kt.
- [ ] Add Compose instrumentation coverage for side labels, steps, transition and review actions where possible.
- [ ] Implement isolated per-attempt camera owner, callback channel cleanup, square guide, manual/auto shutter, review and retakes.
- [ ] Integrate demo; run Android verification commands from AGENTS.md.

### Task 4: Consumer documentation and final review
Files: docs/agents and docs/es guides plus generated API inventory updated via scripts/docs.py.
- [ ] Document API, ownership, localization, threshold tuning and limitations separately from runtime changes.
- [ ] Run docs/media/history/publication verification and full-history site check.
- [ ] Fresh whole-branch code review, resolve important findings, final verification and commit.

## Implementation and validation record

Implemented all four deliverables. The demo enables auto-capture; library default remains manual. A fresh code review identified two important races, addressed by worker-ordered evaluation readiness (including misses) before UI conflation, and whole-session invalidation on configuration changes. Retained-image stores permanently close on ViewModel disposal to reject late allocations.

Validation in this environment:
- Kotlin 2.4.20 + Compose compiler compiled core, model and demo sources against cached dependencies, with a temporary R shim for the new strings. This is a source check, not an Android package build.
- 75 available JVM tests passed with the Android mockable jar. Tests requiring Mockito could not run because that dependency is unavailable.
- Docs API, media, release-history and module metadata verification passed.
- Required Gradle unit/APK/androidTest build command blocked by unavailable JDK 21 download; an alternate local-JDK attempt also hit unavailable Maven publishing plugin dependencies.
- New Compose instrumentation tests were added; no emulator/device is connected.
- Site check blocked: npm offline installation lacks zwitch; both site test modules cannot import unified. No full-history HTML build claimed.
- Python full-suite attempt could not import the environment-setup test (httpx unavailable); the other available test modules were checked separately.

Implementation decisions: explicit Continue to back; configuration changes restart both sides; image sharpness is a heuristic requiring device/document calibration, not AF confirmation or side classification.
