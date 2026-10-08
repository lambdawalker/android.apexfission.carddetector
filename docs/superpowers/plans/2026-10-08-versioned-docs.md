# Versioned bilingual documentation implementation plan

> **For agentic workers:** Use superpowers:executing-plans to implement task-by-task.

**Goal:** Preserve released library guides and installation facts with English/Spanish navigation.
**Architecture:** Validated per-module JSON archives drive static pages from immutable Git Markdown. Current English contracts remain canonical; translations track source hashes.
**Tech Stack:** Python release tooling, Node build scripts, Astro/Starlight.
**Spec:** docs/superpowers/specs/2026-10-08-versioned-docs.md

## Global Constraints
- Never execute historical code; never publish artifacts during this work.
- Preserve existing documentation URLs and release destination behavior.
- No silent latest-version fallback or invented historical confirmations.
- Keep Kotlin blocks unchanged in Spanish; mark unavailable/stale translations.

## Review Focus
- Second-destination publication must retain the first destination and original source.
- New destination pointers must not erase older versions.
- Missing Git revisions must fail rather than show current API under an old version.
- Internal guide links must stay within selected module/version/language.
- tfmodel pinned detector API must not be mistaken for newest detector release.

### Task 1: Confirmed history archive
Files: scripts/documentation_history.py, scripts/module_release.py, scripts/tests/test_documentation_history.py, docs/releases/history/.
Interface: archive_record(root, record) -> relative catalog Path; seed(root); load_catalog(root) -> list of validated records. Each catalog stores schema=1, module, version, source, documentation_ref, destinations.
- [x] Test retention, idempotence, mirror merging, source conflicts, pending records and archive verification (RED).
- [x] Implement validation/archive and call it during finalization before pointer replacement (GREEN).
- [x] Seed confirmed records, run Python tests, commit.

### Task 2: Versioned bilingual pages
Files: sites/scripts/versioned.mjs, sites/scripts/sync.mjs, sites/scripts/tests/versioned.test.mjs, sites/src/components/VersionBanner.astro, sites/astro.config.mjs, workflow checkout configuration.
Interface: versioned build writes content routes, raw guides and navigation JSON consumed by the banner/sidebar; revision reads are Git data reads.
- [x] Test pinned revisions, scoped guide links, exact-version installs and translation fallback (RED).
- [x] Render all archived versions and bilingual development routes with accessible selectors and localized sidebar (GREEN).
- [x] Include full history checkout, retain legacy routes, validate generated links and build.

### Task 3: Spanish guides and maintenance
Files: docs/es/, docs/maintenance.md, AGENTS.md, README.md.
- [x] Translate all consumer guides and API prose, preserve code fences, record English SHA256.
- [x] Verify translation coverage and code equality; explain archive/correction/fallback maintenance.
- [x] Independent review complete; fixed bundled-model installation guidance and Spanish catalog navigation, verified rendered HTML and full site.
- [ ] Open PR.
- [x] Complete Python suite: 153 tests pass; site: 14 Node tests and 151 pages pass; docs/media/IMPORT/archive checks pass.

## Execution notes

- Preserve canonical docs/agents instead of duplicating English into docs/en; existing contributor contract requires one source.
- Initial Spanish snapshots use an immutable translation_tree so a squash merge cannot strand a pinned unmerged commit. English hash checks remain mandatory.
- Initial archive is intentionally limited to confirmed metadata available at adoption.

## Final review

- Fixed: detector quickstart imports optional tfmodel but its install link previously led to detector-only coordinates. Both languages now direct bundled-model installation to independent model releases and explicitly explain the exported detector pin, JitPack artifact conflict, and own-model alternative. Regression tests and rendered HTML checks pass.
- Fixed: Spanish catalog sidebar left the selected language and version catalog. Regraded as a functional localization issue; catalog links now stay in the selected language. Regression test and rendered HTML check pass.
- Review exclusions: pre-adoption release completeness and unavailable 0.1.0 detector docs remain explicitly outside the confirmed archive; no historical publication, Android runtime compatibility, external URL availability, browser visual behavior, or global search filtering claim is made. The existing search remains global within the chosen language.
