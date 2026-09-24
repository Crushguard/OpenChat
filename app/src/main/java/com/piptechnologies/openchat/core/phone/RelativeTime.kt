package com.piptechnologies.openchat.core.phone

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Formats timestamps into the short labels the recents row, chat header and media groups use (§5.1).
 *
 * Every function defaults to the design's English: [TimeWords.ENGLISH], [Locale.US] and the
 * [DAY_MONTH_PATTERN] / [WEEKDAY_DAY_MONTH_PATTERN] patterns. `locale` supplies the weekday and month
 * names and the digits, `words` the rest. The UI passes all of them for the app language through
 * ui/components/TimeFormat.kt.
 */
object RelativeTime {

    /** The design's day-month pattern: "22 Sep". */
    const val DAY_MONTH_PATTERN: String = "d MMM"

    /** The design's weekday-day-month pattern: "Mon 22 Sep". */
    const val WEEKDAY_DAY_MONTH_PATTERN: String = "EEE d MMM"

    private const val MINUTE_MS = 60_000L
    private const val HOUR_MS = 3_600_000L

    /**
     * Recent rows: [TimeWords.now] "Now" (<60 s), [TimeWords.minutesAgo] "5m", [TimeWords.hoursAgo] "2h"
     * (same calendar day), [TimeWords.yesterday] "Yesterday", weekday "Mon" (<7 days), else
     * [dayMonthPattern] "22 Sep".
     */
    fun label(
        timestampMs: Long,
        nowMs: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale.US,
        words: TimeWords = TimeWords.ENGLISH,
        dayMonthPattern: String = DAY_MONTH_PATTERN,
    ): String {
        val dayDiff = dayDifference(timestampMs, nowMs, timeZone)
        if (dayDiff == 0) {
            val elapsed = nowMs - timestampMs
            return when {
                elapsed < MINUTE_MS -> words.now
                elapsed < HOUR_MS -> words.minutesAgo(elapsed / MINUTE_MS)
                else -> words.hoursAgo(elapsed / HOUR_MS)
            }
        }
        return dayBucketLabel(dayDiff, timestampMs, timeZone, locale, words, dayMonthPattern)
    }

    /** "14:26" (24 h clock, the design's, in every language; [locale] only picks the digits). */
    fun clock(
        timestampMs: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale.US,
    ): String = format(timestampMs, timeZone, locale, "HH:mm")

    /** Inbox rows: "14:26" today, [TimeWords.yesterday] "Yesterday", "Mon" (<7 days), else [dayMonthPattern] "22 Sep". */
    fun conversationTime(
        timestampMs: Long,
        nowMs: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale.US,
        words: TimeWords = TimeWords.ENGLISH,
        dayMonthPattern: String = DAY_MONTH_PATTERN,
    ): String {
        val dayDiff = dayDifference(timestampMs, nowMs, timeZone)
        if (dayDiff == 0) return clock(timestampMs, timeZone, locale)
        return dayBucketLabel(dayDiff, timestampMs, timeZone, locale, words, dayMonthPattern)
    }

    /** Media day groups: [TimeWords.today] "Today", [TimeWords.yesterday] "Yesterday", else [weekdayDayMonthPattern] "Mon 22 Sep". */
    fun dayLabel(
        timestampMs: Long,
        nowMs: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale.US,
        words: TimeWords = TimeWords.ENGLISH,
        weekdayDayMonthPattern: String = WEEKDAY_DAY_MONTH_PATTERN,
    ): String {
        val dayDiff = dayDifference(timestampMs, nowMs, timeZone)
        return when (dayDiff) {
            0 -> words.today
            1 -> words.yesterday
            else -> format(timestampMs, timeZone, locale, weekdayDayMonthPattern)
        }
    }

    /**
     * Epoch-day index of the local calendar date [ts] falls on in [timeZone]. Each instant is
     * placed using its own UTC offset (DST included), so the result is the plain date part of its
     * local wall-clock reading — never a division of elapsed real time by a fixed 24 h, which is
     * wrong on the 23 h/25 h days either side of a DST transition.
     */
    private fun localDayIndex(ts: Long, timeZone: TimeZone): Long =
        Math.floorDiv(ts + timeZone.getOffset(ts), HOUR_MS * 24)

    /** Number of calendar days [nowMs] is after [timestampMs] in [timeZone] (0 = same day, 1 = yesterday, …). */
    private fun dayDifference(timestampMs: Long, nowMs: Long, timeZone: TimeZone): Int =
        (localDayIndex(nowMs, timeZone) - localDayIndex(timestampMs, timeZone)).toInt()

    /** The yesterday / weekday / day-month tail shared by [label] and [conversationTime] once same-day is ruled out. */
    private fun dayBucketLabel(
        dayDiff: Int,
        timestampMs: Long,
        timeZone: TimeZone,
        locale: Locale,
        words: TimeWords,
        dayMonthPattern: String,
    ): String =
        when (dayDiff) {
            1 -> words.yesterday
            in 2..6 -> format(timestampMs, timeZone, locale, "EEE")
            else -> format(timestampMs, timeZone, locale, dayMonthPattern)
        }

    private fun format(ms: Long, timeZone: TimeZone, locale: Locale, pattern: String): String {
        val sdf = SimpleDateFormat(pattern, locale)
        sdf.timeZone = timeZone
        return sdf.format(Date(ms))
    }
}
