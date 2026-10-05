<!-- Generated from confirmed public release metadata. Run ./gradlew generateImportDocs. -->
# Install Card Detection Lite

**No Maven Central release has been confirmed yet.**

Installation snippets will appear here after the first successful publication.
For now, use the [source-module instructions](README.md#use-as-a-source-module).

Both artifacts are Android AARs requiring minSdk 28, compileSdk 37, JVM 17 and a
Kotlin compiler compatible with 2.4.20. Add google() and mavenCentral() repositories.
The core exports YOLO and coordinates transitively. The optional model artifact
exports the matching core and packages the Sentinel model assets and catalog.
See [README.md](README.md) for APIs and [docs/releases.md](docs/releases.md) for releases.
