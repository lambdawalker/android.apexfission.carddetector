# Shared module releases implementation plan

Goal: Share per-module version allocation across registries and advertise only the latest usable releases.
Architecture: A canonical tag resolver supplies version/source. Existing destination journals retain recovery. JitPack uses a separate build/verification adapter, and installation rendering selects confirmed records.
Tech stack: Python, Git, GitHub Actions, Gradle Kotlin DSL, JitPack.
Spec: ../specs/2026-10-08-shared-releases.md

## Constraints and review focus
- Never move tags or retry an unknown Maven upload.
- Reuse original source, never publish changed HEAD under an old identity.
- Documentation commits and sibling-only changes do not allocate versions.
- Model dependency provenance and real LFS asset bytes must be checked.
- Delayed finalization cannot overwrite newer destination metadata or sibling records.

## Tasks
- [x] Add canonical module release resolver and Git regression tests for catch-up, input changes, conflicts, ancestry and reservations.
- [x] Add JitPack build/verification adapter and Gradle selection; test module parsing, dependencies, malformed artifacts and provenance.
- [x] Add pure latest-confirmed installation renderer and tests for stale/newer/tied sources and model dependencies.
- [x] Integrate destination config, workflows, journals and finalization; retain explicit recovery and no-op repeated releases.
- [x] Run Python suite, documentation checks, workflow lint and available Gradle validation; review branch and open PR.

Execution: Native integration with independent renderer and JitPack adapter delegated to separate file owners. User explicitly requested proceeding; no production release will be dispatched as part of this change.

Validation: Python suite, generated configuration/docs, docs/media integrity, site build and actionlint pass. Local Gradle validation could not start because the wrapper download is network-blocked; CI includes both unsigned JitPack publications. Live JitPack publication remains unverified and must not be advertised until confirmation succeeds.
