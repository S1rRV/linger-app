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

// The pure-Kotlin domain, and a build-time tool that exports a Trip as JSON for
// the Expo app to render. No Android module yet; see
// docs/adr/0002-android-first.md and docs/research/expo-go-constraints.md.
include(":core")
include(":tools")
