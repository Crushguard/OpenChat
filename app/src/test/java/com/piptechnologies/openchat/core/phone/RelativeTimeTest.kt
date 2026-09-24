package com.piptechnologies.openchat.core.phone

import org.junit.Assert.assertEquals
import org.junit.Test
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
}
