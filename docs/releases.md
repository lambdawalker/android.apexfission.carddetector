# Publish libraries independently

The repository has two independently versioned Maven Central publications:

| Workflow module | Gradle module | Maven artifact | Dependency policy |
| --- | --- | --- | --- |
| `carddetector` | `:carddetector` | `com.apexfission.android.carddetector:core` | Exports YOLO and coordinates; no model dependency |
| `tfmodel` | `:tfmodel` | `com.apexfission.android.carddetector:sentinel-card-model` | Exports the published core version pinned by `modelCoreVersion` in `gradle.properties` |

Publishing core does not publish the model. Publishing the model does not publish core. Versions can diverge: for example, a later model version can still depend on core 0.1.0. The model release builds against that Maven dependency, so a local unpublished core API cannot silently leak into its artifact. Normal development builds still use the local project dependency.

## Artifact rename status

`gradle.properties` now selects `card-detector` and `card-detector-model`, but the release validators and confirmed metadata still describe `core` and `sentinel-card-model`. The dedicated actions do not migrate coordinates. Until that migration is completed, preflight stops with `Unexpected module coordinates` before any upload. Keep the old confirmed metadata as historical publication facts; changing a property does not publish a renamed artifact.

## Run a release

1. Merge the intended source to `main`. Review the selected module's changes and, for model releases, its `modelCoreVersion` compatibility pin.
2. Open **Actions → Publish card-detector** for the detector or **Actions → Publish card-detector-model** for the bundled model.
3. Choose **Run workflow** on `main`. Each entry fixes its own module; there is no module selector.
4. Leave **initial_version** blank for both existing artifacts. Each already has a confirmed 0.1.0 release. The workflow independently allocates the selected artifact's next stable patch version. Only a module with no release history needs an explicit first version.
5. Leave **resume_version** blank for a new upload.

Both dedicated workflows call `publish-card-detection.yml`, a reusable implementation with no manual entry point. They retain the same environment secrets, release lock, delayed finalization, and recovery inputs. The documentation workflow listens for either dedicated publication to finish.

Only the selected module is staged for local publication verification and uploaded to Central. Common tests and the demo still build as integration checks. Attempting a Gradle Central invocation for both modules, or without `releaseModule`, fails before upload.

## Existing environments and secrets

Keep the `maven-central` environment and its existing secrets:

- `MAVEN_CENTRAL_USERNAME`
- `MAVEN_CENTRAL_PASSWORD`
- `SIGNING_IN_MEMORY_KEY`
- `SIGNING_IN_MEMORY_KEY_PASSWORD` (if the key has a passphrase)

Keep **delayed-docs** with the **15-minute wait timer**. After upload, the automatic flow waits there without holding a runner or the release lock, then polls Central for up to **40 minutes** for the selected artifact's POM, AAR, sources, documentation, Gradle metadata, and signatures. The other module need not have a matching version. No additional GitHub environment or secret is required.

## Recovery and races

Use **Actions → Finalize CardDetector release → Run workflow**, choosing the original **module** and **version**. It checks and finalizes an existing publication; it never uploads. The publishing workflow's `resume_version` path provides the same recovery behavior for the selected module.

Automatic and manual publication/finalization share the existing repository-wide lock, with `cancel-in-progress: false`. This intentionally serializes jobs that can update `main`, while keeping module versions and attempt state independent. The wait job is outside that lock. GitHub can replace queued jobs in a concurrency group; if a queued finalization is displaced, manually run Finalize for its module/version. Durable journals prevent lost upload state.

Per-module immutable refs are:

| State | Core example | Model example |
| --- | --- | --- |
| Reserved source and artifact hashes | `release-pending/carddetector/0.1.1` | `release-pending/tfmodel/0.1.1` |
| Upload may have begun | `release-uploading/carddetector/0.1.1` | `release-uploading/tfmodel/0.1.1` |
| Confirmed source release | `carddetector/v0.1.1` | `tfmodel/v0.1.1` |

A pending model attempt does not block allocating a core release, and vice versa. An upload-started marker rejects any second upload for that module/version. A retry after completed finalization exits successfully; it does not move documentation backwards.

Finalization verifies reserved hashes/signatures against the immutable source, then reads latest `main`, updates only the selected module's confirmed metadata, and regenerates the combined installation document. It preserves a sibling release that finished during the wait. Metadata, stable tag creation, and attempt-marker removal are pushed atomically without force. A race or branch-protection rejection preserves remote state for investigation and retry.

If relevant build/release tooling changed during the wait, finalization stops instead of mixing source assumptions. Inspect and reconcile deliberately. If artifacts are partial, rejected or in an unknown Central Portal state, keep the attempt refs and logs; do not delete markers merely to retry an upload. The Finalize workflow can be retried after propagation. A known rejected upload that never became public requires maintainer investigation before any marker removal or version reuse.

## Confirmed metadata and migration

- `docs/releases/carddetector.json`: latest confirmed core release.
- `docs/releases/tfmodel.json`: latest confirmed model release and its `core_version`.
- `IMPORT.md`: generated from both records and `docs/templates/MODULE_IMPORT.md.template`.
- `docs/release.json`, `scripts/release.py` legacy journal commands, and `scripts/finalize-release.sh`: preserved for old paired-release provenance/recovery. New workflows use `scripts/module_release.py`. Shared artifact-validation helpers remain in `release.py`.

The initial records were split from the already confirmed 0.1.0 pair; their hashes and source were preserved. They refer to the existing `v0.1.0` tag through `legacy_tag`. No new tag, package upload, or registry claim is created by this migration. Later releases use module-prefixed tags. Old unscoped pending/uploading markers block new releases until resolved using the original paired-release tooling. See the [legacy runbook](legacy-releases.md) only for that historical recovery case.

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
