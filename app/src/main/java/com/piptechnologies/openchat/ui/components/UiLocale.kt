package com.piptechnologies.openchat.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import com.piptechnologies.openchat.ui.settings.Languages
import java.util.Locale

/**
 * The language the UI is shown in, for formatting (dates, digits, decimal separators) and case rules: the
 * first configuration locale whose language the app ships (Android picks the same language's strings), or
 * US English when the app falls back to English (a Dutch or Traditional-Chinese phone without an in-app
 * choice sees English text, so dates and numbers are English too). Keeps the region of a supported locale
 * (ar-EG, pt-BR). The configuration follows the per-app language on every API level, unlike the
 * application context on API 24-32. Never null; reading it recomposes the caller on a language change.
 */
@Composable
@ReadOnlyComposable
fun currentUiLocale(): Locale {
    val locales = LocalConfiguration.current.locales
    val requested = (0 until locales.size()).mapNotNull { locales[it] }.filter { it.language.isNotEmpty() }
    return uiLocaleFor(requested)
}

/**
 * [currentUiLocale] for an explicit locale list, in order of preference: the first requested locale of the
 * matched language (an unsupported locale never counts, even though matching it alone falls back to English),
 * else that language itself, with a bare "pt" read as pt-PT because values-pt holds European Portuguese
 * (CLDR's bare "pt" is Brazilian: "Irã", not "Irão").
 */
fun uiLocaleFor(requested: List<Locale>): Locale {
    val option = Languages.match(requested)
    val english = option.tag == ENGLISH
    val chosen = requested.firstOrNull { locale ->
        Languages.match(listOf(locale)) == option && (!english || locale.language == ENGLISH)
    } ?: return if (english) Locale.US else portugalForBarePortuguese(Locale.forLanguageTag(option.tag))
    return portugalForBarePortuguese(chosen)
}

private fun portugalForBarePortuguese(locale: Locale): Locale =
    if (locale.language == "pt" && locale.country.isEmpty()) Locale.forLanguageTag("pt-PT") else locale

private const val ENGLISH = "en"
