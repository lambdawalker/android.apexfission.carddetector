# Card Detector documentation website

The shared documentation CLI owns Astro/Starlight, rendering, history, translations, link rewriting, scoped search, and navigation. Install the configured CLI pin and Node 24, hydrate Git LFS media, then run:

```bash
npm ci
npm run check
npm run dev
```

This npm package is a dependency-free command/test wrapper. `npm run check` runs model-domain tests, the shared full-history build, then emitted route/media/pin checks. No repository-owned Astro dependency tree or generic renderer remains.

`prepare.mjs` is a small current trusted projection of staged data. It keeps tfmodel declarations separate, authors its bilingual model entry, reads exact detector pins from immutable archive records, links optional model prerequisites independently, and preserves root current-detector raw endpoints. `scripts/showcase.mjs` preserves the current synthetic UI illustration, explicitly not release proof. The media component presents the historical recording with unknown capture/device provenance. It does not change release verification.

`astro/styles/brand.css` and the media component are current presentation overrides. Canonical guides remain under `docs/agents/`, reviewed translations under `docs/es/`, immutable release facts under `docs/releases/history/`. Do not edit generated `dist/` or shared staging directories. Full Git history and hydrated `docs/demo.mp4` are required. Existing module scopes, `/screenshots/`, `/media/card-detection.mp4`, raw agents and installation aliases remain available.

See [maintenance](../docs/maintenance.md) for source/media validation and independently retryable documentation deployment.

This migration requires corrective shared CLI source [`1da53392b262c7c3129d2bd645dfd7b34ead81e1`](https://github.com/lambdawalker/gradle.android-repo-setup/pull/37). Official v0.1.21 lacks required projection/current-resource metadata and retained-translation/media fixes. Both setup and documentation launchers pin this exact source revision. The validated source-built binaries still report package version 0.1.21; they are cached only under this SHA, never represented as official v0.1.21 release assets.
