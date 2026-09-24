package com.piptechnologies.openchat.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import com.piptechnologies.openchat.core.phone.DialCountry
import java.util.Formattable
import java.util.FormattableFlags
import java.util.Formatter
import java.util.Locale

/** The UI language: LocalConfiguration.current.locales[0], else the JVM default (as in rememberTimeFormatter). */
@Composable
@ReadOnlyComposable
internal fun uiLocale(): Locale =
    LocalConfiguration.current.locales[0]?.takeIf { it.language.isNotEmpty() } ?: Locale.getDefault()

/** True when [locale] shows the dataset's own country names and order: English, in any region (the design's list). */
internal fun usesDatasetNames(locale: Locale): Boolean = locale.language.isEmpty() || locale.language == ENGLISH

/**
 * [country]'s name in [locale] (design map §4.4). English keeps the dataset's name ([DialCountry.name], the design's list:
 * "Bosnia and Herzegovina", "DR Congo", "Myanmar", where the platform says "Bosnia & Herzegovina", "Congo - Kinshasa",
 * "Myanmar (Burma)"). Every other language takes the platform's name for the ISO code (java.util.Locale, ICU's CLDR names
 * on Android), and falls back to the English name when the platform has none (it returns the code itself, or nothing).
 */
internal fun countryName(country: DialCountry, locale: Locale): String {
    if (usesDatasetNames(locale)) return country.name
    val name = Locale("", country.iso2).getDisplayCountry(locale)
    return if (name.isBlank() || name.equals(country.iso2, ignoreCase = true)) country.name else name
}

/**
 * [country] as a format argument of a UiText, such as "Pasted · country set to %s": it formats as its [countryName] in the
 * language the text is resolved in. Resources formats with its configuration's locale, which the Formatter hands to
 * [formatTo], so the ViewModel can put a country into a toast without knowing the UI language.
 */
internal data class CountryNameArg(val country: DialCountry) : Formattable {
    override fun formatTo(formatter: Formatter, flags: Int, width: Int, precision: Int) {
        // The name, as %s with the same flags, width and precision would format it.
        val spec = buildString {
            append('%')
            if (flags and FormattableFlags.LEFT_JUSTIFY != 0) append('-')
            if (width >= 0) append(width)
            if (precision >= 0) append('.').append(precision)
            append(if (flags and FormattableFlags.UPPERCASE != 0) 'S' else 's')
        }
        formatter.format(spec, countryName(country, formatter.locale() ?: Locale.getDefault()))
    }

    /** The English name, for logs; formatting goes through [formatTo]. */
    override fun toString(): String = country.name
}

private const val ENGLISH = "en"
