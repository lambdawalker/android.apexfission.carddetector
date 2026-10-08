<!-- Generated from independently confirmed module metadata. Run ./gradlew generateImportDocs. -->
# Install Card Detector

The two Android libraries have independent releases. Choose the detector for your own
model, or the model artifact for bundled assets and its pinned detector dependency.
An explicit detector dependency can select a newer version only when its artifact ID matches the model's pin; verify compatibility before overriding it.
Do not combine `core` and `card-detector` in one app: they contain overlapping classes. Check the model's exported coordinate below before adding a detector dependency.

Only the newest confirmed semantic version of each module is shown. When that release
is available from multiple destinations, choose one destination; the examples include
all repositories needed by that artifact and its pinned dependencies. Unconfirmed
uploads and older destination releases are never installation recommendations.

## carddetector: android.apexfission.carddetector

Confirmed version: **0.1.2**. Source: [21affd730d629430c11e7e01eec59cdc0a1bbaaf](https://github.com/lambdawalker/android.apexfission.carddetector/commit/21affd730d629430c11e7e01eec59cdc0a1bbaaf).

Choose **one** destination below and **one** dependency syntax. Each destination provides this same release; do not add duplicate dependencies.

### jitpack

Repository: **jitpack**.

Choose either the detector or the model dependency. Their module tags share one JitPack artifact ID; adding both creates a version conflict. The model already includes its pinned detector dependency.

#### Gradle Kotlin DSL

In `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

In the app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.lambdawalker:android.apexfission.carddetector:carddetector~v0.1.2")
}
```

#### Gradle Groovy DSL

In `settings.gradle`:

```groovy
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

In the app's `build.gradle`:

```groovy
dependencies {
    implementation 'com.github.lambdawalker:android.apexfission.carddetector:carddetector~v0.1.2'
}
```

#### Version catalog

Use the dependency repositories shown above. Add to `gradle/libs.versions.toml`:

```toml
[libraries]
carddetector = { module = "com.github.lambdawalker:android.apexfission.carddetector", version = "carddetector~v0.1.2" }
```

Then use this instead of the direct dependency in the app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(libs.carddetector)
}
```

#### Maven

Add these repositories and dependency to `pom.xml`:

```xml
<repositories>
  <repository>
    <id>google</id>
    <url>https://dl.google.com/dl/android/maven2</url>
  </repository>
  <repository>
    <id>central</id>
    <url>https://repo.maven.apache.org/maven2</url>
  </repository>
  <repository>
    <id>confirmed-2</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>
<dependencies>
  <dependency>
    <groupId>com.github.lambdawalker</groupId>
    <artifactId>android.apexfission.carddetector</artifactId>
    <version>carddetector~v0.1.2</version>
    <type>aar</type>
  </dependency>
</dependencies>
```

## tfmodel: card-detector-model

Confirmed version: **0.1.1**. Source: [acb0035aee1a0aec0c6886617243514a040fa650](https://github.com/lambdawalker/android.apexfission.carddetector/commit/acb0035aee1a0aec0c6886617243514a040fa650).

Choose **one** destination below and **one** dependency syntax. Each destination provides this same release; do not add duplicate dependencies.

### maven-central

Repository: **maven-central**.

Exports `com.apexfission.android.carddetector:core:0.1.0`; the model version is independent. The repository examples include its pinned detector repository.

#### Gradle Kotlin DSL

In `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

In the app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.apexfission.android.carddetector:card-detector-model:0.1.1")
}
```

#### Gradle Groovy DSL

In `settings.gradle`:

```groovy
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

In the app's `build.gradle`:

```groovy
dependencies {
    implementation 'com.apexfission.android.carddetector:card-detector-model:0.1.1'
}
```

#### Version catalog

Use the dependency repositories shown above. Add to `gradle/libs.versions.toml`:

```toml
[libraries]
tfmodel = { module = "com.apexfission.android.carddetector:card-detector-model", version = "0.1.1" }
```

Then use this instead of the direct dependency in the app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(libs.tfmodel)
}
```

#### Maven

Add these repositories and dependency to `pom.xml`:

```xml
<repositories>
  <repository>
    <id>google</id>
    <url>https://dl.google.com/dl/android/maven2</url>
  </repository>
  <repository>
    <id>central</id>
    <url>https://repo.maven.apache.org/maven2</url>
  </repository>
</repositories>
<dependencies>
  <dependency>
    <groupId>com.apexfission.android.carddetector</groupId>
    <artifactId>card-detector-model</artifactId>
    <version>0.1.1</version>
    <type>aar</type>
  </dependency>
</dependencies>
```

## Requirements

Both artifacts target Android API 28+; this repository uses compileSdk 37,
JVM 17 bytecode, and Kotlin 2.4.20-compatible tooling. The Gradle daemon uses JDK 21.
Core exports YOLO and coordinates. The optional model artifact exports its declared
core dependency; model and core version numbers need not match.

See [quickstart](docs/agents/quickstart.md) and [independent releases](docs/releases.md).
