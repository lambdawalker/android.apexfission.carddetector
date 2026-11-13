# Publishing and release recovery

This project uses the shared CLI release pin in `.project/setup/config.yaml` and released Gradle plugins v0.1.14. Both libraries publish independently. Runtime APIs, confirmed records, archive entries and existing tags are preserved.

## Install and configure

Use the repository launcher to install and run the configured CLI:

```sh
sh ./setup.sh --version
sh ./setup.sh
```

Run `setup.bat` on Windows or `sh ./setup.sh` on Linux/macOS. These shared CLI-generated launchers install and verify the configured CLI, cache it under `.project/setup/cache/`, and forward arguments. Release pins use verified binaries; full commit pins require Cargo and build the exact source. Run `setup check` to validate generated files and evaluated module configuration.

The first menu discovers evaluated Gradle libraries/apps and restores saved selections. `carddetector`, `tfmodel`, and `app` are selected in `.project/setup/config.yaml`. Discovery requires the normal JDK 21/Android SDK 37 build environment and resolvable dependencies. JDK 17 is also needed for library toolchains. When using the cached CLI locally, replace `android-repo-setup` in commands below with `sh ./setup.sh` or `setup.bat`.

```sh
android-repo-setup discover
android-repo-setup select-modules
android-repo-setup workflows --check
android-repo-setup workflows --overwrite
android-repo-setup publishing_config generate
```

Commit `.project/setup/config.yaml`, `modules.properties`, `repositories.yml`, `repositories.properties`, `workflow-generation.json`, and the workflows together. The Gradle plugin resolves stable library IDs from the generated inventory. `POM_ARTIFACT_ID.carddetector` and `POM_ARTIFACT_ID.tfmodel` select `card-detector` and `card-detector-model` respectively.

## Environments

| Environment | Settings |
| --- | --- |
| `maven-central` | Secrets `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `SIGNING_IN_MEMORY_KEY`, and optional `SIGNING_IN_MEMORY_KEY_PASSWORD` |
| `jitpack` | No publishing credentials for this public repository |
| `delayed-docs` | Preserve the 15-minute wait timer for Central propagation |
| `github-pages` | Keep Pages configured for GitHub Actions |
| `demo-apk-release` | Secrets `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` |

The two libraries share destination environments. Existing Central secret names do not change. The old `MAVEN_REPOSITORY_URL` variable is no longer consumed for Central. Custom `publisher: maven` destinations are not supported by this CLI version; archived `apexfission-maven` confirmations remain historical facts.

The optional repository-level `WORKFLOW_UPDATE_TOKEN` allows the Update GitHub workflows action to open a PR. Without it, the action still produces a patch artifact. It needs permission to update repository contents/workflows and create pull requests. Environment secrets and protection rules must be reviewed in GitHub; committing these files does not configure them.

## Publish one library

Run **Publish carddetector** or **Publish tfmodel** from `main`. Choose `maven-central` or `jitpack` and optionally a stable `X.Y.Z` version. Leave the version blank for the CLI's version selection. Both entry points use `publish-library.yml`, the selected environment and a shared release lock. Do not run both libraries in a single Gradle publishing invocation.

The plugin handles publication configuration and guards; the CLI owns reservation, artifact verification, confirmation and finalization. Manual **SUB - Finalize Card Detector release** remains available and never uploads artifacts. Documentation deployment is independently retryable.

The model retains `modelCoreArtifact=core` and `modelCoreVersion=0.1.0`. When publishing `tfmodel`, Gradle compiles against that published dependency. Development builds may use the sibling project. Change the pin only after the desired detector release is confirmed. Its verification contract checks the exact exported API dependency and every committed `.tflite` asset; the detector contract prohibits bundled `.tflite` assets.

JitPack installs the pinned CLI from source and runs `./setup.sh jitpack_build`. The CLI validates the canonical `<module>/vX.Y.Z-jitpack` tag and prepares that module's assets before invoking Gradle. The model contract enables scoped LFS hydration, committed hash/size checks, and TFL3 header identification; local and public AAR verification enforce the same contract. Generated release workflows also prepare assets before building. The Python JitPack helper is no longer needed. Choose either detector or model JitPack coordinates: their versioned module tags share a single repository artifact ID.

## Recovery and legacy attempts

Source tags and release histories are immutable. Keep all pending/upload-started markers until the destination outcome is known. The migration observed old `release-pending/carddetector/0.1.10`, `release-uploading/carddetector/0.1.10`, and `release-pending/jitpack/carddetector/0.1.9` refs; it does not delete, confirm or retry them. Check their current remote status before the next release.

An in-flight Python publication must be reconciled using its original source and tooling. The CLI is absent from historical immutable source revisions, and its new verification records differ from legacy model records. See [archived Python instructions](legacy-python-publishing.md). Legacy scripts and their tests were removed from current source; retrieve them in a separate checkout at the original release source SHA. Do not delete or rewrite old release markers. New releases use the CLI.

## Documentation and verification

The Astro site remains in place. It uses `android-repo-docs history export` and renders both legacy `core_version` model pins and new resolved verification pins. Historical Markdown is read as data; archived build scripts are never executed. The CLI generates `IMPORT.md` from confirmed records only.

```sh
android-repo-setup module_release generate
android-repo-setup module_release verify
android-repo-docs history verify
sh ./docs.sh sources check
sh ./docs.sh media verify
cd sites
npm ci
npm run check
```

Fetch historical media with `git lfs pull --include="docs/**" --exclude=""` before the site build. Do not regenerate confirmed archives from current properties.

The generated workflows verify both libraries against Central and JitPack coordinates without uploading, run unit tests, render/validate previews for both screenshot-enabled modules, and provide signed APK builds. The specialized capture-guide emulator tests remain separate. The workflow generator protects locally edited generated files; review changes before overwriting them.
