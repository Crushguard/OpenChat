package com.piptechnologies.openchat.core.phone

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Formats timestamps into the short labels the recents row, chat header and media groups use (§5.1). */
object RelativeTime {

    private const val MINUTE_MS = 60_000L
    private const val HOUR_MS = 3_600_000L

    /** Recent rows: "Now" (<60 s), "5m", "2h" (same calendar day), "Yesterday", weekday "Mon" (<7 days), else "22 Sep". */
    fun label(
        timestampMs: Long,
        nowMs: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale.US,
    ): String {
        val dayDiff = dayDifference(timestampMs, nowMs, timeZone)
        if (dayDiff == 0) {
            val elapsed = nowMs - timestampMs
            return when {
                elapsed < MINUTE_MS -> "Now"
                elapsed < HOUR_MS -> "${elapsed / MINUTE_MS}m"
                else -> "${elapsed / HOUR_MS}h"
            }
        }
        return when (dayDiff) {
            1 -> "Yesterday"
            in 2..6 -> format(timestampMs, timeZone, locale, "EEE")
            else -> format(timestampMs, timeZone, locale, "d MMM")
        }
    }

    /** "14:26" (24 h clock). */
    fun clock(
        timestampMs: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale.US,
    ): String = format(timestampMs, timeZone, locale, "HH:mm")

    /** Inbox rows: "14:26" today, "Yesterday", "Mon" (<7 days), else "22 Sep". */
    fun conversationTime(
        timestampMs: Long,
        nowMs: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale.US,
    ): String {
        val dayDiff = dayDifference(timestampMs, nowMs, timeZone)
        return when (dayDiff) {
            0 -> clock(timestampMs, timeZone, locale)
            1 -> "Yesterday"
            in 2..6 -> format(timestampMs, timeZone, locale, "EEE")
            else -> format(timestampMs, timeZone, locale, "d MMM")
        }
    }

    /** Media day groups: "Today", "Yesterday", else "Mon 22 Sep". */
    fun dayLabel(
        timestampMs: Long,
        nowMs: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale.US,
    ): String {
        val dayDiff = dayDifference(timestampMs, nowMs, timeZone)
        return when (dayDiff) {
            0 -> "Today"
            1 -> "Yesterday"
            else -> format(timestampMs, timeZone, locale, "EEE d MMM")
        }
    }

    /** Midnight of [ms] in [timeZone], as epoch millis. */
    private fun startOfDay(ms: Long, timeZone: TimeZone): Long {
        val cal = Calendar.getInstance(timeZone)
        cal.timeInMillis = ms
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** Number of calendar days [nowMs] is after [timestampMs] in [timeZone] (0 = same day, 1 = yesterday, …). */
    private fun dayDifference(timestampMs: Long, nowMs: Long, timeZone: TimeZone): Int {
        val today = startOfDay(nowMs, timeZone)
        val that = startOfDay(timestampMs, timeZone)
        return ((today - that) / (HOUR_MS * 24)).toInt()
    }

    private fun format(ms: Long, timeZone: TimeZone, locale: Locale, pattern: String): String {
        val sdf = SimpleDateFormat(pattern, locale)
        sdf.timeZone = timeZone
        return sdf.format(Date(ms))
    }
}
