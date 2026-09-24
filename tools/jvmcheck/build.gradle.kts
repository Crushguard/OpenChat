// Local JVM harness: compiles and tests the pure-Kotlin core of the app
// (app/src/main/java/com/piptechnologies/openchat/core) without the Android SDK.
// Usage, from the repo root:  (cd tools/jvmcheck && gradle test)
// Android code is verified by the GitHub Actions build; this harness only covers
// platform-independent logic and its unit tests.
plugins {
    kotlin("jvm") version "2.0.21"
}

kotlin {
    jvmToolchain(21)
}

sourceSets {
    main {
        kotlin.setSrcDirs(listOf("../../app/src/main/java"))
        kotlin.include("com/piptechnologies/openchat/core/**")
    }
    test {
        kotlin.setSrcDirs(listOf("../../app/src/test/java"))
        kotlin.include("com/piptechnologies/openchat/core/**")
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}

tasks.test {
    useJUnit()
    testLogging { events("passed", "failed", "skipped"); showStandardStreams = false; exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
}
