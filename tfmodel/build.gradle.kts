import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar
import org.gradle.api.publish.PublishingExtension


plugins {
    alias(libs.plugins.android.library)

    id("com.vanniktech.maven.publish")
}

android {
    namespace = "com.apexfission.android.carddetectionlite.tfmodel"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 28

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    androidResources {
        noCompress += setOf("tflite")
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":cardDetectionLite"))

    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}


val isCore = project.name == "cardDetectionLite"
val artifact = providers.gradleProperty(if (isCore) "POM_ARTIFACT_ID" else "MODEL_ARTIFACT_ID").get()
val releaseVersion = providers.gradleProperty("releaseVersion")
require(releaseVersion.orNull?.matches(Regex("(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)")) != false) {
    "releaseVersion must be a stable X.Y.Z"
}
val projectUrl = "https://github.com/lambdawalker/android.card_detection_lite"
mavenPublishing {
    coordinates(providers.gradleProperty("GROUP").get(), artifact, releaseVersion.orElse("0.0.0-SNAPSHOT").get())
    configure(AndroidSingleVariantLibrary(variant = "release", javadocJar = JavadocJar.Empty(), sourcesJar = SourcesJar.Sources()))
    publishToMavenCentral()
    if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
    pom {
        name.set(if (isCore) "Card Detection Lite" else "Sentinel Card Model")
        description.set(if (isCore) "Compose and CameraX card detection, tracking, overlays, and image extraction." else "Sentinel YOLO card model assets and catalog extensions for Card Detection Lite.")
        inceptionYear.set("2026")
        url.set(projectUrl)
        licenses { license {
            name.set("The Apache License, Version 2.0")
            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            distribution.set("repo")
        } }
        developers { developer {
            id.set("lambdawalker")
            name.set("David Garcia")
            url.set("https://github.com/lambdawalker")
        } }
        scm {
            url.set(projectUrl)
            connection.set("scm:git:$projectUrl.git")
            developerConnection.set("scm:git:ssh://git@github.com/lambdawalker/android.card_detection_lite.git")
        }
    }
}
tasks.withType<com.vanniktech.maven.publish.tasks.JavadocJar>().configureEach {
    from("README.md")
    from("docs") { into("docs") }
    from(rootProject.file("LICENSE"))
}
tasks.withType<org.gradle.jvm.tasks.Jar>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
extensions.configure<PublishingExtension> {
    repositories.maven {
        name = "verification"
        url = rootProject.layout.buildDirectory.dir("verification-repository").get().asFile.toURI()
    }
}
tasks.configureEach {
    if (name.contains("MavenCentral", ignoreCase = true)) dependsOn(rootProject.tasks.named("verifyCentralReservation"))
}
