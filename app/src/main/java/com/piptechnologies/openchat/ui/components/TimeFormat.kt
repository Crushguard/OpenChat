package com.piptechnologies.openchat.ui.components

import android.content.res.Resources
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.core.phone.RelativeTime
import com.piptechnologies.openchat.core.phone.TimeWords
import java.util.Locale
import java.util.TimeZone

/**
 * The time labels of the UI in one language: [RelativeTime] with [words] from string resources and the
 * weekday and month names and digits of [locale]. Build it with [rememberTimeFormatter].
 *
 * English, in any region, keeps the design's labels exactly: "22 Sep" and "Mon 22 Sep"
 * ([RelativeTime.DAY_MONTH_PATTERN], [RelativeTime.WEEKDAY_DAY_MONTH_PATTERN]) in US English names. The
 * locale's own best pattern would change them: getBestDateTimePattern(en, "dMMM") is the US order "MMM d"
 * ("Sep 22"), and en-GB abbreviates September "Sept". Every other language uses its best pattern for the
 * skeletons "dMMM" and "EEEdMMM" (fr "22 sept.", de "22. Sept.", zh "9月22日"). The clock stays 24 h
 * ("14:26", the design's) in every language.
 */
@Stable
class TimeFormatter(
    val words: TimeWords,
    locale: Locale,
    val timeZone: TimeZone,
) {
    /** The locale the labels are formatted in: the given one, except that every English is the design's [Locale.US]. */
    val locale: Locale = if (locale.language == ENGLISH) Locale.US else locale

    /** Day and month, "22 Sep" in English; for [RelativeTime.label] and [RelativeTime.conversationTime]. */
    val dayMonthPattern: String = bestPattern(this.locale, "dMMM", RelativeTime.DAY_MONTH_PATTERN)

    /** Weekday, day and month, "Mon 22 Sep" in English; for [RelativeTime.dayLabel] and MediaDayGrouper.group. */
    val weekdayDayMonthPattern: String = bestPattern(this.locale, "EEEdMMM", RelativeTime.WEEKDAY_DAY_MONTH_PATTERN)

    /** Recent rows: "Now", "5m", "2h", "Yesterday", "Mon", "22 Sep" (English). */
    fun label(timestampMs: Long, nowMs: Long): String =
        RelativeTime.label(timestampMs, nowMs, timeZone, locale, words, dayMonthPattern)

    /** Inbox rows: "14:26", "Yesterday", "Mon", "22 Sep" (English). */
    fun conversationTime(timestampMs: Long, nowMs: Long): String =
        RelativeTime.conversationTime(timestampMs, nowMs, timeZone, locale, words, dayMonthPattern)

    /** Day headers: "Today", "Yesterday", "Mon 22 Sep" (English). */
    fun dayLabel(timestampMs: Long, nowMs: Long): String =
        RelativeTime.dayLabel(timestampMs, nowMs, timeZone, locale, words, weekdayDayMonthPattern)

    /** "14:26" (24 h in every language, in the locale's digits). */
    fun clock(timestampMs: Long): String = RelativeTime.clock(timestampMs, timeZone, locale)
}

/**
 * A [TimeFormatter] for the current UI language: words from the time_* string resources, locale
 * LocalConfiguration.current.locales[0]. Remembered per configuration (so a language change rebuilds it)
 * and [timeZone].
 */
@Composable
fun rememberTimeFormatter(timeZone: TimeZone = TimeZone.getDefault()): TimeFormatter {
    val configuration = LocalConfiguration.current
    val resources = LocalContext.current.resources
    return remember(configuration, timeZone) {
        // An empty locale list (possible in tools that render without a locale) falls back to the JVM default.
        val locale = configuration.locales[0]?.takeIf { it.language.isNotEmpty() } ?: Locale.getDefault()
        TimeFormatter(words = timeWords(resources, locale), locale = locale, timeZone = timeZone)
    }
}

private fun timeWords(resources: Resources, locale: Locale): TimeWords {
    val minutes = resources.getString(R.string.time_minutes_short)
    val hours = resources.getString(R.string.time_hours_short)
    return TimeWords(
        now = resources.getString(R.string.time_now),
        today = resources.getString(R.string.time_today),
        yesterday = resources.getString(R.string.time_yesterday),
        minutesAgo = { String.format(locale, minutes, it) },
        hoursAgo = { String.format(locale, hours, it) },
    )
}

private const val ENGLISH = "en"

/** The design's [english] pattern for English; otherwise [locale]'s best pattern for [skeleton]. */
private fun bestPattern(locale: Locale, skeleton: String, english: String): String =
    if (locale.language == ENGLISH) english else DateFormat.getBestDateTimePattern(locale, skeleton)?.let(::javaDatePattern) ?: english

/**
 * [icuPattern] for java.text.SimpleDateFormat, which RelativeTime formats with (and which Paparazzi runs
 * on the JVM): ICU's stand-alone weekday letters c and e (Russian's "ccc, d MMM") become E; quoted
 * literals ("d 'de' MMM") are kept.
 */
private fun javaDatePattern(icuPattern: String): String {
    val pattern = StringBuilder(icuPattern.length)
    var quoted = false
    for (char in icuPattern) {
        if (char == '\'') quoted = !quoted
        pattern.append(if (!quoted && (char == 'c' || char == 'e')) 'E' else char)
    }
    return pattern.toString()
}
