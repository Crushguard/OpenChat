package com.piptechnologies.openchat.screenshots

import com.piptechnologies.openchat.ui.onboarding.OnboardingScreen
import com.piptechnologies.openchat.ui.splash.SplashScreen
import org.junit.Rule
import org.junit.Test

/** Launch screen and the two onboarding slides (design map §4.1, §4.2). */
object LaunchScenes {
    val splash = Scene("splash") {
        SplashScreen()
    }

    val onboarding1 = Scene("onboarding_1") {
        OnboardingScreen(slide = 0, onSkip = {}, onNext = {})
    }

    val onboarding2 = Scene("onboarding_2") {
        OnboardingScreen(slide = 1, onSkip = {}, onNext = {})
    }

    val all: List<Scene> = listOf(splash, onboarding1, onboarding2)
}

/** [LaunchScenes] in English. */
class LaunchScreenshotTests {
    @get:Rule
    val paparazzi = ScreenshotDevice.paparazzi()

    @Test
    fun splash() = paparazzi.snapshot(LaunchScenes.splash)

    @Test
    fun onboarding_1() = paparazzi.snapshot(LaunchScenes.onboarding1)

    @Test
    fun onboarding_2() = paparazzi.snapshot(LaunchScenes.onboarding2)
}
