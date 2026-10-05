# Legacy paired-release procedure: Publishing Card Detector

## Publications

The `carddetector` and `tfmodel` modules publish together at one stable X.Y.Z:

- `com.apexfission.android.carddetector:core:X.Y.Z`
- `com.apexfission.android.carddetector:sentinel-card-model:X.Y.Z`

`gradle.properties` declares group/artifact IDs. Core exports the pinned YOLO and
coordinates dependencies from the version catalog. The optional model exports
that same release of core and includes its original Sentinel asset. The demo is
built from local core/model projects; extracted dependencies resolve from Maven
Central. The app itself is not published to Maven.

Both AARs include sources, a documentation JAR, POM, module metadata, and detached
signatures. Documentation archives contain maintained module guides. The local
validator checks public API dependency scopes, JVM 17 classes, the model checksum,
and both complete publication sets. The core must not contain model binaries.

## Environment

The publishing workflow uses this repository's **maven-central** environment:

- `MAVEN_CENTRAL_USERNAME`
- `MAVEN_CENTRAL_PASSWORD`
- `SIGNING_IN_MEMORY_KEY`
- `SIGNING_IN_MEMORY_KEY_PASSWORD` (for an encrypted signing key)

Use a Central Portal token authorized for the group and a signing key registered
with the public keyservers required by Central. Secret values are not printed.
The workflow checks required secret presence before creating release markers.

Create **delayed-docs** in this repository with a **15-minute wait timer**.
Leave required reviewers unset for automatic continuation, and allow `main` if
restricting deployment branches. No secrets are needed in this environment.
Configure the timer before the next publish; referencing an environment in YAML
alone does not configure a wait timer. Environments are repository-specific.
The existing permission to write release docs and tags is still required; no new
personal access token or Maven credentials are needed.

## Validate before release

The non-publishing verification workflow runs on main pushes, pull requests, or
manual dispatch. It runs helper tests, library unit tests and lint, builds the
app/device-test APKs, creates both unsigned publications in
`build/verification-repository`, and validates their contents. The demo APK is
uploaded as `card-detection-demo`. Device tests must still be run on hardware or
an emulator; building them does not establish camera/model accuracy.

The build uses AGP 9.4.1, Gradle 9.6.0, Kotlin 2.4.20 (explicitly aligned with the
released dependencies), JDK 21 for the daemon and JDK 17 for compilation. CI uses
the hosted Android SDK as in the permissions repo; no command-line SDK replacement
or explicit sdkmanager package installation is performed. Git LFS assets are hydrated.

```bash
python3 -m unittest discover -s scripts/tests -v
python3 scripts/release.py verify
./gradlew verifyImportDocs :carddetector:testDebugUnitTest :app:assembleDebug
./gradlew :carddetector:publishAllPublicationsToVerificationRepository :tfmodel:publishAllPublicationsToVerificationRepository -PreleaseVersion=9.8.7
python3 scripts/release.py check-local --version 9.8.7 --source "$(git rev-parse HEAD)"
```

The sentinel version above is local test data. Use a clean tracked checkout for
artifact validation. Normal builds use `0.0.0-SNAPSHOT` and require no secrets.

## Publish

Run **Publish Card Detector libraries** on main. For the first coordinated
stable release, enter `initial_version` (for example `0.1.0`) and leave
`resume_version` empty. Subsequent normal runs leave both empty and increment the
stable patch version. Older beta/model-prefixed versions are not stable history.
If only one artifact has a stable version, preparation stops for reconciliation.

The workflow validates both artifacts and the demo before reserving
`release-pending/X.Y.Z`. The annotated tag records the source SHA and hashes of
both full artifact sets. One shared Gradle guard creates
`release-uploading/X.Y.Z` before either Central publication task. Both modules
are signed and published in one Gradle invocation; an existing upload marker
prevents blind re-upload attempts.

After the publishing plugin completes, a separate job waits on **delayed-docs**
for 15 minutes without occupying a runner or holding the release lock. It then
calls **Finalize CardDetector release**, which polls Maven Central for up to
40 minutes, stopping as soon as both complete artifact sets are available.
This allows roughly **15 + 40 = 55 minutes** after plugin completion, plus
runner queue/setup time. The finalization job has a 50-minute execution timeout.
Any waiting inside the Gradle publishing plugin remains in the publishing job.

Only after both public artifact sets and signatures are available and their
hashes match does the workflow generate `IMPORT.md` and `docs/release.json`.
Finalization atomically commits the confirmed docs, creates vX.Y.Z at the source
commit, and removes attempt markers. It never force-pushes or moves stable tags.
No GitHub Release is created. Only manual dispatch can publish to Central.

## Manual finalization and race protection

Open **Actions > Finalize CardDetector release > Run workflow**, select **main**,
and enter the existing version (for example `0.1.0`, without the `v` prefix).
This skips the 15-minute delay and uses the same verification/finalization code
as the automatic path. It never builds, reserves a new version, or uploads again.
Both `core` and `sentinel-card-model` must pass the existing hash, content, and
signature-availability checks before documentation or tags are updated.

Publishing and automatic/manual finalization share the job-level concurrency
group `maven-central-card-detection` with `cancel-in-progress: false`. The delay
job is outside this group, so manual verification can run during the delay. If
publishing or another finalization is active, the manual job waits. GitHub's
default queue retains one pending job; repeated requests can replace pending
requests but do not cancel an active upload or finalization.

After acquiring the lock, the finalizer reloads remote release state. If another
run already finalized the version, it exits successfully without polling or
writing again. Older finalized versions cannot overwrite newer documentation.
Conflicting source commits, inconsistent release state, and unknown versions
stop the job. Immutable source checks, paired-publication validation, and the
atomic non-force Git push remain in place. New publication attempts are still
blocked while an earlier release has unresolved markers.

The original `resume_version` input remains available and skips the delay;
the dedicated finalization action is the preferred recovery entry point.

## Recovery

- Before reservation: fix the error and start a fresh normal run.
- After upload starts, on timeout, or with a partial release: retain attempt tags,
  inspect Central Portal and deployment logs, and determine the actual outcome.
- If both artifacts are published but confirmation/finalization failed, run **Finalize CardDetector release**
  with `version=X.Y.Z`. Recovery only verifies/finalizes the
  original attempt; it never uploads again.
- If the attempt definitely failed before any artifact became public and no
  deployment can still publish, an authorized maintainer can remove that exact
  pending/uploading tag pair before starting a new attempt. Never delete stable
  tags or reuse any version already partially published.
- Partial immutable releases require manual reconciliation with Central. The
  workflow deliberately does not claim success or allocate another version.

Keep branch/environment protections in place. A protected-branch or concurrent
build-input change can prevent finalization; preserve the markers and reconcile
through the repository's normal review process rather than force-pushing.

Edit `docs/templates/IMPORT.md.template` and run `./gradlew generateImportDocs`
for installation wording changes. The renderer only advertises confirmed
releases; it never derives released versions from a proposed build version.


This archived procedure applies only to journals created before independent module publishing. For current workflows, use [independent releases](releases.md).
