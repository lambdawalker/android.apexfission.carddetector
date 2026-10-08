# Publish libraries independently

The repository has two independently versioned Maven publications:

| Workflow module | Gradle module | Maven artifact | Dependency policy |
| --- | --- | --- | --- |
| `carddetector` | `:carddetector` | `com.apexfission.android.carddetector:card-detector` | Exports YOLO and coordinates; no model dependency |
| `tfmodel` | `:tfmodel` | `com.apexfission.android.carddetector:card-detector-model` | Exports the published core version pinned by `modelCoreArtifact` and `modelCoreVersion` in `gradle.properties` |

Publishing core does not publish the model. Publishing the model does not publish core. Versions can diverge: for example, a later model version can still depend on core 0.1.0. The model release builds against that Maven dependency, so a local unpublished core API cannot silently leak into its artifact. Normal development builds still use the local project dependency.

## Artifact rename status

`gradle.properties` selects `card-detector` and `card-detector-model`. Validation accepts these names and the historical `core` / `sentinel-card-model` records. Installation examples use the newest confirmed semantic release of each module across all destinations. A configured rename alone does not change confirmed coordinates; pending names are explicitly labeled.

Module version histories continue across the rename: with confirmed 0.1.0 and no later history, the next version is 0.1.1 even if the new artifact has no Maven history. Leave `version` blank. Existing journal and tag safeguards still apply.

The model currently pins `modelCoreArtifact=core` and `modelCoreVersion=0.1.0`, an already published dependency. To move it to the renamed detector, first publish and finalize `card-detector`, then update both pin properties to that confirmed artifact/version. Publishing the model first remains supported with its existing dependency. Avoid adding both old and renamed detector artifacts to one app: their classes overlap.

## Configure publishing repositories

Only Maven Central and JitPack are enabled. The retired `apexfission-maven` service is no longer offered in publish or finalization dropdowns; historical metadata remains intact.

[`publishing/repositories.yml`](../publishing/repositories.yml) is the source of truth for available destinations. Each entry has a stable repository ID, a GitHub environment name, and a publishing protocol:

```yaml
repositories:
  maven-central:
    environment: maven-central
    publisher: central
  jitpack:
    environment: jitpack
    publisher: jitpack
```

Use `central` only for the existing `maven-central` ID. Other Maven-compatible services use `maven`; the `jitpack` ID uses `jitpack` for its public build service. IDs identify immutable release history, so do not rename an ID after publishing. Give each destination a separate environment. Maven URLs and credentials belong in those environments, not in this file. JitPack uses its fixed public endpoint and needs no publishing credentials or signing key.

After editing the registry, run:

```bash
python -m pip install -r scripts/requirements-publishing.txt
python scripts/publishing_config.py generate
python scripts/publishing_config.py check
```

Commit the YAML, generated `publishing/repositories.properties`, and changes to both publish workflows and the Finalize workflow. GitHub's manual dropdown cannot read an external file at display time; its choices are generated ahead of time. CI rejects stale generated configuration. The Gradle properties file is generated public configuration, keeping YAML dependencies out of ordinary Android builds. New destinations need no manually seeded release records.

## Interactive environment setup

The Python **Textual** wizard reads the same YAML and guides you through every configured environment. It runs on Windows, Linux, and macOS with Python 3.10 or newer. Run it locally from the repository root.

Windows (Command Prompt or PowerShell; activation is not needed):

```text
py -3 -m venv .venv
.venv\Scripts\python -m pip install -r scripts/requirements-setup.txt
.venv\Scripts\python scripts/setup_github_environments.py
```

Linux/macOS:

```bash
python3 -m venv .venv
.venv/bin/python -m pip install -r scripts/requirements-setup.txt
.venv/bin/python scripts/setup_github_environments.py
```

The defaults target this GitHub repository and `publishing/repositories.yml`. For another GitHub repository or registry, use `--repo OWNER/REPOSITORY --config path/to/repositories.yml`.

The first screen asks for a GitHub token in a masked field. Use a fine-grained token restricted to the target repository with **Administration: read and write** (environment creation) and **Environments: read and write** (variables/secrets). Your account must have repository administration access. A classic token with `repo` scope also works; organization approval or SSO may be required. The setup token is separate from Maven credentials and is never stored in the GitHub environments or a local configuration file.

The wizard:

1. Inspects each environment and its existing variables and secret names.
2. Prompts for each required setting. Existing variable values are shown; GitHub does not disclose secret values.
3. Preserves an existing setting when you leave its input blank. Missing required values must be supplied; the missing Central URL offers its standard default. A missing signing-key password may remain unset for an unencrypted key.
4. Accepts a path to your ASCII-armored private signing key file, preserving its multiline contents. Other secret inputs are masked too.
5. Shows a review screen before creating environments or saving changes. Cancel exits without applying the pending changes.
6. Creates missing environments and updates only entered settings. Existing environment protection rules remain untouched. Secret values are encrypted with GitHub's environment public key before upload.

Tokens and entered secret values stay in process memory and are excluded from summaries. Writes are not atomic: if a request fails or is interrupted, the wizard lists completed setting names and the failed or uncertain operation. Rerun it to inspect current state and finish the remaining settings; completed secret values cannot be read back or rolled back automatically.

The wizard manages all configured environments. JitPack has no required variables or secrets; its environment can still have approval/protection rules. Keep the separate `delayed-docs` environment's 15-minute timer configured as described below.

## Run a release

1. Merge the intended source to `main`. Review the selected module's changes and, for model releases, its `modelCoreVersion` compatibility pin.
2. Open **Actions → Publish card-detector** for the detector or **Actions → Publish card-detector-model** for the bundled model.
3. Choose **Run workflow** on `main` and select a configured **repository**, `maven-central` or `jitpack`. Each workflow fixes its own module; there is no module selector.
4. Leave **version** blank for automatic selection. With no module history anywhere, it starts at 0.1.0. If the selected module's release inputs match its latest immutable source, the workflow reuses that semantic version and original source when publishing to another destination. If release inputs changed, it increments the latest module patch (for example, 1.2.9 → 1.2.10). Changing only the destination does not allocate another version.
5. To choose a version yourself, enter stable **X.Y.Z**. An existing latest version may only identify the same release inputs and original source; an older version or conflicting source is rejected. A new explicit version must be greater than the module's latest version. To recover a destination whose upload may already have started, use Finalize instead of publishing again.

Each module has one shared semantic version/source identity across destinations. Detector and model versions remain independent. The release fingerprint covers the selected module, build inputs and release tooling; sibling module or generated installation metadata changes alone do not create a new release. Existing history, including historical destination-scoped tags, must agree on the source of a given module/version. Conflicting legacy identities fail closed and require maintainer reconciliation.
Both dedicated workflows call `publish-card-detection.yml`, a reusable implementation with no manual entry point. They select the matching GitHub environment and retain the shared release lock and publication safeguards. Only Maven Central uses delayed finalization. The documentation workflow listens for either dedicated publication to finish.

Only the selected module is staged for local publication verification. Maven destinations upload it; JitPack builds it from the immutable module source tag when its artifact is requested. Common tests and the demo still build as integration checks. Attempting a Gradle upload for both modules, for the wrong repository, or without `releaseModule`, fails before upload.

## GitHub environments and configuration

Create these environments in **Settings → Environments**. The selected repository ID resolves to the environment named in the registry.

| Environment | Environment secrets | Environment variables |
| --- | --- | --- |
| `maven-central` | `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `SIGNING_IN_MEMORY_KEY`, `SIGNING_IN_MEMORY_KEY_PASSWORD` | `MAVEN_REPOSITORY_URL` |
| `jitpack` | None | None |

Add **MAVEN_REPOSITORY_URL** under **Environment variables** in every Maven environment (not in the workflow form). Set it to the canonical HTTPS Maven repository base URL, including the repository path, for example `https://maven.example.com/releases`. This example is a placeholder. For `maven-central`, use `https://repo.maven.apache.org/maven2`; it is the artifact read/verification endpoint. Central uploads continue through its publishing service, not this URL. Do not include a username, password, query string, or fragment. For a `maven` publisher, the endpoint must support standard Maven uploads and artifact/metadata reads at that same URL. Redirects are rejected during verification. A signing-key password can be empty for an unencrypted key.

The self-hosted credentials are used for upload and artifact verification; signing remains enabled for both Maven upload destinations. JitPack artifacts are unsigned and are verified through immutable source provenance, artifact contents and pinned dependencies. The repository URL is public configuration and is recorded in release metadata and installation instructions. For a private repository, consumers need their own read credentials; never copy publishing credentials into application source.

Keep **delayed-docs** with the **15-minute wait timer**. After a Central upload, the automatic flow waits there without holding a runner or the release lock, then polls for up to **40 minutes** for the selected artifact's POM, AAR, sources, documentation, Gradle metadata, and signatures. Self-hosted uploads skip that environment delay and begin the same verification immediately. Neither upload success alone nor a pending release updates installation claims.

Version identity is independent per module and shared across destinations. Publication attempts, confirmed records, credentials and recovery remain destination-specific. The model's pinned detector must already exist on Central or the selected Maven repository; Central is checked first, matching Gradle resolution. JitPack model releases require the detector pin on Maven Central.

## JitPack build and consumer coordinates

`jitpack.yml` invokes `scripts/jitpack_build.py` for a module tag such as `carddetector/v1.2.3` or `tfmodel/v2.0.0`. The build selects exactly that module for `publishToMavenLocal`, uses the pinned Android/Gradle toolchains, and makes model assets available for a model release. JitPack does not publish the sibling module as part of this build.

JitPack's single-publication coordinate uses the GitHub repository name as its artifact ID. Its consumer version encodes the slash in the source tag as `~`:

| Module | Example JitPack dependency |
| --- | --- |
| `carddetector` | `com.github.lambdawalker:android.apexfission.carddetector:carddetector~v1.2.3` |
| `tfmodel` | `com.github.lambdawalker:android.apexfission.carddetector:tfmodel~v2.0.0` |

These are coordinate examples, not claims that those releases exist. No live JitPack build has been verified as part of this implementation. `IMPORT.md` advertises JitPack only after finalization confirms a public build at the reserved source and verifies its POM, AAR, sources, documentation and any available Gradle metadata. The model's POM must export the exact Central detector pin; the AAR must contain the expected model assets. A successful build status alone is insufficient.

The two JitPack module tags use the same artifact ID with different versions. Choose the detector **or** the bundled model coordinate; use the model's exported Central detector dependency when choosing the bundled model. Gradle would otherwise resolve their shared JitPack artifact ID as competing versions. The generated examples include `google()`, `mavenCentral()` and `https://jitpack.io`, plus any separately recorded dependency repository.
## Recovery and races

Use **Actions → Finalize CardDetector release → Run workflow**, choosing the original **repository**, **module**, and **version**. It checks and finalizes an existing publication; it never uploads. Publishing has the repository dropdown and optional version field; use this dedicated Finalize workflow to recover an interrupted release without another upload.

Automatic and manual publication/finalization share the existing repository-wide lock, with `cancel-in-progress: false`. This intentionally serializes jobs that can update `main`, while keeping module versions and attempt state independent. The wait job is outside that lock. GitHub can replace queued jobs in a concurrency group; if a queued finalization is displaced, manually run Finalize for its module/version. Durable journals prevent lost upload state.

Canonical source tags are destination-independent and become immutable at reservation, before upload or a JitPack build. A tag proves source identity, not publication availability.

| State | Detector example | Model example |
| --- | --- | --- |
| Reserved canonical source identity | `carddetector/v1.2.3` | `tfmodel/v2.0.0` |
| Central reservation journal | `release-pending/carddetector/1.2.3` | `release-pending/tfmodel/2.0.0` |
| Central upload may have begun | `release-uploading/carddetector/1.2.3` | `release-uploading/tfmodel/2.0.0` |
| JitPack reservation journal | `release-pending/jitpack/carddetector/1.2.3` | `release-pending/jitpack/tfmodel/2.0.0` |
| JitPack build may have been requested | `release-uploading/jitpack/carddetector/1.2.3` | `release-uploading/jitpack/tfmodel/2.0.0` |

Other destinations use the same destination prefix in their attempt refs, for example `release-pending/apexfission-maven/carddetector/1.2.3`. Historical tags such as `apexfission-maven/carddetector/v1.2.3` are still accepted as provenance; new releases use canonical module tags. The journal pins the repository URL; changing it while a release is pending stops recovery until the original URL is restored. Attempts on one destination do not block another destination from publishing the same reserved identity.
A pending model attempt does not block allocating a core release, and vice versa. An upload-started marker rejects any second upload for that module/version. A retry after completed finalization exits successfully; it does not move documentation backwards.

Finalization verifies Maven hashes/signatures, or JitPack build provenance and public artifact contents, against the immutable source, then reads latest `main`, updates only the selected module's confirmed metadata, and regenerates the combined installation document. It preserves a sibling release that finished during the wait. Confirmed metadata and attempt-marker removal are pushed atomically without force. The canonical source tag remains unchanged; legacy recovery can create it if missing. A race or branch-protection rejection preserves remote state for investigation and retry.

Finalization permits `main` to advance with application source, Gradle build configuration, tests and sibling releases during the wait: it verifies the reserved source and preserves current `main`. Changes to the release protocol in `scripts/` (excluding `scripts/tests/`), `publishing/`, or the installation template stop finalization instead of mixing protocol assumptions. Inspect and reconcile those changes deliberately. If artifacts are partial, rejected or in an unknown Central Portal state, keep the attempt refs and logs; do not delete markers merely to retry an upload. The Finalize workflow can be retried after propagation. A known rejected upload that never became public requires maintainer investigation before any marker removal or version reuse.

## Confirmed metadata and migration

- `docs/releases/carddetector.json`: latest confirmed detector release on Central.
- `docs/releases/tfmodel.json`: latest confirmed model release on Central and its `core_version`.
- `docs/releases/<repository>/carddetector.json` and `tfmodel.json`: destination-specific confirmed records, including self-hosted Maven and JitPack. Missing or null records make no publication claim.
- `IMPORT.md`: generated from confirmed records and `docs/templates/MODULE_IMPORT.md.template`. For each module, it shows only the highest confirmed semantic version. It offers multiple destinations only when that exact version has the same confirmed source; a source conflict fails generation. Older destinations and pending releases are omitted. A newer pending attempt cannot replace the last confirmed release. Each destination has choose-one Kotlin, Groovy, version-catalog and Maven examples using its actual consumer coordinate and required repositories.
- `docs/release.json`, `scripts/release.py` legacy journal commands, and `scripts/finalize-release.sh`: preserved for old paired-release provenance/recovery. New workflows use `scripts/module_release.py`. Shared artifact-validation helpers remain in `release.py`.

The initial records were split from the already confirmed 0.1.0 pair; their hashes and source were preserved. They refer to the existing `v0.1.0` tag through `legacy_tag`. No new tag, package upload, or registry claim is created by this migration. Later releases use module-prefixed tags; the new shared identity allocator also accepts the historical destination-scoped module tags. Old unscoped pending/uploading markers block new Central releases until resolved using the original paired-release tooling. See the [legacy runbook](legacy-releases.md) only for that historical recovery case.

## Local verification

```bash
python3 -m pip install -r scripts/requirements-setup.txt
python3 scripts/publishing_config.py check
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

A successful JitPack publication verifies the public artifacts, commits the confirmed metadata and regenerated `IMPORT.md`, then completes the module's publish workflow. The **Documentation** workflow listens for that completion, checks out latest `main` (including the new installation instructions), builds the site, and deploys it to GitHub Pages. This also applies when the **Finalize CardDetector release** recovery workflow completes successfully. No additional token or manual documentation run is needed.

The trigger watches the two manually launched module workflows, which call the reusable JitPack workflow. It does not depend on the bot's metadata commit producing a `push` event. Failed publications do not trigger this successful-release rebuild.

Documentation still deploys separately from Maven publication. Completion of publication/finalization rebuilds the site with the current independently confirmed installation facts. Retry **Documentation** for site failures; do not publish another package. No package is published by this change alone.
