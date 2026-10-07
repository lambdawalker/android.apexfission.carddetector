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
val releaseRepository = providers.environmentVariable("RELEASE_REPOSITORY").orElse("maven-central")
require(releaseRepository.get() in listOf("maven-central", "apexfission-maven")) { "Unknown RELEASE_REPOSITORY" }
// Reject accidental multi-module or cross-repository invocations before any reservation is consumed.
gradle.taskGraph.whenReady {
    allTasks.filter { it.name.contains("MavenCentral", ignoreCase = true) || it.name.endsWith("ToApexfissionRepository") }.forEach {
        val taskRepository = if (it.name.contains("MavenCentral", ignoreCase = true)) "maven-central" else "apexfission-maven"
        require(taskRepository == releaseRepository.get()) { "Upload task does not match RELEASE_REPOSITORY: ${it.path}" }
        require(it.project.name == releaseModule.get()) { "Only the selected releaseModule may publish: ${it.path}" }
    }
}
tasks.register<Exec>("verifyPublicationReservation") {
    workingDir(rootDir)
    commandLine("python3", "scripts/module_release.py", "guard", "--module", releaseModule.get(), "--version", releaseVersion.get())
    doFirst {
        require(releaseModule.get().isNotBlank()) { "Set releaseModule to carddetector or tfmodel" }
        val credentials = if (releaseRepository.get() == "maven-central") listOf("mavenCentralUsername", "mavenCentralPassword", "signingInMemoryKey") else listOf("signingInMemoryKey")
        if (releaseRepository.get() == "apexfission-maven") {
            listOf("MAVEN_REPOSITORY_URL", "MAVEN_REPOSITORY_USERNAME", "MAVEN_REPOSITORY_PASSWORD").forEach {
                require(!providers.environmentVariable(it).orNull.isNullOrBlank()) { "Missing repository setting: $it" }
            }
        }
        credentials.forEach {
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
