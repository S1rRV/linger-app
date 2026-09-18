rootProject.name = "linger"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

// Only the pure-Kotlin domain module for now. The Android app module is added
// when there is an SDK to build it against; see docs/adr/0002-android-first.md.
include(":core")
