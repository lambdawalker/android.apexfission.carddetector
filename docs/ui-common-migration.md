# UI Common migration

This branch replaces Demo Common directly with [UI Common](https://github.com/lambdawalker/android.ui.common).
It uses the confirmed JitPack artifact:

```kotlin
implementation("com.github.lambdawalker:android.ui.common:ui-common~v0.1.0-jitpack")
```

The dependency version is pinned in `gradle/libs.versions.toml`. Gradle resolves it from
`https://jitpack.io`; no source checkout, composite substitution or mavenLocal fallback is required.

The [successful JitPack build](https://jitpack.io/com/github/lambdawalker/android.ui.common/ui-common~v0.1.0-jitpack/build.log)
and [generated installation instructions](https://github.com/lambdawalker/android.ui.common/blob/main/IMPORT.md)
confirm version 0.1.0 from source `4a87e79ab912dc97452b28d6fa2eed26362a3f40`. The AAR and POM are downloadable;
the artifact contains the shared components and declares Apache 2.0.

The integration workflow builds against this published artifact using ordinary Gradle resolution.

CatalogScreen replaces DemoScreen, ContentCard replaces the content overload of DemoCard, ActionCard
replaces its action overload, and SectionHeading replaces CardTitle. Settings labels/navigation belong
to each app. UI Common inherits the host MaterialTheme. No compatibility wrappers are retained.

UI Common is a dependency of :app only. Published library APIs and minimum SDKs are unchanged.

Historical accepted screenshot metadata remains evidence for its original source. Review affected
consumer screenshots before accepting new baselines; do not rewrite historical acceptance hashes.
