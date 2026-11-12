# Adopt repository setup v0.1.12

Goal: migrate Card Detector and Sentinel Card Model to the released CLI and Gradle plugin without changing runtime APIs or confirmed history.

- [x] Apply both Gradle conventions, retaining module POM metadata and the published core pin.
- [x] Configure module inventory and independent verification contracts; generate workflows using v0.1.12.
- [x] Preserve the Astro documentation site and media checks; retire duplicate publishing entry points and document environment requirements.
- [x] Verify generated configuration, release metadata, documentation and available Gradle checks; open a migration PR.

Validation: generated workflow/registry/import/history checks pass; 171 Python tests and 26 site tests pass. Full Gradle validation requires CI because this environment cannot resolve AGP; the inventory comparison runs there.
