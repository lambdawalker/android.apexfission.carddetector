

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


apply(from = rootProject.file("gradle/publish-library.gradle.kts"))
