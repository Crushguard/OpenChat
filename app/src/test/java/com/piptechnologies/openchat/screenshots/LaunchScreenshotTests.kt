package com.piptechnologies.openchat.screenshots

import com.piptechnologies.openchat.ui.onboarding.OnboardingScreen
import com.piptechnologies.openchat.ui.splash.SplashScreen
import com.piptechnologies.openchat.ui.theme.OpenChatTheme
import org.junit.Rule
import org.junit.Test

/** Launch screen and the two onboarding slides (design map §4.1, §4.2). */
class LaunchScreenshotTests {
    @get:Rule
    val paparazzi = ScreenshotDevice.paparazzi()

    @Test
    fun splash() {
        paparazzi.snapshot {
            OpenChatTheme {
                SplashScreen()
            }
        }
    }

    @Test
    fun onboarding_1() {
        paparazzi.snapshot {
            OpenChatTheme {
                OnboardingScreen(slide = 0, onSkip = {}, onNext = {})
            }
        }
    }

    @Test
    fun onboarding_2() {
        paparazzi.snapshot {
            OpenChatTheme {
                OnboardingScreen(slide = 1, onSkip = {}, onNext = {})
            }
        }
    }
}
