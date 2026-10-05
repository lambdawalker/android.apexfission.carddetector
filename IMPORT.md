<!-- Generated from independently confirmed module metadata. Run ./gradlew generateImportDocs. -->
# Install Card Detector

The two Android libraries have independent releases. Choose the detector for your own
model, or the model artifact for bundled assets and its pinned detector dependency.
An explicit detector dependency can select a newer version only when its artifact ID matches the model's pin; verify compatibility before overriding it.
Do not combine `core` and `card-detector` in one app: they contain overlapping classes. Check the model's exported coordinate below before adding a detector dependency.

Add `google()` and `mavenCentral()` to your dependency repositories.

## core

Confirmed version: **0.1.0**. Source: [223f543e5f6862d296b2a54e96c35651a6d33eaa](https://github.com/lambdawalker/android.apexfission.carddetector/commit/223f543e5f6862d296b2a54e96c35651a6d33eaa).

Configured next publication: `com.apexfission.android.carddetector:card-detector` — not yet confirmed here. The dependency below remains the last confirmed publication.

### Gradle Kotlin DSL

```kotlin
implementation("com.apexfission.android.carddetector:core:0.1.0")
```

### Gradle Groovy DSL

```groovy
implementation 'com.apexfission.android.carddetector:core:0.1.0'
```

### Version catalog

```toml
[libraries]
carddetector = { module = "com.apexfission.android.carddetector:core", version = "0.1.0" }
```

### Maven

```xml
<dependency>
  <groupId>com.apexfission.android.carddetector</groupId>
  <artifactId>core</artifactId>
  <version>0.1.0</version>
  <type>aar</type>
</dependency>
```

## sentinel-card-model

Confirmed version: **0.1.0**. Source: [223f543e5f6862d296b2a54e96c35651a6d33eaa](https://github.com/lambdawalker/android.apexfission.carddetector/commit/223f543e5f6862d296b2a54e96c35651a6d33eaa).

Configured next publication: `com.apexfission.android.carddetector:card-detector-model` — not yet confirmed here. The dependency below remains the last confirmed publication.

Exports `com.apexfission.android.carddetector:core:0.1.0`; the model version is independent.

### Gradle Kotlin DSL

```kotlin
implementation("com.apexfission.android.carddetector:sentinel-card-model:0.1.0")
```

### Gradle Groovy DSL

```groovy
implementation 'com.apexfission.android.carddetector:sentinel-card-model:0.1.0'
```

### Version catalog

```toml
[libraries]
tfmodel = { module = "com.apexfission.android.carddetector:sentinel-card-model", version = "0.1.0" }
```

### Maven

```xml
<dependency>
  <groupId>com.apexfission.android.carddetector</groupId>
  <artifactId>sentinel-card-model</artifactId>
  <version>0.1.0</version>
  <type>aar</type>
</dependency>
```

## Requirements

Both artifacts target Android API 28+; this repository uses compileSdk 37,
JVM 17 bytecode, and Kotlin 2.4.20-compatible tooling. The Gradle daemon uses JDK 21.
Core exports YOLO and coordinates. The optional model artifact exports its declared
core dependency; model and core version numbers need not match.

See [quickstart](docs/agents/quickstart.md) and [independent releases](docs/releases.md).
