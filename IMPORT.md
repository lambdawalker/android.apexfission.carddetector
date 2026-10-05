<!-- Generated from confirmed public release metadata. Run ./gradlew generateImportDocs. -->
# Install Card Detector

Confirmed release: **0.1.0** · Maven coordinates: `com.apexfission.android.carddetector:core:0.1.0`.

## Gradle Kotlin DSL

Add `mavenCentral()` to your settings repositories, then:

```kotlin
dependencies {
    implementation("com.apexfission.android.carddetector:core:0.1.0")
}
```

## Gradle Groovy DSL

```groovy
dependencies {
    implementation 'com.apexfission.android.carddetector:core:0.1.0'
}
```

## Version catalog

```toml
[versions]
card-detection-lite = "0.1.0"

[libraries]
card-detection-lite = { module = "com.apexfission.android.carddetector:core", version.ref = "card-detection-lite" }
```

```kotlin
implementation(libs.card.detection.lite)
```

## Maven

```xml
<dependency>
    <groupId>com.apexfission.android.carddetector</groupId>
    <artifactId>core</artifactId>
    <version>0.1.0</version>
    <type>aar</type>
</dependency>
```

Built from source commit [`223f543e5f6862d296b2a54e96c35651a6d33eaa`](https://github.com/lambdawalker/android.apexfission.carddetector/commit/223f543e5f6862d296b2a54e96c35651a6d33eaa).

Optional bundled model (exports the matching core):

```kotlin
implementation("com.apexfission.android.carddetector:sentinel-card-model:0.1.0")
```


Both artifacts are Android AARs requiring minSdk 28, compileSdk 37, JVM 17 and a
Kotlin compiler compatible with 2.4.20. Add google() and mavenCentral() repositories.
The core exports YOLO and coordinates transitively. The optional model artifact
exports the matching core and packages the Sentinel model assets and catalog.
See [README.md](README.md) for APIs and [docs/releases.md](docs/releases.md) for releases.
