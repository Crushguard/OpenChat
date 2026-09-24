package com.piptechnologies.openchat.core.messages

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun msg(id: Long, text: String, ts: Long, deletedAt: Long? = null, seen: Boolean = false, key: String = "com.whatsapp|ayu lestari", title: String = "Ayu Lestari") =
    CapturedMessage(id, "com.whatsapp", key, title, null, text, NotificationText.kindOf(text), ts, seen, deletedAt, null)

class InboxBuilderTest {
    private val all = listOf(
        msg(1, "Hi kak, is the blue one still available?", 1000),
        msg(2, "I can do 300k if you ship today", 2000, deletedAt = 2500),
        msg(3, "Sorry, wrong chat", 3000),
        msg(4, "Order is on the way", 1500, seen = true, key = "com.whatsapp|budi", title = "Budi"),
    )
    @Test fun `all mode lists every conversation newest first with unread counts`() {
        val rows = InboxBuilder.build(all, InboxMode.ALL)
        assertEquals(listOf("Ayu Lestari", "Budi"), rows.map { it.title })
        assertEquals("Sorry, wrong chat", rows[0].preview); assertEquals(3, rows[0].unreadCount); assertEquals(1, rows[0].deletedCount); assertFalse(rows[0].previewDeleted)
        assertEquals(0, rows[1].unreadCount)
    }
    @Test fun `deleted mode keeps only conversations with deletions and previews the deleted text`() {
        val rows = InboxBuilder.build(all, InboxMode.DELETED)
        assertEquals(1, rows.size); assertEquals("I can do 300k if you ship today", rows[0].preview); assertTrue(rows[0].previewDeleted); assertEquals(1, rows[0].deletedCount)
    }
}
