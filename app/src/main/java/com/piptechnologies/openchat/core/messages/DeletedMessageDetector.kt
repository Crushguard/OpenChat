package com.piptechnologies.openchat.core.messages

import kotlin.math.abs

/**
 * Compares the lines of a conversation's latest notification with the messages already stored for that
 * conversation: which stored messages does the notification now show as deleted, and which lines are new?
 *
 * The lines (by time when they all carry one, else in notification order, oldest first) are aligned in order
 * with the stored messages (by timestamp, then id), each line pairing with at most one message and each
 * message with at most one line:
 * - a regular line pairs with a message of the same trimmed text;
 * - a deleted-pattern line ([NotificationText.isDeletedPattern]) pairs with the message it replaced, any
 *   text; an already deleted message takes its own placeholder back without being reported again;
 * - a line with a timestamp only pairs with a message whose timestamp is within the tolerance.
 * The alignment keeps as many regular pairs as possible, then as many deleted pairs, then the smallest total
 * time difference, and gives any remaining tie to the latest messages. So a timestamped placeholder takes the
 * closest message, and an untimestamped one the message at the same position from the end among those the
 * notification no longer shows verbatim.
 */
object DeletedMessageDetector {
    /** Ids of [stored] (not yet deleted) messages that the latest [incoming] lines show as deleted. Timestamped deleted lines match by |Δt| ≤ [toleranceMs]; untimestamped ones match the stored message at the same position from the end that is no longer present verbatim. */
    fun detect(stored: List<CapturedMessage>, incoming: List<NotificationLine>, toleranceMs: Long = 2_000): List<Long> {
        val partners = align(stored, incoming, toleranceMs)
        return incoming.indices
            .filter { NotificationText.isDeletedPattern(incoming[it].text) }
            .mapNotNull { partners[it] }
            .filterNot { it.isDeleted }
            .map { it.id }
            .distinct()
    }

    /** Incoming lines that are neither deleted-pattern nor already stored (same trimmed text and, when both have timestamps, |Δt| ≤ tolerance). A stored message accounts for one line only, so a burst of identical lines ("📷 Photo" three times) stays three messages. */
    fun newLines(stored: List<CapturedMessage>, incoming: List<NotificationLine>, toleranceMs: Long = 2_000): List<NotificationLine> {
        val partners = align(stored, incoming, toleranceMs)
        return incoming.filterIndexed { i, line -> partners[i] == null && !NotificationText.isDeletedPattern(line.text) }
    }

    /** Quality of a partial alignment: more weight wins, then less drift (summed |Δt| of timestamped pairs). */
    private data class Fit(val weight: Long, val drift: Long) {
        fun beats(other: Fit) = weight > other.weight || (weight == other.weight && drift < other.drift)
        operator fun plus(other: Fit) = Fit(weight + other.weight, drift + other.drift)
    }

    /** For each line of [incoming], the stored message it pairs with, or null. */
    private fun align(stored: List<CapturedMessage>, incoming: List<NotificationLine>, toleranceMs: Long): Array<CapturedMessage?> {
        // Lines that all carry a time are aligned in time order (a stable sort: equal times keep notification order).
        val indexed = incoming.withIndex().toList()
        val ordered = if (incoming.all { it.timestamp != null }) indexed.sortedBy { it.value.timestamp } else indexed
        val lines = ordered.map { it.value }
        val deleted = BooleanArray(lines.size) { NotificationText.isDeletedPattern(lines[it].text) }
        val texts = lines.map { it.text.trim() }
        val regularWeight = lines.size + 1L // one regular pair outweighs every possible deleted pair
        fun pairFit(i: Int, message: CapturedMessage): Fit? {
            val drift = lines[i].timestamp?.let { abs(it - message.timestamp) } ?: 0L
            return when {
                drift > toleranceMs -> null
                deleted[i] -> Fit(1, drift)
                texts[i] == message.text.trim() -> Fit(regularWeight, drift)
                else -> null
            }
        }
        // A message no line can pair with never changes the result; leaving it out keeps the table small.
        val messages = stored.sortedWith(compareBy({ it.timestamp }, { it.id }))
            .filter { message -> lines.indices.any { pairFit(it, message) != null } }

        // best[i][j]: the best alignment of lines[i..] with messages[j..].
        val none = Fit(0, 0)
        val best = Array(lines.size + 1) { Array(messages.size + 1) { none } }
        for (i in lines.indices.reversed()) {
            for (j in messages.indices.reversed()) {
                var fit = best[i + 1][j] // line i stays unpaired
                if (best[i][j + 1].beats(fit)) fit = best[i][j + 1] // message j stays unpaired
                val paired = pairFit(i, messages[j])?.plus(best[i + 1][j + 1])
                if (paired != null && paired.beats(fit)) fit = paired
                best[i][j] = fit
            }
        }

        // Walk an optimal alignment, leaving a message unpaired whenever that costs nothing: pairs land on the latest messages.
        val partners = arrayOfNulls<CapturedMessage>(lines.size)
        var i = 0
        var j = 0
        while (i < lines.size && j < messages.size) {
            when {
                best[i][j + 1] == best[i][j] -> j++
                pairFit(i, messages[j])?.plus(best[i + 1][j + 1]) == best[i][j] -> {
                    partners[ordered[i].index] = messages[j]
                    i++
                    j++
                }
                else -> i++
            }
        }
        return partners
    }
}
