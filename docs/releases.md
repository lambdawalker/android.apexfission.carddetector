# Publish libraries independently

The repository has two independently versioned Maven publications:

| Workflow module | Gradle module | Maven artifact | Dependency policy |
| --- | --- | --- | --- |
| `carddetector` | `:carddetector` | `com.apexfission.android.carddetector:card-detector` | Exports YOLO and coordinates; no model dependency |
| `tfmodel` | `:tfmodel` | `com.apexfission.android.carddetector:card-detector-model` | Exports the published core version pinned by `modelCoreArtifact` and `modelCoreVersion` in `gradle.properties` |

Publishing core does not publish the model. Publishing the model does not publish core. Versions can diverge: for example, a later model version can still depend on core 0.1.0. The model release builds against that Maven dependency, so a local unpublished core API cannot silently leak into its artifact. Normal development builds still use the local project dependency.

## Artifact rename status

`gradle.properties` selects `card-detector` and `card-detector-model`. Validation accepts these names and the historical `core` / `sentinel-card-model` records. Installation examples continue to show the last confirmed publication until a renamed release is verified on Central; pending names are explicitly labeled.

Module version histories continue across the rename: with confirmed 0.1.0 and no later history, the next version is 0.1.1 even if the new artifact has no Maven history. Leave `version` blank. Existing journal and tag safeguards still apply.

The model currently pins `modelCoreArtifact=core` and `modelCoreVersion=0.1.0`, an already published dependency. To move it to the renamed detector, first publish and finalize `card-detector`, then update both pin properties to that confirmed artifact/version. Publishing the model first remains supported with its existing dependency. Avoid adding both old and renamed detector artifacts to one app: their classes overlap.

## Run a release

1. Merge the intended source to `main`. Review the selected module's changes and, for model releases, its `modelCoreVersion` compatibility pin.
2. Open **Actions → Publish card-detector** for the detector or **Actions → Publish card-detector-model** for the bundled model.
3. Choose **Run workflow** on `main` and select **repository**: `maven-central` or `apexfission-maven`. Each entry fixes its own module; there is no module selector.
4. Leave **version** blank for automatic numbering: 0.1.0 when the selected module/repository has no history, otherwise the latest version with its patch incremented by one (for example, 1.2.9 → 1.2.10). On Maven Central these modules already have confirmed 0.1.0 history, so their next automatic version is 0.1.1 unless a newer release exists.
5. To choose a version yourself, enter a stable **X.Y.Z** such as **1.0.0**. It must be newer than the latest release. Existing versions cannot be republished; use the Finalize workflow for recovery.

Both dedicated workflows call `publish-card-detection.yml`, a reusable implementation with no manual entry point. They select the matching GitHub environment and retain the shared release lock and publication safeguards. Only Maven Central uses delayed finalization. The documentation workflow listens for either dedicated publication to finish.

Only the selected module is staged for local publication verification and uploaded to the selected repository. Common tests and the demo still build as integration checks. Attempting a Gradle upload for both modules, for the wrong repository, or without `releaseModule`, fails before upload.

## GitHub environments and configuration

Create these environments in **Settings → Environments**. The dropdown value is the environment name.

| Environment | Environment secrets | Environment variables |
| --- | --- | --- |
| `maven-central` | `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `SIGNING_IN_MEMORY_KEY`, `SIGNING_IN_MEMORY_KEY_PASSWORD` | None required |
| `apexfission-maven` | `MAVEN_REPOSITORY_USERNAME`, `MAVEN_REPOSITORY_PASSWORD`, `SIGNING_IN_MEMORY_KEY`, `SIGNING_IN_MEMORY_KEY_PASSWORD` | `MAVEN_REPOSITORY_URL` |

Add **MAVEN_REPOSITORY_URL** under the `apexfission-maven` environment's **Environment variables** (not in the workflow form). Set it to the canonical HTTPS Maven repository base URL, including the repository path, for example `https://maven.example.com/releases`. This example is a placeholder. Do not include a username, password, query string, or fragment. The endpoint must support standard Maven uploads and artifact/metadata reads at that same URL. Redirects are rejected during verification. A signing-key password can be empty for an unencrypted key.

The self-hosted credentials are used for upload and artifact verification; signing remains enabled for both destinations. The repository URL is public configuration and is recorded in release metadata and installation instructions. For a private repository, consumers need their own read credentials; never copy publishing credentials into application source.

Keep **delayed-docs** with the **15-minute wait timer**. After a Central upload, the automatic flow waits there without holding a runner or the release lock, then polls for up to **40 minutes** for the selected artifact's POM, AAR, sources, documentation, Gradle metadata, and signatures. Self-hosted uploads skip that environment delay and begin the same verification immediately. Neither upload success alone nor a pending release updates installation claims.

Version history is independent per module **and destination**. A first self-hosted release defaults to **0.1.0**, even if Central already has later versions. A blank version subsequently increments that destination's latest patch. The model's pinned detector must already exist on Central or the selected repository; Central is checked first, matching Gradle resolution. Keep the same GAV consistent across repositories if you choose to publish it to both.

## Recovery and races

Use **Actions → Finalize CardDetector release → Run workflow**, choosing the original **repository**, **module**, and **version**. It checks and finalizes an existing publication; it never uploads. Publishing has the repository dropdown and optional version field; use this dedicated Finalize workflow to recover an interrupted release without another upload.

Automatic and manual publication/finalization share the existing repository-wide lock, with `cancel-in-progress: false`. This intentionally serializes jobs that can update `main`, while keeping module versions and attempt state independent. The wait job is outside that lock. GitHub can replace queued jobs in a concurrency group; if a queued finalization is displaced, manually run Finalize for its module/version. Durable journals prevent lost upload state.

Maven Central keeps its existing per-module immutable refs:

| State | Core example | Model example |
| --- | --- | --- |
| Reserved source and artifact hashes | `release-pending/carddetector/0.1.1` | `release-pending/tfmodel/0.1.1` |
| Upload may have begun | `release-uploading/carddetector/0.1.1` | `release-uploading/tfmodel/0.1.1` |
| Confirmed source release | `carddetector/v0.1.1` | `tfmodel/v0.1.1` |

Self-hosted refs add `apexfission-maven/` before the module: for example `release-pending/apexfission-maven/carddetector/0.1.0`, `release-uploading/apexfission-maven/carddetector/0.1.0`, and `apexfission-maven/carddetector/v0.1.0`. The journal pins the repository URL; changing it while a release is pending stops recovery until the original URL is restored. Attempts on one destination do not block the other.

A pending model attempt does not block allocating a core release, and vice versa. An upload-started marker rejects any second upload for that module/version. A retry after completed finalization exits successfully; it does not move documentation backwards.

Finalization verifies reserved hashes/signatures against the immutable source, then reads latest `main`, updates only the selected module's confirmed metadata, and regenerates the combined installation document. It preserves a sibling release that finished during the wait. Metadata, stable tag creation, and attempt-marker removal are pushed atomically without force. A race or branch-protection rejection preserves remote state for investigation and retry.

If relevant build/release tooling changed during the wait, finalization stops instead of mixing source assumptions. Inspect and reconcile deliberately. If artifacts are partial, rejected or in an unknown Central Portal state, keep the attempt refs and logs; do not delete markers merely to retry an upload. The Finalize workflow can be retried after propagation. A known rejected upload that never became public requires maintainer investigation before any marker removal or version reuse.

## Confirmed metadata and migration

- `docs/releases/carddetector.json`: latest confirmed core release.
- `docs/releases/tfmodel.json`: latest confirmed model release and its `core_version`.
- `docs/releases/apexfission-maven/carddetector.json` and `tfmodel.json`: independent self-hosted confirmed records (initially null).
- `IMPORT.md`: generated from all confirmed records and `docs/templates/MODULE_IMPORT.md.template`.
- `docs/release.json`, `scripts/release.py` legacy journal commands, and `scripts/finalize-release.sh`: preserved for old paired-release provenance/recovery. New workflows use `scripts/module_release.py`. Shared artifact-validation helpers remain in `release.py`.

The initial records were split from the already confirmed 0.1.0 pair; their hashes and source were preserved. They refer to the existing `v0.1.0` tag through `legacy_tag`. No new tag, package upload, or registry claim is created by this migration. Later releases use module-prefixed tags. Old unscoped pending/uploading markers block new Central releases until resolved using the original paired-release tooling. See the [legacy runbook](legacy-releases.md) only for that historical recovery case.

## Local verification

```bash
python3 -m unittest discover -s scripts/tests -v
python3 scripts/module_release.py verify

# Core artifact only, with no publishing credentials:
./gradlew :carddetector:publishAllPublicationsToVerificationRepository \
  -PreleaseModule=carddetector -PreleaseVersion=9.8.7
python3 scripts/module_release.py check-local --module carddetector \
  --version 9.8.7 --source "$(git rev-parse HEAD)"

# Model artifact only, compiled against modelCoreVersion from Maven:
./gradlew :tfmodel:publishAllPublicationsToVerificationRepository \
  -PreleaseModule=tfmodel -PreleaseVersion=9.8.7
python3 scripts/module_release.py check-local --module tfmodel \
  --version 9.8.7 --source "$(git rev-parse HEAD)"

./gradlew generateImportDocs verifyImportDocs
```

CI validates each selection in a matrix. Checks include model POM and both Gradle consumption variants exporting the pinned core, complete artifact contents, source consistency and independent Git journal/finalization races. Model asset hashes come from the selected source and are recorded with the release; replacing a model does not require editing a hardcoded old asset hash.

## Website deployment

Documentation still deploys separately from Maven publication. Completion of publication/finalization rebuilds the site with the current independently confirmed installation facts. Retry **Documentation** for site failures; do not publish another package. No package is published by this change alone.
