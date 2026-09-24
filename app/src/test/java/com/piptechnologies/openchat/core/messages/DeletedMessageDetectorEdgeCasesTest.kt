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
    @Test fun `a burst that scrolls past the notification keeps the newest photo and marks the right one`() {
        val stored = (1L..8L).map { message(it, PHOTO, 1000 + 300 * it) }
        val shown = listOf(line(PHOTO, 1900), line(PHOTO, 2200), line(PLACEHOLDER, 2500), line(PHOTO, 2800),
            line(PHOTO, 3100), line(PHOTO, 3400), line(PHOTO, 3700))       // m1-m2 scrolled out, m5 deleted, m9 new
        assertEquals(listOf(5L), detect(stored, shown))
        assertEquals(listOf(PHOTO to 3700L), fresh(stored, shown))
        assertEquals(listOf(PHOTO to 3400L), fresh(stored.take(7), (2L..8L).map { line(PHOTO, 1000 + 300 * it) }))
    }
    @Test fun `placeholders at their own timestamps anchor a burst`() {
        // Both photos were deleted; a third arriving 1 s later must stay new, not be taken for one of them.
        val bothDeleted = listOf(message(1, PHOTO, 2000, deletedAt = 2100), message(2, PHOTO, 3000, deletedAt = 3100))
        val shown = listOf(line(PLACEHOLDER, 2000), line(PLACEHOLDER, 3000), line(PHOTO, 4000))
        assertTrue(detect(bothDeleted, shown).isEmpty())
        assertEquals(listOf(PHOTO to 4000L), fresh(bothDeleted, shown))
        // The oldest photo scrolled out: the placeholder keeps its own photo, the scrolled-out one is not marked.
        val scrolled = listOf(message(1, PHOTO, 1000), message(2, PHOTO, 1300, deletedAt = 1400))
        val window = listOf(line(PLACEHOLDER, 1300), line(PHOTO, 1600))
        assertTrue(detect(scrolled, window).isEmpty())
        assertEquals(listOf(PHOTO to 1600L), fresh(scrolled, window))
    }
    @Test fun `a text match outweighs a placeholder pairing`() {
        val stored = listOf(message(1, "x", 1000))
        val incoming = listOf(line(PLACEHOLDER), line("x"))
        assertTrue(detect(stored, incoming).isEmpty())
        assertTrue(fresh(stored, incoming).isEmpty())
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
        // Far more than the 25 messages the listener passes; untimestamped lines with a placeholder pair with every one: the largest table.
        val stored = (1L..5000L).map { message(it, "message $it", it * 60_000) }
        val incoming = (4976L..5000L).map { line(if (it == 4990L) PLACEHOLDER else "message $it") }
        assertEquals(listOf(4990L), detect(stored, incoming))
    }
}
