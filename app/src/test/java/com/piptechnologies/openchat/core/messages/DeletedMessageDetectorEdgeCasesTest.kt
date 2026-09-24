package com.piptechnologies.openchat.core.messages

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun message(id: Long, text: String, ts: Long, deletedAt: Long? = null) =
    CapturedMessage(id, "com.whatsapp", "com.whatsapp|ayu", "Ayu", null, text, NotificationText.kindOf(text), ts, false, deletedAt, null)
private fun line(text: String, ts: Long? = null) = NotificationLine(text, ts, null)
private const val PLACEHOLDER = "This message was deleted"
private const val PHOTO = "📷 Photo"

/** Cases beyond the brief's DeletedMessageDetectorTest: bursts, re-posted placeholders, positions and ordering. */
class DeletedMessageDetectorEdgeCasesTest {
    private fun detect(stored: List<CapturedMessage>, incoming: List<NotificationLine>) = DeletedMessageDetector.detect(stored, incoming)
    private fun fresh(stored: List<CapturedMessage>, incoming: List<NotificationLine>) =
        DeletedMessageDetector.newLines(stored, incoming).map { it.text to it.timestamp }

    @Test fun `photo burst within tolerance keeps every photo`() {
        assertEquals(listOf(PHOTO to 1500L), fresh(listOf(message(1, PHOTO, 1000)), listOf(line(PHOTO, 1000), line(PHOTO, 1500))))
        assertEquals(listOf(PHOTO to 1000L), fresh(listOf(message(1, PHOTO, 1000)), listOf(line(PHOTO, 1000), line(PHOTO, 1000))))
        assertEquals(2, fresh(emptyList(), listOf(line(PHOTO, 1000), line(PHOTO, 1000))).size)
    }
    @Test fun `deleting one photo of a burst marks that photo`() {
        val burst = listOf(message(1, PHOTO, 1000), message(2, PHOTO, 1500), message(3, PHOTO, 2200))
        assertEquals(listOf(3L), detect(burst, listOf(line(PHOTO, 1000), line(PHOTO, 1500), line(PLACEHOLDER, 2200))))
        assertEquals(listOf(2L), detect(burst, listOf(line(PHOTO, 1000), line(PLACEHOLDER, 1500), line(PHOTO, 2200))))
        assertTrue(fresh(burst, listOf(line(PHOTO, 1000), line(PLACEHOLDER, 1500), line(PHOTO, 2200))).isEmpty())
        val sameSecond = listOf(message(1, PHOTO, 1000), message(2, PHOTO, 1000), message(3, PHOTO, 1000))
        assertEquals(listOf(1L), detect(sameSecond, listOf(line(PLACEHOLDER, 1000), line(PHOTO, 1000), line(PHOTO, 1000))))
        assertEquals(listOf(2L), detect(sameSecond, listOf(line(PHOTO, 1000), line(PLACEHOLDER, 1000), line(PHOTO, 1000))))
    }
    @Test fun `re-posted placeholder is absorbed by the message it already deleted`() {
        // Untimestamped single line: the re-post must not move on to the previous message.
        assertTrue(detect(listOf(message(1, "a", 1000), message(2, "b", 2000, deletedAt = 2100)), listOf(line(PLACEHOLDER))).isEmpty())
        // Timestamped: "a" is within tolerance (1.5 s) but the already deleted "b" is the closer match.
        assertTrue(detect(listOf(message(1, "a", 1000), message(2, "b", 2500, deletedAt = 2600)), listOf(line(PLACEHOLDER, 2500))).isEmpty())
        assertTrue(detect(listOf(message(1, "a", 1000), message(2, "b", 2500, deletedAt = 2600)), listOf(line("a", 1000), line(PLACEHOLDER, 2500))).isEmpty())
    }
    @Test fun `a second deletion next to an old one is still found`() {
        val stored = listOf(message(1, "a", 1000), message(2, "b", 2000, deletedAt = 2100))
        assertEquals(listOf(1L), detect(stored, listOf(line(PLACEHOLDER, 1000), line(PLACEHOLDER, 2000))))
        assertEquals(listOf(1L), detect(stored, listOf(line(PLACEHOLDER), line(PLACEHOLDER))))
        // A single line shows the latest message deleted after an older deletion: the latest is the new one.
        assertEquals(listOf(2L), detect(listOf(message(1, "a", 1000, deletedAt = 1100), message(2, "b", 2000)), listOf(line(PLACEHOLDER))))
    }
    @Test fun `placeholder after lines still shown never lands on an older message`() {
        val stored = listOf(message(1, "w", 1000), message(2, "x", 2000), message(3, "y", 3000))
        assertTrue(detect(stored, listOf(line("x"), line("y"), line(PLACEHOLDER))).isEmpty())
        assertEquals(listOf(1L), detect(stored, listOf(line(PLACEHOLDER), line("x"), line("y"))))
    }
    @Test fun `new line arriving with a deletion`() {
        val stored = listOf(message(1, "a", 1000), message(2, "b", 2000))
        val incoming = listOf(line("a"), line(PLACEHOLDER), line("f"))
        assertEquals(listOf(2L), detect(stored, incoming))
        assertEquals(listOf("f" to null), fresh(stored, incoming))
    }
    @Test fun `positions from the end with several placeholders`() {
        val stored = listOf(message(1, "a", 1000), message(2, "b", 2000), message(3, "c", 3000), message(4, "d", 4000))
        assertEquals(listOf(2L, 4L), detect(stored, listOf(line("a"), line(PLACEHOLDER), line("c"), line(PLACEHOLDER))))
        assertEquals(listOf(3L, 4L), detect(stored, listOf(line(PLACEHOLDER), line(PLACEHOLDER))))
        assertEquals(listOf(4L), detect(stored, listOf(line(PLACEHOLDER))))
        val five = stored + message(5, "e", 5000)
        assertEquals(listOf(3L), detect(five, listOf(line(PLACEHOLDER), line("d"), line("e"))))
    }
    @Test fun `duplicate texts resolved by order`() {
        val stored = listOf(message(1, "ok", 1000), message(2, "ok", 2000))
        assertEquals(listOf(2L), detect(stored, listOf(line("ok"), line(PLACEHOLDER))))
        assertEquals(listOf(1L), detect(stored, listOf(line(PLACEHOLDER), line("ok"))))
        assertEquals(listOf(2L), detect(stored, listOf(line("ok", 1000), line(PLACEHOLDER, 2000))))
        assertEquals(listOf("ok" to null), fresh(listOf(message(1, "ok", 1000)), listOf(line("ok"), line("ok"))))
    }
    @Test fun `deleted then re-sent with the same text`() {
        val stored = listOf(message(1, "x", 1000))
        val incoming = listOf(line(PLACEHOLDER, 1000), line("x", 9000))
        assertEquals(listOf(1L), detect(stored, incoming))
        assertEquals(listOf("x" to 9000L), fresh(stored, incoming))
    }
    @Test fun `stored order does not matter`() {
        val stored = listOf(message(3, "c", 3000), message(1, "a", 1000), message(2, "b", 2000))
        assertEquals(listOf(2L), detect(stored, listOf(line("a"), line(PLACEHOLDER), line("c"))))
        assertEquals(listOf(2L), detect(stored, listOf(line("a", 1000), line(PLACEHOLDER, 2000), line("c", 3000))))
    }
    @Test fun `listing order that disagrees with timestamps`() {
        val stored = listOf(message(1, "a", 1000), message(2, "b", 1200))
        assertTrue(fresh(stored, listOf(line("b", 1200), line("a", 1000))).isEmpty())
        assertTrue(detect(stored, listOf(line("b", 1200), line("a", 1000))).isEmpty())
        assertEquals(listOf(2L), detect(stored, listOf(line(PLACEHOLDER, 1200), line("a", 1000))))
        // Results keep the caller's line order.
        assertEquals(listOf("z" to 5000L, "y" to 4000L), fresh(stored, listOf(line("z", 5000), line("a", 1000), line("y", 4000))))
    }
    @Test fun `empty inputs tolerance boundary and trimmed text`() {
        assertTrue(detect(emptyList(), emptyList()).isEmpty())
        assertTrue(detect(emptyList(), listOf(line(PLACEHOLDER, 1000))).isEmpty())
        assertTrue(fresh(listOf(message(1, "a", 1000)), emptyList()).isEmpty())
        assertTrue(detect(listOf(message(1, "a", 1000)), listOf(line(PLACEHOLDER, 3001))).isEmpty())
        assertEquals(listOf(1L), detect(listOf(message(1, "a", 1000)), listOf(line(PLACEHOLDER, 3000))))
        assertEquals(listOf(" padded " to 1000L), fresh(emptyList(), listOf(line(" padded ", 1000))))
        assertTrue(fresh(listOf(message(1, "padded", 1000)), listOf(line(" padded ", 1000))).isEmpty())
        assertTrue(fresh(listOf(message(1, "a", 1000, deletedAt = 1200)), listOf(line("a", 1000))).isEmpty())
        // A stored list holding the same id twice still reports it once.
        assertEquals(listOf(1L), detect(listOf(message(1, "a", 1000), message(1, "a", 1000)), listOf(line(PLACEHOLDER, 1000), line(PLACEHOLDER, 1000))))
    }
    @Test(timeout = 10_000) fun `a long conversation is aligned quickly`() {
        // Untimestamped lines with a placeholder can pair with every stored message: the largest table.
        val stored = (1L..5000L).map { message(it, "message $it", it * 60_000) }
        val incoming = (4976L..5000L).map { line(if (it == 4990L) PLACEHOLDER else "message $it") }
        assertEquals(listOf(4990L), detect(stored, incoming))
    }
}
