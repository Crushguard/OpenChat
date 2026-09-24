package com.piptechnologies.openchat.ui.messages

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.core.messages.ConversationSummary
import com.piptechnologies.openchat.core.messages.InboxBuilder
import com.piptechnologies.openchat.core.messages.InboxMode
import com.piptechnologies.openchat.data.prefs.SettingsRepository
import com.piptechnologies.openchat.data.repo.MessagesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The Messages inbox (design map §4.8, ruling R5) with its tool settings (§4.10), exclude chats (§4.11)
 * and clear-all confirmation (§4.20). One ViewModel serves both entries: the route sets the [InboxMode]
 * the user came in with through [setMode], and the chips switch it.
 *
 * Takes the application context (for toast texts) like the other toast-emitting ViewModels; it needs no
 * MediaRepository, since [MessagesRepository.clearAll] already deletes the notification image copies.
 */
@HiltViewModel
class MessagesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val messages: MessagesRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val mode = MutableStateFlow(InboxMode.ALL)
    private val sheets = MutableStateFlow(Sheets())

    /** Serializes the pause and exclude toggles, so a double tap flips twice instead of racing. */
    private val toggleLock = Mutex()
    private val _toasts = MutableSharedFlow<String>(extraBufferCapacity = 1)

    /** Toast texts: "Recovery paused" / "Recovery resumed" and "Cleared". */
    val toasts: SharedFlow<String> = _toasts.asSharedFlow()

    /** The grouped inbox for the current mode, built off the main thread. */
    private val inbox: Flow<Inbox> = combine(messages.observeAll(), mode) { all, m ->
        Inbox(
            mode = m,
            conversations = InboxBuilder.build(all, m),
            unreadTotal = all.count { !it.seenLocally },
            deletedTotal = all.count { it.isDeleted },
        )
    }.flowOn(Dispatchers.Default)

    private val exclusions: Flow<Exclusions> =
        combine(messages.observeConversationRefs(), messages.observeExcluded()) { refs, excluded ->
            Exclusions(chats = excludableChats(refs, excluded), count = excluded.size)
        }

    val state: StateFlow<MessagesUiState> = combine(inbox, exclusions, settings.recoveryPaused, sheets) { box, excluded, paused, open ->
        MessagesUiState(
            mode = box.mode,
            conversations = box.conversations,
            unreadTotal = box.unreadTotal,
            deletedTotal = box.deletedTotal,
            paused = paused,
            excludedCount = excluded.count,
            excludable = excluded.chats,
            toolSettingsOpen = open.toolSettingsOpen,
            excludeOpen = open.excludeOpen,
            confirmClear = open.confirmClear,
            nowMs = System.currentTimeMillis(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MessagesUiState(
            mode = InboxMode.ALL,
            conversations = emptyList(),
            unreadTotal = 0,
            deletedTotal = 0,
            paused = false,
            excludedCount = 0,
            excludable = emptyList(),
            toolSettingsOpen = false,
            excludeOpen = false,
            confirmClear = false,
            nowMs = System.currentTimeMillis(),
        ),
    )

    fun setMode(m: InboxMode) {
        mode.value = m
    }

    fun openToolSettings() {
        sheets.update { it.copy(toolSettingsOpen = true) }
    }

    fun dismissToolSettings() {
        sheets.update { it.copy(toolSettingsOpen = false) }
    }

    /** Flips `recovery_paused` (§5.2: the listener ignores everything while paused) and says so in a toast. */
    fun togglePause() {
        viewModelScope.launch {
            toggleLock.withLock {
                val paused = !settings.recoveryPaused.first()
                try {
                    settings.setRecoveryPaused(paused)
                } catch (e: IOException) {
                    return@withLock // Not saved: the switch keeps showing the stored value.
                }
                toast(if (paused) R.string.toast_paused else R.string.toast_resumed)
            }
        }
    }

    /** The exclude sheet replaces the tool settings sheet. */
    fun openExclude() {
        sheets.update { it.copy(toolSettingsOpen = false, excludeOpen = true) }
    }

    fun dismissExclude() {
        sheets.update { it.copy(excludeOpen = false) }
    }

    /** Excludes or lets back in [c] (from its stored state); the listener checks the list for every notification (ruling R15). */
    fun toggleExclude(c: ExcludableChat) {
        viewModelScope.launch {
            toggleLock.withLock { messages.setExcluded(c.key, c.title, !messages.isExcluded(c.key)) }
        }
    }

    /** The confirmation replaces the tool settings sheet. */
    fun askClear() {
        sheets.update { it.copy(toolSettingsOpen = false, confirmClear = true) }
    }

    fun dismissClear() {
        sheets.update { it.copy(confirmClear = false) }
    }

    /** Deletes every kept message and notification image copy (§5.2), once per confirmation. */
    fun confirmClear() {
        val before = sheets.getAndUpdate { it.copy(toolSettingsOpen = false, confirmClear = false) }
        if (!before.confirmClear) return
        viewModelScope.launch {
            messages.clearAll()
            toast(R.string.toast_cleared)
        }
    }

    private suspend fun toast(@StringRes text: Int) {
        _toasts.emit(context.getString(text))
    }

    private data class Inbox(val mode: InboxMode, val conversations: List<ConversationSummary>, val unreadTotal: Int, val deletedTotal: Int)

    private data class Exclusions(val chats: List<ExcludableChat>, val count: Int)

    private data class Sheets(val toolSettingsOpen: Boolean = false, val excludeOpen: Boolean = false, val confirmClear: Boolean = false)
}
