package com.piptechnologies.openchat.ui.settings

import android.content.Context
import android.content.res.Configuration
import android.icu.text.DisplayContext
import android.icu.text.LocaleDisplayNames
import android.icu.util.ULocale
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.LocaleManagerCompat
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * One UI language (plan 2026-09-24): the BCP-47 [tag] handed to AppCompat and listed in
 * res/xml/locales_config.xml, the [native] name the Language screen shows large, the [english] name,
 * whether it is written right to left, and the resource [qualifier] of its strings (the folder
 * values-<qualifier>, as listed in resourceConfigurations in app/build.gradle.kts; English is the
 * default values/ folder).
 */
data class LanguageOption(
    val tag: String,
    val native: String,
    val english: String,
    val rtl: Boolean,
    val qualifier: String,
)

/**
 * The 19 UI languages, in the order of the Language screen: the union of RecoverMe's and Status Saver's
 * sets. Indonesian and Hebrew use the legacy folders values-in / values-iw with the BCP-47 tags id / he;
 * "pt" is European Portuguese, "pt-BR" Brazilian; "zh" is Simplified Chinese. Keep in step with
 * res/xml/locales_config.xml and resourceConfigurations in app/build.gradle.kts: LocalesConfigTest fails
 * if they drift.
 *
 * AppCompat's application locales are the source of truth for the chosen language ([current]); nothing
 * else stores it.
 */
object Languages {
    val all: List<LanguageOption> = listOf(
        LanguageOption(tag = "en", native = "English", english = "English", rtl = false, qualifier = "en"),
        LanguageOption(tag = "id", native = "Bahasa Indonesia", english = "Indonesian", rtl = false, qualifier = "in"),
        LanguageOption(tag = "pt-BR", native = "Português (Brasil)", english = "Portuguese (Brazil)", rtl = false, qualifier = "pt-rBR"),
        LanguageOption(tag = "pt", native = "Português", english = "Portuguese", rtl = false, qualifier = "pt"),
        LanguageOption(tag = "ur", native = "اردو", english = "Urdu", rtl = true, qualifier = "ur"),
        LanguageOption(tag = "hi", native = "हिन्दी", english = "Hindi", rtl = false, qualifier = "hi"),
        LanguageOption(tag = "tr", native = "Türkçe", english = "Turkish", rtl = false, qualifier = "tr"),
        LanguageOption(tag = "es", native = "Español", english = "Spanish", rtl = false, qualifier = "es"),
        LanguageOption(tag = "ar", native = "العربية", english = "Arabic", rtl = true, qualifier = "ar"),
        LanguageOption(tag = "fa", native = "فارسی", english = "Persian", rtl = true, qualifier = "fa"),
        LanguageOption(tag = "ps", native = "پښتو", english = "Pashto", rtl = true, qualifier = "ps"),
        LanguageOption(tag = "he", native = "עברית", english = "Hebrew", rtl = true, qualifier = "iw"),
        LanguageOption(tag = "fr", native = "Français", english = "French", rtl = false, qualifier = "fr"),
        LanguageOption(tag = "de", native = "Deutsch", english = "German", rtl = false, qualifier = "de"),
        LanguageOption(tag = "it", native = "Italiano", english = "Italian", rtl = false, qualifier = "it"),
        LanguageOption(tag = "ru", native = "Русский", english = "Russian", rtl = false, qualifier = "ru"),
        LanguageOption(tag = "zh", native = "中文", english = "Chinese (Simplified)", rtl = false, qualifier = "zh"),
        LanguageOption(tag = "ha", native = "Hausa", english = "Hausa", rtl = false, qualifier = "ha"),
        LanguageOption(tag = "my", native = "မြန်မာ", english = "Burmese", rtl = false, qualifier = "my"),
    )

    /**
     * The option for [tag] ("pt-BR", "pt_BR", "PT-br"; the legacy "in" and "iw" give Indonesian and
     * Hebrew), matched like [match]: exact, else by language ("pt-PT" gives Português), else English
     * ("zh-TW" too: Traditional Chinese is not 中文 here).
     */
    fun byTag(tag: String): LanguageOption = match(listOf(Locale.forLanguageTag(tag.trim().replace('_', '-'))))

    /**
     * The language to show for [requested], a locale list in order of preference (AppCompat's application
     * locales, or the device's): the first locale that matches one of [all] by exact tag (with the
     * in ↔ id and iw ↔ he aliases, so pt-BR gives pt-BR), else by language alone (pt-PT and pt-AO give pt,
     * es-MX gives es, en-GB gives en); English when none does. Traditional Chinese (zh-TW, zh-HK,
     * zh-Hant-…) matches nothing, as Android serves it no values-zh; see [isTraditionalChinese].
     */
    fun match(requested: List<Locale>): LanguageOption {
        for (locale in requested) {
            val language = canonicalLanguage(locale.language)
            if (language.isEmpty() || locale.isTraditionalChinese()) continue
            val region = locale.country.uppercase(Locale.ROOT)
            val sameLanguage = all.filter { it.language() == language }
            val option = sameLanguage.firstOrNull { it.region() == region }
                ?: sameLanguage.firstOrNull { it.region().isEmpty() }
                ?: sameLanguage.firstOrNull()
            if (option != null) return option
        }
        return all.first()
    }

    /**
     * The language the app shows: the per-app choice (AppCompatDelegate.getApplicationLocales()) when
     * there is one, else the best [match] of the device locales, else English. Call it after
     * MainActivity.onCreate, as AppCompat requires: it answers through its live activities.
     */
    fun current(context: Context): LanguageOption {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        val locales = if (appLocales.isEmpty()) LocaleManagerCompat.getSystemLocales(context) else appLocales
        return match(locales.toLocales())
    }

    /**
     * The locale the UI is shown in under [configuration]: the [match] of its locales, which is the language
     * whose strings Android picks (English when none matches), for naming languages with [displayName].
     * Portuguese is pt-PT: values-pt holds European Portuguese, while a bare "pt" names things in Brazilian
     * Portuguese.
     */
    fun uiLocale(configuration: Configuration): Locale {
        val locales = configuration.locales
        val option = match((0 until locales.size()).mapNotNull { locales[it] })
        return Locale.forLanguageTag(if (option.tag == "pt") "pt-PT" else option.tag)
    }

    /**
     * [option]'s name in the UI language [inLocale] as a list item gives it ("Allemand" for German in
     * French): ICU's LocaleDisplayNames, capitalised by [inLocale]'s rules for UI lists. zh is named as
     * zh-Hans, the script values-zh holds: "Chinese (Simplified)". Where android.icu cannot be used
     * (plain JVM unit tests), java.util.Locale names it instead, zh by its language alone, since on
     * Android that path writes ICU's stand-alone script name ("Chinese (Simplified Han)"). Falls back
     * to [LanguageOption.english] when neither has a name for the language in [inLocale].
     */
    fun displayName(option: LanguageOption, inLocale: Locale): String {
        val tag = if (option.tag == "zh") "zh-Hans" else option.tag
        return icuDisplayName(tag, inLocale) ?: localeDisplayName(tag, inLocale) ?: option.english
    }

    /** ICU's UI-list name of [tag] in [inLocale]; null when it has none (it would return the code) or android.icu is not there. */
    private fun icuDisplayName(tag: String, inLocale: Locale): String? =
        try {
            val names: LocaleDisplayNames? =
                LocaleDisplayNames.getInstance(ULocale.forLocale(inLocale), DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU)
            val locale = ULocale.forLanguageTag(tag)
            val languageName: String? = names?.languageDisplayName(locale.language)
            if (names == null || languageName.isNullOrBlank() || languageName.equals(locale.language, ignoreCase = true)) {
                null
            } else {
                names.localeDisplayName(locale)
            }
        } catch (e: RuntimeException) {
            null // android.jar's stubs, in unit tests that run without layoutlib.
        } catch (e: LinkageError) {
            null // No android.icu at all.
        }

    /** java.util.Locale's name of [tag] in [inLocale], first letter capitalised, a script left out; null when it has none. */
    private fun localeDisplayName(tag: String, inLocale: Locale): String? {
        val locale = Locale.forLanguageTag(tag)
        val languageName = locale.getDisplayLanguage(inLocale)
        if (languageName.isBlank() || languageName.equals(locale.language, ignoreCase = true)) return null
        val name = if (locale.script.isEmpty()) locale.getDisplayName(inLocale) else languageName
        return name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(inLocale) else it.toString() }
    }

    /**
     * Chinese written in Traditional characters: the Hant script, or Taiwan, Hong Kong or Macau with no
     * script given (their likely script is Hant). values-zh holds Simplified Chinese, and since Android 7.0
     * resource matching compares scripts (a folder without one takes its language's likely script, Hans for
     * zh), so such a locale gets the default English strings, not values-zh.
     */
    private fun Locale.isTraditionalChinese(): Boolean {
        if (canonicalLanguage(language) != "zh") return false
        return if (script.isEmpty()) country.uppercase(Locale.ROOT) in TRADITIONAL_CHINESE_REGIONS else script.equals("Hant", ignoreCase = true)
    }

    /**
     * Regions whose script-less Chinese CLDR's likely subtags complete to Traditional (zh-US → zh-Hant-US), for which
     * Android therefore serves no values-zh (Simplified).
     */
    private val TRADITIONAL_CHINESE_REGIONS = setOf("AU", "BN", "GB", "GF", "HK", "ID", "MO", "PA", "PF", "PH", "SR", "TH", "TW", "US", "VN")

    /** Android (and Java before 17) reports Indonesian and Hebrew as the legacy "in" and "iw"; compare on "id" and "he". */
    private fun canonicalLanguage(language: String): String =
        when (val lower = language.lowercase(Locale.ROOT)) {
            "in" -> "id"
            "iw" -> "he"
            else -> lower
        }

    private fun LanguageOption.language(): String = canonicalLanguage(tag.substringBefore('-'))

    private fun LanguageOption.region(): String = tag.substringAfter('-', missingDelimiterValue = "").uppercase(Locale.ROOT)

    private fun LocaleListCompat.toLocales(): List<Locale> = (0 until size()).mapNotNull { get(it) }
}
