package com.piptechnologies.openchat.e2e

import android.app.Activity
import androidx.annotation.StringRes
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isSelectable
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.piptechnologies.openchat.MainActivity
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.ui.settings.LanguageOption
import com.piptechnologies.openchat.ui.settings.Languages
import java.util.Locale
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The translation proof (plan 2026-09-24, V2-e2e). Every language of [Languages.all] is picked through the real
 * Language screen (rows found by their native name, which reads the same in every UI language); once the activity
 * has been recreated in it, the Settings title must be that language's own `settings_title`, right-to-left
 * languages must put the back arrow at the right edge, and a language whose values-<qualifier> folder is in the APK
 * must really translate (settings_title and gate_body differ from English). Settings, Language, Home, Messages
 * (one conversation seeded through the stand-in), Deleted media, the gate (notification access off) and the Second
 * account entry are captured for each language, then the app goes back to English. A language that fails is
 * recorded and the sweep goes on with the next one; the test fails at the end with every failure listed.
 */
@RunWith(AndroidJUnit4::class)
class LanguageSweepTest : E2eTest() {
    @Test
    fun everyLanguageIsAppliedThroughTheLanguageScreen() {
        Grants.mediaPermissions()
        Grants.notificationListener(allowed = true)
        launchToHome()

        Fixture.postChat(AYU, listOf(ChatLine(System.currentTimeMillis() - 60_000, AYU, "Hi kak, is the blue one still available?")))
        openTool(R.string.tool_unseen_title)
        compose.waitFor(hasTextOf(AYU) and hasClickAction(), timeoutMs = 30_000)
        back()
        waitForHome()

        val failures = mutableListOf<String>()
        for (option in Languages.all) {
            try {
                sweep(option)
            } catch (t: Throwable) {
                failures += "${option.tag}: ${t.message.orEmpty().take(MESSAGE_LIMIT)}"
                runCatching { Capture.screen("failures", "LanguageSweepTest-${option.tag}") }
                val recovered = runCatching { recover() }
                if (recovered.isFailure) {
                    failures += "could not get back to Home after ${option.tag}: ${recovered.exceptionOrNull()?.message.orEmpty().take(MESSAGE_LIMIT)}"
                    break
                }
            }
        }
        try {
            val english = Languages.all.first()
            switchTo(english)
            back()
            compose.waitFor(hasTextOf(Strings.of(Locale.forLanguageTag(english.tag), R.string.settings_title)))
            shot(english.tag, "settings_again")
        } catch (t: Throwable) {
            failures += "back to English: ${t.message.orEmpty().take(MESSAGE_LIMIT)}"
        }
        assertTrue("Languages that failed (${failures.size}):\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    /** From Home: pick [option], check it on Language and Settings, capture the seven screens, end on Home. */
    private fun sweep(option: LanguageOption) {
        val locale = Locale.forLanguageTag(option.tag)
        switchTo(option)

        compose.waitFor(hasTextOf(Strings.of(locale, R.string.language_title)))
        assertBackArrowSide(option, "Language")
        shot(option.tag, "language")
        back()

        compose.waitFor(hasTextOf(Strings.of(locale, R.string.settings_title)))
        assertBackArrowSide(option, "Settings")
        shot(option.tag, "settings")
        if (option.tag != Languages.all.first().tag && Strings.hasTranslations(locale)) {
            assertTranslated(locale, R.string.settings_title, "settings_title")
            assertTranslated(locale, R.string.gate_body, "gate_body")
        }
        back()

        waitForHome()
        shot(option.tag, "home")

        openTool(R.string.tool_unseen_title)
        compose.waitFor(hasTextOf(AYU) and hasClickAction())
        shot(option.tag, "messages")
        back()
        waitForHome()

        openTool(R.string.tool_media_title)
        compose.waitFor(hasTextOf(text(R.string.media_title)))
        shot(option.tag, "media")
        back()
        waitForHome()

        Grants.notificationListener(allowed = false)
        try {
            openTool(R.string.tool_unseen_title)
            compose.waitFor(hasTextOf(text(R.string.gate_body)))
            shot(option.tag, "gate")
            back()
            waitForHome()
        } finally {
            Grants.notificationListener(allowed = true)
        }

        openTool(R.string.tool_second_title)
        compose.waitFor(hasTextOf(text(R.string.second_entry_title)))
        shot(option.tag, "second")
        back()
        waitForHome()
    }

    /** From Home: Settings → Language → the row named [LanguageOption.native], then waits for the recreated activity. */
    private fun switchTo(option: LanguageOption) {
        openSettings()
        compose.tap(hasTextOf(text(R.string.settings_language)))
        compose.waitFor(hasTextOf(text(R.string.language_title)))
        compose.tap(hasTextOf(option.native) and isSelectable())
        Waits.until("the app to show ${option.tag}", timeoutMs = 30_000, pollMs = 200) {
            val activity = Activities.resumed() as? MainActivity
            activity != null && languageOf(activity) == option.tag
        }
    }

    /** The language an activity is shown in, as the app names it (its configuration's first locale, matched). */
    private fun languageOf(activity: Activity): String {
        var tag = ""
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            tag = Languages.match(listOf(activity.resources.configuration.locales[0])).tag
        }
        return tag
    }

    /** The top bar's back arrow: at the start edge, which is the right edge in a right-to-left language. */
    private fun assertBackArrowSide(option: LanguageOption, screen: String) {
        val arrow = compose.waitFor(hasDescriptionOf(text(R.string.cd_back)) and hasClickAction()).fetchSemanticsNode()
        val activity = Activities.resumedMain()
        var width = 0
        InstrumentationRegistry.getInstrumentation().runOnMainSync { width = activity.window.decorView.width }
        val x = arrow.boundsInWindow.center.x
        if (option.rtl) {
            assertTrue("$screen in ${option.tag}: the back arrow is at the right edge (x = $x of $width)", x > width * 0.75f)
        } else {
            assertTrue("$screen in ${option.tag}: the back arrow is at the left edge (x = $x of $width)", x < width * 0.25f)
        }
    }

    /** A translated language must not fall back to English for [id]: it would mean a missing translation. */
    private fun assertTranslated(locale: Locale, @StringRes id: Int, name: String) {
        assertNotEquals(
            "${locale.toLanguageTag()} has translations in the APK but $name is still English",
            Strings.of(Strings.ENGLISH, id),
            Strings.of(locale, id),
        )
    }

    private fun shot(folder: String, name: String) {
        compose.waitForIdle()
        Capture.screen(folder, name)
    }

    /** After a failure: a fresh launch lands on Home (onboarding is done), in whatever language was applied last. */
    private fun recover() {
        runCatching { Grants.notificationListener(allowed = true) }
        relaunch()
        waitForHome()
    }

    private companion object {
        const val AYU = "Ayu Lestari"
        const val MESSAGE_LIMIT = 700
    }
}
