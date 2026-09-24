package com.piptechnologies.openchat.core.media

/** An original WhatsApp media file currently present on disk. */
data class OriginalFile(val path: String, val displayName: String, val sizeBytes: Long, val modifiedAt: Long)

/** A previously copied media row known to the app. */
data class KnownCopy(val id: Long, val originalPath: String, val deletedAt: Long?, val capturedAt: Long)

data class ReconcilePlan(val toCopy: List<OriginalFile>, val toMarkDeleted: List<Long>, val toPrune: List<Long>)

/** Diffs the files WhatsApp currently has on disk against the copies the app already knows about. */
object MediaReconciler {
    const val KEEP_UNDELETED_MS: Long = 14L * 24 * 60 * 60 * 1000

    /**
     * toCopy = present files with no known copy (deduplicated by path); toMarkDeleted = known
     * copies (deletedAt == null) whose original is absent; toPrune = known undeleted copies older
     * than [keepUndeletedForMs] whose original is still present.
     */
    fun plan(known: List<KnownCopy>, present: List<OriginalFile>, nowMs: Long, keepUndeletedForMs: Long = KEEP_UNDELETED_MS): ReconcilePlan {
        val knownPaths = known.map { it.originalPath }.toSet()
        val presentPaths = present.map { it.path }.toSet()

        val toCopy = present.distinctBy { it.path }.filter { it.path !in knownPaths }

        val toMarkDeleted = known
            .filter { it.deletedAt == null && it.originalPath !in presentPaths }
            .map { it.id }

        val toPrune = known
            .filter { it.deletedAt == null && it.originalPath in presentPaths && nowMs - it.capturedAt > keepUndeletedForMs }
            .map { it.id }

        return ReconcilePlan(toCopy, toMarkDeleted, toPrune)
    }
}
