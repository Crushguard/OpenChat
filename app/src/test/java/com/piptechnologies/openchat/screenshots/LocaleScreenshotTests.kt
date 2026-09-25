package com.piptechnologies.openchat.screenshots

import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import app.cash.paparazzi.Paparazzi
import com.piptechnologies.openchat.ui.settings.LanguageOption
import com.piptechnologies.openchat.ui.settings.Languages
import com.piptechnologies.openchat.ui.theme.OpenChatTheme
import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Every scene of [Scenes.all] in each of the 18 translations (plan 2026-09-24): the English screenshots' composables
 * and fake data, with the strings of values-<qualifier>, laid out right to left for Urdu, Arabic, Persian, Pashto and
 * Hebrew. Named after the language's BCP-47 tag, the files are
 * `<package>_LocaleScreenshotTests_everyScene[<tag>]_<scene>.png`; CI collects them as
 * screenshots/locales/<tag>/<scene>.png, next to the English screenshots/<scene>.png.
 *
 * 612 renders: they run under recordPaparazziDebug (CI's screenshots job) and verifyPaparazziDebug. A plain
 * testDebugUnitTest skips them and renders only the 34 English screenshots.
 */
@RunWith(Parameterized::class)
class LocaleScreenshotTests(private val tag: String, qualifier: String, rtl: Boolean) {
    @get:Rule
    val paparazzi: Paparazzi = ScreenshotDevice.paparazzi(qualifier, rtl)

    private val defaultZone: TimeZone = TimeZone.getDefault()
    private val defaultLocale: Locale = Locale.getDefault()

    /**
     * As in the English tests: the screens format times in the default zone; the fake wall-clock times are UTC.
     * The default locale is the language's, as on a phone once the app shows that language; layoutlib formats
     * `getString(id, args)` with it (a phone uses the configuration's), so "%d" gets that language's digits.
     */
    @Before
    fun useFakeTimeZoneAndLanguage() {
        TimeZone.setDefault(Fakes.timeZone)
        Locale.setDefault(Locale.forLanguageTag(Languages.all.first { it.tag == tag }.localeTag))
    }

    @After
    fun restoreTimeZoneAndLanguage() {
        TimeZone.setDefault(defaultZone)
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun everyScene() {
        val language = Languages.all.first { it.tag == tag }
        for (scene in Scenes.all) {
            paparazzi.snapshot(name = scene.name) {
                InLanguage(language) {
                    OpenChatTheme {
                        scene.content()
                    }
                }
            }
        }
    }

    companion object {
        /**
         * (BCP-47 tag, device locale qualifier, right to left) of every language but English; the tag names the files.
         * The device locale is the one Android applies ([LanguageOption.localeTag]): European Portuguese renders as
         * pt-rPT, so its plurals follow Portugal's rules, and its strings still resolve from values-pt.
         */
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun languages(): List<Array<Any>> =
            Languages.all.filter { it.tag != "en" }.map { arrayOf(it.tag, deviceQualifier(it), it.rtl) }

        /** The resource qualifier of [option]'s [LanguageOption.localeTag]: "pt-rPT" for pt-PT, else its folder's. */
        private fun deviceQualifier(option: LanguageOption): String {
            val locale = Locale.forLanguageTag(option.localeTag)
            return if (option.localeTag == option.tag || locale.country.isEmpty()) option.qualifier else "${option.qualifier}-r${locale.country}"
        }

        @JvmStatic
        @BeforeClass
        fun onlyWhenRecordingOrVerifying() {
            assumeTrue(
                "the locale matrix renders under recordPaparazziDebug or verifyPaparazziDebug",
                System.getProperty("paparazzi.test.record") == "true" || System.getProperty("paparazzi.test.verify") == "true",
            )
        }
    }
}

/**
 * [content] as the app shows it in [language]: the language in effect ([LocalSceneLanguage]), and a configuration
 * whose locale is the language's BCP-47 tag, as on a device, which the app reads for dates, digits, case and country
 * and language names. layoutlib may build Paparazzi's configuration locale with `new Locale(tag)`, which reads
 * "pt-BR" as an unknown language "pt-br" and so formats in English. Strings are unaffected: they resolve from
 * values-<qualifier> through the context's resources.
 */
@Composable
private fun InLanguage(language: LanguageOption, content: @Composable () -> Unit) {
    val base = LocalConfiguration.current
    val configuration = remember(base, language) {
        Configuration(base).apply { setLocales(LocaleList(Locale.forLanguageTag(language.localeTag))) }
    }
    CompositionLocalProvider(
        LocalConfiguration provides configuration,
        LocalSceneLanguage provides language,
        content = content,
    )
}
