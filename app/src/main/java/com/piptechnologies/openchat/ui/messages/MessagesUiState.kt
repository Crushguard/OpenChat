package com.piptechnologies.openchat.ui.messages

import com.piptechnologies.openchat.core.messages.ConversationSummary
import com.piptechnologies.openchat.core.messages.InboxMode
import com.piptechnologies.openchat.data.db.ConversationRef
import com.piptechnologies.openchat.data.repo.ExcludedChat

/** A conversation in the Exclude chats sheet (§4.11, ruling R15); the listener skips [excluded] ones. */
data class ExcludableChat(val key: String, val title: String, val excluded: Boolean)

/**
 * What the Messages screen shows (design map §4.8, ruling R5): the [mode] filter, its [conversations]
 * (newest first), the chip totals, whether recovery is [paused], the excluded chat count, and which of
 * the tool settings, exclude chats and clear-all sheets is up. [nowMs] anchors the row times.
 */
data class MessagesUiState(
    val mode: InboxMode,
    val conversations: List<ConversationSummary>,
    val unreadTotal: Int,
    val deletedTotal: Int,
    val paused: Boolean,
    val excludedCount: Int,
    val excludable: List<ExcludableChat>,
    val toolSettingsOpen: Boolean,
    val excludeOpen: Boolean,
    val confirmClear: Boolean,
    val nowMs: Long,
)

/** Everything the Messages screen and its sheets can ask for; screenshots use the no-op defaults. */
class MessagesCallbacks(
    val onBack: () -> Unit = {},
    val onMode: (InboxMode) -> Unit = {},
    val onOpenToolSettings: () -> Unit = {},
    val onDismissToolSettings: () -> Unit = {},
    val onTogglePause: () -> Unit = {},
    val onOpenExclude: () -> Unit = {},
    val onDismissExclude: () -> Unit = {},
    val onToggleExclude: (ExcludableChat) -> Unit = {},
    val onAskClear: () -> Unit = {},
    val onDismissClear: () -> Unit = {},
    val onConfirmClear: () -> Unit = {},
    val onOpenConversation: (ConversationSummary) -> Unit = {},
)

/**
 * The Exclude chats list: every conversation with stored messages (one row per key, since the query can
 * repeat a key for titles that differ only in case), plus chats excluded earlier whose messages are gone,
 * so they can still be let back in. Sorted by title, ignoring case.
 */
internal fun excludableChats(refs: List<ConversationRef>, excluded: List<ExcludedChat>): List<ExcludableChat> {
    val excludedKeys = excluded.mapTo(HashSet()) { it.conversationKey }
    val known = refs.distinctBy { it.conversationKey }
        .map { ExcludableChat(key = it.conversationKey, title = it.conversationTitle, excluded = it.conversationKey in excludedKeys) }
    val knownKeys = known.mapTo(HashSet()) { it.key }
    val gone = excluded.filter { it.conversationKey !in knownKeys }
        .map { ExcludableChat(key = it.conversationKey, title = it.title, excluded = true) }
    return (known + gone).sortedWith(compareBy<ExcludableChat, String>(String.CASE_INSENSITIVE_ORDER) { it.title }.thenBy { it.key })
}
