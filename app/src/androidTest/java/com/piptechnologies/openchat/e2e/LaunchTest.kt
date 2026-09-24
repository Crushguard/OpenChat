package com.piptechnologies.openchat.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.piptechnologies.openchat.R
import org.junit.Test
import org.junit.runner.RunWith

/** Design map §3, §4.1–§4.3: the first run goes launch → onboarding (2 slides) → Home; later launches skip onboarding. */
@RunWith(AndroidJUnit4::class)
class LaunchTest : E2eTest() {
    @Test
    fun firstRunGoesThroughOnboardingToHome_andARelaunchSkipsOnboarding() {
        launch()
        // The launch screen: brand name and spinner (its 1.2 s run on the Compose clock, which moves only while we wait).
        compose.waitFor(isIndeterminateProgress)
        compose.waitFor(hasTextOf(text(R.string.app_name)))
        capture("splash")

        compose.waitFor(hasTextOf(text(R.string.onboarding_1_title)), timeoutMs = 30_000)
        capture("onboarding_1")
        compose.tap(hasTextOf(text(R.string.onboarding_next)))
        compose.waitFor(hasTextOf(text(R.string.onboarding_2_title)))
        capture("onboarding_2")
        compose.tap(hasTextOf(text(R.string.onboarding_start)))

        waitForHome()
        compose.waitFor(hasTextOf(text(R.string.home_send)) and hasClickAction()).assertIsDisplayed()
        capture("home_first_run")

        relaunch()
        val onboardingTitle = hasTextOf(text(R.string.onboarding_1_title))
        val home = homeGear()
        compose.waitUntil(timeoutMillis = 30_000) {
            check(!compose.exists(onboardingTitle)) { "Onboarding was shown again after it had been finished" }
            compose.exists(home)
        }
        capture("home_relaunch")
    }
}
