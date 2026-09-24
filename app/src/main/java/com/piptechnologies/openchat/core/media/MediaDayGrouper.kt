package com.piptechnologies.openchat.core.media

import com.piptechnologies.openchat.core.phone.RelativeTime
import com.piptechnologies.openchat.core.phone.TimeWords
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** Groups recovered media into calendar-day sections, newest day and newest item first. */
object MediaDayGrouper {
    data class Group(val label: String, val items: List<RecoveredMedia>)

    /**
     * Groups by calendar day of [RecoveredMedia.originalModifiedAt], newest first, labels from
     * RelativeTime.dayLabel with [words], [locale] and [weekdayDayMonthPattern] (English by default);
     * item order within a group newest first.
     */
    fun group(
        items: List<RecoveredMedia>,
        nowMs: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale.US,
        words: TimeWords = TimeWords.ENGLISH,
        weekdayDayMonthPattern: String = RelativeTime.WEEKDAY_DAY_MONTH_PATTERN,
    ): List<Group> {
        val newestFirst = items.sortedByDescending { it.originalModifiedAt }
        val byDay = LinkedHashMap<Long, MutableList<RecoveredMedia>>()
        for (item in newestFirst) {
            val dayKey = startOfDay(item.originalModifiedAt, timeZone)
            byDay.getOrPut(dayKey) { mutableListOf() }.add(item)
        }
        return byDay.entries
            .sortedByDescending { it.key }
            .map { (_, dayItems) ->
                val label = RelativeTime.dayLabel(
                    dayItems.first().originalModifiedAt, nowMs, timeZone, locale, words, weekdayDayMonthPattern,
                )
                Group(label, dayItems)
            }
    }

    private fun startOfDay(timestampMs: Long, timeZone: TimeZone): Long {
        val calendar = Calendar.getInstance(timeZone)
        calendar.timeInMillis = timestampMs
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
