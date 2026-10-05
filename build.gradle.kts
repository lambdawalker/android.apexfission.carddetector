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

// A release invocation selects exactly one publication. The sibling keeps its development version.
val releaseModule = providers.gradleProperty("releaseModule").orElse("")
val releaseVersion = providers.gradleProperty("releaseVersion").orElse("0.0.0-SNAPSHOT")
require(releaseModule.get() in listOf("", "carddetector", "tfmodel")) { "Unknown releaseModule" }
if (releaseModule.get().isNotBlank()) {
    require(releaseVersion.get().matches(Regex("(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)"))) {
        "Selected module requires a stable releaseVersion"
    }
}
subprojects {
    group = rootProject.providers.gradleProperty("GROUP").get()
    version = if (name == releaseModule.get()) releaseVersion.get()
        else providers.gradleProperty(if (name == "tfmodel") "modelVersion" else "coreVersion").orElse("0.0.0-SNAPSHOT").get()
}
// Reject accidental multi-module Central invocations before any reservation is consumed.
gradle.taskGraph.whenReady {
    allTasks.filter { it.name.contains("MavenCentral", ignoreCase = true) }.forEach {
        require(it.project.name == releaseModule.get()) { "Only the selected releaseModule may publish: ${it.path}" }
    }
}
tasks.register<Exec>("verifyCentralReservation") {
    workingDir(rootDir)
    commandLine("python3", "scripts/module_release.py", "guard", "--module", releaseModule.get(), "--version", releaseVersion.get())
    doFirst {
        require(releaseModule.get().isNotBlank()) { "Set releaseModule to carddetector or tfmodel" }
        listOf("mavenCentralUsername", "mavenCentralPassword", "signingInMemoryKey").forEach {
            require(!providers.gradleProperty(it).orNull.isNullOrBlank()) { "Missing release credential: $it" }
        }
    }
}
listOf("generateImportDocs" to "generate", "verifyImportDocs" to "verify").forEach { (taskName, command) ->
    tasks.register<Exec>(taskName) {
        group = "documentation"
        workingDir(rootDir)
        commandLine("python3", "scripts/module_release.py", command)
    }
}
