package com.piptechnologies.openchat.ui.settings

import android.content.Context
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
     * Hebrew), matched like [match]: exact, else by language ("pt-PT" gives Português, "zh-TW" 中文),
     * else English.
     */
    fun byTag(tag: String): LanguageOption = match(listOf(Locale.forLanguageTag(tag.trim().replace('_', '-'))))

    /**
     * The language to show for [requested], a locale list in order of preference (AppCompat's application
     * locales, or the device's): the first locale that matches one of [all] by exact tag (with the
     * in ↔ id and iw ↔ he aliases, so pt-BR gives pt-BR), else by language alone (pt-PT and pt-AO give pt,
     * zh-TW gives zh, en-GB gives en); English when none does.
     */
    fun match(requested: List<Locale>): LanguageOption {
        for (locale in requested) {
            val language = canonicalLanguage(locale.language)
            if (language.isEmpty()) continue
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
     * [option]'s name in the UI language [inLocale], from java.util.Locale ("Allemand" for German in
     * French), capitalised for [inLocale] since it stands alone as a label. zh is named as zh-Hans
     * ("Chinese (Simplified)"), the script values-zh holds. Falls back to [LanguageOption.english] when
     * Locale has no name for it and returns the code itself.
     */
    fun displayName(option: LanguageOption, inLocale: Locale): String {
        val locale = Locale.forLanguageTag(if (option.tag == "zh") "zh-Hans" else option.tag)
        val languageName = locale.getDisplayLanguage(inLocale)
        val untranslated = languageName.isBlank() ||
            languageName.equals(locale.language, ignoreCase = true) ||
            languageName.equals(option.tag, ignoreCase = true)
        if (untranslated) return option.english
        return locale.getDisplayName(inLocale).replaceFirstChar { if (it.isLowerCase()) it.titlecase(inLocale) else it.toString() }
    }

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
