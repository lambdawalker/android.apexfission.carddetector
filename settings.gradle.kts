pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}


dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        if (providers.environmentVariable("RELEASE_REPOSITORY").orElse("maven-central").get() != "maven-central") {
            providers.environmentVariable("MAVEN_REPOSITORY_URL").orNull?.takeIf { it.isNotBlank() }?.let { endpoint ->
                maven {
                    url = uri(endpoint)
                    content { includeGroup("com.apexfission.android.carddetector") }
                    credentials {
                        username = providers.environmentVariable("MAVEN_REPOSITORY_USERNAME").orNull
                        password = providers.environmentVariable("MAVEN_REPOSITORY_PASSWORD").orNull
                    }
                }
            }
        }
    }
}

rootProject.name = "carddetector"

include(":app")
include(":carddetector")
include(":tfmodel")
