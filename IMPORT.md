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

Choose either the detector or the model when using JitPack: their module tags share one artifact ID, so adding both creates a version conflict. The model exports a pinned, independently versioned detector dependency; its version does not imply the detector version. Check the selected model release's installation page for its confirmed detector pin.

## carddetector: card-detector

Choose one publishing destination and one dependency syntax.

### maven-central

Repository: **maven-central**. Confirmed version **0.1.10**. [Source commit](https://github.com/lambdawalker/android.apexfission.carddetector/commit/b36e4fce5b7583cb1f8a4fb6fe8c0888727b9e2c).

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
    implementation("com.apexfission.android.carddetector:card-detector:0.1.10")
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
    implementation 'com.apexfission.android.carddetector:card-detector:0.1.10'
}
```

#### Version catalog

Use the dependency repositories shown above. Add to `gradle/libs.versions.toml`:

```toml
[libraries]
carddetector = { module = "com.apexfission.android.carddetector:card-detector", version = "0.1.10" }
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
</repositories>
<dependencies>
  <dependency>
    <groupId>com.apexfission.android.carddetector</groupId>
    <artifactId>card-detector</artifactId>
    <version>0.1.10</version>
    <type>aar</type>
  </dependency>
</dependencies>
```

### jitpack

Repository: **jitpack**. Confirmed version **0.1.10**. [Source commit](https://github.com/lambdawalker/android.apexfission.carddetector/commit/39e57dcf08453daf350d3f402eeea301f834aced).

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
    implementation("com.github.lambdawalker:android.apexfission.carddetector:carddetector~v0.1.10-jitpack")
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
    implementation 'com.github.lambdawalker:android.apexfission.carddetector:carddetector~v0.1.10-jitpack'
}
```

#### Version catalog

Use the dependency repositories shown above. Add to `gradle/libs.versions.toml`:

```toml
[libraries]
carddetector = { module = "com.github.lambdawalker:android.apexfission.carddetector", version = "carddetector~v0.1.10-jitpack" }
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
    <version>carddetector~v0.1.10-jitpack</version>
    <type>aar</type>
  </dependency>
</dependencies>
```

## tfmodel: android.apexfission.carddetector

Confirmed version: **0.1.2**. Source: [f8ba3438cf6750592b2e620020f8b9d9a81945ea](https://github.com/lambdawalker/android.apexfission.carddetector/commit/f8ba3438cf6750592b2e620020f8b9d9a81945ea).

Choose **one** destination below and **one** dependency syntax. Each destination provides this same release; do not add duplicate dependencies.

### jitpack

Repository: **jitpack**.

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
    implementation("com.github.lambdawalker:android.apexfission.carddetector:tfmodel~v0.1.2")
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
    implementation 'com.github.lambdawalker:android.apexfission.carddetector:tfmodel~v0.1.2'
}
```

#### Version catalog

Use the dependency repositories shown above. Add to `gradle/libs.versions.toml`:

```toml
[libraries]
tfmodel = { module = "com.github.lambdawalker:android.apexfission.carddetector", version = "tfmodel~v0.1.2" }
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
  <repository>
    <id>confirmed-2</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>
<dependencies>
  <dependency>
    <groupId>com.github.lambdawalker</groupId>
    <artifactId>android.apexfission.carddetector</artifactId>
    <version>tfmodel~v0.1.2</version>
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
