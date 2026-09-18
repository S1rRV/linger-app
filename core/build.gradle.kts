plugins {
    kotlin("jvm")
}

// The domain layer. Imports nothing platform-specific, so it stays portable to
// an iOS shell later: see docs/adr/0002-android-first.md. That is why times use
// kotlinx-datetime rather than java.time, which is JVM only.
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
    }
}
