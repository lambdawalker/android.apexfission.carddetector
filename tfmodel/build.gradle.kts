

plugins {
    alias(libs.plugins.android.library)

    id("com.apexfission.android-repo-setup")
}

android {
    namespace = "com.apexfission.android.carddetector.tfmodel"
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
    if (providers.gradleProperty("releaseModule").orNull == "tfmodel") {
        // Compile and publish against an already available core, not an unpublished sibling.
        val coreVersion = providers.gradleProperty("modelCoreVersion").get()
        require(coreVersion.matches(Regex("(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)"))) {
            "modelCoreVersion must pin a stable published core version"
        }
        val coreArtifact = providers.gradleProperty("modelCoreArtifact").get()
        api("${providers.gradleProperty("GROUP").get()}:$coreArtifact:$coreVersion")
    } else {
        api(project(":carddetector"))
    }

    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}


val projectUrl = "https://github.com/lambdawalker/android.apexfission.carddetector"
extensions.configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
    pom {
        name.set("Sentinel Card Model")
        description.set("Sentinel YOLO card model assets and catalog extensions for Card Detector.")
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
