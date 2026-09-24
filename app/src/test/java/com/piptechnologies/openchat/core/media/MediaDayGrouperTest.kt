package com.piptechnologies.openchat.core.media

import com.piptechnologies.openchat.core.phone.TimeWords
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaDayGrouperTest {
    private fun m(id: Long, at: Long) = RecoveredMedia(id, "/l/$id.jpg", "$id.jpg", "image/jpeg", MediaCategory.PHOTO, 1, at, at, at + 60_000, null)
    @Test fun `groups newest first with day labels`() {
        val now = 1_790_000_000_000L; val day = 86_400_000L
        val g = MediaDayGrouper.group(listOf(m(1, now - 3600_000), m(2, now - day), m(3, now - 1800_000)), now, java.util.TimeZone.getTimeZone("UTC"))
        assertEquals(listOf("Today", "Yesterday"), g.map { it.label }); assertEquals(listOf(3L, 1L), g[0].items.map { it.id })
    }
    @Test fun `labels groups in the given words and locale`() {
        val now = 1_790_000_000_000L; val day = 86_400_000L // Monday 21 Sep 2026
        val french = TimeWords("À l’instant", "Aujourd’hui", "Hier", { "$it min" }, { "$it h" })
        val g = MediaDayGrouper.group(
            listOf(m(1, now - 3600_000), m(2, now - day), m(3, now - 3 * day)), now, java.util.TimeZone.getTimeZone("UTC"), Locale.FRENCH, french,
        )
        assertEquals(listOf("Aujourd’hui", "Hier", "ven. 18 sept."), g.map { it.label })
    }
}
