# Publishing and release recovery

This project uses the shared CLI pin in `.project/setup/config.yaml` and released Gradle plugins v0.1.14. Both libraries publish independently. Runtime APIs, confirmed records, archive entries and existing tags are preserved.

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

### Recover the published Central 0.1.10 release

The failed `0.1.15` and `0.1.16` runs selected **maven-central**, not JitPack.
They stopped before uploading because the Central `0.1.10` reservation and
upload marker were still present. Documentation was skipped after publication
failed. The Central artifacts are public and all five SHA-256 values match the
original reservation at `b36e4fce5b7583cb1f8a4fb6fe8c0888727b9e2c`.

Run **Recover published Central carddetector 0.1.10** from `main` once. This
manual workflow uses the normal release lock and Central environment. It checks
the exact reservation, verifies the public files with the original source's
Python verifier, and passes that confirmation record to the current CLI
finalizer. Finalization updates the generated installation guide and history,
then atomically clears only this completed Central attempt. It preserves the
independent JitPack journal and all existing source tags. A repeat run recognizes
the completed release and skips confirmation/finalization.

This split is necessary because the old source predates
`.project/setup/config.yaml`: current CLI `confirm` cannot load its verification
contract. Do not substitute today's contract, delete markers manually, or
re-upload `0.1.10`. The original verifier checks artifact structure, dependencies,
bytecode, signature presence and every reserved hash; the current finalizer rechecks the
reservation, source ancestry, metadata ordering and remote ref leases.

After recovery, start a new **Publish carddetector** run and explicitly select
the destination. Selecting **jitpack** publishes the coordinates used by auth;
leaving **maven-central** selected uses Central. `bypass_version_verification`
does not switch destinations and cannot ignore an upload-started Central journal.
The JitPack pending-attempt procedure below still applies independently.

### Publish after an unresolved older JitPack attempt

The [failed publish run](https://github.com/lambdawalker/android.apexfission.carddetector/actions/runs/43357262211) stopped during preparation, before reservation or upload. The old JitPack `0.1.9` journal remains pending even though `0.1.10` is confirmed. During investigation, JitPack returned `tagNotFound` with no source commit for `carddetector~v0.1.9`. That is not proof of a failed build; do not delete the pending journal, its legacy source tag, or confirmed release records to bypass the check.

To intentionally publish current source as a separate version:

1. Start a **new** run of **Publish carddetector** from `main` with repository **jitpack**.
2. Enter an explicit version newer than all detector release history. `0.1.11` passed preparation against the source of the failed run; choose a later version if history has advanced.
3. Enable **bypass_version_verification**. For JitPack this preserves older pending attempts while allowing the separate newer release. Upload-started markers still block it.

Do not use **Re-run failed jobs** for this recovery: that repeats the original blank version and disabled override. The equivalent preparation check does not reserve or upload artifacts:

```sh
RELEASE_REPOSITORY=jitpack ./setup.sh module_release prepare --module carddetector --version 0.1.11 --allow-pending-jitpack
```

Use **Reconcile failed JitPack attempts** only when the provider can prove terminal failure at the journal's exact source and no public artifacts exist. Preview first. The current `v0.1.17` reconciliation command rejects legacy unqualified source tags; the shared CLI correction adds verified legacy-tag support without rewriting their identity. A `tagNotFound` response still requires investigation or the explicit separate-release path above.

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
