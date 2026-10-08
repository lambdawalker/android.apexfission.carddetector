# Repository contributor instructions

This file guides contributors. The consumer API manual starts at [docs/agents/index.md](docs/agents/index.md).

- Keep runtime changes separate from documentation work. Source declarations and tests define behavior.
- `docs/agents/` owns integration contracts; the website renders those same guides as HTML and publishes raw Markdown. Do not edit generated `sites/public/` or `sites/src/content/docs/`.
- Keep installation values generated from confirmed release metadata. Run `python3 scripts/module_release.py verify`; do not infer publication from Gradle properties or upload success.
- Run `python3 scripts/docs.py verify`, `python3 scripts/media.py verify`, and `cd sites && npm ci && npm run check` after documentation edits.
- If API inputs change, review the corresponding guides, then intentionally run `python3 scripts/docs.py update`. Review its diff; this is not an API compatibility checker.
- The recording and extracted frames are historical demonstration media. Never describe them as release verification. See `docs/screenshots/README.md`.
- Android verification: `./gradlew :carddetector:testDebugUnitTest :app:assembleDebug :carddetector:assembleDebugAndroidTest :app:assembleDebugAndroidTest`.
- Preserve the manual upload/finalization workflow and environment delay. Documentation deployment must remain independently retryable and must not upload Maven artifacts.

- Publish only one selected `releaseModule` per invocation. Model publication must use the stable `modelCoreVersion` pin, not an unpublished project dependency. Keep the two confirmed module records separate and preserve sibling metadata during finalization. See `docs/releases.md`.
- `docs/releases/history/<module>/<version>.json` archives confirmed publication facts and immutable documentation revisions. Preserve old entries and destination confirmations; never regenerate history from current versions alone.
- Spanish consumer guides live in `docs/es/` with English SHA-256 entries in `translations.json`. Translate prose, preserve code, and only refresh hashes after review. Missing/stale translations intentionally fall back to that revision's English.
- Run `python3 scripts/documentation_history.py verify` and the full-history site build for documentation changes. Historical Markdown is data; never execute build scripts from archived releases. See `docs/maintenance.md` for correction and translation-tree provenance.
