package com.piptechnologies.openchat.ui.media

import com.piptechnologies.openchat.core.media.MediaCategory
import com.piptechnologies.openchat.core.media.MediaDayGrouper
import com.piptechnologies.openchat.core.media.RecoveredMedia
import com.piptechnologies.openchat.ui.messages.ExcludableChat

/**
 * What the Deleted media screen shows (design map §4.12): the selected [tab] and its recovered items in
 * day [groups] (newest day and item first), whether the storage/media permission is granted (ruling R17:
 * without it the grid offers "Allow media access"), the tool settings values ([paused], [excludedCount]),
 * which sheet is open, the time the day labels were computed for ([nowMs]), and the chats of the Exclude
 * chats sheet ([excludable], shared with Messages, §4.11).
 */
data class MediaUiState(
    val tab: MediaCategory,
    val groups: List<MediaDayGrouper.Group>,
    val hasPermission: Boolean,
    val paused: Boolean,
    val excludedCount: Int,
    val toolSettingsOpen: Boolean,
    val confirmClear: Boolean,
    val nowMs: Long,
    val excludeOpen: Boolean = false,
    val excludable: List<ExcludableChat> = emptyList(),
)

/**
 * Everything the Deleted media screen and its sheets report back: tool settings (§4.10), whose "Exclude
 * chats" row is [onExclude], exclude chats (§4.11) and the clear-all confirmation. Each callback defaults
 * to a no-op, so screenshots pass `MediaCallbacks()`.
 */
class MediaCallbacks(
    val onBack: () -> Unit = {},
    val onTab: (MediaCategory) -> Unit = {},
    val onOpen: (RecoveredMedia) -> Unit = {},
    val onRequestPermission: () -> Unit = {},
    val onOpenToolSettings: () -> Unit = {},
    val onDismissToolSettings: () -> Unit = {},
    val onTogglePause: () -> Unit = {},
    val onExclude: () -> Unit = {},
    val onDismissExclude: () -> Unit = {},
    val onToggleExclude: (ExcludableChat) -> Unit = {},
    val onAskClear: () -> Unit = {},
    val onDismissClear: () -> Unit = {},
    val onConfirmClear: () -> Unit = {},
)
