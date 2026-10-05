// Use the compiler required by the released YOLO/coordinates Kotlin metadata.
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    }
}
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    id("com.vanniktech.maven.publish") version "0.37.0" apply false
}

val releaseVersion = providers.gradleProperty("releaseVersion").orElse("0.0.0-SNAPSHOT")
subprojects {
    group = rootProject.providers.gradleProperty("GROUP").get()
    version = releaseVersion.get()
}
// One guard per Gradle invocation, shared by both Maven publications.
tasks.register<Exec>("verifyCentralReservation") {
    workingDir(rootDir)
    commandLine("python3", "scripts/release.py", "guard", "--version", providers.gradleProperty("releaseVersion").orElse("").get())
    doFirst {
        listOf("mavenCentralUsername", "mavenCentralPassword", "signingInMemoryKey").forEach {
            require(!providers.gradleProperty(it).orNull.isNullOrBlank()) { "Missing release credential: $it" }
        }
    }
}
listOf("generateImportDocs" to "generate", "verifyImportDocs" to "verify").forEach { (taskName, command) ->
    tasks.register<Exec>(taskName) {
        group = "documentation"
        workingDir(rootDir)
        commandLine("python3", "scripts/release.py", command)
    }
}
