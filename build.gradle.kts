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
    id("com.apexfission.android-repo-setup.root") version "v0.1.12"
    id("com.apexfission.android-repo-setup") version "v0.1.12" apply false
}
