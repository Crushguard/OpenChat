import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.paparazzi)
}

android {
    namespace = "com.piptechnologies.openchat"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.piptechnologies.openchat"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables { useSupportLibrary = true }

        // Emulator end-to-end suite (app/src/androidTest/.../e2e, run by .github/workflows/device.yml): each test runs
        // in its own instrumentation under the AndroidX Test Orchestrator, which clears the app's data before it.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"

        // The 19 UI languages (res/xml/locales_config.xml): library translations (AndroidX, Material) outside
        // them are stripped, like Status Saver's localeFilters. Resource-folder notation, as our values-<qualifier>
        // folders are named (in = Indonesian, iw = Hebrew: Android resolves those, never values-id / values-he).
        // Languages.all's qualifiers, in order, then two library-only extras: aapt2 keeps a region folder only
        // when that region is listed, and AndroidX/Material ship Chinese only as values-zh-rCN (TalkBack's "On",
        // "Selected"…) and European Portuguese as values-pt-rPT. We ship no folders for those two.
        // LocalesConfigTest parses this list.
        resourceConfigurations += listOf(
            "en", "in", "pt-rBR", "pt", "ur", "hi", "tr", "es", "ar", "fa", "ps", "iw", "fr", "de", "it", "ru", "zh", "ha", "my",
            "zh-rCN", "pt-rPT",
        )
    }

    buildTypes {
        // Debug builds only: no release signing, no keystore.
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
        buildConfig = true
    }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        animationsDisabled = true
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.coil.compose)
    implementation(libs.coil.video)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext.junit)
    // Keeps Espresso (pulled in by the Compose test rule) on the same release train as the androidx.test libraries.
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.uiautomator)
    // StandardTestDispatcher for the Compose rule's effect context (e2e/E2eTest.kt).
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestUtil(libs.androidx.test.orchestrator)
    androidTestUtil(libs.androidx.test.services)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
