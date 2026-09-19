plugins {
    kotlin("jvm")
    application
}

// Kept out of :core on purpose. Writing a file needs java.io, and
// docs/adr/0002-android-first.md says the domain imports nothing
// platform-specific. This module is a build-time tool, not part of the app.
dependencies {
    implementation(project(":core"))
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("app.linger.tools.ExportTripKt")
}

// Gradle runs a JavaExec from the module directory, so without this the
// fixture lands at tools/app/assets/ and the app never sees it.
tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
    args = listOf("app/assets/trip.json")
}

/** What the app reads. Regenerate with `gradle exportTrip`. */
tasks.register("exportTrip") {
    group = "linger"
    description = "Writes the sample Trip to app/assets/trip.json for the Expo app"
    dependsOn("run")
}
