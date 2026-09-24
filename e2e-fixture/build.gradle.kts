import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// CI-only stand-in for the messaging app the end-to-end suite talks to (app/src/androidTest/.../e2e). It takes the
// package name com.whatsapp so that OpenChat's real send flow, notification listener and media watcher treat it as
// that app on the emulator. Debug builds only: it is never published, signed for release or uploaded, and only the
// device workflow (.github/workflows/device.yml) installs it.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.piptechnologies.openchat.e2efixture"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.whatsapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "e2e"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// No release variant at all: the stand-in exists only as a debug APK on CI emulators.
androidComponents {
    beforeVariants(selector().withBuildType("release")) { it.enable = false }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}
