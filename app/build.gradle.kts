
plugins {
    alias(libs.plugins.compose.screenshot)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
    namespace = "com.apexfission.android.carddetector.demo"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.apexfission.android.carddetector.demo"
        minSdk = 28
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.androidx.ui.tooling)


    // The demo exercises the current capture API. The model module still builds
    // and publishes against its pinned core when releaseModule=tfmodel.
    implementation(project(":carddetector"))
    implementation(project(":tfmodel")) {
        exclude(
            group = providers.gradleProperty("GROUP").get(),
            module = providers.gradleProperty("modelCoreArtifact").get()
        )
    }
    implementation(libs.apexfission.permission)
    implementation(libs.apexfission.yolo)

    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
