package com.piptechnologies.openchat.e2e

import android.os.Handler
import android.os.Looper
import androidx.annotation.StringRes
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.piptechnologies.openchat.MainActivity
import com.piptechnologies.openchat.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
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
 *
 * The app's composition coroutines (LaunchedEffect, rememberCoroutineScope) run on [effects], a StandardTestDispatcher
 * that [E2eSession] keeps draining on the main thread, as AndroidUiDispatcher does in the app. The rule's default,
 * UnconfinedTestDispatcher, resumes a coroutine on whatever thread wakes it: the launch screen, which navigates after
 * reading DataStore, then navigated from DataStore's IO thread ("Method setCurrentState must be called on the main
 * thread", device run 13). Delays and frames still follow the rule's clock, which the waits advance.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
abstract class E2eTest {
    private val effects = StandardTestDispatcher()

    @get:Rule(order = 0)
    val compose: ComposeTestRule = createEmptyComposeRule(effects)

    @get:Rule(order = 1)
    val session = E2eSession(effects.scheduler)

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
 * at that moment is captured first (folder "failures"). While the test runs, [MainThreadDrain] runs the coroutines
 * queued on [scheduler] on the main thread.
 */
class E2eSession(private val scheduler: TestCoroutineScheduler) : TestRule {
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
            val drain = MainThreadDrain(scheduler)
            drain.start()
            try {
                base.evaluate()
                drain.failure?.let { throw AssertionError("Running the app's queued coroutines failed", it) }
            } catch (t: Throwable) {
                runCatching { Capture.screen("failures", "${description.className.substringAfterLast('.')}-${description.methodName}") }
                throw t
            } finally {
                close()
                drain.stop()
            }
        }
    }
}

/**
 * Runs the coroutines queued on [scheduler] at its current time, on the main thread, every [PERIOD_MS] while
 * started: what AndroidUiDispatcher does for the app's composition in production. It never moves the clock, so
 * delays and frames still wait for the Compose rule's clock. The period stays well above Espresso's look-ahead
 * (a main-thread message due within about 15 ms keeps Espresso waiting for idle), so the queue reads idle between
 * ticks; taps also run the queue themselves (ComposeTestRule.settle).
 */
@OptIn(ExperimentalCoroutinesApi::class)
private class MainThreadDrain(private val scheduler: TestCoroutineScheduler) {
    private val handler = Handler(Looper.getMainLooper())

    @Volatile private var running = false

    /** The first exception running the queue threw (coroutine failures go to the Compose rule instead). */
    @Volatile var failure: Throwable? = null
        private set

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            try {
                scheduler.runCurrent()
            } catch (t: Throwable) {
                if (failure == null) failure = t
            }
            handler.postDelayed(this, PERIOD_MS)
        }
    }

    fun start() {
        running = true
        handler.post(tick)
    }

    fun stop() {
        running = false
        handler.removeCallbacks(tick)
    }

    private companion object {
        const val PERIOD_MS = 50L
    }
}
