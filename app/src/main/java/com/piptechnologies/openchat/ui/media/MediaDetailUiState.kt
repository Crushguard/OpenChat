package com.piptechnologies.openchat.ui.media

import com.piptechnologies.openchat.core.media.RecoveredMedia

/**
 * What the media detail shows (design map §4.13): the [item] (null until it is loaded, or when there is no
 * such copy), its position "[index] / [total]" among the recovered items of its category, newest first
 * (0 / 0 hides the counter: a copy that was not deleted), [nowMs], the "now" the screen writes the meta line
 * against in the UI language ("From Ayu Lestari · Today 14:25 · Deleted 14:26"), and whether the delete
 * confirmation is open.
 */
data class MediaDetailUiState(
    val item: RecoveredMedia?,
    val index: Int,
    val total: Int,
    val nowMs: Long,
    val confirmDelete: Boolean,
)
