package com.piptechnologies.openchat.core.messages

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun message(id: Long, text: String, ts: Long, deletedAt: Long? = null, seen: Boolean = false, key: String = "com.whatsapp|ayu", title: String = "Ayu") =
    CapturedMessage(id, "com.whatsapp", key, title, null, text, NotificationText.kindOf(text), ts, seen, deletedAt, null)

/** Cases beyond the brief's InboxBuilderTest: deleted-mode time and order, row fields, ties and empty input. */
class InboxBuilderEdgeCasesTest {
    @Test fun `deleted mode uses the deleted message time and sorts by it`() {
        val messages = listOf(
            message(1, "a1", 1000, deletedAt = 1100, key = "k|a", title = "A"),
            message(2, "a2", 9000, key = "k|a", title = "A"),
            message(3, "b1", 5000, deletedAt = 5100, key = "k|b", title = "B"),
        )
        val deleted = InboxBuilder.build(messages, InboxMode.DELETED)
        assertEquals(listOf("B", "A"), deleted.map { it.title })
        assertEquals(listOf(5000L, 1000L), deleted.map { it.lastTimestamp })
        val all = InboxBuilder.build(messages, InboxMode.ALL)
        assertEquals(listOf("A", "B"), all.map { it.title })
        assertEquals(listOf(9000L, 5000L), all.map { it.lastTimestamp })
        assertTrue(all[1].previewDeleted)
    }
    @Test fun `row fields come from the latest title and the whole conversation`() {
        val key = "com.whatsapp|+62 813"
        val row = InboxBuilder.build(
            listOf(
                message(1, "hi", 1000, key = key, title = "+62 813-9922-0417"),
                message(2, "yo", 2000, seen = true, key = key, title = "+62 813-9922-0417 "),
            ),
            InboxMode.ALL,
        ).single()
        assertEquals("+62 813-9922-0417 ", row.title)
        assertEquals("6281399220417", row.phoneNumber)
        assertEquals("#", row.initial)
        assertEquals("yo", row.preview)
        assertEquals(1, row.unreadCount)
        assertEquals(0, row.deletedCount)
        assertEquals(Math.floorMod(key.hashCode(), 5), row.colorIndex)
    }
    @Test fun `empty input no deletions and a negative hash`() {
        assertTrue(InboxBuilder.build(emptyList(), InboxMode.ALL).isEmpty())
        assertTrue(InboxBuilder.build(listOf(message(1, "x", 1)), InboxMode.DELETED).isEmpty())
        val negative = "polygenelubricants" // hashCode() == Int.MIN_VALUE
        assertEquals(Int.MIN_VALUE, negative.hashCode())
        assertEquals(2, InboxBuilder.build(listOf(message(1, "x", 1, key = negative)), InboxMode.ALL).single().colorIndex)
    }
    @Test fun `ties sort by key`() {
        val rows = InboxBuilder.build(listOf(message(1, "x", 1000, key = "k|b", title = "B"), message(2, "y", 1000, key = "k|a", title = "A")), InboxMode.ALL)
        assertEquals(listOf("A", "B"), rows.map { it.title })
    }
}
