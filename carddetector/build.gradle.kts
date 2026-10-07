import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar
import org.gradle.api.publish.PublishingExtension

import com.android.build.gradle.BaseExtension

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)

    id("com.vanniktech.maven.publish")
}

android {
    namespace = "com.apexfission.android.carddetector"
    compileSdk {
        version = release(37)
    }
    defaultConfig {
        minSdk = 28


        consumerProguardFiles("consumer-rules.pro")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
        testInstrumentationRunnerArguments["useTestStorageService"] = "true"
        if (project.hasProperty("imageTestsOnly")) {
            testInstrumentationRunnerArguments["annotation"] = "com.apexfission.android.carddetector.GenerateImage"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }

        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }


}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(libs.apexfission.coordinates)
    api(libs.apexfission.yolo)

    implementation(libs.androidx.compose.material.icons.extended)

    /* -------------------- CameraX -------------------- */
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)


    implementation(libs.androidx.media3.inspector)
    implementation(libs.androidx.media3.inspectorframe)

    /* ---------------- TensorFlow Lite ---------------- */
    implementation(libs.litert.gpu)
    implementation(libs.litert.support) {
        exclude(group = "com.google.ai.edge.litert", module = "litert-support-api")
    }

    /* -------------------- ExoPlayer -------------------- */
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    implementation(libs.text.recognition)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.accompanist.permissions)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)


    androidTestImplementation(libs.androidx.junit.ktx)
    androidTestImplementation(project(":tfmodel"))
    androidTestImplementation(libs.androidx.testservices.storage)
    androidTestUtil(libs.androidx.testservices.testservices)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
}




tasks.register<Copy>("runTestsAndExtractImages") {
    description = "Runs UI tests, copies generated images to the project, and cleans the device."
    group = "verification"

    dependsOn("connectedDebugAndroidTest")
    from(layout.buildDirectory.dir("outputs/connected_android_test_additional_output"))
    include("**/*.png")
    includeEmptyDirs = false

    // Intercept the path and strip the first 3 folders
    // (e.g. debugAndroidTest/connected/emulator_name/)
    eachFile {
        val segments = relativePath.segments
        if (segments.size > 3) {
            // Drops the top 3 directories and joins the rest back together
            path = segments.drop(3).joinToString("/")
        }
    }

    into(layout.projectDirectory.dir("test/results/detection"))

    doLast {
        // Asks the Android Gradle Plugin for the exact path to adb.exe
        val adbPath = project.extensions.getByType<BaseExtension>().adbExecutable.absolutePath

        ProcessBuilder(adbPath, "shell", "rm", "-rf", "/sdcard/googletest/test_outputfiles/*").start().waitFor()

        println("Cleaned up test images from the device.")
    }
}




val artifact = providers.gradleProperty("POM_ARTIFACT_ID").get()
val projectUrl = "https://github.com/lambdawalker/android.apexfission.carddetector"
mavenPublishing {
    coordinates(providers.gradleProperty("GROUP").get(), artifact, project.version.toString())
    configure(AndroidSingleVariantLibrary(variant = "release", javadocJar = JavadocJar.Empty(), sourcesJar = SourcesJar.Sources()))
    publishToMavenCentral()
    if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
    pom {
        name.set("Card Detector")
        description.set("Compose and CameraX card detection, tracking, overlays, and image extraction.")
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
            developerConnection.set("scm:git:ssh://git@github.com/lambdawalker/android.apexfission.carddetector.git")
        }
    }
}
tasks.withType<com.vanniktech.maven.publish.tasks.JavadocJar>().configureEach {
    from("README.md")
    // Publish text documentation only; demonstrations remain on the website.
    from("docs") {
        include("**/*.md")
        into("docs")
    }
    from(rootProject.file("LICENSE"))
}
tasks.withType<org.gradle.jvm.tasks.Jar>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
extensions.configure<PublishingExtension> {
    providers.environmentVariable("MAVEN_REPOSITORY_URL").orNull?.takeIf { it.isNotBlank() }?.let { endpoint ->
        repositories.maven {
            name = "selectedMaven"
            url = uri(endpoint)
            credentials {
                username = providers.environmentVariable("MAVEN_REPOSITORY_USERNAME").orNull
                password = providers.environmentVariable("MAVEN_REPOSITORY_PASSWORD").orNull
            }
        }
    }
    repositories.maven {
        name = "verification"
        url = rootProject.layout.buildDirectory.dir("verification-repository").get().asFile.toURI()
    }
}
tasks.configureEach {
    if (name.contains("MavenCentral", ignoreCase = true) || name.endsWith("ToSelectedMavenRepository")) {
        dependsOn(rootProject.tasks.named("verifyPublicationReservation"))
    }
}
