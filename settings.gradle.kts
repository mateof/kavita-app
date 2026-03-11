pluginManagement {
    includeBuild("build-logic")
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
    @Suppress("UnstableApiUsage")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://s3.amazonaws.com/repo.commonsware.com") }
        maven {
            url = uri("https://jitpack.io")
            content {
                includeGroupByRegex("com\\.github.*")
            }
        }
    }
}

rootProject.name = "kavita-app"

include(":app")

// Core modules
include(":core:core-model")
include(":core:core-common")
include(":core:core-network")
include(":core:core-database")
include(":core:core-data")
include(":core:core-ui")
include(":core:core-readium")

// Feature modules
include(":feature:feature-auth")
include(":feature:feature-home")
include(":feature:feature-library")
include(":feature:feature-search")
include(":feature:feature-reader")
include(":feature:feature-downloads")
include(":feature:feature-stats")
include(":feature:feature-opds")
include(":feature:feature-admin")
include(":feature:feature-settings")

// Sync module
include(":sync")
