package com.piptechnologies.openchat.e2e

import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.piptechnologies.openchat.MainActivity
import com.piptechnologies.openchat.R
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Base of the emulator end-to-end tests. The Orchestrator runs every test in a fresh instrumentation with the
 * app's data cleared; each test then starts in English with a clean stand-in (Fixture) and launches the real
 * MainActivity itself ([launch]), so it can prepare the device first. The Compose rule is empty: it attaches to
 * whatever MainActivity is showing, including one recreated by a language change.
 */
abstract class E2eTest {
    @get:Rule(order = 0)
    val compose: ComposeTestRule = createEmptyComposeRule()

    @get:Rule(order = 1)
    val session = E2eSession()

    protected val device: UiDevice by lazy { UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()) }

    @Before
    fun startClean() {
        Grants.forceEnglish()
        Grants.postNotifications(Fixture.PACKAGE)
        Fixture.reset()
    }

    protected fun launch() = session.launch()

    /** Closes the activity and starts it again, as a new launch from the launcher would. */
    protected fun relaunch() {
        session.close()
        session.launch()
    }

    /** UI text in the language the app shows right now. */
    protected fun text(@StringRes id: Int, vararg args: Any): String = Strings.current(id, *args)

    /** Home's settings gear: only Home has it, so it tells Home from onboarding (which also shows "Send Message"). */
    protected fun homeGear(): SemanticsMatcher = hasDescriptionOf(text(R.string.home_settings_cd)) and hasClickAction()

    /** First run: launch screen, both onboarding slides, then Home. */
    protected fun launchToHome() {
        launch()
        finishOnboarding()
        waitForHome()
    }

    protected fun finishOnboarding() {
        compose.waitFor(hasTextOf(text(R.string.onboarding_1_title)), timeoutMs = 30_000)
        compose.tap(hasTextOf(text(R.string.onboarding_next)))
        compose.waitFor(hasTextOf(text(R.string.onboarding_2_title)))
        compose.tap(hasTextOf(text(R.string.onboarding_start)))
    }

    protected fun waitForHome() {
        compose.waitFor(homeGear(), timeoutMs = 30_000)
    }

    protected fun openSettings() {
        compose.tap(homeGear())
        compose.waitFor(hasTextOf(text(R.string.settings_title)))
    }

    /** Taps a Home tool row by its title. */
    protected fun openTool(@StringRes title: Int) {
        compose.tap(hasTextOf(text(title)))
    }

    /** The top bar's back arrow. */
    protected fun back() {
        compose.tap(hasDescriptionOf(text(R.string.cd_back)))
    }

    /** A screenshot of a flow step, once the UI is idle (only while OpenChat is in front). */
    protected fun capture(name: String) {
        compose.waitForIdle()
        Capture.screen("flows", "${javaClass.simpleName}-$name")
    }
}

/**
 * Owns the activity of one test: launches it, and closes it when the test ends. When the test fails, the screen
 * at that moment is captured first (folder "failures").
 */
class E2eSession : TestRule {
    private var scenario: ActivityScenario<MainActivity>? = null

    fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    fun close() {
        scenario?.let { runCatching { it.close() } }
        scenario = null
    }

    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            try {
                base.evaluate()
            } catch (t: Throwable) {
                runCatching { Capture.screen("failures", "${description.className.substringAfterLast('.')}-${description.methodName}") }
                throw t
            } finally {
                close()
            }
        }
    }
}
