package com.piptechnologies.openchat.core.messages

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun msg(id: Long, text: String, ts: Long, deletedAt: Long? = null, seen: Boolean = false, key: String = "com.whatsapp|ayu lestari", title: String = "Ayu Lestari") =
    CapturedMessage(id, "com.whatsapp", key, title, null, text, NotificationText.kindOf(text), ts, seen, deletedAt, null)

class DeletedMessageDetectorTest {
    @Test fun `timestamped deleted line marks the stored message`() {
        val stored = listOf(msg(1, "Hi kak, is the blue one still available?", 1000), msg(2, "I can do 300k if you ship today", 5000))
        val incoming = listOf(NotificationLine("Hi kak, is the blue one still available?", 1000, null), NotificationLine("This message was deleted", 5000, null))
        assertEquals(listOf(2L), DeletedMessageDetector.detect(stored, incoming))
    }
    @Test fun `untimestamped deleted line matches by position from the end`() {
        val stored = listOf(msg(1, "first", 1000), msg(2, "second", 2000))
        val incoming = listOf(NotificationLine("first", null, null), NotificationLine("🚫 This message was deleted", null, null))
        assertEquals(listOf(2L), DeletedMessageDetector.detect(stored, incoming))
    }
    @Test fun `unchanged notification produces no deletions and no new lines`() {
        val stored = listOf(msg(1, "first", 1000), msg(2, "second", 2000))
        val incoming = listOf(NotificationLine("first", 1000, null), NotificationLine("second", 2001, null))
        assertTrue(DeletedMessageDetector.detect(stored, incoming).isEmpty())
        assertTrue(DeletedMessageDetector.newLines(stored, incoming).isEmpty())
    }
    @Test fun `new lines exclude deleted pattern and known texts`() {
        val stored = listOf(msg(1, "first", 1000))
        val incoming = listOf(NotificationLine("first", 1000, null), NotificationLine("third", 3000, null), NotificationLine("This message was deleted", 4000, null))
        assertEquals(listOf("third"), DeletedMessageDetector.newLines(stored, incoming).map { it.text })
    }
    @Test fun `already deleted messages are not matched twice`() {
        val stored = listOf(msg(2, "second", 2000, deletedAt = 2500))
        assertTrue(DeletedMessageDetector.detect(stored, listOf(NotificationLine("This message was deleted", 2000, null))).isEmpty())
    }
}
