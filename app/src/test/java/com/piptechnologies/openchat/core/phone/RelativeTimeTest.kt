package com.piptechnologies.openchat.core.phone

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class RelativeTimeTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private val now = 1_790_000_000_000L // 2026-09-21T14:13:20Z (Monday)
    private val h = 3_600_000L

    @Test fun `labels`() {
        assertEquals("Now", RelativeTime.label(now - 30_000, now, utc, Locale.US))
        assertEquals("5m", RelativeTime.label(now - 5 * 60_000, now, utc, Locale.US))
        assertEquals("2h", RelativeTime.label(now - 2 * h, now, utc, Locale.US))
        assertEquals("Yesterday", RelativeTime.label(now - 24 * h, now, utc, Locale.US))
        assertEquals("Sat", RelativeTime.label(now - 2 * 24 * h, now, utc, Locale.US))
        assertEquals("1 Sep", RelativeTime.label(now - 20 * 24 * h, now, utc, Locale.US))
    }
    @Test fun `clock and conversation time`() {
        assertEquals("14:13", RelativeTime.clock(now, utc, Locale.US))
        assertEquals("12:13", RelativeTime.conversationTime(now - 2 * h, now, utc, Locale.US))
        assertEquals("Yesterday", RelativeTime.conversationTime(now - 24 * h, now, utc, Locale.US))
        assertEquals("Sat", RelativeTime.conversationTime(now - 2 * 24 * h, now, utc, Locale.US))
    }
    @Test fun `day labels`() {
        assertEquals("Today", RelativeTime.dayLabel(now - h, now, utc, Locale.US))
        assertEquals("Yesterday", RelativeTime.dayLabel(now - 24 * h, now, utc, Locale.US))
        assertEquals("Sat 19 Sep", RelativeTime.dayLabel(now - 2 * 24 * h, now, utc, Locale.US))
    }

    // Regression coverage for a DST-unsafe day-bucket computation: America/New_York springs
    // forward on 2026-03-08 (a 23 h day), so a naive "(startOfDay(now) - startOfDay(ts)) / 24h"
    // truncates to 0 instead of 1 for a message the previous night.
    @Test fun `day buckets survive a dst spring-forward transition`() {
        val nyc = TimeZone.getTimeZone("America/New_York")
        fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
            val cal = Calendar.getInstance(nyc)
            cal.clear()
            cal.set(year, month - 1, day, hour, minute, 0)
            return cal.timeInMillis
        }
        val message = at(2026, 3, 8, 23, 0) // 11pm local on the spring-forward day
        val nextMorning = at(2026, 3, 9, 8, 0)
        assertEquals("Yesterday", RelativeTime.label(message, nextMorning, nyc, Locale.US))
        assertEquals("Yesterday", RelativeTime.conversationTime(message, nextMorning, nyc, Locale.US))
        assertEquals("Yesterday", RelativeTime.dayLabel(message, nextMorning, nyc, Locale.US))

        val twoMorningsLater = at(2026, 3, 10, 8, 0)
        assertEquals("Sun", RelativeTime.label(message, twoMorningsLater, nyc, Locale.US))
    }

    // Localized labels: the words come from TimeWords, weekday and month names from the locale.
    private val french = TimeWords(
        now = "À l’instant",
        today = "Aujourd’hui",
        yesterday = "Hier",
        minutesAgo = { "$it min" },
        hoursAgo = { "$it h" },
    )

    @Test fun `french labels`() {
        assertEquals("À l’instant", RelativeTime.label(now - 30_000, now, utc, Locale.FRENCH, french))
        assertEquals("5 min", RelativeTime.label(now - 5 * 60_000, now, utc, Locale.FRENCH, french))
        assertEquals("2 h", RelativeTime.label(now - 2 * h, now, utc, Locale.FRENCH, french))
        assertEquals("Hier", RelativeTime.label(now - 24 * h, now, utc, Locale.FRENCH, french))
        assertEquals("sam.", RelativeTime.label(now - 2 * 24 * h, now, utc, Locale.FRENCH, french))
        assertEquals("lun.", RelativeTime.label(now, now + 2 * 24 * h, utc, Locale.FRENCH, french))
        assertEquals("1 sept.", RelativeTime.label(now - 20 * 24 * h, now, utc, Locale.FRENCH, french))
    }

    @Test fun `french conversation time and day labels`() {
        assertEquals("12:13", RelativeTime.conversationTime(now - 2 * h, now, utc, Locale.FRENCH, french))
        assertEquals("Hier", RelativeTime.conversationTime(now - 24 * h, now, utc, Locale.FRENCH, french))
        assertEquals("sam.", RelativeTime.conversationTime(now - 2 * 24 * h, now, utc, Locale.FRENCH, french))
        assertEquals("Aujourd’hui", RelativeTime.dayLabel(now - h, now, utc, Locale.FRENCH, french))
        assertEquals("Hier", RelativeTime.dayLabel(now - 24 * h, now, utc, Locale.FRENCH, french))
        assertEquals("sam. 19 sept.", RelativeTime.dayLabel(now - 2 * 24 * h, now, utc, Locale.FRENCH, french))
    }

    @Test fun `day-month patterns are parameters`() {
        assertEquals("1 septembre", RelativeTime.label(now - 20 * 24 * h, now, utc, Locale.FRENCH, french, dayMonthPattern = "d MMMM"))
        assertEquals("Sep 1", RelativeTime.conversationTime(now - 20 * 24 * h, now, utc, Locale.US, dayMonthPattern = "MMM d"))
        assertEquals(
            "samedi 19 septembre",
            RelativeTime.dayLabel(now - 2 * 24 * h, now, utc, Locale.FRENCH, french, weekdayDayMonthPattern = "EEEE d MMMM"),
        )
    }
}
